package com.app.railnav.data
object SearchUtils {
    /**
     * Simple Levenshtein distance to check how many changes 
     * are needed to turn string A into string B.
     */
    fun fuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return target.contains(query, ignoreCase = true)
        
        val maxErrors = 2
        val dp = Array(query.length + 1) { IntArray(target.length + 1) }

        for (i in 0..query.length) dp[i][0] = i
        for (j in 0..target.length) dp[0][j] = j

        for (i in 1..query.length) {
            for (j in 1..target.length) {
                val cost = if (query[i - 1].lowercaseChar() == target[j - 1].lowercaseChar()) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[query.length][target.length] <= maxErrors
    }
}