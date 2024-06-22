package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.Power
import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueType
import java.math.BigDecimal

class SquareRootBlock : Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val operation = Power(listOf(inputs.getOrDefault(0, Undefined), ValueDecimal(BigDecimal("0.5"))))
        return mapOf(Pair(0, operation()))
    }
}