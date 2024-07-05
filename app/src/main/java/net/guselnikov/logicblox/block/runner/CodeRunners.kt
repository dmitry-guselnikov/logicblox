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
import net.guselnikov.logicblox.block.parser.ConditionGroup
import net.guselnikov.logicblox.block.parser.EmptyGroup
import net.guselnikov.logicblox.block.parser.FormulaGroup
import net.guselnikov.logicblox.block.parser.Literal
import net.guselnikov.logicblox.block.parser.Number
import net.guselnikov.logicblox.block.parser.Operator
import net.guselnikov.logicblox.block.parser.Token
import net.guselnikov.logicblox.block.parser.TokenGroup
import net.guselnikov.logicblox.block.parser.Value
import net.guselnikov.logicblox.block.parser.Word
import net.guselnikov.logicblox.block.parser.sortTokens

abstract class Console {
    abstract fun print(vararg values: Value)
    abstract fun clear()
}

//enum class Verbosity {
//    NONE, // Never prints
//    NORMAL, // Only prints what passed to print and println methods
//    DEBUG, // Prints everything that NORMAL and debug messages
//    DETAILED, // Prints every new variable
//    VERBOSE // Prints every evaluated statement, line numbers
//}

fun runGroup(tokenGroup: TokenGroup, params: Map<String, ValueType>, console: Console? = null): Map<String?, ValueType> {
    return when (tokenGroup) {
        is EmptyGroup -> mapOf(Pair(null, Undefined))
        is FormulaGroup -> mapOf(runFormula(tokenGroup.tokens, params, console))
        is BlockGroup -> {
            val blockParams = hashMapOf<String, ValueType>()
            val ret = hashMapOf<String?, ValueType>()
            params.forEach { (key, value) ->
                blockParams[key] = value
            }
            tokenGroup.expressions.forEach { formulaTokens ->
                val result = runGroup(formulaTokens, blockParams, console)
                result.forEach { (key, value) ->
                    if (key != null) blockParams[key] = value
                }
            }

            blockParams.forEach { (key, value) ->
                ret[key] = value
            }

            ret
        }
        is ConditionGroup -> {
            val condition = tokenGroup.condition
            val conditionResult = runFormula(condition.tokens, params, console).second
            if (conditionResult !is ValueNumber) return mapOf()

            if (conditionResult.toBoolean()) {
                runGroup(tokenGroup.onTrueBlock, params, console)
            } else {
                runGroup(tokenGroup.onFalseBlock, params, console)
            }
        }
    }
}

fun runFormula(
    tokens: List<Token>,
    params: Map<String, ValueType>,
    console: Console? = null
): Pair<String?, ValueType> {
    return try {
        var tokensToRun = tokens
        val variableName: String? =
            if (tokens.getOrNull(1) == Assign) {
                val name = (tokensToRun[0] as Word).string
                tokensToRun = tokensToRun.subList(2, tokens.size)
                name
            } else null

        val sortedTokens = sortTokens(tokensToRun)
        Pair(variableName, runFormulaTokens(sortedTokens, params, console))
    } catch (e: Exception) {
        Pair(null, Undefined)
    }
}

fun runFormulaTokens(tokens: List<Token>, params: Map<String, ValueType>, console: Console? = null): ValueType {
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
            it is Operator
        }
        val operator = transformedTokens[indexOfOperator] as Operator
        if (indexOfOperator < operator.argumentsNumber) return Undefined // For binary [-1] Not found, [0],[1] must be numbers

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

    return (transformedTokens.getOrNull(0) as? Value)?.toValueNumber() ?: Undefined
}