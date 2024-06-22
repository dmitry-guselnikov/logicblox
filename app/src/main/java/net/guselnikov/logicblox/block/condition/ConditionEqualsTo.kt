package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueText
import net.guselnikov.logicblox.block.ValueType
import kotlin.math.abs

class ConditionEqualsTo(private val value: ValueType) : Condition() {
    override fun invoke(vararg input: ValueType): Boolean {
        val lhs = input.getOrNull(0) ?: Undefined
        val rhs = value

        return when {
            lhs is ValueNumber && rhs is ValueNumber -> {
                abs(lhs.toDouble() - rhs.toDouble()) < 0.0000001
            }

            lhs is ValueText && rhs is ValueText -> {
                lhs.text == rhs.text
            }

            else -> false
        }
    }
}