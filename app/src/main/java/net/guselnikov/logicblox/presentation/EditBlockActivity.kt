package net.guselnikov.logicblox.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import net.guselnikov.logicblox.R
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.base.FormulaBlock
import java.lang.Exception
import java.math.BigDecimal

class EditBlockActivity : AppCompatActivity() {

    private lateinit var inputsNumberET: EditText
    private lateinit var inputsContainer: ViewGroup
    private lateinit var outputsNumberET: EditText
    private lateinit var outputsFormulaContainer: ViewGroup
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var consoleLog: TextView
    private lateinit var runButton: Button

    private var numberOfInputs: Int = 0
    private var numberOfOutputs: Int = 0

    private val viewModel: EditBlockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_block)

        layoutInflater = LayoutInflater.from(this)
        inputsNumberET = findViewById(R.id.inputs_number)
        inputsContainer = findViewById(R.id.inputs_container)
        outputsNumberET = findViewById(R.id.outputs_number)
        outputsFormulaContainer = findViewById(R.id.outputs_container)
        runButton = findViewById(R.id.button_run)
        consoleLog = findViewById(R.id.concole_log)

        inputsNumberET.setText(viewModel.inputs.size.toString())
        inputsNumberET.doOnTextChanged { text, _, _, _ ->
            val variableNames = "abcdefghijklmnopqrstqvwxyz"
            numberOfInputs = try {
                text.toString().toInt()
            } catch (e: Exception) {
                0
            }
            if (numberOfInputs > variableNames.length) numberOfInputs = variableNames.length
            createInputs(numberOfInputs, viewModel.inputs)
        }

        outputsNumberET.setText(viewModel.formulaList.size.toString())
        outputsNumberET.doOnTextChanged { text, _, _, _ ->
            val maxOutputNumber = 15
            numberOfOutputs = try {
                text.toString().toInt()
            } catch (e: Exception) {
                0
            }
            if (numberOfOutputs > maxOutputNumber) numberOfOutputs = maxOutputNumber
            createOutputs(numberOfOutputs, viewModel.formulaList)

            runButton.isVisible = numberOfOutputs > 0
        }

        runButton.setOnClickListener { run() }

        viewModel.outputs.observe(this) { values ->
            val stringBuilder = StringBuilder()
            values.forEach { (index, value) ->
                stringBuilder.append("out ${index + 1} = ")
                stringBuilder.append(value.toText())
                stringBuilder.append("\n")
            }

            consoleLog.text = stringBuilder.toString()
        }

        createInputs(viewModel.inputs.size, viewModel.inputs)
        createOutputs(viewModel.formulaList.size, viewModel.formulaList)
        runButton.isVisible = viewModel.formulaList.size > 0
    }

    private fun run() {
        val inputsNum = inputsNumberET.text.toString().toIntOrNull() ?: 0
        val outputNum = outputsNumberET.text.toString().toIntOrNull() ?: 0
        viewModel.updateData(inputsNum, outputNum)

        val formulaList = viewModel.formulaList.toSortedMap().map { (_, formula) -> formula }
        val inputs = viewModel.inputs.map { (index, pair) ->
            index to ValueDecimal(BigDecimal(pair.second))
        }.toMap()

        val inputNames = viewModel.inputs.map { (index, pair) ->
            index to pair.first
        }

        val formulaBlock = FormulaBlock(*formulaList.toTypedArray()).also {
            inputNames.forEach { (index, name) ->
                it[index] = name
            }
        }

        viewModel.runBlock(formulaBlock, inputs)
    }

    private fun createInputs(num: Int, values: Map<Int, Pair<String, String>>) {
        val variableNames = "abcdefghijklmnopqrstqvwxyz"
        val inputsNumber = if (num < variableNames.length) num else variableNames.length

        inputsContainer.removeAllViews()
        for (i in 0 until inputsNumber) {
            val inputViewGroup =
                layoutInflater.inflate(R.layout.input_name_item, inputsContainer, false)
            val inputNameET: EditText = inputViewGroup.findViewById(R.id.input_name)
            val inputDefaultValueET: EditText =
                inputViewGroup.findViewById(R.id.input_default_value)

            val input = values[i] ?: Pair(variableNames[i].toString(), "0")
            inputNameET.setText(input.first)
            inputDefaultValueET.setText(input.second)
            inputNameET.doOnTextChanged { text, _, _, _ ->
                viewModel.inputs[i] = Pair(text.toString(), inputDefaultValueET.text.toString())
            }
            inputDefaultValueET.doOnTextChanged { text, _, _, _ ->
                viewModel.inputs[i] = Pair(inputNameET.text.toString(), text.toString())
            }

            inputsContainer.addView(inputViewGroup)

            viewModel.inputs[i] = Pair(input.first, input.second)
        }
    }

    private fun createOutputs(num: Int, formulaList: Map<Int, String>) {
        outputsFormulaContainer.removeAllViews()
        for (i in 0 until num) {
            val outputViewGroup =
                layoutInflater.inflate(R.layout.output_formula_item, outputsFormulaContainer, false)
            val outputTitle: TextView = outputViewGroup.findViewById(R.id.output_title)
            val outputFormula: EditText = outputViewGroup.findViewById(R.id.output_formula)
            outputFormula.setText(formulaList[i] ?: "")

            outputFormula.doOnTextChanged { text, _, _, _ ->
                viewModel.formulaList[i] = text.toString()
            }

            outputTitle.text = "out ${i + 1} formula"
            outputsFormulaContainer.addView(outputViewGroup)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // The state is restored via the view model
    }
}