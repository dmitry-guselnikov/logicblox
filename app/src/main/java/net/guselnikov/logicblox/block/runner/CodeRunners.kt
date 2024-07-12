package net.guselnikov.logicblox.block.runner

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.block.parser.BlockGroup
import net.guselnikov.logicblox.block.parser.ConditionGroup
import net.guselnikov.logicblox.block.parser.ForLoopGroup
import net.guselnikov.logicblox.block.parser.FormulaGroup
import net.guselnikov.logicblox.block.parser.TokenGroup
import net.guselnikov.logicblox.block.parser.Value
import net.guselnikov.logicblox.block.parser.WhileLoopGroup
import net.guselnikov.logicblox.block.parser.printGroup
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
    val shouldReturnFlag: Boolean = false,
    val shouldBreakFlag: Boolean = false,
    val shouldContinueFlag: Boolean = false
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
        is FormulaGroup -> {
            runFormula(tokenGroup, params, console).toGroupResults()
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
            val conditionResult = runFormula(condition, params, console).variable.second
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
                    runFormula(condition, loopParams, console).variable.second
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

            val startValue =
                runFormula(tokenGroup.start, loopParams, console).variable.second
            if (startValue !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            val endValue = runFormula(tokenGroup.end, loopParams, console).variable.second
            if (endValue !is ValueNumber) return GroupResults(mapOf(), false, false, false)

            val stepFormula = tokenGroup.step
            val stepValue: ValueNumber = when {
                stepFormula != null -> runFormula(
                    stepFormula,
                    loopParams,
                    console
                ).variable.second as? ValueNumber ?: ValueDecimal(BigDecimal.ONE)
                endValue.toBigDecimal() > startValue.toBigDecimal() -> ValueDecimal(BigDecimal.ONE)
                else -> ValueDecimal(BigDecimal(-1))
            }

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

                iterationValue =
                    ValueDecimal(iterationValue.toBigDecimal() + stepValue.toBigDecimal())
            }

            GroupResults(loopParams, false, false, false)
        }
    }
}

suspend fun runFormula(
    formulaGroup: FormulaGroup,
    params: Map<String, ValueType>,
    console: Console? = null
): FormulaResults {
    return try {
        when {
            formulaGroup.shouldReturn -> FormulaResults(Pair(null, Undefined), true, true, true)
            formulaGroup.shouldBreak -> FormulaResults(Pair(null, Undefined), false, true, true)
            formulaGroup.shouldContinue -> FormulaResults(Pair(null, Undefined), false, false, true)
            else -> {
                val result = formulaGroup.calculate(params, console)
                FormulaResults(Pair(formulaGroup.variableName, result))
            }
        }
    } catch (e: Exception) {
        console?.println("${e.message} in \"${printGroup(formulaGroup)}\" at line ${formulaGroup.lineNumber} ")
        FormulaResults(Pair(null, Undefined))
    }
}