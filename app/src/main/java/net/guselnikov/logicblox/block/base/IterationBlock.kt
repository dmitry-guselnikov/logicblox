package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import java.math.BigDecimal

abstract class IterationBlock: Block() {

    companion object {
        const val ITERATION_INDEX = -1
    }

    var loopInputs: Map<Int, ValueType> = mapOf()

    var forceBreak: Boolean = false

    abstract suspend fun compute(iteration: Int, iterationInputs: Map<Int, ValueType>): Map<Int, ValueType>

    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val iterationIndexValueType = inputs.getOrDefault(ITERATION_INDEX, ValueDecimal(BigDecimal.ZERO))
        val iterationIndex = if (iterationIndexValueType is ValueNumber) iterationIndexValueType.toLong() else 0L
        return compute(iterationIndex.toInt(), inputs)
    }
}