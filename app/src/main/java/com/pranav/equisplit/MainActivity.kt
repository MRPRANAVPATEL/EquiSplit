package com.pranav.equisplit

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val model: EquiSplitModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EquiSplitTheme {
                EquiSplitApp(model)
            }
        }
    }
}

internal sealed interface Route {
    data object Home : Route
    data class Group(val id: String) : Route
    data class GroupEditor(val id: String?) : Route
    data class ExpenseEditor(val groupId: String, val expenseId: String?) : Route
}

internal class EquiSplitModel(application: Application) : AndroidViewModel(application) {
    private val store = GroupStore(application)

    var groups by mutableStateOf(emptyList<SplitGroup>())
        private set
    var route by mutableStateOf<Route>(Route.Home)
        private set
    var loading by mutableStateOf(false)
        private set
    var ready by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        reload()
    }

    fun reload() {
        if (loading || saving) return
        loading = true
        viewModelScope.launch {
            try {
                groups = withContext(Dispatchers.IO) { store.read() }
                ready = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ready = false
            } finally {
                loading = false
            }
        }
    }

    fun show(destination: Route) {
        if (!saving) route = destination
    }

    fun back() {
        show(
            when (val current = route) {
                Route.Home, is Route.Group -> Route.Home
                is Route.GroupEditor -> current.id?.let { Route.Group(it) } ?: Route.Home
                is Route.ExpenseEditor -> Route.Group(current.groupId)
            }
        )
    }

    fun saveGroup(group: SplitGroup) {
        if (!validGroup(group)) {
            message = "Check the payer names and people covered."
            return
        }
        val updated = group.copy(updatedAt = System.currentTimeMillis())
        val next = (groups.filterNot { it.id == updated.id } + updated).sortedByDescending { it.updatedAt }
        commit(next, Route.Group(updated.id))
    }

    fun saveExpense(group: SplitGroup, expense: Expense) {
        val current = groups.firstOrNull { it.id == group.id } ?: return
        val index = current.expenses.indexOfFirst { it.id == expense.id }
        val expenses = current.expenses.toMutableList()
        if (index < 0) expenses.add(expense) else expenses[index] = expense
        saveGroup(current.copy(expenses = expenses))
    }

    fun deleteExpense(group: SplitGroup, expense: Expense) {
        val current = groups.firstOrNull { it.id == group.id } ?: return
        saveGroup(current.copy(expenses = current.expenses.filterNot { it.id == expense.id }))
    }

    fun deleteGroup(group: SplitGroup) {
        commit(groups.filterNot { it.id == group.id }, Route.Home)
    }

    fun dismissMessage() {
        message = null
    }

    private fun commit(next: List<SplitGroup>, destination: Route) {
        if (saving || !ready) return
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { store.write(next) }
                groups = next
                route = destination
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "EquiSplit couldn’t save your changes. Try again."
            } finally {
                saving = false
            }
        }
    }
}
