package com.kosh.app.ui.screens

import com.kosh.app.data.model.PaymentInstrumentEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditCardBillPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CardCycleTest {

    @Test
    fun testCycleEndDayAutoCalculation() {
        // When start day is 15, end day should be 14
        val start15 = 15
        val calculatedEnd15 = if (start15 == 1) 31 else start15 - 1
        assertEquals(14, calculatedEnd15)

        // User case: When start day is changed from 15 to 19, end day must be 18 (not 14)
        val start19 = 19
        val calculatedEnd19 = if (start19 == 1) 31 else start19 - 1
        assertEquals(18, calculatedEnd19)

        // Boundary case: start day 1 -> end day 31
        val start1 = 1
        val calculatedEnd1 = if (start1 == 1) 31 else start1 - 1
        assertEquals(31, calculatedEnd1)
    }

    @Test
    fun testGetCardCycleDateRangeWhenTodayIsAfterCycleStart() {
        // Today is October 25, 2026. Cycle is 19th to 18th.
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMillis = cal.timeInMillis

        val (startMillis, endMillis) = getCardCycleDateRange(
            cycleStartDay = 19,
            cycleEndDay = 18,
            currentEpochMillis = currentMillis
        )

        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        // Start should be Oct 19, 2026 at 00:00:00
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        // End should be Nov 18, 2026 at 23:59:59
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(18, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertTrue(endMillis > startMillis)
    }

    @Test
    fun testGetCardCycleDateRangeWhenTodayIsBeforeCycleStart() {
        // Today is October 5, 2026. Cycle is 19th to 18th.
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMillis = cal.timeInMillis

        val (startMillis, endMillis) = getCardCycleDateRange(
            cycleStartDay = 19,
            cycleEndDay = 18,
            currentEpochMillis = currentMillis
        )

        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        // Start should be September 19, 2026
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))

        // End should be October 18, 2026
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, endCal.get(Calendar.MONTH))
        assertEquals(18, endCal.get(Calendar.DAY_OF_MONTH))
        assertTrue(currentMillis in startMillis..endMillis)
    }

    @Test
    fun testCardSpendCalculationExcludesBillPaymentsAndOtherCycles() {
        // Base time: Oct 25, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 12, 0, 0)
        }
        val currentMillis = cal.timeInMillis
        val (cycleStart, cycleEnd) = getCardCycleDateRange(19, 18, currentMillis)

        val card = PaymentInstrumentEntity(
            id = 1L,
            bankName = "HDFC",
            cardOrAccountName = "Regalia Gold",
            last4Digits = "4321",
            instrumentType = "CREDIT_CARD",
            creditLimit = 300000.0,
            cycleStartDay = 19,
            cycleEndDay = 18
        )

        // 1. Valid purchase inside current cycle (Oct 21)
        val txnInCycle = TransactionEntity(
            title = "Apple Store",
            grossAmount = 45000.0,
            trueNetAmount = 45000.0,
            category = "ELECTRONICS",
            instrumentName = "Regalia Gold (4321)",
            timestamp = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 21, 15, 0)
            }.timeInMillis
        )

        // 2. Bill payment inside current cycle (Oct 20) - MUST NOT BE COUNTED AS EXPENSE
        val billPaymentTxn = TransactionEntity(
            title = "HDFC Credit Card Bill",
            grossAmount = 40000.0,
            trueNetAmount = 0.0,
            category = "BILL_PAYMENT",
            instrumentName = "Regalia Gold (4321)",
            note = "Credit Card Bill Payment [CC_PAYMENT] - Excluded from expenses",
            timestamp = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 20, 10, 0)
            }.timeInMillis
        )

        // 3. Purchase in previous billing cycle (Oct 10) - outside current cycle [Oct 19 - Nov 18]
        val oldTxn = TransactionEntity(
            title = "Flight Ticket",
            grossAmount = 12000.0,
            trueNetAmount = 12000.0,
            category = "TRAVEL",
            instrumentName = "Regalia Gold (4321)",
            timestamp = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 10, 12, 0)
            }.timeInMillis
        )

        // 4. Purchase on a different card (SBI)
        val otherCardTxn = TransactionEntity(
            title = "Groceries",
            grossAmount = 5000.0,
            trueNetAmount = 5000.0,
            category = "GROCERY",
            instrumentName = "SBI SimplyClick (9999)",
            timestamp = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 22, 12, 0)
            }.timeInMillis
        )

        val allTxns = listOf(txnInCycle, billPaymentTxn, oldTxn, otherCardTxn)

        // Filter using the exact logic in CardsScreen
        val cardSpend = allTxns.filter { tx ->
            val matchesInstrument = tx.instrumentName.contains(card.cardOrAccountName, ignoreCase = true) ||
                    (!card.last4Digits.isNullOrBlank() && tx.instrumentName.contains(card.last4Digits))
            val inCycle = tx.timestamp in cycleStart..cycleEnd
            val notBillPayment = !tx.isCreditCardBillPayment() && tx.category != "BILL_PAYMENT"
            matchesInstrument && inCycle && notBillPayment
        }.sumOf { it.grossAmount }

        assertEquals(45000.0, cardSpend, 0.001)
    }
}
