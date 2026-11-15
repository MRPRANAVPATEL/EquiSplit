package com.pranav.equisplit

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class GroupStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "groups.json"))

    fun read(): List<SplitGroup> {
        val document = try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } catch (missing: FileNotFoundException) {
            if (file.baseFile.exists()) throw missing
            return emptyList()
        }
        if (document.getInt("version") != 1) throw IOException("Unknown format for saved groups.")
        val groups = document.getJSONArray("groups").objects { value ->
            SplitGroup(
                id = value.getString("id"),
                name = value.getString("name"),
                people = value.getInt("people"),
                payers = value.getJSONArray("payers").objects { payer ->
                    Payer(payer.getString("id"), payer.getString("name"), payer.getInt("people"))
                },
                expenses = value.getJSONArray("expenses").objects { expense ->
                    Expense(
                        expense.getString("id"),
                        expense.getString("payer"),
                        expense.getString("description"),
                        expense.getLong("amount"),
                        expense.getLong("day")
                    )
                },
                updatedAt = value.getLong("updatedAt")
            )
        }
        if (groups.any { !validGroup(it) } || groups.map { it.id }.toSet().size != groups.size) {
            throw IOException("Invalid saved group data.")
        }
        return groups.sortedByDescending { it.updatedAt }
    }

    fun write(groups: List<SplitGroup>) {
        val rows = JSONArray()
        for (group in groups) {
            val payers = JSONArray()
            for (payer in group.payers) {
                payers.put(JSONObject().put("id", payer.id).put("name", payer.name).put("people", payer.people))
            }
            val expenses = JSONArray()
            for (expense in group.expenses) {
                expenses.put(
                    JSONObject().put("id", expense.id).put("payer", expense.payerId)
                        .put("description", expense.description).put("amount", expense.amount)
                        .put("day", expense.day)
                )
            }
            rows.put(
                JSONObject().put("id", group.id).put("name", group.name).put("people", group.people)
                    .put("payers", payers).put("expenses", expenses).put("updatedAt", group.updatedAt)
            )
        }
        val bytes = JSONObject().put("version", 1).put("groups", rows).toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun <T> JSONArray.objects(decode: (JSONObject) -> T): List<T> =
        List(length()) { decode(getJSONObject(it)) }
}
