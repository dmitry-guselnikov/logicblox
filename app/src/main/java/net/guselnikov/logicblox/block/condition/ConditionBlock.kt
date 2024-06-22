package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType

/*
 * Блок «Условие»
 */
class ConditionBlock(
    private val condition: Condition,
    private val blockOnTrue: Int? = null,
    private val blockOnFalse: Int? = null,
    private val inputIndicies: List<Int>? = null
) : Block() {

    private var onConditionEvaluatedListener: ((Int?) -> Unit)? = null

    fun setOnConditionEvaluatedListener(l: (Int?) -> Unit) {
        onConditionEvaluatedListener = l
    }

    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val inputsList = inputIndicies?.mapNotNull { inputs[it] }?.toTypedArray()
            ?: inputs.map { (_, value) -> value }.toTypedArray()

        onConditionEvaluatedListener?.invoke(if (condition(*inputsList)) blockOnTrue else blockOnFalse)
        return inputs
    }
}