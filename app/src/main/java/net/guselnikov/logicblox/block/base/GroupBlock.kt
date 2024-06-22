package net.guselnikov.logicblox.block.base

import net.guselnikov.logicblox.block.Block
import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.condition.ConditionBlock
import java.util.HashMap

/*
 * Содержит дочерние блоки и соответствие между входами и выходами дочерних блоков
 */
class GroupBlock(
    private val blocks: List<Block>,
    relations: List<BlockRelation>
) : Block() {

    companion object {
        const val PARENT_INDEX = -1
    }

    // Вычисленные значения блоков
    private val computedValues: MutableMap<Int, Map<Int, ValueType>> = mutableMapOf()

    // Множество блоков, от которых зависит блок в ключе
    private val parentsOf: MutableMap<Int, HashSet<Int>> = mutableMapOf()

    // Множество блоков, которые зависят от блока в ключе
    private val childrenOf: MutableMap<Int, HashSet<Int>> = mutableMapOf()

    private val conditionalRelations: ArrayList<BlockRelation> = arrayListOf()

    init {
        initRelations(relations)
    }

    private fun recalculateTree() {
        parentsOf.clear()
        childrenOf.clear()
        conditionalRelations.forEach { relation ->
            if (parentsOf[relation.secondBlockIndex] == null) {
                parentsOf[relation.secondBlockIndex] = hashSetOf()
            }

            if (childrenOf[relation.firstBlockIndex] == null) {
                childrenOf[relation.firstBlockIndex] = hashSetOf()
            }
        }

        conditionalRelations.forEach { relation ->
            parentsOf[relation.secondBlockIndex]?.add(relation.firstBlockIndex)
            childrenOf[relation.firstBlockIndex]?.add(relation.secondBlockIndex)
        }
    }

    private fun initRelations(relations: List<BlockRelation>) {
        conditionalRelations.clear()
        conditionalRelations.addAll(relations)

        recalculateTree()
    }

    override suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType> {
        computedValues.clear()

        // 0. Заносим в computedValues параметры родителя (parentindex)
        computedValues[PARENT_INDEX] = inputs

        // 1. Находим блоки, вычисляемые в первую очередь, которые не зависят ни от кого и вычисляем их
        while (true) {
            var computedConditions = 0
            run independentCalculation@{
                blocks.forEachIndexed { blockIndex, block ->
                    if (isIndependent(blockIndex)) {
                        val blockInputs: HashMap<Int, ValueType> = hashMapOf()
                        conditionalRelations.filter { relation ->
                            relation.secondBlockIndex == blockIndex
                        }.forEach {
                            // Вот здесь ошибка firstBlockOutputIndex не является индексом инпута для
                            // дочернего элемента
                            blockInputs[it.secondBlockInputIndex] =
                                inputs[it.firstBlockOutputIndex] ?: Undefined
                        }

                        // Если блок без условия

                        if (block is ConditionBlock) {
                            computedConditions += 1
                            block.setOnConditionEvaluatedListener { newBlockIndex ->
                                val oldIncomingRelations = conditionalRelations.filter {
                                    it.secondBlockIndex == blockIndex
                                }.toSet()

                                val oldOutcomingRelations = conditionalRelations.filter {
                                    it.firstBlockIndex == blockIndex
                                }.toSet()

                                conditionalRelations.removeAll(oldIncomingRelations)
                                conditionalRelations.removeAll(oldOutcomingRelations)

                                if (newBlockIndex != null) {
                                    val newRelations: ArrayList<BlockRelation> = arrayListOf()
                                    oldIncomingRelations.forEach { inRelation ->
                                        oldOutcomingRelations.forEach { outRelation ->
                                            if (outRelation.secondBlockIndex == newBlockIndex && inRelation.secondBlockInputIndex == outRelation.firstBlockOutputIndex) {
                                                newRelations.add(
                                                    BlockRelation(
                                                        inRelation.firstBlockIndex,
                                                        outRelation.secondBlockIndex,
                                                        inRelation.firstBlockOutputIndex,
                                                        outRelation.secondBlockInputIndex
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    conditionalRelations.addAll(newRelations)
                                }
                            }
                            block.compute(blockInputs)
                            recalculateTree()
                            return@independentCalculation
                        }

                        val blockOutputs = block.compute(blockInputs)
                        // 2. Заносим результаты в словарик computed
                        computedValues[blockIndex] = blockOutputs
                    }
                }
            }
            if (computedConditions == 0) break
        }

        // 3. Рекурсивно вычисляем прямых дочек
        // Прямые дочки могут быть не вычислены с первого прохода
        // Каждый проход отслеживаем вычисленное количество, если в какой-то проход оно нулевое, то останавливаем прохождение
        // Возврращаем только вычисленное
        var computedChildrenCount: Int
        while (true) {
            computedChildrenCount = 0
            val blocksToCompute: Set<Int> = computedValues.map { (blockIndex, _) ->
                val relationsToBlocksToCompute = conditionalRelations.filter {
                    it.firstBlockIndex == blockIndex
                }

                relationsToBlocksToCompute.map {
                    it.secondBlockIndex
                }.filter { isComputable(it) }
            }.flatten().toSet()

            run computingLoop@{
                blocksToCompute.forEach { childIndex ->
                    if (!computedValues.contains(childIndex)) {
                        val blockInputs = findInputValues(childIndex)
                        val block = blocks[childIndex]

                        if (block is ConditionBlock) {
                            block.setOnConditionEvaluatedListener { newBlockIndex ->

                                // REPLACE INCOMING RELATIONS
                                val oldIncomingRelations = conditionalRelations.filter {
                                    it.secondBlockIndex == childIndex
                                }.toSet()
                                val oldOutcomingRelations = conditionalRelations.filter {
                                    it.firstBlockIndex == childIndex
                                }.toSet()

                                conditionalRelations.removeAll(oldIncomingRelations)
                                conditionalRelations.removeAll(oldOutcomingRelations)

                                if (newBlockIndex != null) {
                                    val newRelations: ArrayList<BlockRelation> = arrayListOf()
                                    oldIncomingRelations.forEach { inRelation ->
                                        oldOutcomingRelations.forEach { outRelation ->
                                            if (outRelation.secondBlockIndex == newBlockIndex && inRelation.secondBlockInputIndex == outRelation.firstBlockOutputIndex) {
                                                newRelations.add(
                                                    BlockRelation(
                                                        inRelation.firstBlockIndex,
                                                        outRelation.secondBlockIndex,
                                                        inRelation.firstBlockOutputIndex,
                                                        outRelation.secondBlockInputIndex
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    conditionalRelations.addAll(newRelations)
                                }
                            }
                            block.compute(blockInputs)
                            computedChildrenCount += 1
                            return@computingLoop
                        }

                        computedValues[childIndex] = block.compute(blockInputs)
                        computedChildrenCount += 1
                    }
                }
            }

            if (computedChildrenCount == 0) break
        }

        return findInputValues(PARENT_INDEX)
    }

    private fun findInputValues(childIndex: Int): Map<Int, ValueType> {
        val blockInputs: HashMap<Int, ValueType> = hashMapOf()
        conditionalRelations.filter { relation ->
            relation.secondBlockIndex == childIndex
        }.forEach { relation ->
            val prevBlockOutputs = computedValues[relation.firstBlockIndex]
            if (prevBlockOutputs != null) {
                blockInputs[relation.secondBlockInputIndex] = prevBlockOutputs[relation.firstBlockOutputIndex] ?: Undefined
            }
        }

        return blockInputs
    }

    private fun isComputable(blockIndex: Int): Boolean {
        val dependencies = parentsOf[blockIndex]

        dependencies?.forEach {
            if (computedValues[it] == null) return false
        }

        return true
    }

    private fun isIndependent(blockIndex: Int): Boolean {
        val dependencies = parentsOf[blockIndex] ?: return false
        dependencies.forEach {
            if (it != PARENT_INDEX) return false
        }

        return true
    }
}

class BlockRelation(
    // Блок, результат которого подаётся на вход другому блоку или в аутпут родителя
    val firstBlockIndex: Int,
    // Блок, принимающий параметр от другого блока или из инпута родителя
    val secondBlockIndex: Int,
    // Индекс аутпута у блока, результат которого подаётся на вход другому блоку или в аутпут родителя
    val firstBlockOutputIndex: Int,
    // Индекс инпута у принимающего блока (или индекс аутпута родителя)
    val secondBlockInputIndex: Int
)
