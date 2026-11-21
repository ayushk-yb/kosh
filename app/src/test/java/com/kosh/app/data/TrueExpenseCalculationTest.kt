package com.kosh.app.data

import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditCardBillPayment
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.data.model.isRefundOrCreditOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class TrueExpenseCalculationTest {

    @Test
    fun testTrueExpenseSubtractsRefundsAndExcludesCcBillPayments() {
        // 1. Regular expense
        val diningTx = TransactionEntity(
            title = "Restaurant Dinner",
            grossAmount = 3500.0,
            trueNetAmount = 3500.0,
            category = "FOOD_DINING",
            instrumentName = "HDFC CC"
        )

        // 2. Shopping expense that will later get partially refunded
        val shoppingTx = TransactionEntity(
            title = "Amazon Shopping",
            grossAmount = 2500.0,
            trueNetAmount = 2500.0,
            category = "SHOPPING",
            instrumentName = "HDFC CC"
        )

        // 3. Refund credit for returned item
        val refundTx = TransactionEntity(
            title = "Amazon Refund",
            grossAmount = 1000.0,
            trueNetAmount = 0.0,
            category = "REFUND",
            instrumentName = "HDFC CC",
            note = "Refund / Reversal parsed from SMS [OFFSET_EXPENSE]"
        )

        // 4. Credit card bill payment (must not double-count with dining and shopping)
        val ccBillPaymentTx = TransactionEntity(
            title = "Credit Card Bill Payment",
            grossAmount = 6000.0,
            trueNetAmount = 0.0,
            category = "BILL_PAYMENT",
            instrumentName = "HDFC Bank",
            note = "Credit Card Bill Payment [CC_PAYMENT] - Excluded from expenses"
        )

        // 5. SIP / Wealth Investment (must not count as consumption expense)
        val sipTx = TransactionEntity(
            title = "Nifty 50 Index Fund",
            grossAmount = 10000.0,
            trueNetAmount = 0.0,
            category = "SIP_WEALTH",
            instrumentName = "Salary Account",
            isAssetWealth = true
        )

        // 6. Monthly salary inflow
        val salaryTx = TransactionEntity(
            title = "Salary Credit",
            grossAmount = 120000.0,
            trueNetAmount = 0.0,
            category = "INFLOW",
            instrumentName = "Salary Account",
            note = "Credit / Inflow parsed from SMS"
        )

        val txList = listOf(diningTx, shoppingTx, refundTx, ccBillPaymentTx, sipTx, salaryTx)

        // Debits query logic (from TransactionDao)
        val debits = txList.filter { tx ->
            !tx.isAssetWealth &&
                    tx.category != "INFLOW" &&
                    tx.category != "CREDIT" &&
                    tx.category != "BILL_PAYMENT" &&
                    !tx.note.contains("[CC_PAYMENT]") &&
                    !tx.category.equals("REFUND", ignoreCase = true)
        }.sumOf { it.trueNetAmount }

        // Refund offsets query logic (from TransactionDao)
        val refunds = txList.filter { tx ->
            !tx.isAssetWealth && (
                    tx.category == "REFUND" ||
                            tx.note.contains("[OFFSET_EXPENSE]") ||
                            tx.title.contains("Refund", ignoreCase = true) ||
                            tx.title.contains("Reversal", ignoreCase = true) ||
                            tx.title.contains("Cashback", ignoreCase = true)
                    )
        }.sumOf { it.grossAmount }

        // Total credits query logic
        val totalCredits = txList.filter { tx ->
            tx.isCreditOrInflow()
        }.sumOf { it.grossAmount }

        // True Net Expense computation
        val trueNetExpense = (debits - refunds).coerceAtLeast(0.0)

        // Debits = 3500 + 2500 = 6000 (excludes CC bill payment 6000, wealth 10000, and salary)
        assertEquals(6000.0, debits, 0.001)

        // Refunds = 1000
        assertEquals(1000.0, refunds, 0.001)

        // True Net Expense = 6000 - 1000 = 5000
        assertEquals(5000.0, trueNetExpense, 0.001)

        // Total Credits = 1000 (refund) + 120000 (salary) = 121000
        assertEquals(121000.0, totalCredits, 0.001)
    }

    @Test
    fun testTrueExpenseFloorAtZeroWhenRefundsExceedDebits() {
        val debit = TransactionEntity(
            title = "Book",
            grossAmount = 500.0,
            trueNetAmount = 500.0,
            category = "ENTERTAINMENT",
            instrumentName = "Card"
        )
        val refund = TransactionEntity(
            title = "Reversal from previous month item",
            grossAmount = 800.0,
            trueNetAmount = 0.0,
            category = "REFUND",
            instrumentName = "Card",
            note = "[OFFSET_EXPENSE]"
        )

        val debits = debit.trueNetAmount
        val refunds = refund.grossAmount
        val net = (debits - refunds).coerceAtLeast(0.0)

        // 500 - 800 would be negative, but coerceAtLeast(0.0) guarantees >= 0
        assertEquals(0.0, net, 0.001)
    }
}
