package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.ValueType

class ConditionAnd(private vararg val conditions: Condition): Condition() {
    override fun invoke(vararg input: ValueType): Boolean {
        conditions.forEach { if (!it.invoke(*input)) return false }
        return true
    }
}