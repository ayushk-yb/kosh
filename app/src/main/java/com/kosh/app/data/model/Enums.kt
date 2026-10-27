package com.kosh.app.data.model

enum class TransactionCategory(val displayName: String, val icon: String) {
    RENT("Rent & Maintenance", "🏠"),
    COOK_MAID("Cook / Maid", "🍳"),
    CAB_COMMUTE("Cab / Auto", "🚕"),
    GIFTS("Gifts & Celebrations", "🎁"),
    PARTNER_FAMILY("Partner & Family", "👨‍👩‍👧"),
    SIP_WEALTH("SIP / Wealth", "📈"),
    GROCERIES("Groceries & Food", "🛒"),
    UTILITIES("Utilities & Bills", "⚡"),
    SHOPPING("Shopping & Lifestyle", "🛍️"),
    OTHER("Other / Uncategorized", "💳")
}

enum class InstrumentType {
    CREDIT_CARD,
    BANK_ACCOUNT,
    UPI,
    CASH
}

enum class RelationshipType {
    FLATMATE,
    PARTNER,
    FAMILY,
    FRIEND,
    MERCHANT,
    OTHER
}

enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String
) {
    INR("INR", "₹", "₹ INR (Indian Rupee)"),
    USD("USD", "$", "$ USD (US Dollar)"),
    EUR("EUR", "€", "€ EUR (Euro)"),
    GBP("GBP", "£", "£ GBP (British Pound)"),
    AED("AED", "AED ", "AED (UAE Dirham)"),
    CAD("CAD", "CA$", "CA$ CAD (Canadian Dollar)"),
    AUD("AUD", "AU$", "AU$ AUD (Australian Dollar)"),
    SGD("SGD", "SG$", "SG$ SGD (Singapore Dollar)")
}
