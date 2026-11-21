package com.kosh.app.data.scanner

import com.kosh.app.data.model.IgnoreRuleEntity
import com.kosh.app.data.model.TransactionEntity
import com.kosh.app.data.model.isCreditCardBillPayment
import com.kosh.app.data.model.isCreditOrInflow
import com.kosh.app.data.model.isRefundOrCreditOffset
import com.kosh.app.data.model.isUpiTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAutoScannerTest {

    @Test
    fun testOtpMessagesAreRejected() {
        val otp1 = "OTP is 492019 for transaction of Rs. 1,552.00 at AMAZON PAY... Do not share."
        val otp2 = "123456 is your secret OTP for payment of Rs 350 to Zomato. Never share OTP with anyone."
        val otp3 = "Verification code for SBI card txn of Rs 500 is 987654. Valid for 10 mins."
        val otp4 = "Your secret code is 884129 for payment of INR 2000. Don't share."

        assertTrue("OTP 1 should be ignored", SmsAutoScanner.isIgnoredMessage(otp1))
        assertTrue("OTP 2 should be ignored", SmsAutoScanner.isIgnoredMessage(otp2))
        assertTrue("OTP 3 should be ignored", SmsAutoScanner.isIgnoredMessage(otp3))
        assertTrue("OTP 4 should be ignored", SmsAutoScanner.isIgnoredMessage(otp4))
    }

    @Test
    fun testPromoAndStatementMessagesAreRejected() {
        val promo1 = "Congratulations! You have pre-approved loan of Rs 5,00,000. Apply now."
        val promo2 = "Win up to 500 cashback points on your next UPI payment."
        val bill1 = "Your Credit Card statement generated. Total amount due is Rs 12,450. Due date is 05-Nov."
        val failed1 = "Your transaction of Rs 850 at Uber failed due to insufficient funds."

        assertTrue("Promo 1 should be ignored", SmsAutoScanner.isIgnoredMessage(promo1))
        assertTrue("Promo 2 should be ignored", SmsAutoScanner.isIgnoredMessage(promo2))
        assertTrue("Bill 1 should be ignored", SmsAutoScanner.isIgnoredMessage(bill1))
        assertTrue("Failed 1 should be ignored", SmsAutoScanner.isIgnoredMessage(failed1))
    }

    @Test
    fun testValidTransactionsAreNotRejected() {
        val validDebit = "Your A/C ending 4012 is debited for Rs 1552.00 on 26-Oct at AMAZON. Avail bal: Rs 45,210.00."
        val validCredit = "Rs 12000.00 credited to your A/C 9901 by Rahul on 26-Oct. UPI Ref 382910."
        val validUpi = "Paid Rs 350 to Swiggy using UPI on 26-Oct."

        assertFalse("Valid debit should pass filter", SmsAutoScanner.isIgnoredMessage(validDebit))
        assertFalse("Valid credit should pass filter", SmsAutoScanner.isIgnoredMessage(validCredit))
        assertFalse("Valid UPI should pass filter", SmsAutoScanner.isIgnoredMessage(validUpi))
    }

    @Test
    fun testHashIsDeterministic() {
        val timestamp = 1729950000123L
        val amount = 1552.00
        val title = "Amazon India Purchase"
        val instrument = "ICICI Bank"

        val hash1 = SmsAutoScanner.generateHash(timestamp, amount, title, instrument)
        val hash2 = SmsAutoScanner.generateHash(timestamp, amount, title, instrument)

        assertEquals("Hashes must be identical for identical inputs", hash1, hash2)
        assertTrue("Hash should not be empty", hash1.isNotEmpty())
    }

    // --- Custom Ignore / Exception Rules Tests ---

    @Test
    fun testCustomIgnoreRuleCaseInsensitiveSubstring() {
        val rule = IgnoreRuleEntity(pattern = "swiggy", isWholeWord = false, isCaseSensitive = false)
        val rules = listOf(rule)

        val sms1 = "Paid Rs 450 to SWIGGY Instamart via UPI"
        val sms2 = "Rs 500 debited for Zomato order"

        val matched = SmsAutoScanner.matchesCustomIgnoreRule(sms1, rules)
        assertNotNull("Should match Swiggy case-insensitively", matched)
        assertEquals("swiggy", matched?.pattern)

        val unmatched = SmsAutoScanner.matchesCustomIgnoreRule(sms2, rules)
        assertNull("Should not match Zomato", unmatched)

        val reason = SmsAutoScanner.getIgnoreReason(sms1, rules)
        assertNotNull("Should produce custom ignore reason", reason)
        assertTrue(reason!!.contains("Custom exception rule"))
    }

    @Test
    fun testCustomIgnoreRuleCaseSensitive() {
        val rule = IgnoreRuleEntity(pattern = "SALARY", isWholeWord = false, isCaseSensitive = true)
        val rules = listOf(rule)

        val upperSms = "Rs 80,000 credited towards SALARY for Oct"
        val lowerSms = "Rs 80,000 credited towards salary for Oct"

        assertNotNull("Uppercase SALARY should match", SmsAutoScanner.matchesCustomIgnoreRule(upperSms, rules))
        assertNull("Lowercase salary should not match case-sensitive rule", SmsAutoScanner.matchesCustomIgnoreRule(lowerSms, rules))
    }

    @Test
    fun testCustomIgnoreRuleWholeWord() {
        val rule = IgnoreRuleEntity(pattern = "car", isWholeWord = true, isCaseSensitive = false)
        val rules = listOf(rule)

        val wordSms = "Paid Rs 2000 for car wash via UPI"
        val partOfWordSms = "Rs 500 spent on your Card ending 1234"

        assertNotNull("Standalone 'car' should match whole-word rule", SmsAutoScanner.matchesCustomIgnoreRule(wordSms, rules))
        assertNull("'Card' should NOT match whole-word rule for 'car'", SmsAutoScanner.matchesCustomIgnoreRule(partOfWordSms, rules))
    }

    // --- UPI Detection Thumb Rule Tests ---

    @Test
    fun testUpiDetectionThumbRule() {
        val upiSms1 = "Paid Rs. 350.00 to Chai Point using UPI on 04-Nov-26"
        val upiSms2 = "Rs 1,200 debited from A/C 9876 via upi Ref 849201"
        val upiSms3 = "UPI-Mandate executed for Rs 500 to Netflix"
        val nonUpiSms = "Rs 2500 debited from A/C 1234 at ATM cash withdrawal"

        assertTrue("Should detect UPI when uppercase 'UPI' present", SmsAutoScanner.isUpi(upiSms1))
        assertTrue("Should detect UPI when lowercase 'upi' present", SmsAutoScanner.isUpi(upiSms2))
        assertTrue("Should detect UPI with hyphen prefix", SmsAutoScanner.isUpi(upiSms3))
        assertFalse("Should NOT classify ATM debit as UPI", SmsAutoScanner.isUpi(nonUpiSms))
    }

    @Test
    fun testTransactionEntityUpiHelper() {
        val txWithUpiInSms = TransactionEntity(
            title = "Swiggy",
            grossAmount = 300.0,
            trueNetAmount = 300.0,
            category = "FOOD_DINING",
            instrumentName = "HDFC Bank",
            originalSms = "Paid Rs 300 via UPI to Swiggy"
        )
        val txWithUpiInTitle = TransactionEntity(
            title = "UPI Payment to Rahul",
            grossAmount = 500.0,
            trueNetAmount = 500.0,
            category = "SERVICES",
            instrumentName = "SBI Bank"
        )
        val nonUpiTx = TransactionEntity(
            title = "Electricity Bill",
            grossAmount = 1500.0,
            trueNetAmount = 1500.0,
            category = "UTILITIES",
            instrumentName = "ICICI Bank",
            originalSms = "Netbanking txn of Rs 1500 for electricity bill"
        )

        assertTrue(txWithUpiInSms.isUpiTransaction())
        assertTrue(txWithUpiInTitle.isUpiTransaction())
        assertFalse(nonUpiTx.isUpiTransaction())
    }

    // --- Credit Card Bill Payment Detection Tests ---

    @Test
    fun testCreditCardBillPaymentDetection() {
        val ccBill1 = "Payment of Rs 14,500 towards your Credit Card ending 9876 received. Thank you."
        val ccBill2 = "Your CC bill payment of Rs 25,000 for Card XX4321 is successful."
        val ccBill3 = "Autopay debited Rs 18,200 for your SBI Credit Card"
        val regularCardSpend = "Rs 750 spent on your Credit Card ending 9876 at Starbucks"
        val regularUtilityBill = "Your electricity bill payment of Rs 1,400 is confirmed"

        assertTrue("CC bill payment 1 should be recognized", SmsAutoScanner.isCreditCardBillPayment(ccBill1))
        assertTrue("CC bill payment 2 should be recognized", SmsAutoScanner.isCreditCardBillPayment(ccBill2))
        assertTrue("CC bill payment 3 should be recognized", SmsAutoScanner.isCreditCardBillPayment(ccBill3))
        assertFalse("Regular card purchase is not a bill payment", SmsAutoScanner.isCreditCardBillPayment(regularCardSpend))
        assertFalse("Utility bill without credit card is not CC bill payment", SmsAutoScanner.isCreditCardBillPayment(regularUtilityBill))
    }

    @Test
    fun testTransactionEntityCreditCardBillPaymentHelper() {
        val billPaymentTx = TransactionEntity(
            title = "HDFC Credit Card Bill",
            grossAmount = 25000.0,
            trueNetAmount = 0.0,
            category = "BILL_PAYMENT",
            instrumentName = "HDFC CC",
            note = "Credit Card Bill Payment [CC_PAYMENT] - Excluded from expenses"
        )
        val normalTx = TransactionEntity(
            title = "Dinner at Smoke House",
            grossAmount = 2400.0,
            trueNetAmount = 2400.0,
            category = "FOOD_DINING",
            instrumentName = "HDFC CC"
        )

        assertTrue(billPaymentTx.isCreditCardBillPayment())
        assertFalse(normalTx.isCreditCardBillPayment())
    }

    // --- Refund & Expanded Credit Detection Tests ---

    @Test
    fun testRefundAndCreditDetection() {
        val refundSms = "Refund of Rs 699.00 initiated for your Amazon order"
        val reversalSms = "Reversal of Rs 150.00 done for failed UPI transaction"
        val cashbackSms = "Cashback of Rs 50.00 credited to your account"
        val salarySms = "Rs 95,000 credited to your A/C 1234 towards salary"
        val depositSms = "Deposited Rs 20,000 into your account at branch"

        assertTrue("Refund should be flagged as refund", SmsAutoScanner.isRefund(refundSms))
        assertTrue("Reversal should be flagged as refund", SmsAutoScanner.isRefund(reversalSms))
        assertTrue("Cashback should be flagged as refund", SmsAutoScanner.isRefund(cashbackSms))
        assertFalse("Salary is credit, not refund", SmsAutoScanner.isRefund(salarySms))

        assertTrue("Refund is a credit inflow", SmsAutoScanner.isCredit(refundSms))
        assertTrue("Reversal is a credit inflow", SmsAutoScanner.isCredit(reversalSms))
        assertTrue("Cashback is a credit inflow", SmsAutoScanner.isCredit(cashbackSms))
        assertTrue("Salary is a credit inflow", SmsAutoScanner.isCredit(salarySms))
        assertTrue("Deposit is a credit inflow", SmsAutoScanner.isCredit(depositSms))
    }

    @Test
    fun testTransactionEntityRefundOffsetHelper() {
        val refundTx = TransactionEntity(
            title = "Amazon Refund",
            grossAmount = 1200.0,
            trueNetAmount = 0.0,
            category = "REFUND",
            instrumentName = "Amazon Pay ICICI",
            note = "Refund / Reversal parsed from SMS [OFFSET_EXPENSE]"
        )
        val salaryTx = TransactionEntity(
            title = "Monthly Salary",
            grossAmount = 150000.0,
            trueNetAmount = 0.0,
            category = "INFLOW",
            instrumentName = "HDFC Bank",
            note = "Credit / Inflow parsed from SMS"
        )

        assertTrue("Refund tx should be identified as credit/inflow", refundTx.isCreditOrInflow())
        assertTrue("Refund tx should be identified as refund or credit offset", refundTx.isRefundOrCreditOffset())

        assertTrue("Salary tx should be identified as credit/inflow", salaryTx.isCreditOrInflow())
        assertFalse("Salary tx should NOT be treated as expense refund offset", salaryTx.isRefundOrCreditOffset())
    }
}
