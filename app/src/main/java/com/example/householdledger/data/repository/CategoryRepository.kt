package com.example.householdledger.data.repository

import com.example.householdledger.data.local.CategoryDao
import com.example.householdledger.data.local.OfflineQueueDao
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.OfflineQueueItem
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val offlineQueueDao: OfflineQueueDao,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authRepository: AuthRepository
) {
    val categories: Flow<List<Category>> = categoryDao.getAllCategories()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null

    suspend fun syncCategories() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return
        try {
            val remote = postgrest.from("categories")
                .select { filter { eq("household_id", householdId) } }
                .decodeList<Category>()
            categoryDao.insertCategories(remote)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category)
        try {
            postgrest.from("categories").insert(category)
        } catch (e: Exception) {
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "categories",
                    operation = "insert",
                    payload = json.encodeToString(category),
                    entityId = category.id
                )
            )
        }
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
        try {
            postgrest.from("categories").update(category) {
                filter { eq("id", category.id) }
            }
        } catch (e: Exception) {
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "categories",
                    operation = "update",
                    payload = json.encodeToString(category),
                    entityId = category.id
                )
            )
        }
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
        try {
            postgrest.from("categories").delete {
                filter { eq("id", category.id) }
            }
        } catch (e: Exception) {
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "categories",
                    operation = "delete",
                    payload = json.encodeToString(category),
                    entityId = category.id
                )
            )
        }
    }

    /**
     * Subscribe to realtime INSERT/UPDATE/DELETE events on the categories table,
     * scoped to the user's household. Cancels any prior subscription.
     */
    fun subscribeRealtime() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                val channel = realtime.channel("categories:$householdId")
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "categories"
                }
                channel.subscribe()
                changes.catch { it.printStackTrace() }.collect { action ->
                    // Simple approach: pull fresh snapshot when anything changes.
                    syncCategories()
                    @Suppress("UNUSED_VARIABLE") val ignored = action
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unsubscribeRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
    }
}
