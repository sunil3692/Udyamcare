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

    @Test fun sbiStyle() {
        val p = SmsParser.parse("Dear UPI user A/C X1234 debited by 500.0 on date 26Nov26 trf to RAMESH KUMAR Refno 123456789012. If not u? call 1800")!!
        assertEquals(50000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("RAMESH KUMAR", p.party)
        assertEquals("123456789012", p.ref)
    }

    @Test fun paytmPaid() {
        val p = SmsParser.parse("Paid Rs.120 to Sharma Tea Stall using Paytm UPI. UPI Ref: 556677889900")!!
        assertEquals(12000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("Sharma Tea Stall", p.party)
    }

    @Test fun infoField() {
        val p = SmsParser.parse("A/c XX1234 credited Rs.2000.00 on 26-11-26. Info: UPI/CR/445566778899/SITA DEVI/HDFC. Avl Bal Rs.7000")!!
        assertEquals(TxType.CREDIT, p.type)
        assertEquals("SITA DEVI", p.party)
        assertEquals(200000L, p.amountPaise)
    }

    @Test fun bobDebitToVpa() {
        val p = SmsParser.parse("Rs.500.00 Dr. from A/C XXXXXX2106 and Cr. to googleads.hypg@yespay. Ref:347333499804. AvlBal:Rs774.75(2026:11:18 04:53:46). Not you? Call 18005700/5000-BOB")!!
        assertEquals(50000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("googleads.hypg@yespay", p.party)
        assertEquals("347333499804", p.ref)
    }

    @Test fun bobDebitToAccount() {
        val p = SmsParser.parse("Rs.50000.00 Dr. from A/C XXXXXX2106 and Cr. to XXXXXX2712. Ref:225992860689. AvlBal:Rs54.75(2026:11:26 10:16:48). Not you? Call 18005700/5000-BOB")!!
        assertEquals(5000000L, p.amountPaise)
        assertEquals(TxType.DEBIT, p.type)
        assertEquals("A/C XXXXXX2712", p.party)
        assertEquals("225992860689", p.ref)
    }

    @Test fun bobCredit() {
        val p = SmsParser.parse("Rs.1500.00 Cr. to A/C XXXXXX2106 and Dr. from rahul@okaxis. Ref:111122223333. AvlBal:Rs2000.00(2026:11:26 10:16:48). Not you? Call 18005700/5000-BOB")!!
        assertEquals(150000L, p.amountPaise)
        assertEquals(TxType.CREDIT, p.type)
        assertEquals("rahul@okaxis", p.party)
    }

    @Test fun doctorNameIsNotDebit() {
        assertNull(SmsParser.parse("Dr. Sharma clinic appointment Rs.500 tomorrow"))
    }

    @Test fun readsBalance() {
        val p = SmsParser.parse("Rs.500.00 Dr. from A/C XXXXXX2106 and Cr. to googleads.hypg@yespay. Ref:347333499804. AvlBal:Rs774.75(2026:11:18 04:53:46). Not you? Call 18005700/5000-BOB")!!
        assertEquals(77475L, p.balancePaise)
        val q = SmsParser.parse("Rs.40 debited from A/c XX1234 to VPA a@ybl (UPI Ref No 123456789012). Avl Bal Rs 9,000.00")!!
        assertEquals(900000L, q.balancePaise)
        assertNull(SmsParser.parse("Rs.40 debited from A/c XX1234 to VPA a@ybl (UPI Ref No 123456789012)")!!.balancePaise)
    }
}
