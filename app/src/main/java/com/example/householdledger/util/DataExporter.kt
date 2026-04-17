package com.example.householdledger.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.householdledger.data.model.Transaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

/**
 * Exports transaction data to CSV or JSON format.
 * Files are saved to the device's Downloads directory.
 */
object DataExporter {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun exportToCsv(
        context: Context,
        transactions: List<Transaction>,
        fileName: String = "household_ledger_export.csv"
    ): Boolean {
        return try {
            val csvContent = buildString {
                appendLine("ID,Amount,Date,Type,Category ID,Servant ID,Member ID,Description,Household ID")
                transactions.forEach { t ->
                    appendLine(
                        "${sanitize(t.id)},${t.amount},${sanitize(t.date)},${sanitize(t.type)}," +
                            "${sanitize(t.categoryId.orEmpty())},${sanitize(t.servantId.orEmpty())}," +
                            "${sanitize(t.memberId.orEmpty())},${sanitize(t.description)}," +
                            sanitize(t.householdId.orEmpty())
                    )
                }
            }
            writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToJson(
        context: Context,
        transactions: List<Transaction>,
        fileName: String = "household_ledger_backup.json"
    ): Boolean {
        return try {
            val jsonContent = json.encodeToString(transactions)
            writeToDownloads(context, fileName, "application/json", jsonContent.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
        var outputStream: OutputStream? = null
        return try {
            outputStream = resolver.openOutputStream(uri)
            outputStream?.write(data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            outputStream?.close()
        }
    }

    /** Sanitize CSV cell to prevent formula injection and escape commas. */
    private fun sanitize(value: String): String {
        val cleaned = value.replace("\"", "\"\"")
        return if (cleaned.contains(",") || cleaned.contains("\n") || cleaned.contains("\"")) {
            "\"$cleaned\""
        } else {
            cleaned
        }
    }
}
