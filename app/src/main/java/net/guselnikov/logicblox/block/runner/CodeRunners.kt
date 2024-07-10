package net.guselnikov.logicblox.block.runner

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueText
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.parser.Assign
import net.guselnikov.logicblox.block.parser.BlockGroup
import net.guselnikov.logicblox.block.parser.Bool
import net.guselnikov.logicblox.block.parser.Break
import net.guselnikov.logicblox.block.parser.ConditionGroup
import net.guselnikov.logicblox.block.parser.Continue
import net.guselnikov.logicblox.block.parser.EmptyGroup
import net.guselnikov.logicblox.block.parser.ForLoopGroup
import net.guselnikov.logicblox.block.parser.FormulaGroup
import net.guselnikov.logicblox.block.parser.Literal
import net.guselnikov.logicblox.block.parser.Number
import net.guselnikov.logicblox.block.parser.Operator
import net.guselnikov.logicblox.block.parser.Return
import net.guselnikov.logicblox.block.parser.Token
import net.guselnikov.logicblox.block.parser.TokenGroup
import net.guselnikov.logicblox.block.parser.Value
import net.guselnikov.logicblox.block.parser.WhileLoopGroup
import net.guselnikov.logicblox.block.parser.Word
import net.guselnikov.logicblox.block.parser.sortTokens
import java.math.BigDecimal

abstract class Console {
    abstract fun print(str: String)
    abstract fun println(str: String)
    abstract fun print(vararg values: Value)
    abstract fun clear()
}

data class GroupResults(
    val variables: Map<String, ValueType> = mapOf(),
    val shouldReturnFlag: Boolean = false,
    val shouldBreakFlag: Boolean = false,
    val shouldContinueFlag: Boolean = false
)

data class FormulaResults(
    val variable: Pair<String?, ValueType>,
    val shouldReturnFlag: Boolean,
    val shouldBreakFlag: Boolean,
    val shouldContinueFlag: Boolean
) {
    fun toGroupResults() = GroupResults(
        variables = if (variable.first != null) mapOf(
            Pair(
                variable.first!!,
                variable.second
            )
        ) else mapOf(),
        shouldReturnFlag = shouldReturnFlag,
        shouldBreakFlag = shouldBreakFlag,
        shouldContinueFlag = shouldContinueFlag
    )
}

data class FormulaCalculationResult(
    val value: ValueType,
    val shouldReturnFlag: Boolean
)

//enum class Verbosity {
//    NONE, // Never prints
//    NORMAL, // Only prints what passed to print and println methods
//    DEBUG, // Prints everything that NORMAL and debug messages
//    DETAILED, // Prints every new variable
//    VERBOSE // Prints every evaluated statement, line numbers
//}

suspend fun runGroup(
    tokenGroup: TokenGroup,
    params: Map<String, ValueType>,
    console: Console? = null
): GroupResults {
    return when (tokenGroup) {
        is EmptyGroup -> GroupResults(mapOf(), false, false, false)
        is FormulaGroup -> {
            runFormula(tokenGroup.tokens, params, console).toGroupResults()
        }

        is BlockGroup -> {
            val blockParams = hashMapOf<String, ValueType>()
            params.forEach { (key, value) ->
                blockParams[key] = value
            }
            tokenGroup.expressions.forEach { expression ->
                val result = runGroup(expression, blockParams, console)
                result.variables.forEach { (key, value) ->
                    blockParams[key] = value
                }
                if (result.shouldReturnFlag) {
                    return GroupResults(result.variables, true, true, true)
                }
                if (result.shouldBreakFlag) {
                    return GroupResults(blockParams, false, true, true)
                }

                if (result.shouldContinueFlag) {
                    return GroupResults(blockParams, false, false, true)
                }
            }

            GroupResults(blockParams, false, false, false)
        }

        is ConditionGroup -> {
            val condition = tokenGroup.condition
            val conditionResult = runFormula(condition.tokens, params, console).variable.second
            if (conditionResult !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            if (conditionResult.toBoolean()) {
                runGroup(tokenGroup.onTrueBlock, params, console)
            } else {
                runGroup(tokenGroup.onFalseBlock, params, console)
            }
        }

        is WhileLoopGroup -> {
            val condition = tokenGroup.condition
            val loopParams: HashMap<String, ValueType> = hashMapOf()
            params.forEach { (key, value) ->
                loopParams[key] = value
            }

            while (true) {
                val conditionResult =
                    runFormula(condition.tokens, loopParams, console).variable.second
                if (conditionResult !is ValueNumber) return GroupResults(mapOf(), false)

                if (conditionResult.toBoolean()) {
                    val iterationResult = runGroup(tokenGroup.loopBlock, loopParams, console)
                    iterationResult.variables.forEach { (key, value) ->
                        loopParams[key] = value
                    }

                    if (iterationResult.shouldReturnFlag) {
                        return GroupResults(loopParams, true, true)
                    }

                    if (iterationResult.shouldBreakFlag) {
                        break
                    }
                } else {
                    break
                }
            }

            GroupResults(loopParams, false, false, false)
        }

        is ForLoopGroup -> {
            val loopParams: HashMap<String, ValueType> = hashMapOf()
            params.forEach { (key, value) ->
                loopParams[key] = value
            }

            val variableName = tokenGroup.variable

            val startValue = runFormula(tokenGroup.start.tokens, loopParams, console).variable.second
            if (startValue !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            val endValue = runFormula(tokenGroup.end.tokens, loopParams, console).variable.second
            if (endValue !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            val stepValue = runFormula(tokenGroup.step.tokens, loopParams, console).variable.second
            if (stepValue !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            var iterationValue = startValue as ValueNumber

            while (
                if (endValue.toBigDecimal() > startValue.toBigDecimal()) {
                    iterationValue.toBigDecimal() <= endValue.toBigDecimal()
                } else {
                    iterationValue.toBigDecimal() >= endValue.toBigDecimal()
                }
            ) {
                loopParams[variableName] = iterationValue
                val iterationResult = runGroup(tokenGroup.loopBlock, loopParams, console)
                iterationResult.variables.forEach { (key, value) ->
                    loopParams[key] = value
                }

                if (iterationResult.shouldReturnFlag) {
                    return GroupResults(loopParams, true, true, true)
                }

                if (iterationResult.shouldBreakFlag) {
                    break
                }

                iterationValue = ValueDecimal(iterationValue.toBigDecimal() + stepValue.toBigDecimal())
            }

            GroupResults(loopParams, false, false, false)
        }
    }
}

suspend fun runFormula(
    tokens: List<Token>,
    params: Map<String, ValueType>,
    console: Console? = null
): FormulaResults {
    return try {
        var tokensToRun = tokens
        val variableName: String? =
            if (tokens.getOrNull(1) == Assign) {
                val name = (tokensToRun[0] as Word).string
                tokensToRun = tokensToRun.subList(2, tokens.size)
                name
            } else null

        when {
            tokens.contains(Return) -> FormulaResults(Pair(null, Undefined), true, true, true)
            tokens.contains(Break) -> FormulaResults(Pair(null, Undefined), false, true, true)
            tokens.contains(Continue) -> FormulaResults(Pair(null, Undefined), false, false, true)
            else -> {
                val sortedTokens = sortTokens(tokensToRun)
                val result = runFormulaTokens(sortedTokens, params, console)

//            if (variableName == null && !tokens.contains(Print) && !tokens.contains(Println)) {
//                console?.print(printTokens(tokens))
//                console?.print("= ")
//                console?.println(result.toText())
//            }
                FormulaResults(Pair(variableName, result), false, false, false)
            }
        }
    } catch (e: Exception) {
        FormulaResults(Pair(null, Undefined), false, false, false)
    }
}

suspend fun runFormulaTokens(
    tokens: List<Token>,
    params: Map<String, ValueType>,
    console: Console? = null
): ValueType {
    // 1. Заменить words и numbers на Value
    val transformedTokens = arrayListOf<Token>()
    transformedTokens.addAll(
        tokens.map { token ->
            when {
                token is Word -> {
                    val param = params[token.string] ?: Undefined
                    when (param) {
                        is ValueBoolean -> Bool(param.bool)
                        is ValueDecimal -> Number(param.decimal)
                        is ValueText -> Literal(param.text)
                        Undefined -> Literal("undefined")
                    }
                }

                else -> token
            }
        }
    )

    while (transformedTokens.size > 1 || transformedTokens.getOrNull(0) !is Value) {
        val indexOfOperator = transformedTokens.indexOfFirst {
            it is Operator || it is Return || it is Break || it is Continue
        }

        val operatorToken = transformedTokens[indexOfOperator]
        val operator = operatorToken as? Operator ?: return Undefined
        if (indexOfOperator < operator.argumentsNumber) return Undefined

        when (operator.argumentsNumber) {
            2 -> {
                val lhs = transformedTokens[indexOfOperator - 2] as Value
                val rhs = transformedTokens[indexOfOperator - 1] as Value
                val newValue = operator.calculate(lhs, rhs)
                transformedTokens.add(indexOfOperator - 2, newValue)
                transformedTokens.remove(lhs)
                transformedTokens.remove(rhs)
            }

            1 -> {
                val value = transformedTokens[indexOfOperator - 1] as Value
                val newValue = operator.calculate(value)
                transformedTokens.add(indexOfOperator - 1, newValue)
                transformedTokens.remove(value)

                if (operator.doesPrint()) {
                    console?.print(newValue)
                }
            }

            0 -> {
                transformedTokens.add(indexOfOperator, operator.calculate())
            }
        }

        transformedTokens.remove(operator)
    }

    val value = (transformedTokens.getOrNull(0) as? Value) ?: Undefined
    return when (value) {
        is Number -> value.toValueNumber()
        is Bool -> ValueBoolean(value.toBoolean())
        is Literal -> ValueText(value.toText())
        else -> Undefined
    }
}