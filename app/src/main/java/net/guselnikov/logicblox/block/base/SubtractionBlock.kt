package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Addition
import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType

class SubtractionBlock: Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val undefinedResult = mapOf(Pair(0, Undefined))

        val minuend = inputs.getOrDefault(0, Undefined)
        val subtrahend = inputs.getOrDefault(1, Undefined)

        if (subtrahend is ValueNumber && minuend is ValueNumber) {
            val operation = Addition(listOf(minuend, subtrahend.negative()))
            return mapOf(Pair(0, operation()))
        }

        return undefinedResult
    }
}