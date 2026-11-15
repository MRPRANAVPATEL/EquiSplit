package com.pranav.equisplit

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private data class PayerDraft(val id: String, val name: String, val people: String)

private val payerDraftSaver = listSaver<SnapshotStateList<PayerDraft>, String>(
    save = { drafts -> drafts.flatMap { listOf(it.id, it.name, it.people) } },
    restore = { values -> values.chunked(3).map { PayerDraft(it[0], it[1], it[2]) }.toMutableStateList() }
)

private fun newPayer() = PayerDraft(UUID.randomUUID().toString(), "", "1")

@Composable
internal fun GroupEditorScreen(
    group: SplitGroup?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (SplitGroup) -> Unit
) {
    val id = rememberSaveable(group?.id) { group?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable(group?.id) { mutableStateOf(group?.name.orEmpty()) }
    var people by rememberSaveable(group?.id) { mutableStateOf((group?.people ?: 2).toString()) }
    val drafts = rememberSaveable(group?.id, saver = payerDraftSaver) {
        (group?.payers?.map { PayerDraft(it.id, it.name, it.people.toString()) } ?: listOf(newPayer()))
            .toMutableStateList()
    }
    val payers = drafts.map { Payer(it.id, it.name.trim(), it.people.toIntOrNull() ?: 0) }
    val totalPeople = people.toIntOrNull() ?: 0
    val covered = payers.sumOf { it.people.toLong() }
    val candidate = SplitGroup(id, title.trim(), totalPeople, payers, group?.expenses.orEmpty(), 0L)
    val duplicates = payers.map { it.name.lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
        .let { it.size != it.toSet().size }
    val existingPayers = candidate.expenses.mapTo(HashSet()) { it.payerId }
    val changedCoverage = group != null && group.expenses.isNotEmpty() &&
        group.payers.associate { it.id to it.people } != payers.associate { it.id to it.people }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { SplitTopBar(if (group == null) "New group" else "Edit group", onBack) },
        bottomBar = {
            SaveBar(
                label = if (group == null) "Create group" else "Save group",
                saving = saving,
                enabled = validGroup(candidate),
                onClick = { onSave(candidate) }
            )
        }
    ) { insets ->
        PageList(insets) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Group name") },
                    placeholder = { Text("e.g. Goa weekend") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    enabled = !saving
                )
            }
            item {
                OutlinedTextField(
                    value = people,
                    onValueChange = { people = it.filter(Char::isDigit).take(10) },
                    label = { Text("Total people") },
                    supportingText = { Text("Count everyone whose expenses will be shared.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !saving,
                    isError = people.isNotEmpty() && totalPeople < 1
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SectionTitle("Payers")
                    Text(
                        "Add one name for each payer. They can cover themselves and other people.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                CoverageCard(covered, totalPeople, duplicates)
            }
            itemsIndexed(drafts, key = { _, draft -> draft.id }) { index, draft ->
                PayerForm(
                    draft = draft,
                    enabled = !saving,
                    removable = drafts.size > 1 && draft.id !in existingPayers,
                    onChange = { drafts[index] = it },
                    onRemove = { drafts.removeAt(index) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { drafts.add(newPayer()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add payer", Modifier.padding(start = 6.dp))
                }
            }
            if (changedCoverage) item {
                Notice("Shares will update", "Saved expenses will be split using the updated people counts.")
            }
        }
    }
}

@Composable
private fun CoverageCard(covered: Long, total: Int, duplicates: Boolean) {
    val proportion = if (total > 0) (covered.toDouble() / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val excessive = total > 0 && covered > total
    val ink = if (excessive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (total > 0) "$covered of $total people covered" else "$covered people covered",
                style = MaterialTheme.typography.labelLarge,
                color = ink
            )
            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(proportion).height(5.dp).clip(CircleShape).background(ink))
            }
            if (duplicates) Text(
                "Give each payer a different name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PayerForm(
    draft: PayerDraft,
    enabled: Boolean,
    removable: Boolean,
    onChange: (PayerDraft) -> Unit,
    onRemove: () -> Unit
) {
    val count = draft.people.toIntOrNull() ?: 0
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(60))) },
                label = { Text("Payer name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                enabled = enabled
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("People covered", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onChange(draft.copy(people = (count - 1).toString())) },
                    enabled = enabled && count > 1
                ) {
                    Text("−", Modifier.clearAndSetSemantics { contentDescription = "Decrease people covered" })
                }
                OutlinedTextField(
                    value = draft.people,
                    onValueChange = { onChange(draft.copy(people = it.filter(Char::isDigit).take(10))) },
                    modifier = Modifier.weight(0.85f).semantics { contentDescription = "People covered" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = enabled,
                    isError = count < 1
                )
                IconButton(
                    onClick = { onChange(draft.copy(people = (count + 1).toString())) },
                    enabled = enabled && count < Int.MAX_VALUE
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase people covered")
                }
            }
            if (removable) {
                TextButton(onClick = onRemove, enabled = enabled, modifier = Modifier.align(Alignment.End)) {
                    Text("Remove payer")
                }
            }
        }
    }
}

@Composable
internal fun ExpenseEditorScreen(
    group: SplitGroup,
    expense: Expense?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val context = LocalContext.current
    val id = rememberSaveable(expense?.id) { expense?.id ?: UUID.randomUUID().toString() }
    var description by rememberSaveable(id) { mutableStateOf(expense?.description.orEmpty()) }
    var amount by rememberSaveable(id) { mutableStateOf(expense?.let { amountInput(it.amount) }.orEmpty()) }
    var payer by rememberSaveable(id) { mutableStateOf(expense?.payerId ?: group.payers.first().id) }
    var day by rememberSaveable(id) { mutableLongStateOf(expense?.day ?: LocalDate.now().toEpochDay()) }
    var showPayers by rememberSaveable(id) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(id) { mutableStateOf(false) }
    val paidBy = group.payers.first { it.id == payer }
    val parsed = parsePaise(amount)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { SplitTopBar(if (expense == null) "Add expense" else "Edit expense", onBack) },
        bottomBar = {
            SaveBar(
                label = "Save expense",
                saving = saving,
                enabled = description.isNotBlank() && parsed != null,
                onClick = {
                    if (parsed != null) onSave(Expense(id, payer, description.trim(), parsed, day))
                }
            )
        }
    ) { insets ->
        PageList(insets) {
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.take(18) },
                    label = { Text("Amount") },
                    placeholder = { Text("0") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineMedium,
                    singleLine = true,
                    enabled = !saving,
                    isError = amount.isNotBlank() && parsed == null,
                    supportingText = if (amount.isNotBlank() && parsed == null) {
                        { Text("Enter a positive amount with up to two decimal places.") }
                    } else null
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(100) },
                    label = { Text("What was it for?") },
                    placeholder = { Text("e.g. Breakfast or hotel") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    enabled = !saving
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("Paid by")
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showPayers = true },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) {
                            Text(paidBy.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showPayers, onDismissRequest = { showPayers = false }) {
                            for (option in group.payers) {
                                DropdownMenuItem(
                                    text = { Text("${option.name} · ${peopleLabel(option.people)}") },
                                    onClick = { payer = option.id; showPayers = false }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("Payment date")
                    OutlinedButton(
                        onClick = {
                            val current = LocalDate.ofEpochDay(day)
                            DatePickerDialog(
                                context,
                                { _, year, month, date -> day = LocalDate.of(year, month + 1, date).toEpochDay() },
                                current.year, current.monthValue - 1, current.dayOfMonth
                            ).show()
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Text(dateText(day), Modifier.weight(1f))
                        Text("Change")
                    }
                }
            }
            if (expense != null) item {
                TextButton(onClick = { confirmDelete = true }, enabled = !saving) {
                    Text("Delete expense", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete && expense != null) ConfirmDelete(
        title = "Delete this expense?",
        body = "The group’s balances will update.",
        onDismiss = { confirmDelete = false },
        onDelete = { confirmDelete = false; onDelete(expense) }
    )
}

@Composable
private fun SaveBar(label: String, saving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClick,
                enabled = enabled && !saving,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text(if (saving) "Saving…" else label)
            }
        }
    }
}
