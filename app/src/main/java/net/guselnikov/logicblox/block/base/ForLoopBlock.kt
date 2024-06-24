package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.ValueType

/**
 * For loop всегда передает в дочерние элементы номер итерации
 *
 * Принимаемый параметр block должен быть наследником класса IterationBlock,
 * IterationBlock уже знает, как обращаться с номером итерации, принимает
 * родительские инпуты, а на вход метода compute ожидает выходные значения предыдущей итерации
 */
abstract class LoopBlock(private val iterationBlock: IterationBlock) : Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        iterationBlock.loopInputs = inputs
        var iterationInputs: Map<Int, ValueType> = mapOf()
        var index = 0
        while (true) {
            iterationInputs = iterationBlock.compute(index, iterationInputs)
            if (iterationBlock.shouldBreak) break
            index++
        }

        return iterationInputs
    }
}