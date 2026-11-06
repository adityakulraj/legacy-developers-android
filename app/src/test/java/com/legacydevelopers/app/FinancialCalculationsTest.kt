package com.legacydevelopers.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialCalculationsTest {
    @Test
    fun flatDue_isTheUnpaidBalance() {
        val flat = FlatRecord(
            flatNo = "A-101",
            area = 1_250.0,
            status = "Booked",
            consideration = 8_000_000.0,
            paid = 3_250_000.0
        )

        assertEquals(4_750_000.0, flat.due, 0.001)
    }

    @Test
    fun flatDue_neverBecomesNegative() {
        val flat = FlatRecord(
            flatNo = "A-102",
            area = 1_100.0,
            status = "Booked",
            consideration = 5_000_000.0,
            paid = 5_250_000.0
        )

        assertEquals(0.0, flat.due, 0.001)
    }

    @Test
    fun intraStatePayment_splitsGstEquallyBetweenCgstAndSgst() {
        val payment = payment(supplyType = "Intra-state")

        assertEquals(9_000.0, payment.cgst, 0.001)
        assertEquals(9_000.0, payment.sgst, 0.001)
        assertEquals(0.0, payment.igst, 0.001)
        assertEquals(82_000.0, payment.taxableComponent, 0.001)
    }

    @Test
    fun interStatePayment_assignsAllGstToIgst() {
        val payment = payment(supplyType = "Inter-state")

        assertEquals(0.0, payment.cgst, 0.001)
        assertEquals(0.0, payment.sgst, 0.001)
        assertEquals(18_000.0, payment.igst, 0.001)
    }

    @Test
    fun taxableComponent_neverBecomesNegative() {
        val payment = payment(amountReceived = 10_000.0, gstAmount = 12_000.0)

        assertEquals(0.0, payment.taxableComponent, 0.001)
    }

    private fun payment(
        amountReceived: Double = 100_000.0,
        gstAmount: Double = 18_000.0,
        supplyType: String = "Intra-state"
    ) = GstEntry(
        date = "2026-11-06",
        receiptNo = "R-001",
        flatNo = "A-101",
        customer = "Test Customer",
        amountReceived = amountReceived,
        gstAmount = gstAmount,
        supplyType = supplyType
    )
}
