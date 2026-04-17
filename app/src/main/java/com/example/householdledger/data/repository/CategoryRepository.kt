package com.example.householdledger.data.repository

import com.example.householdledger.data.local.CategoryDao
import com.example.householdledger.data.model.Category
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    val categories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun syncCategories() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val remoteCategories = postgrest.from("categories")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Category>()
            
            categoryDao.insertCategories(remoteCategories)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category)
        try {
            postgrest.from("categories").insert(category)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
