package com.pranav.equisplit

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

internal val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun EquiSplitApp(model: EquiSplitModel) {
    val route = model.route
    BackHandler(enabled = route != Route.Home) { model.back() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            model.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !model.ready -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Couldn’t open your saved groups.", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = model::reload) { Text("Try again") }
            }
            else -> when (route) {
                Route.Home -> GroupsScreen(
                    model.groups,
                    onAdd = { model.show(Route.GroupEditor(null)) },
                    onOpen = { model.show(Route.Group(it.id)) }
                )
                is Route.Group -> model.groups.firstOrNull { it.id == route.id }?.let { group ->
                    GroupScreen(
                        group,
                        onBack = model::back,
                        onEdit = { model.show(Route.GroupEditor(group.id)) },
                        onAddExpense = { model.show(Route.ExpenseEditor(group.id, null)) },
                        onOpenExpense = { model.show(Route.ExpenseEditor(group.id, it.id)) },
                        onDelete = { model.deleteGroup(group) }
                    )
                }
                is Route.GroupEditor -> GroupEditorScreen(
                    group = model.groups.firstOrNull { it.id == route.id },
                    saving = model.saving,
                    onBack = model::back,
                    onSave = model::saveGroup
                )
                is Route.ExpenseEditor -> model.groups.firstOrNull { it.id == route.groupId }?.let { group ->
                    ExpenseEditorScreen(
                        group = group,
                        expense = group.expenses.firstOrNull { it.id == route.expenseId },
                        saving = model.saving,
                        onBack = model::back,
                        onSave = { model.saveExpense(group, it) },
                        onDelete = { model.deleteExpense(group, it) }
                    )
                }
            }
        }
    }
    model.message?.let { message ->
        AlertDialog(
            onDismissRequest = model::dismissMessage,
            title = { Text("Unable to save") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = model::dismissMessage) { Text("OK") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SplitTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            if (onBack == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark()
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun BrandMark() {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Forest),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(16.dp).height(4.dp).clip(CircleShape).background(Mint))
            Box(Modifier.width(16.dp).height(4.dp).clip(CircleShape).background(HeroMuted))
        }
    }
}

@Composable
internal fun PageList(
    insets: PaddingValues,
    bottom: Dp = 24.dp,
    content: LazyListScope.() -> Unit
) {
    Box(Modifier.padding(insets).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun GroupsScreen(
    groups: List<SplitGroup>,
    onAdd: () -> Unit,
    onOpen: (SplitGroup) -> Unit
) {
    Scaffold(topBar = { SplitTopBar("EquiSplit") }) { insets ->
        PageList(insets) {
            item {
                Banner {
                    Text(
                        "Split shared expenses",
                        style = MaterialTheme.typography.displaySmall,
                        color = Mint
                    )
                    Text(
                        "Record who paid and see how to settle up.",
                        color = HeroMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Forest)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New group")
                    }
                }
            }
            if (groups.isNotEmpty()) {
                item { SectionTitle("Your groups", Modifier.padding(top = 10.dp)) }
                items(groups, key = { it.id }) { group ->
                    GroupRow(group, onClick = { onOpen(group) })
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: SplitGroup, onClick: () -> Unit) {
    val total = remember(group.expenses) { group.expenses.sumOf { it.amount } }
    Surface(
        onClick = onClick,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(group.name)
            Column(Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${peopleLabel(group.people)} · ${payerLabel(group.payers.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(money(total), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GroupScreen(
    group: SplitGroup,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (Expense) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val result = remember(group) { settle(group) }
    val days = remember(group.expenses) { group.expenses.asReversed().sortedByDescending { it.day }.groupBy { it.day } }
    val names = remember(group.payers) { group.payers.associateBy { it.id } }
    var selectedTab by rememberSaveable(group.id) { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(group.id) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SplitTopBar(group.name, onBack) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Group options")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit group") }, onClick = {
                            menu = false
                            onEdit()
                        })
                        DropdownMenuItem(text = { Text("Delete group") }, onClick = {
                            menu = false
                            confirmDelete = true
                        })
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpense,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add expense") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { insets ->
        PageList(insets, bottom = if (selectedTab == 0) 96.dp else 28.dp) {
            item { TotalBanner(group, result) }
            item { GroupTabs(selectedTab) { selectedTab = it } }
            if (selectedTab == 0) {
                if (days.isEmpty()) item {
                    Notice("No expenses yet", "Add the first bill and choose who paid.")
                }
                for ((day, expenses) in days) {
                    item(key = "day-$day") { SectionTitle(dateText(day), Modifier.padding(top = 6.dp)) }
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense, names.getValue(expense.payerId)) { onOpenExpense(expense) }
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Who pays whom", Modifier.weight(1f))
                        if (group.expenses.isNotEmpty()) {
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, settlementMessage(group, result))
                                context.startActivity(Intent.createChooser(send, "Share settlement"))
                            }) { Icon(Icons.Default.Share, contentDescription = "Share settlement") }
                        }
                    }
                }
                if (result.transfers.isEmpty()) {
                    item {
                        if (group.expenses.isEmpty()) Notice("No balances yet", "Add an expense to see the split.")
                        else Notice("Everyone is settled", "Each payer has covered their share.")
                    }
                } else {
                    items(result.transfers, key = { "transfer-${it.from.id}-${it.to.id}" }) { transfer ->
                        TransferRow(transfer)
                    }
                }
                item { SectionTitle("Payer balances", Modifier.padding(top = 10.dp)) }
                items(result.balances, key = { "balance-${it.payer.id}" }) { BalanceCard(it) }
            }
        }
    }
    if (confirmDelete) ConfirmDelete(
        title = "Delete this group?",
        body = "This removes the group and all its expenses from this device.",
        onDismiss = { confirmDelete = false },
        onDelete = {
            confirmDelete = false
            onDelete()
        }
    )
}

@Composable
private fun TotalBanner(group: SplitGroup, result: Settlement) {
    Banner {
        Text("Total spent", color = HeroMuted, style = MaterialTheme.typography.labelLarge)
        Text(money(result.total), color = Mint, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(peopleLabel(group.people), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(payerLabel(group.payers.size), color = HeroMuted, style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(money(result.averagePerPerson), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Average per person", color = HeroMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Banner(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().clip(CardShape).background(Forest)) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.95f, size.height * 0.2f)
            val radius = size.width * 0.28f
            drawCircle(HeroEdge, radius, center, style = Stroke(1.2.dp.toPx()))
            drawCircle(HeroEdge, radius * 0.78f, center, style = Stroke(1.2.dp.toPx()))
            drawCircle(HeroEdge, radius * 0.56f, center, style = Stroke(1.2.dp.toPx()))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun GroupTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).selectableGroup().padding(4.dp)
    ) {
        listOf("Expenses", "Settle up").forEachIndexed { index, title ->
            val active = index == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, payer: Payer, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(payer.name)
            Column(Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Paid by ${payer.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(expense.amount), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TransferRow(transfer: Transfer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("${transfer.from.name} pays ${transfer.to.name}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(money(transfer.amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BalanceCard(balance: Balance) {
    val net = balance.net
    val label = when {
        net > 0L -> "Gets ${money(net)}"
        net < 0L -> "Owes ${money(-net)}"
        else -> "Settled"
    }
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(balance.payer.name)
                Column(Modifier.weight(1f)) {
                    Text(balance.payer.name, style = MaterialTheme.typography.titleMedium)
                    Text(peopleLabel(balance.payer.people), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    label,
                    color = if (net < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paid ${money(balance.paid)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("Share ${money(balance.share)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun Notice(title: String, description: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Avatar(name: String) {
    val letter = remember(name) { String(Character.toChars(name.codePointAt(0))).uppercase(Locale.ROOT) }
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
internal fun ConfirmDelete(title: String, body: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
