package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueType

class ConditionLess : Condition() {
    override fun invoke(vararg input: ValueType): Boolean = ConditionGreater(
    ).invoke(input.getOrNull(1) ?: Undefined, input.getOrNull(0) ?: Undefined)
}