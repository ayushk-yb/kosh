package com.kosh.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
