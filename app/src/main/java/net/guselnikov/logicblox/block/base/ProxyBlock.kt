package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType

class ProxyBlock(private val outputs: Map<Int, ValueType>): Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        return outputs
    }
}