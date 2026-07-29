package com.sylphx.luau

/**
 * The LeakD deobfuscation endpoints (moonsec/prometheus/ironbrew2/ironveil)
 * prepend a fixed watermark comment as the first line of the returned code
 * (e.g. "-- Deobfuscated by LeakD..."). Since the API isn't ours to edit,
 * we replace that first line client-side, and as a safety net also scrub
 * any other stray "LeakD" mentions that might appear anywhere in output
 * from any endpoint (obfuscate/beautify included), so all credit shown to
 * the user reads as SylphX Luau.
 */
object WatermarkUtil {

    private const val NEW_WATERMARK = "-- Deobfuscator By SylphX Luau"
    private const val NEW_OBFUSCATE_WATERMARK = "-- Obfuscated By SylphX Luau"
    private const val NEW_BEAUTIFY_WATERMARK = "-- Beautified By SylphX Luau"

    /** Used for the 4 deobfuscation engine endpoints: replace the whole first line. */
    fun replaceFirstLineWatermark(code: String): String {
        if (code.isBlank()) return code
        val lines = code.lines().toMutableList()
        if (lines.isNotEmpty()) {
            lines[0] = NEW_WATERMARK
        }
        return scrubStrayCredits(lines.joinToString("\n"))
    }

    /** Used for obfuscate/beautify: prepend our own credit line and scrub any stray mentions. */
    fun withObfuscateCredit(code: String): String {
        if (code.isBlank()) return code
        return scrubStrayCredits("$NEW_OBFUSCATE_WATERMARK\n$code")
    }

    fun withBeautifyCredit(code: String): String {
        if (code.isBlank()) return code
        return scrubStrayCredits("$NEW_BEAUTIFY_WATERMARK\n$code")
    }

    /** Safety net: replace any remaining case-insensitive "LeakD" mention anywhere in the text. */
    private fun scrubStrayCredits(code: String): String {
        return Regex("leakd", RegexOption.IGNORE_CASE).replace(code, "SylphX Luau")
    }
}
