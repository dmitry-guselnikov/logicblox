package net.guselnikov.logicblox.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.guselnikov.logicblox.block.parser.Value
import net.guselnikov.logicblox.block.parser.parseCode
import net.guselnikov.logicblox.block.runner.Console
import net.guselnikov.logicblox.block.runner.runGroup
import net.guselnikov.logicblox.datasource.SnippetsDataSource

class EditCodeViewModel(private val snippetsDataSource: SnippetsDataSource) : ViewModel() {

    private val _savedFlag = MutableLiveData(false)
    val savedFlag: LiveData<Boolean> = _savedFlag

    private val _consoleMessage = MutableLiveData<String?>(null)
    val consoleMessage: LiveData<String?> = _consoleMessage

    private val console: Console

    init {
        console = LiveDataConsole(this)
    }

    private var lastSavedMillis = 0L

    companion object {
        const val snippetId = "0"
        const val timeout = 4000L
    }

    fun sendToLiveData(str: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _consoleMessage.value = str
            }
        }
    }

    class LiveDataConsole(val vm: EditCodeViewModel) : Console() {
        override fun print(str: String) {
            vm.sendToLiveData(str)
        }

        override fun print(vararg values: Value) {
            values.forEach {
                vm.sendToLiveData(it.toText())
            }
        }

        override fun println(str: String) {
            vm.sendToLiveData(str + "\n")
        }

        override fun clear() {
            vm.sendToLiveData(null)
        }
    }

    fun setNotSaved() {
        _savedFlag.value = false
    }

    fun saveCode(code: String) {
        snippetsDataSource.saveCodeSnippet(snippetId, code)
        _savedFlag.value = true
    }

    fun getCode(): String = snippetsDataSource.getCodeSnippet(snippetId)

    fun runCode(code: String) {
        viewModelScope.launch {
            val beginning = System.currentTimeMillis()
            _consoleMessage.value = "Parsing code...\n"
            val blockGroup = withContext(Dispatchers.IO) {
                parseCode(code)
            }
            val codeParsedTime = System.currentTimeMillis()
            _consoleMessage.value = "Code parsed in ${codeParsedTime - beginning} ms\n"
            withContext(Dispatchers.IO) {
                runGroup(blockGroup, mapOf(), console)
            }
            val end = System.currentTimeMillis()
            _consoleMessage.value = "\nExecution finished in ${end - codeParsedTime} ms"
        }
    }
}