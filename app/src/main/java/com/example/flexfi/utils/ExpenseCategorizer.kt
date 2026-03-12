package com.example.flexfi.utils

object ExpenseCategorizer {

    private val keywordMapping = mapOf(
        "Food" to listOf("zomato", "swiggy", "pizza", "burger", "restaurant", "dinner", "lunch", "breakfast", "cafe", "coffee", "starbucks"),
        "Transport" to listOf("uber", "ola", "rapido", "petrol", "fuel", "gas", "taxi", "bus", "train", "flight"),
        "Shopping" to listOf("amazon", "flipkart", "mall", "store", "myntra", "clothes", "shoes", "grocery", "mart", "supermarket", "blinkit", "zepto", "instamart"),
        "Entertainment" to listOf("movie", "cinema", "netflix", "prime", "spotify", "concert", "game", "ticket", "bookmyshow"),
        "Utilities" to listOf("electricity", "water", "internet", "wifi", "bill", "recharge", "phone", "mobile", "broadband"),
        "Health" to listOf("doctor", "hospital", "pharmacy", "medicine", "clinic", "gym", "workout", "fitness")
    )

    /**
     * Tries to find a matching category based on the description/title words.
     * Returns the category string if found, otherwise null.
     */
    fun categorize(description: String): String? {
        if (description.isBlank()) return null
        
        val lowercaseDesc = description.lowercase()
        
        for ((category, keywords) in keywordMapping) {
            for (keyword in keywords) {
                // simple substring match: "uber ride" contains "uber"
                if (lowercaseDesc.contains(keyword)) {
                    return category
                }
            }
        }
        return null
    }
}
