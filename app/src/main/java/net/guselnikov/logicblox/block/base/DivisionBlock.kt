package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.Multiplication
import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType

class DivisionBlock : Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val undefinedResult = mapOf(Pair(0, Undefined))

        val dividend = inputs.getOrDefault(0, Undefined)
        val divisor = inputs.getOrDefault(1, Undefined)

        if (dividend is ValueNumber && divisor is ValueNumber) {
            val reversedDivisor = divisor.reversed()
            if (reversedDivisor is ValueNumber) {
                val operation = Multiplication(listOf(dividend, reversedDivisor))
                return mapOf(Pair(0, operation()))
            }
        }

        return undefinedResult
    }
}