package net.guselnikov.logicblox.block.parser

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueText
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.runner.Console

sealed class TokenGroup {
    abstract fun isEmpty(): Boolean
}

class FormulaGroup(val unsortedTokens: List<Token>, val lineNumber: Int): TokenGroup() {
    val tokens: List<Token>
    val variableName: String?
    private val transformedTokens: ArrayList<Token?>
    private val actions: ArrayList<OperationAction> = arrayListOf()
    private var isImmediateUndefinedReturn: Boolean
    val shouldReturn: Boolean = unsortedTokens.contains(Return)
    val shouldContinue: Boolean = unsortedTokens.contains(Continue)
    val shouldBreak: Boolean = unsortedTokens.contains(Break)

    data class OperationAction(
        val operator: Operator,
        val indicies: List<Int>
    )

    init {
        variableName = if (unsortedTokens.getOrNull(1) == Assign) {
            val name = (unsortedTokens[0] as Word).string
            tokens = sortTokens(unsortedTokens.subList(2, unsortedTokens.size))
            name
        } else {
            tokens = sortTokens(unsortedTokens)
            null
        }

        transformedTokens = ArrayList(tokens.size)
        tokens.forEach {
            transformedTokens.add(it)
        }

        isImmediateUndefinedReturn = false

        while (transformedTokens.filterNotNull().size > 1 || transformedTokens.getOrNull(0) !is Value) {
            val indexOfOperator = transformedTokens.indexOfFirst {
                it is Operator || it is Return || it is Break || it is Continue
            }

            if (indexOfOperator < 0) {
                break
            }

            val operatorToken = transformedTokens[indexOfOperator]
            val operator = operatorToken as? Operator
            if (operator == null) {
                isImmediateUndefinedReturn = true
                break
            }
            if (indexOfOperator < operator.argumentsNumber) {
                isImmediateUndefinedReturn = true
                break
            }

            when (operator.argumentsNumber) {
                2 -> {
                    // Ищем индекс rhs
                    var rhsIndex = indexOfOperator - 1
                    for (i in 1..indexOfOperator) {
                        val candidateToken = transformedTokens[indexOfOperator - i]
                        if (candidateToken is Value || candidateToken is Word) {
                            rhsIndex = indexOfOperator - i
                            break
                        }
                    }

                    // Ищем индекс lhs
                    var lhsIndex = rhsIndex - 1
                    for (i in 1..rhsIndex) {
                        val candidateToken = transformedTokens[rhsIndex - i]
                        if (candidateToken is Value || candidateToken is Word) {
                            lhsIndex = rhsIndex - i
                            break
                        }
                    }

                    actions.add(
                        OperationAction(operator, listOf(lhsIndex, rhsIndex))
                    )

                    transformedTokens[rhsIndex] = null
                    transformedTokens[indexOfOperator] = null
                }

                1 -> {
                    var valueIndex = indexOfOperator - 1
                    for (i in 1..indexOfOperator) {
                        val candidateToken = transformedTokens[indexOfOperator - i]
                        if (candidateToken is Value || candidateToken is Word) {
                            valueIndex = indexOfOperator - i
                            break
                        }
                    }

                    actions.add(
                        OperationAction(operator, listOf(valueIndex))
                    )

                    transformedTokens[indexOfOperator] = null
                }

                0 -> {
                    actions.add(OperationAction(operator, listOf()))
                }
            }
        }
    }

    override fun isEmpty(): Boolean = tokens.isEmpty()

    suspend fun calculate(params: Map<String, ValueType>, console: Console? = null): ValueType {
        if (isImmediateUndefinedReturn) return Undefined

        tokens.forEachIndexed { index, token ->
            transformedTokens[index] = token.let {
                when {
                    it is Word -> {
                        val param = params[it.string] ?: Undefined
                        when (param) {
                            is ValueBoolean -> Bool(param.bool)
                            is ValueDecimal -> Number(param.decimal)
                            is ValueText -> Literal(param.text)
                            else -> throw IllegalArgumentException("Unknown parameter ${it.string}")
                        }
                    }
                    else -> it
                }
            }
        }

        actions.forEach { action ->
            val operatorParams = action.indicies.map { index ->
                transformedTokens[index] as Value
            }.toTypedArray()

            val newValue = action.operator.calculate(*operatorParams)
            val newValueIndex = action.indicies.getOrNull(0)
            if (newValueIndex != null) {
                transformedTokens[newValueIndex] = newValue
            }
            if (action.operator.doesPrint()) {
                console?.print(newValue)
            }
        }

        val value = (transformedTokens.getOrNull(0) as? Value) ?: Undefined
        return when (value) {
            is Number -> value.toValueNumber()
            is Bool -> ValueBoolean(value.toBoolean())
            is Literal -> ValueText(value.toText())
            else -> Undefined
        }
    }
}

class BlockGroup(val expressions: List<TokenGroup>): TokenGroup() {
    override fun isEmpty(): Boolean = expressions.isEmpty()
}

class ConditionGroup(
    val condition: FormulaGroup,
    val onTrueBlock: BlockGroup,
    val onFalseBlock: BlockGroup
): TokenGroup() {
    override fun isEmpty(): Boolean = condition.isEmpty()
}

class WhileLoopGroup(
    val condition: FormulaGroup,
    val loopBlock: BlockGroup
): TokenGroup() {
    override fun isEmpty(): Boolean = condition.isEmpty()
}

// for (i from 1 to 10 step 0.5)
class ForLoopGroup(
    val variable: String,
    val loopBlock: BlockGroup,
    val start: FormulaGroup,
    val end: FormulaGroup,
    val step: FormulaGroup?) : TokenGroup() {
    override fun isEmpty(): Boolean = loopBlock.isEmpty()
}

@Suppress("Unused")
fun printGroup(tokenGroup: TokenGroup, nesting:Int = 0): String {
    val stringBuilder = StringBuilder()
    val spacesBuilder = StringBuilder()
    for (i in 0 until nesting * 3) {
        spacesBuilder.append(" ")
    }
    val spaces = spacesBuilder.toString()

    when (tokenGroup) {
        is FormulaGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append(printTokens(tokenGroup.unsortedTokens, ""))
        }
        is BlockGroup -> {
            stringBuilder.append("{\n")
            tokenGroup.expressions.forEach { expression ->
                stringBuilder.append(printGroup(expression, nesting + 1))
                stringBuilder.append("\n")
            }
            stringBuilder.append(spaces)
            stringBuilder.append("}")
        }
        is ConditionGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("if ")
            stringBuilder.append(printTokens(tokenGroup.condition.tokens))
            stringBuilder.append(printGroup(tokenGroup.onTrueBlock, nesting + 1))
            stringBuilder.append(" else ")
            stringBuilder.append(printGroup(tokenGroup.onFalseBlock, nesting + 1))
        }

        is WhileLoopGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("while")
            stringBuilder.append(printTokens(tokenGroup.condition.tokens))
            stringBuilder.append(printGroup(tokenGroup.loopBlock), nesting + 1)
        }

        is ForLoopGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("for (")
            stringBuilder.append(tokenGroup.variable)
            stringBuilder.append(" from ")
            stringBuilder.append(printGroup(tokenGroup.start))
            stringBuilder.append(" to ")
            stringBuilder.append(printGroup(tokenGroup.end))
            val step = tokenGroup.step
            if (step != null) {
                stringBuilder.append(" step ")
                stringBuilder.append(printGroup(step))
            }
            stringBuilder.append(")")
            stringBuilder.append(printGroup(tokenGroup.loopBlock))
        }
    }

    return stringBuilder.toString()
}