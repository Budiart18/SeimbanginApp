package com.aeryz.seimbanginapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeryz.seimbanginapp.data.local.database.entity.TransactionEntity
import com.aeryz.seimbanginapp.data.local.datastore.UserPreferenceDataSource
import com.aeryz.seimbanginapp.data.network.model.advisor.AdvisorResponse
import com.aeryz.seimbanginapp.data.network.model.profile.FinanceProfile
import com.aeryz.seimbanginapp.data.network.model.profile.ProfileResponse
import com.aeryz.seimbanginapp.data.network.model.transactionHistory.TransactionHistoryResponse
import com.aeryz.seimbanginapp.data.repository.AuthRepository
import com.aeryz.seimbanginapp.data.repository.GeminiAiRepository
import com.aeryz.seimbanginapp.data.repository.LocalTransactionRepository
import com.aeryz.seimbanginapp.data.repository.TransactionRepository
import com.aeryz.seimbanginapp.model.TransactionItem
import com.aeryz.seimbanginapp.utils.ResultWrapper
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val localTransactionRepository: LocalTransactionRepository,
    private val userPreferenceDataSource: UserPreferenceDataSource,
    private val geminiAiRepository: GeminiAiRepository
) : ViewModel() {

    private val _profileData = MutableLiveData<ResultWrapper<ProfileResponse>>()
    val profileData: LiveData<ResultWrapper<ProfileResponse>>
        get() = _profileData

    private val _transactionHistory = MutableLiveData<ResultWrapper<TransactionHistoryResponse>>()
    val transactionHistory: LiveData<ResultWrapper<TransactionHistoryResponse>> =
        _transactionHistory

    private val _insertListToDatabaseResult = MutableLiveData<ResultWrapper<Boolean>>()
    val insertListToDatabaseResult: LiveData<ResultWrapper<Boolean>>
        get() = _insertListToDatabaseResult

    private val _localAdvisor = MutableLiveData<String?>()
    val localAdvisor: LiveData<String?>
        get() = _localAdvisor

    private val _aiAdvisor = MutableLiveData<ResultWrapper<String>>()
    val aiAdvisor: LiveData<ResultWrapper<String>>
        get() = _aiAdvisor

    fun getProfileData() {
        viewModelScope.launch {
            authRepository.getUserProfile().collect {
                _profileData.postValue(it)
            }
        }
    }

    fun getTransactionHistory(limit: Int?, page: Int?) {
        viewModelScope.launch {
            transactionRepository.getTransactionHistory(limit, page).collect {
                _transactionHistory.postValue(it)
            }
        }
    }

    fun insertListToDatabase(transactions: List<TransactionItem>) {
        viewModelScope.launch {
            val entities = transactions.map { item ->
                TransactionEntity(
                    id = item.id,
                    userId = item.userId,
                    name = item.name,
                    type = item.type,
                    category = item.category,
                    amount = item.amount,
                    description = item.description,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    items = item.items
                )
            }
            localTransactionRepository.insertListTransaction(entities).collect {
                _insertListToDatabaseResult.postValue(it)
            }
        }
    }

    fun saveAdvise(advise: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferenceDataSource.saveAdvise(advise)
        }
    }

    fun getAdviseFromAI(financeProfile: FinanceProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            geminiAiRepository.generateFinancialAdvice(financeProfile).collect {
                _aiAdvisor.postValue(it)
            }
        }
    }

    fun getAdviseFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferenceDataSource.getAdvise().collect {
                _localAdvisor.postValue(it)
            }
        }
    }
}
