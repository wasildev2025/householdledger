package com.example.householdledger.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class InsightRequest(val householdId: String)

@Serializable
data class InsightResponse(val insights: String)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        generateInsights()
    }

    fun generateInsights() {
        viewModelScope.launch {
            _uiState.value = InsightsUiState(isLoading = true)
            val householdId = authRepository.currentUser.value?.householdId
            
            if (householdId == null) {
                _uiState.value = InsightsUiState(error = "Not connected to a household.")
                return@launch
            }

            try {
                // Call Supabase Edge Function to analyze spending and generate insights
                // Note: Assumes an edge function named "generate-insights" exists.
                val response = supabaseClient.functions.invoke(
                    function = "generate-insights",
                    body = InsightRequest(householdId)
                )
                
                // Decode assuming the edge function returns { "insights": "..." }
                val insightData = response.body<InsightResponse>()
                _uiState.value = InsightsUiState(insights = insightData.insights)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = InsightsUiState(
                    error = "Could not load AI insights. Check your network connection or verify the Edge Function.",
                    insights = "Fallback Insight: Your spending looks normal, but try to cut down on dining out!" // Fallback for UI demo
                )
            }
        }
    }
}

data class InsightsUiState(
    val isLoading: Boolean = false,
    val insights: String? = null,
    val error: String? = null
)
