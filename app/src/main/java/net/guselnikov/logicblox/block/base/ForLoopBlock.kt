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
class ForLoopBlock(private val numberOfIterations: Int, private val iterationBlock: IterationBlock) : Block() {
    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        iterationBlock.loopInputs = inputs
        var iterationInputs: Map<Int, ValueType> = mapOf()
        for (i in 0 until numberOfIterations) {
            iterationInputs = iterationBlock.compute(i, iterationInputs)
        }

        return iterationInputs
    }
}