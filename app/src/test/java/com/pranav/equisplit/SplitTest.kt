package com.pranav.equisplit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SplitTest {
    @Test
    fun eightPeopleCanUseFivePayersAndManyExpenses() {
        val people = listOf(payer("Pranav", 2), payer("Ram", 2), payer("Shyam", 2), payer("X"), payer("Y"))
        val trip = group(people, listOf(
            bill("Pranav", 200_000L, 1),
            bill("Pranav", 100_000L, 2),
            bill("Pranav", 100_000L, 3),
            bill("Pranav", 100_000L, 4),
            bill("Ram", 1_000_000L, 5),
            bill("Shyam", 1_700_000L, 6)
        ))
        val result = settle(trip)
        assertEquals(3_200_000L, result.total)
        assertEquals(400_000L, result.averagePerPerson)
        val ram = result.balances.single { it.payer.name == "Ram" }
        assertEquals(800_000L, ram.share)
        assertEquals(200_000L, ram.net)
        assertEquals(200_000L, result.transfers.filter { it.to.name == "Ram" }.sumOf { it.amount })
        assertSettledByTransfers(result)
    }

    @Test
    fun fractionsAllocateEveryPaise() {
        val result = settle(group(listOf(payer("A", 2), payer("B")), listOf(bill("A", 10_000L))))
        assertEquals(listOf(6_667L, 3_333L), result.balances.map { it.share })
        assertEquals(Transfer(payer("B"), payer("A", 2), 3_333L), result.transfers.single())
        assertSettledByTransfers(result)
    }

    @Test
    fun balanceAndTransfersNeverLoseMoney() {
        val random = Random(73941)
        repeat(250) {
            val payers = List(random.nextInt(1, 12)) { payer("P$it", random.nextInt(1, 7)) }
            val expenses = List(random.nextInt(0, 26)) { index ->
                bill(payers.random(random).id, random.nextLong(1, MAX_EXPENSE_PAISE), index)
            }
            val result = settle(group(payers, expenses))
            assertEquals(result.total, result.balances.sumOf { it.paid })
            assertEquals(result.total, result.balances.sumOf { it.share })
            assertSettledByTransfers(result)
        }
    }

    @Test
    fun invalidGroupIsRejectedAndMoneyInputsAreExact() {
        val trip = group(listOf(payer("Ram", 2), payer("X")), emptyList())
        assertTrue(validGroup(trip))
        assertFalse(validGroup(trip.copy(people = 4)))
        assertFalse(validGroup(trip.copy(expenses = listOf(bill("Unknown", 4L)))))
        assertEquals(500_025L, parsePaise("5,000.25"))
        assertEquals(50L, parsePaise(".50"))
        assertNull(parsePaise("-1"))
        assertNull(parsePaise("1.234"))
        assertNull(parsePaise("0"))
    }

    private fun assertSettledByTransfers(result: Settlement) {
        assertTrue(result.transfers.all { it.amount > 0 && it.from.id != it.to.id })
        for (balance in result.balances) {
            val outgoing = result.transfers.filter { it.from == balance.payer }.sumOf { it.amount }
            val incoming = result.transfers.filter { it.to == balance.payer }.sumOf { it.amount }
            assertEquals(0L, balance.net + outgoing - incoming)
        }
    }

    private fun payer(name: String, people: Int = 1) = Payer(name, name, people)

    private fun bill(payer: String, amount: Long, index: Int = 1) =
        Expense("bill-$index", payer, "Expense $index", amount, 20000L)

    private fun group(payers: List<Payer>, expenses: List<Expense>) =
        SplitGroup("group", "Trip", payers.sumOf { it.people }, payers, expenses, 0L)
}
