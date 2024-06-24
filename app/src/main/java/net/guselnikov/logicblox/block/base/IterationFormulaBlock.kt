package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.ValueType

class IterationFormulaBlock: IterationBlock() {
    override suspend fun compute(
        iteration: Int,
        iterationInputs: Map<Int, ValueType>
    ): Map<Int, ValueType> {

        TODO("Not yet implemented")
    }
}