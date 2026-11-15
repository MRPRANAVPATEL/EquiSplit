package com.pranav.equisplit

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class Payer(val id: String, val name: String, val people: Int)

internal data class Expense(
    val id: String,
    val payerId: String,
    val description: String,
    val amount: Long,
    val day: Long
)

internal data class SplitGroup(
    val id: String,
    val name: String,
    val people: Int,
    val payers: List<Payer>,
    val expenses: List<Expense>,
    val updatedAt: Long
)

internal data class Balance(val payer: Payer, val paid: Long, val share: Long) {
    val net: Long get() = paid - share
}

internal data class Transfer(val from: Payer, val to: Payer, val amount: Long)

internal data class Settlement(
    val total: Long,
    val averagePerPerson: Long,
    val balances: List<Balance>,
    val transfers: List<Transfer>
)

internal const val MAX_EXPENSE_PAISE = 99_999_999_999L
private val amountPattern = Regex("(?:[0-9]+(?:\\.[0-9]{0,2})?|\\.[0-9]{1,2})")
private val india = Locale.forLanguageTag("en-IN")
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", india)

internal fun parsePaise(input: String): Long? {
    val value = input.trim().replace(",", "")
    if (value.length > 15 || !amountPattern.matches(value)) return null
    return runCatching {
        BigDecimal(value).movePointRight(2).longValueExact()
    }.getOrNull()?.takeIf { it in 1..MAX_EXPENSE_PAISE }
}

internal fun money(paise: Long): String {
    val digits = if (paise % 100L == 0L) 0 else 2
    return NumberFormat.getCurrencyInstance(india).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }.format(BigDecimal.valueOf(paise, 2))
}

internal fun amountInput(paise: Long): String =
    BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString()

internal fun dateText(day: Long): String = LocalDate.ofEpochDay(day).format(dateFormat)

internal fun peopleLabel(people: Int): String = if (people == 1) "1 person" else "$people people"

internal fun payerLabel(payers: Int): String = if (payers == 1) "1 payer" else "$payers payers"

internal fun validGroup(group: SplitGroup): Boolean {
    val payerIds = group.payers.mapTo(HashSet()) { it.id }
    val names = group.payers.map { it.name.trim().lowercase(Locale.ROOT) }
    return group.name.isNotBlank() &&
        group.people > 0 &&
        group.payers.isNotEmpty() &&
        group.payers.all { it.name.isNotBlank() && it.people > 0 } &&
        group.payers.sumOf { it.people.toLong() } == group.people.toLong() &&
        payerIds.size == group.payers.size &&
        names.toSet().size == names.size &&
        group.expenses.map { it.id }.toSet().size == group.expenses.size &&
        group.expenses.all { it.payerId in payerIds && it.description.isNotBlank() &&
            it.amount in 1..MAX_EXPENSE_PAISE }
}

internal fun settle(group: SplitGroup): Settlement {
    require(validGroup(group)) { "The group must account for everyone and have valid expenses." }
    val paid = mutableMapOf<String, Long>()
    var total = 0L
    for (expense in group.expenses) {
        paid[expense.payerId] = Math.addExact(paid[expense.payerId] ?: 0L, expense.amount)
        total = Math.addExact(total, expense.amount)
    }

    val people = group.people.toLong()
    val whole = total / people
    val remainder = total % people
    // Divide before multiplying to keep the calculation within Long, even for large totals.
    val shares = group.payers.map { payer ->
        val fractional = remainder * payer.people
        Allocation(payer, whole * payer.people + fractional / people, fractional % people)
    }
    val extraPaise = (total - shares.sumOf { it.share }).toInt()
    // Allocate leftover paise to the largest fractional shares, preserving payer order for ties.
    val roundedUp = shares.sortedByDescending { it.remainder }.take(extraPaise)
        .mapTo(HashSet()) { it.payer.id }
    val balances = shares.map {
        Balance(it.payer, paid[it.payer.id] ?: 0L, it.share + if (it.payer.id in roundedUp) 1 else 0)
    }
    val average = whole + if (remainder * 2 >= people) 1 else 0
    return Settlement(total, average, balances, transfersFor(balances))
}

private data class Allocation(val payer: Payer, val share: Long, val remainder: Long)

private data class Outstanding(val payer: Payer, var amount: Long)

private fun transfersFor(balances: List<Balance>): List<Transfer> {
    val debtors = balances.filter { it.net < 0 }.sortedBy { it.net }
        .map { Outstanding(it.payer, -it.net) }
    val creditors = balances.filter { it.net > 0 }.sortedByDescending { it.net }
        .map { Outstanding(it.payer, it.net) }
    val transfers = mutableListOf<Transfer>()
    var debtorIndex = 0
    var creditorIndex = 0
    while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
        val debtor = debtors[debtorIndex]
        val creditor = creditors[creditorIndex]
        val amount = minOf(debtor.amount, creditor.amount)
        transfers += Transfer(debtor.payer, creditor.payer, amount)
        debtor.amount -= amount
        creditor.amount -= amount
        if (debtor.amount == 0L) debtorIndex++
        if (creditor.amount == 0L) creditorIndex++
    }
    check(debtorIndex == debtors.size && creditorIndex == creditors.size)
    return transfers
}

internal fun settlementMessage(group: SplitGroup, result: Settlement): String = buildString {
    appendLine(group.name)
    appendLine("Total shared spending  ${money(result.total)}")
    appendLine("${peopleLabel(group.people)} · ${payerLabel(group.payers.size)}")
    appendLine()
    if (result.transfers.isEmpty()) {
        appendLine("Everyone is settled.")
    } else {
        appendLine("Payments to make")
        for (payment in result.transfers) {
            appendLine("${payment.from.name} pays ${payment.to.name} ${money(payment.amount)}")
        }
    }
    appendLine()
    appendLine("Balances")
    for (balance in result.balances) {
        appendLine("${balance.payer.name} · ${peopleLabel(balance.payer.people)}")
        appendLine("Paid ${money(balance.paid)} · Share ${money(balance.share)}")
    }
}.trimEnd()
