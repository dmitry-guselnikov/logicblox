package net.guselnikov.logicblox.di

import net.guselnikov.logicblox.datasource.PreferencesSnippetsDataSource
import net.guselnikov.logicblox.datasource.SnippetsDataSource
import net.guselnikov.logicblox.presentation.EditCodeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<SnippetsDataSource> { PreferencesSnippetsDataSource(androidContext()) }
    viewModel { EditCodeViewModel(get()) }
}