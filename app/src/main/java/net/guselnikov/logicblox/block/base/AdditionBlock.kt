package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Addition
import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType

class AdditionBlock: Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val operation = Addition(inputs.map { (_, value) -> value })
        return mapOf(Pair(0, operation()))
    }
}