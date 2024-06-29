package net.guselnikov.logicblox.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.guselnikov.logicblox.datasource.SnippetsDataSource

class EditCodeViewModel(private val snippetsDataSource: SnippetsDataSource) : ViewModel() {

    private val _savedFlag = MutableLiveData(false)
    val savedFlag: LiveData<Boolean> = _savedFlag

    private var lastSavedMillis = 0L

    companion object {
        const val snippetId = "0"
        const val timeout = 4000L
    }

    fun setNotSaved() {
        _savedFlag.value = false
    }

    fun saveCode(code: String) {
        snippetsDataSource.saveCodeSnippet(snippetId, code)
        _savedFlag.value = true
    }

    fun getCode(): String = snippetsDataSource.getCodeSnippet(snippetId)
}