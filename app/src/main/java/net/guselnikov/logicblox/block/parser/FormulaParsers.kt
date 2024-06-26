package net.guselnikov.logicblox.block.parser

import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import java.math.BigDecimal
import java.util.Stack
import kotlin.Exception
import kotlin.text.StringBuilder

private val supportedOperators: List<Operator> = listOf(
    Or, And, Plus, Minus, Div, Mult, Sqrt, Pow, LessOrEqual, GreaterOrEqual, Less, Greater, Equals, NotEquals, Mod, Sin, Cos, Tan, Abs, Ln, Lg, ToInt, Rand
)
private val operationStrings = supportedOperators.map { it.symbols }.flatten().toTypedArray()

private fun String.startsWithOneOf(vararg substring: String): String? {
    substring.forEach {
        if (startsWith(prefix = it, ignoreCase = true)) return it
    }
    return null
}

private fun parse(formula: String): List<Token> {
    // Parse the given string to tokens
    val tokens = arrayListOf<Token>()
    var readingNumber = false
    var readingWord = false
    val currentNumber = StringBuilder()
    val currentWord = StringBuilder()

    fun writeWord() {
        readingWord = false
        when (val word = currentWord.toString()) {
            "π" -> tokens.add(Number(BigDecimal("3.1415926535897932384626433832795")))
            "true" -> tokens.add(Bool(true))
            "false" -> tokens.add(Bool(false))
            else -> tokens.add(Word(word))
        }
        currentWord.clear()
    }

    fun stopReadings() {
        if (readingWord) {
            writeWord()
        }

        if (readingNumber) {
            readingNumber = false
            tokens.add(Number(BigDecimal(currentNumber.toString())))
            currentNumber.clear()
        }
    }

    var symbolsToSkip = 0

    formula.forEachIndexed { index, symbol ->
        if (symbolsToSkip > 0) {
            symbolsToSkip--
            return@forEachIndexed
        }

        val operatorStr: String? = formula.slice(index..formula.lastIndex).startsWithOneOf(*operationStrings)

        when {
            operatorStr != null -> {
                symbolsToSkip = operatorStr.length - 1

                stopReadings()

                val lastToken = tokens.lastOrNull()
                val currentOperator = operatorStr.toOperator()
                when {
                    symbol == '-' && (tokens.isEmpty() || lastToken is Operator || lastToken == LeftBracket) -> tokens.add(UnaryMinus)
                    currentOperator != null -> tokens.add(currentOperator)
                }
            }

            symbol == '(' -> {
                stopReadings()
                tokens.add(LeftBracket)
            }

            symbol == ')' -> {
                stopReadings()
                tokens.add(RightBracket)
            }

            symbol == '=' -> {
                val variableName = currentWord.toString()
                if (tokens.isNotEmpty() || readingNumber || !readingWord || variableName.isBlank()) return listOf()
                writeWord()
                tokens.add(Assign)

                readingWord = false
                currentWord.clear()
            }

            symbol == '.' -> {
                if (!readingNumber) return listOf()
                currentNumber.append(symbol)
            }

            symbol.isDigit() -> {
                if (readingWord) currentWord.append(symbol)
                else {
                    readingNumber = true
                    currentNumber.append(symbol)
                }
            }

            symbol.isLetter() -> {
                if (readingNumber) return listOf()
                readingWord = true
                currentWord.append(symbol)
            }
        }
    }

    if (readingWord) {
        writeWord()
    }

    if (readingNumber) {
        tokens.add(Number(BigDecimal(currentNumber.toString())))
    }

    return tokens
}

private fun sortTokens(tokens: List<Token>): List<Token> {
    val outputQueue = arrayListOf<Token>()
    val operatorsStack = Stack<Token>()
    tokens.forEach { token ->
        when (token) {
            is Value, is Word -> outputQueue.add(token)
            is LeftBracket -> {
                operatorsStack.push(token)
            }

            is RightBracket -> {
                var leftBracketToPop = false
                while (!leftBracketToPop) {
                    val topStackOperator = try {
                        operatorsStack.peek()
                    } catch (e: Exception) {
                        return listOf()
                    }
                    leftBracketToPop = if (topStackOperator == null) true
                    else topStackOperator is LeftBracket

                    if (!leftBracketToPop) {
                        outputQueue.add(operatorsStack.pop())
                    } else {
                        try {
                            operatorsStack.pop()
                        } catch (e: Exception) {
                            return listOf()
                        }
                    }
                }
            }

            is Operator -> {
                var thereIsHigherPrecedence = true
                while (thereIsHigherPrecedence) {
                    val topStackOperator = try {
                        operatorsStack.peek()
                    } catch (e: Exception) {
                        null
                    }
                    thereIsHigherPrecedence = if (topStackOperator == null || topStackOperator !is Operator) false
                    else {
                        if (token.precedence <= 3) topStackOperator.precedence >= token.precedence
                        else topStackOperator.precedence > token.precedence
                    }

                    if (thereIsHigherPrecedence) {
                        outputQueue.add(operatorsStack.pop())
                    }
                }
                if (token.isRightHand) {
                    outputQueue.add(token)
                } else {
                    operatorsStack.push(token)
                }
            }

            else -> Unit
        }
    }

    while (operatorsStack.isNotEmpty()) {
        outputQueue.add(operatorsStack.pop())
    }

    return outputQueue
}

fun calculateTokens(tokens: List<Token>, params: Map<String, ValueNumber>): ValueType {
    // 1. Заменить words и numbers на ValueReal
    val transformedTokens = arrayListOf<Token>()
    transformedTokens.addAll(
        tokens.map { token ->
            when {
                token is Word -> {
                    val decimal = params[token.string]?.toBigDecimal() ?: return Undefined
                    Number(decimal)
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
        if (indexOfOperator < operator.argumentsNumber) return Undefined // [-1] Not found, [0],[1] must be numbers
        // Это справедливо только для бинарных операторов, нужно сделать поддержку унарных
        // типа квадратного корня, синуса, косинуса, тангенса

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
            }

            0 -> {
                transformedTokens.add(indexOfOperator, operator.calculate())
            }
        }

        transformedTokens.remove(operator)
    }

    return (transformedTokens.getOrNull(0) as? Value)?.toValueNumber() ?: Undefined
}

@Suppress("Unused")
fun printTokens(tokens: List<Token>): String {
    val builder = StringBuilder()
    tokens.forEach {
        when (it) {
            is Operator -> builder.append(it.symbols.firstOrNull() ?: "?")
            is Value -> builder.append(it.toDouble())
            is Word -> builder.append(it.string)
            LeftBracket -> builder.append('(')
            RightBracket -> builder.append(')')
            Assign -> builder.append('=')
            BlockStart -> builder.append('{')
            BlockEnd -> builder.append('}')
            Else -> builder.append("else")
            If -> builder.append("if")
            Return -> builder.append("return")
            NewLine -> builder.append("\n")
            is Literal -> builder.append('\"', it.string, '\"')
            Print -> builder.append("print")
        }

        if (it != NewLine) builder.append(" ")
    }

    return builder.toString()
}

fun calculateFormula(formula: String, params: Map<String, ValueNumber>): Pair<String?, ValueType> =
    try {
        var tokens = parse(formula)
        val variableName: String? =
            if (tokens.getOrNull(1) == Assign) {
                val name = (tokens[0] as Word).string
                tokens = tokens.subList(2, tokens.size)
                name
            } else null

        val sortedTokens = sortTokens(tokens)
        Pair(variableName, calculateTokens(sortedTokens, params))
    } catch (e: Exception) {
        Pair(null, Undefined)
    }

fun String.toOperator(): Operator? {
    supportedOperators.forEach {
        if (it.symbols.contains(lowercase())) return it
    }
    return null
}