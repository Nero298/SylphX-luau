package com.sylphx.luau

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the LeakD API (https://leakd.up.railway.app).
 * Endpoint contracts and JSON shapes as given directly by the API owner.
 */
object LeakDApi {

    private const val BASE_URL = "https://leakd.up.railway.app"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ApiResult(
        val success: Boolean,
        val message: String,       // resulting code OR error text
        val extra: String? = null, // e.g. detect confidence, file size info
        val presets: List<String>? = null // populated on "invalid preset" errors
    )

    enum class Endpoint(val path: String) {
        DETECT("/detect"),
        MOONSEC("/moonsec"),
        PROMETHEUS("/prometheus"),
        IRONBREW2("/ironbrew2"),
        IRONVEIL("/ironveil"),
        BEAUTIFY("/beautify"),
        OBFUSCATE("/obfuscate")
    }

    /**
     * Sends the given Lua/text source as a multipart file (LeakD expects
     * `file=@script.lua` for every endpoint). Works uniformly whether the
     * text came from a pasted textbox or was read from a picked file
     * (.lua, .txt, or otherwise).
     */
    suspend fun sendFile(
        endpoint: Endpoint,
        code: String,
        fileName: String = "script.lua",
        preset: String? = null
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder(BASE_URL + endpoint.path)
            if (preset != null && endpoint == Endpoint.OBFUSCATE) {
                urlBuilder.append("?preset=").append(preset)
            }

            val fileBody = code.toRequestBody("text/plain".toMediaTypeOrNull())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build()

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .post(multipart)
                .build()

            client.newCall(request).execute().use { response ->
                parseResponse(response.body?.string().orEmpty(), endpoint)
            }
        } catch (e: Exception) {
            ApiResult(success = false, message = "Connection error: ${e.message}")
        }
    }

    private fun parseResponse(raw: String, endpoint: Endpoint): ApiResult {
        if (raw.isBlank()) {
            return ApiResult(success = false, message = "Server returned an empty response.")
        }
        return try {
            val obj = JSONObject(raw)
            val success = obj.optBoolean("success", false)

            if (!success) {
                val err = obj.optString("error", "Unknown error")
                val presetsArr = obj.optJSONArray("presets")
                val presetsList = presetsArr?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                }
                return ApiResult(success = false, message = err, presets = presetsList)
            }

            when (endpoint) {
                Endpoint.DETECT -> {
                    val top = obj.optJSONObject("top_result")
                    val name = top?.optString("name") ?: "Unknown"
                    val confidence = top?.optInt("confidence") ?: 0
                    ApiResult(
                        success = true,
                        message = name,
                        extra = "Confidence: $confidence%"
                    )
                }
                Endpoint.BEAUTIFY -> {
                    val fileInfo = obj.optJSONObject("file")
                    val ratio = fileInfo?.optDouble("ratio")
                    ApiResult(
                        success = true,
                        message = obj.optString("beautified_code", ""),
                        extra = ratio?.let { "Ratio: ${"%.1f".format(it)}%" }
                    )
                }
                Endpoint.OBFUSCATE -> {
                    val fileInfo = obj.optJSONObject("file")
                    val ratio = fileInfo?.optDouble("ratio")
                    val presetUsed = obj.optString("preset", "")
                    ApiResult(
                        success = true,
                        message = obj.optString("obfuscated_code", ""),
                        extra = buildString {
                            append("Preset: $presetUsed")
                            ratio?.let { append(" • Ratio: ${"%.1f".format(it)}%") }
                        }
                    )
                }
                else -> {
                    // moonsec / prometheus / ironbrew2 / ironveil deobfuscation endpoints
                    val fileInfo = obj.optJSONObject("file")
                    val outSize = fileInfo?.optDouble("output_size_kb")
                    ApiResult(
                        success = true,
                        message = obj.optString("deobfuscated_code", ""),
                        extra = outSize?.let { "Output: ${"%.2f".format(it)} KB" }
                    )
                }
            }
        } catch (e: Exception) {
            ApiResult(success = false, message = "Failed to parse response: ${e.message}")
        }
    }
}
