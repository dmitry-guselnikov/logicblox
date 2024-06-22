package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.condition.ConditionBlock

// Блок, у которого внутренние блоки выполняются последовательно
// Аутпуты каждого блока передаются в инпуты следующему блоку
// Аутпуты последнего блока = аутпуты родителя
class LinearBlock(private vararg val blocks: Block): Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        val blocksList = blocks.toList()
        var iterator = blocksList.listIterator()
        var blockInputs = inputs
        while (iterator.hasNext()) {
            val block = iterator.next()
            if (block is ConditionBlock) {
                block.setOnConditionEvaluatedListener {
                    if (it != null && it >= 0 && it <= blocksList.lastIndex) {
                        iterator = blocksList.listIterator(it)
                    }
                }
            }
            blockInputs = block.compute(blockInputs)
            if (block.isFinal) break
        }
        return blockInputs
    }
}