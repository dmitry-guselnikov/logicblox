package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.parser.calculateFormula
import java.math.BigDecimal
import java.util.HashMap

class FormulaCondition(private val formula: String) : Condition() {
    private val inputNames: HashMap<Int, String> = hashMapOf()

    operator fun set(index: Int, name: String) {
        inputNames[index] = name
    }

    override fun invoke(vararg input: ValueType): Boolean {
        val params = hashMapOf<String, ValueNumber>()
        input.forEachIndexed { index, valueType ->
            if (valueType is ValueNumber) {
                params[inputNames.getOrDefault(index, "in${index}")] = valueType
            } else {
                params[inputNames.getOrDefault(index, "in${index}")] = ValueDecimal(BigDecimal.ZERO)
            }
        }

        val result = calculateFormula(formula, params)
        return if (result is ValueNumber) result.toBoolean() else false
    }
}