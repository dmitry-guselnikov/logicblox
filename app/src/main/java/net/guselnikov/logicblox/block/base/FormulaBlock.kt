package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.parser.calculateFormula
import java.util.HashMap

class FormulaBlock(private vararg val formula: String) :
    Block() {

    private val inputNames: HashMap<Int, String> = hashMapOf()

    operator fun set(index: Int, name: String) {
        inputNames[index] = name
    }

    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val params = hashMapOf<String, ValueNumber>()
        inputs.forEach { (index, value) ->
            if (value is ValueNumber) {
                params[inputNames.getOrDefault(index, "in${index}")] = value
            }
        }

        val outputs = hashMapOf<Int, ValueType>()

        formula.forEachIndexed { index, s ->
            outputs[index] = calculateFormula(s, params)
        }

        return outputs
    }
}