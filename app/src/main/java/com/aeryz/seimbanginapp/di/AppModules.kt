package com.aeryz.seimbanginapp.di

import com.aeryz.seimbanginapp.data.local.database.TransactionDatabase
import com.aeryz.seimbanginapp.data.local.datasource.IntroPageDataSource
import com.aeryz.seimbanginapp.data.local.datasource.IntroPageDataSourceImpl
import com.aeryz.seimbanginapp.data.local.datastore.UserPreferenceDataSource
import com.aeryz.seimbanginapp.data.local.datastore.UserPreferenceDataSourceImpl
import com.aeryz.seimbanginapp.data.local.datastore.appDataStore
import com.aeryz.seimbanginapp.data.network.datasource.SeimbanginDataSource
import com.aeryz.seimbanginapp.data.network.datasource.SeimbanginDataSourceImpl
import com.aeryz.seimbanginapp.data.network.service.ApiConfig
import com.aeryz.seimbanginapp.data.repository.AuthRepository
import com.aeryz.seimbanginapp.data.repository.AuthRepositoryImpl
import com.aeryz.seimbanginapp.data.repository.GeminiAiRepository
import com.aeryz.seimbanginapp.data.repository.LocalTransactionRepository
import com.aeryz.seimbanginapp.data.repository.LocalTransactionRepositoryImpl
import com.aeryz.seimbanginapp.data.repository.TransactionRepository
import com.aeryz.seimbanginapp.data.repository.TransactionRepositoryImpl
import com.aeryz.seimbanginapp.ui.chatAi.ChatAiViewModel
import com.aeryz.seimbanginapp.ui.dashboard.DashboardViewModel
import com.aeryz.seimbanginapp.ui.editProfile.EditProfileViewModel
import com.aeryz.seimbanginapp.ui.financialProfile.FinancialProfileViewModel
import com.aeryz.seimbanginapp.ui.home.HomeViewModel
import com.aeryz.seimbanginapp.ui.login.LoginViewModel
import com.aeryz.seimbanginapp.ui.main.MainViewModel
import com.aeryz.seimbanginapp.ui.ocr.OcrViewModel
import com.aeryz.seimbanginapp.ui.profile.ProfileViewModel
import com.aeryz.seimbanginapp.ui.register.RegisterViewModel
import com.aeryz.seimbanginapp.ui.splash.SplashViewModel
import com.aeryz.seimbanginapp.ui.transaction.createTransaction.CreateTransactionViewModel
import com.aeryz.seimbanginapp.ui.transaction.transactionDetail.TransactionDetailViewModel
import com.aeryz.seimbanginapp.ui.transaction.transactionHistory.TransactionHistoryViewModel
import com.aeryz.seimbanginapp.utils.PreferenceDataStoreHelper
import com.aeryz.seimbanginapp.utils.PreferenceDataStoreHelperImpl
import com.chuckerteam.chucker.api.ChuckerInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object AppModules {
    fun getModules(): List<Module> = listOf(
        localModule,
        viewModelsModule,
        networkModule,
        dataSourceModule,
        repositoryModule
    )

    private val localModule = module {
        single { TransactionDatabase.getDatabase(androidContext()) }
        single { get<TransactionDatabase>().transactionDao() }
        single { androidContext().appDataStore }
        single<PreferenceDataStoreHelper> { PreferenceDataStoreHelperImpl(get()) }
        single<UserPreferenceDataSource> { UserPreferenceDataSourceImpl(get()) }
    }

    private val networkModule = module {
        single { ChuckerInterceptor(androidContext()) }
        single { ApiConfig.getApiService(get(), get()) }
    }

    private val dataSourceModule = module {
        single<SeimbanginDataSource> { SeimbanginDataSourceImpl(get()) }
        single<IntroPageDataSource> { IntroPageDataSourceImpl() }
    }

    private val repositoryModule = module {
        single<AuthRepository> { AuthRepositoryImpl(get()) }
        single<TransactionRepository> { TransactionRepositoryImpl(get()) }
        single { GeminiAiRepository() }
        single<LocalTransactionRepository> { LocalTransactionRepositoryImpl(get()) }
    }

    private val viewModelsModule = module {
        viewModel { LoginViewModel(get(), get()) }
        viewModel { RegisterViewModel(get()) }
        viewModel { SplashViewModel(get(), get()) }
        viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
        viewModel { ProfileViewModel(get(), get(), get()) }
        viewModel { FinancialProfileViewModel(get()) }
        viewModel { EditProfileViewModel(get()) }
        viewModel { CreateTransactionViewModel(get()) }
        viewModel { TransactionHistoryViewModel(get()) }
        viewModel { TransactionDetailViewModel(get(), get()) }
        viewModel { ChatAiViewModel(get()) }
        viewModel { DashboardViewModel(get()) }
        viewModel { MainViewModel(get()) }
        viewModel { OcrViewModel(get()) }
    }
}
