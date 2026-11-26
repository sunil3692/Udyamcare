package com.udyamcare.kharcha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test fun upiDebitToVpa() {
        val p = SmsParser.parse("Rs.500.00 debited from A/c XX1234 on 26-11-26 to VPA ramesh@okaxis (UPI Ref No 123456789012). Avl Bal Rs 9,000.00")!!
        assertEquals(50000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("ramesh@okaxis", p.party)
        assertEquals("123456789012", p.ref)
    }

    @Test fun sentToName() {
        val p = SmsParser.parse("Sent Rs.250.00 from HDFC Bank A/c 1234 to RAMESH KUMAR on 26/11/26 UPI Ref 987654321098")!!
        assertEquals(25000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("RAMESH KUMAR", p.party)
    }

    @Test fun creditFromVpa() {
        val p = SmsParser.parse("Rs 1,200 credited to A/c XX1234 on 26-Nov-26 by VPA sita@ybl (UPI Ref 112233445566)")!!
        assertEquals(120000L, p.amountPaise)
        assertEquals(TxType.CREDIT, p.type)
        assertEquals("sita@ybl", p.party)
    }

    @Test fun creditReceivedFromName() {
        val p = SmsParser.parse("Dear Customer, Rs.100 received from MOHAN LAL on 26-11-26. Bal Rs.5000")!!
        assertEquals(TxType.CREDIT, p.type)
        assertEquals("MOHAN LAL", p.party)
        assertEquals(10000L, p.amountPaise)
    }

    @Test fun cardSpend() {
        val p = SmsParser.parse("INR 799.00 spent on Card XX1234 at AMAZON on 26-11-26. Avl limit INR 50,000")!!
        assertEquals(79900L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("AMAZON", p.party)
    }

    @Test fun balanceAmountIsNotPicked() {
        val p = SmsParser.parse("Avl Bal Rs 9,000.00. Rs.40 debited from A/c XX1234")!!
        assertEquals(4000L, p.amountPaise)
    }

    @Test fun ignoresOtpAndPromo() {
        assertNull(SmsParser.parse("123456 is your OTP for payment of Rs.500. Do not share."))
        assertNull(SmsParser.parse("Pre-approved loan offer of Rs.5,00,000 for you. Apply now"))
        assertNull(SmsParser.parse("Hello, how are you?"))
    }
}
