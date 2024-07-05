package net.guselnikov.logicblox.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.guselnikov.logicblox.block.parser.TokenGroup
import net.guselnikov.logicblox.block.runner.runGroup
import net.guselnikov.logicblox.datasource.SnippetsDataSource

class EditCodeViewModel(private val snippetsDataSource: SnippetsDataSource) : ViewModel() {

    private val _savedFlag = MutableLiveData(false)
    val savedFlag: LiveData<Boolean> = _savedFlag

    private val _consoleText = MutableLiveData("")
    val consoleText: LiveData<String> = _consoleText

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