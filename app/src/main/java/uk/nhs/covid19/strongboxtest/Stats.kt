package uk.nhs.covid19.strongboxtest

import android.content.Context

object Stats {
    fun getAttempts(context: Context): Int {
        return getPrefs(context).getInt("attempts", 0)
    }

    fun getSuccesses(context: Context): Int {
        return getPrefs(context).getInt("successes", 0)
    }

    fun incrementAttempts(context: Context) {
        incrementValue(context, "attempts")
    }

    fun incrementSuccesses(context: Context) {
        incrementValue(context, "successes")
    }

    private fun incrementValue(context: Context, name: String) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(name, prefs.getInt(name, 0).inc()).apply()
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun reset(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().remove("attempts").remove("successes").apply()
    }
}
