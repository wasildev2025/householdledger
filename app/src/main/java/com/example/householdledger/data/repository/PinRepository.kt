package com.example.householdledger.data.repository

import io.github.jan.supabase.functions.Functions
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PinResult(val success: Boolean, val error: String? = null)

@Singleton
class PinRepository @Inject constructor(
    private val functions: Functions
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Calls the `set-pin` edge function to securely hash + store a PIN for a
     * servant or member. The server performs authorization (owner or household admin).
     */
    suspend fun setPin(
        targetType: String, // "servant" | "member"
        targetId: String,
        pin: String
    ): PinResult {
        return try {
            val response = functions.invoke(
                function = "set-pin",
                body = buildJsonObject {
                    put("target_type", targetType)
                    put("target_id", targetId)
                    put("pin", pin)
                }
            )
            if (response.status == HttpStatusCode.OK) {
                PinResult(success = true)
            } else {
                val text = response.body<String>()
                PinResult(success = false, error = "HTTP ${response.status.value}: $text")
            }
        } catch (e: Exception) {
            PinResult(success = false, error = e.message)
        }
    }

    /** Calls `verify-pin` to check a PIN. */
    suspend fun verifyPin(
        targetType: String,
        targetId: String,
        pin: String
    ): PinResult {
        return try {
            val response = functions.invoke(
                function = "verify-pin",
                body = buildJsonObject {
                    put("target_type", targetType)
                    put("target_id", targetId)
                    put("pin", pin)
                }
            )
            if (response.status == HttpStatusCode.OK) {
                val body = response.body<String>()
                val ok = body.contains("\"success\":true") || body.trim() == "true"
                PinResult(success = ok)
            } else {
                PinResult(success = false, error = "HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            PinResult(success = false, error = e.message)
        }
    }
}
