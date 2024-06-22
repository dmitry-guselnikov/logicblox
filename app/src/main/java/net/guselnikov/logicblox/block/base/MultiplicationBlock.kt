package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.Multiplication
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueType
import java.math.BigDecimal

class MultiplicationBlock(private val initialFactor: ValueType = ValueDecimal(BigDecimal.ONE)) :
    Block() {

    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val factors = arrayListOf<ValueType>()
        factors.add(initialFactor)
        factors.addAll(inputs.map { (_, value) -> value })
        val operation = Multiplication(factors)
        return mapOf(Pair(0, operation()))
    }
}