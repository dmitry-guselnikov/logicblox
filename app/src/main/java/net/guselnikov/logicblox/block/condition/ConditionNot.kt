package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.ValueType

class ConditionNot(private val condition: Condition): Condition() {
    override fun invoke(vararg input: ValueType): Boolean = !condition(*input)
}