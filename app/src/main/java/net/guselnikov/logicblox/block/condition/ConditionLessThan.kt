package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueType

class ConditionLessThan(private val value: ValueType) : Condition() {
    override fun invoke(vararg input: ValueType): Boolean =
        ConditionGreaterThan(input.getOrNull(0) ?: Undefined).invoke(value)
}