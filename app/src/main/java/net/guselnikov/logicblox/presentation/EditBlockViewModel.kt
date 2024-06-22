package net.guselnikov.logicblox.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType

class EditBlockViewModel : ViewModel() {

    private val _outputs = MutableLiveData<Map<Int, ValueType>>(mapOf())
    val outputs: LiveData<Map<Int, ValueType>> = _outputs

    val inputs: HashMap<Int, Pair<String, String>> = hashMapOf()
    val formulaList: HashMap<Int, String> = hashMapOf()

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    fun updateData(inputsNum: Int, outpursNum: Int) {
        val toDeleteInputs = inputs.filter { (key, _) -> key >= inputsNum }.map { (key, _) -> key }
        val toDeleteOutputs = formulaList.filter { (key, _) -> key >= outpursNum }.map { (key, _) -> key }

        toDeleteInputs.forEach { inputs.remove(it) }
        toDeleteOutputs.forEach { formulaList.remove(it) }
    }

    fun runBlock(block: Block, inputs: Map<Int, ValueType>) {
        _outputs.value = mapOf()

        viewModelScope.launch(coroutineExceptionHandler) {
            val values = block.compute(inputs)
            if (values.isNotEmpty()) {
                _outputs.value = values
            }
        }
    }
}