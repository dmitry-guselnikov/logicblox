package net.guselnikov.logicblox.block.parser


import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.util.cosBigDecimal
import net.guselnikov.logicblox.util.lgBigDecimal
import net.guselnikov.logicblox.util.lnBigDecimal
import net.guselnikov.logicblox.util.pow
import net.guselnikov.logicblox.util.roundToInt
import net.guselnikov.logicblox.util.sinBigDecimal
import net.guselnikov.logicblox.util.tanBigDecimal
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Stack
import kotlin.Exception
import kotlin.text.StringBuilder

private fun <T> T.oneOf(vararg candidates: T): Boolean {
    candidates.forEach {
        if (it == this) return true
    }

    return false
}

private fun String.startsWithOneOf(vararg substring: String): String? {
    substring.forEach {
        if (startsWith(prefix = it, ignoreCase = true)) return it
    }
    return null
}

// TODO: Добавить операции со строками
// TODO: Добавить опеации с массивами
enum class OperatorType {
    PLUS,
    MINUS,
    LESS,
    GREATER,
    MULT,
    DIV,
    POW,
    MOD,
    UNARY_MINUS,
    SQRT,
    SIN,
    COS,
    TAN,
    LN,
    LG,
    INT,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    EQUALS,
    NOT_EQUALS,
    OR,
    AND,
    FACTORIAL,
    DEGREES,
    RAND
}

sealed class Token
class Operator(val operation: OperatorType) : Token() {

    val isRightHand: Boolean
        get() = when (operation) {
            OperatorType.FACTORIAL, OperatorType.DEGREES -> true
            else -> false
        }

    val precedence: Int
        get() = when (operation) {
            OperatorType.OR, OperatorType.AND -> 0
            OperatorType.LESS, OperatorType.GREATER, OperatorType.EQUALS, OperatorType.NOT_EQUALS -> 1
            OperatorType.MOD, OperatorType.PLUS, OperatorType.MINUS -> 2
            OperatorType.MULT, OperatorType.DIV -> 3
            OperatorType.POW -> 4
            OperatorType.UNARY_MINUS, OperatorType.SQRT, OperatorType.SIN, OperatorType.COS, OperatorType.TAN, OperatorType.LN, OperatorType.LG, OperatorType.INT -> 5
            OperatorType.FACTORIAL, OperatorType.DEGREES -> 6
            OperatorType.RAND -> 7
            else -> -1
        }

    val argumentsNumber: Int
        get() = when (operation) {
            OperatorType.OR, OperatorType.AND, OperatorType.LESS, OperatorType.EQUALS, OperatorType.NOT_EQUALS, OperatorType.GREATER, OperatorType.PLUS, OperatorType.MINUS, OperatorType.MOD, OperatorType.MULT, OperatorType.DIV, OperatorType.POW -> 2
            OperatorType.RAND -> 0
            else -> 1
        }
}

sealed class Value : Token() {
    abstract fun toValueNumber(): ValueNumber
    abstract fun toDecimal(): BigDecimal
    abstract fun toDouble(): Double
    abstract fun toBoolean(): Boolean
}

class Number(val decimal: BigDecimal) : Value() {
    override fun toValueNumber() = ValueDecimal(decimal)
    override fun toDecimal(): BigDecimal = decimal
    override fun toDouble(): Double = decimal.toDouble()
    override fun toBoolean(): Boolean = decimal != BigDecimal.ZERO
}

class Bool(val bool: Boolean) : Value() {
    override fun toValueNumber(): ValueNumber = ValueBoolean(bool)
    override fun toDecimal(): BigDecimal = if (bool) BigDecimal.ONE else BigDecimal.ZERO
    override fun toDouble(): Double = if (bool) 1.0 else 0.0
    override fun toBoolean(): Boolean = bool
}

class Word(val string: String) : Token()

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

    var symbolsToSkip = 0

    formula.forEachIndexed { index, it ->
        if (symbolsToSkip > 0) {
            symbolsToSkip--
            return@forEachIndexed
        }

        val operatorStr: String? = formula.slice(index..formula.lastIndex).startsWithOneOf(
            "√",
            "+",
            "-",
            "**",
            "*",
            "/",
            "^",
            "(",
            ")",
            "<",
            ">",
            "==",
            "!=",
            "!",
            "||",
            "&&",
            "°",
            "•",
            "×",
            "÷",
            ":",
            "%",
            "mod",
            "sin",
            "cos",
            "tg",
            "tan",
            "ln",
            "lg",
            "int",
            "rand"
        )

        when {
            operatorStr != null -> {
                symbolsToSkip = operatorStr.length - 1

                if (readingWord) {
                    writeWord()
                }

                if (readingNumber) {
                    readingNumber = false
                    tokens.add(Number(BigDecimal(currentNumber.toString())))
                    currentNumber.clear()
                }

                val lastOperator = tokens.lastOrNull() as? Operator
                val currentOperator = operatorStr.toOperatorType()
                when {
                    it == '-' && (tokens.isEmpty() || (lastOperator is Operator && lastOperator.operation != OperatorType.RIGHT_BRACKET)) -> tokens.add(
                        Operator(OperatorType.UNARY_MINUS)
                    )

                    else -> currentOperator?.let { operation ->
                        tokens.add(Operator(operation))
                    }
                }

                currentWord.clear()
            }

            it == '.' -> {
                if (!readingNumber) return listOf()
                currentNumber.append(it)
            }

            it.isDigit() -> {
                if (readingWord) currentWord.append(it)
                else {
                    readingNumber = true
                    currentNumber.append(it)
                }
            }

            it.isLetter() -> {
                if (readingNumber) return listOf()
                readingWord = true
                currentWord.append(it)
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

/*
 * 1.  While there are tokens to be read:
 * 2.        Read a token
 * 3.        If it's a number add it to queue
 * 4.        If it's an operator
 * 5.               While there's an operator on the top of the stack with greater (OR EQUAL?!) precedence:
 * 6.                       Pop operators from the stack onto the output queue
 * 7.               Push the current operator onto the stack
 * 8.        If it's a left bracket push it onto the stack
 * 9.        If it's a right bracket
 * 10.            While there's not a left bracket at the top of the stack:
 * 11.                     Pop operators from the stack onto the output queue.
 * 12.             Pop the left bracket from the stack and discard it
 * 13. While there are operators on the stack, pop them to the queue
 *
 * TODO: Добавить проверку на валидность
 */
private fun sortTokens(tokens: List<Token>): List<Token> {
    val outputQueue = arrayListOf<Token>()
    val operatorsStack = Stack<Operator>()
    tokens.forEach { token ->
        when {
            token is Value || token is Word -> outputQueue.add(token)
            token is Operator && token.operation == OperatorType.LEFT_BRACKET -> {
                operatorsStack.push(token)
            }
            token is Operator && token.operation == OperatorType.RIGHT_BRACKET -> {
                var leftBracketToPop = false
                while (!leftBracketToPop) {
                    val topStackOperator: Operator? = try {
                        operatorsStack.peek()
                    } catch (e: Exception) {
                        return listOf()
                    }
                    leftBracketToPop = if (topStackOperator == null) true
                    else topStackOperator.operation == OperatorType.LEFT_BRACKET

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
            token is Operator -> {
                var thereIsHigherPrecedence = true
                while (thereIsHigherPrecedence) {
                    val topStackOperator: Operator? = try {
                        operatorsStack.peek()
                    } catch (e: Exception) {
                        null
                    }
                    thereIsHigherPrecedence = if (topStackOperator == null) false
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



    while (transformedTokens.size > 1 || transformedTokens.getOrNull(0) !is Number) {
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
                val newValue = calculate(operator, lhs, rhs)
                transformedTokens.add(indexOfOperator - 2, newValue)
                transformedTokens.remove(lhs)
                transformedTokens.remove(rhs)
            }

            1 -> {
                val value = transformedTokens[indexOfOperator - 1] as Value
                val newValue = calculate(operator, value)
                transformedTokens.add(indexOfOperator - 1, newValue)
                transformedTokens.remove(value)
            }

            0 -> {
                transformedTokens.add(indexOfOperator, calculate(operator))
            }
        }

        transformedTokens.remove(operator)
    }

    return (transformedTokens.getOrNull(0) as? Value)?.toValueNumber() ?: Undefined
}

private fun calculate(operator: Operator, vararg args: Value): Value {
    val lhs = args.getOrNull(0) ?: Bool(false)
    val rhs = args.getOrNull(1) ?: Bool(false)

    val mc = MathContext(14, RoundingMode.HALF_UP)

    return when (operator.operation) {
        OperatorType.PLUS -> Number(lhs.toDecimal() + rhs.toDecimal())
        OperatorType.MINUS -> Number(lhs.toDecimal() - rhs.toDecimal())
        OperatorType.MULT -> Number(lhs.toDecimal() * rhs.toDecimal())
        OperatorType.DIV -> try {
            Number(BigDecimal(lhs.toDouble() / rhs.toDouble(), mc))
        } catch (e: Exception) {
            Bool(false)
        }

        OperatorType.MOD -> Number(lhs.toDecimal() % rhs.toDecimal())
        OperatorType.POW -> Number(pow(lhs.toDecimal(), rhs.toDecimal()))
        OperatorType.LESS -> Bool(lhs.toDecimal() < rhs.toDecimal())
        OperatorType.EQUALS -> Bool(lhs.toDecimal() == rhs.toDecimal())
        OperatorType.NOT_EQUALS -> Bool(lhs.toDecimal() != rhs.toDecimal())
        OperatorType.OR -> Bool(lhs.toBoolean() || rhs.toBoolean())
        OperatorType.AND -> Bool(lhs.toBoolean() && rhs.toBoolean())
        OperatorType.GREATER -> Bool(lhs.toDecimal() > rhs.toDecimal())
        OperatorType.SQRT -> Number(pow(lhs.toDecimal(), BigDecimal("0.5")))
        OperatorType.UNARY_MINUS -> Number(lhs.toDecimal().multiply(BigDecimal(-1)))
        OperatorType.SIN -> Number(sinBigDecimal(lhs.toDecimal()))
        OperatorType.COS -> Number(cosBigDecimal(lhs.toDecimal()))
        OperatorType.TAN -> Number(tanBigDecimal(lhs.toDecimal()))
        OperatorType.LN -> Number(lnBigDecimal(lhs.toDecimal()))
        OperatorType.LG -> Number(lgBigDecimal(lhs.toDecimal()))
        OperatorType.INT -> Number(roundToInt(lhs.toDecimal()))
        OperatorType.FACTORIAL -> Number(factorial(lhs.toDecimal()))
        OperatorType.DEGREES -> Number(
            BigDecimal(
                (lhs.toDecimal() * BigDecimal("3.1415926535897932384626433832795")).toDouble() / 180.0,
                MathContext.DECIMAL128
            )
        )
        OperatorType.RAND -> Number(BigDecimal(Math.random()))
        else -> Bool(false)
    }
}

@Suppress("Unused")
private fun printTokens(tokens: List<Token>): String {
    val builder = StringBuilder()
    tokens.forEach {
        when (it) {
            is Operator -> builder.append(it.operation.name)
            is Value -> builder.append(it.toDouble())
            is Word -> builder.append(it.string)
        }

        builder.append(" ")
    }

    return builder.toString()
}

fun factorial(x: BigDecimal): BigDecimal {
    var res = BigDecimal.ONE
    for (i in 2..x.toInt()) {
        res *= BigDecimal(i)
    }

    return res
}

fun calculateFormula(formula: String, params: Map<String, ValueNumber>): ValueType = try {
    val tokens = parse(formula)
    val sortedTokens = sortTokens(tokens)
    calculateTokens(sortedTokens, params)
} catch (e: Exception) {
    Undefined
}

fun String.toOperatorType(): OperatorType? = when (this.lowercase()) {
    "√" -> OperatorType.SQRT
    "+" -> OperatorType.PLUS
    "-" -> OperatorType.MINUS
    "*" -> OperatorType.MULT
    "/" -> OperatorType.DIV
    "^" -> OperatorType.POW
    "<" -> OperatorType.LESS
    ">" -> OperatorType.GREATER
    "(" -> OperatorType.LEFT_BRACKET
    ")" -> OperatorType.RIGHT_BRACKET
    "==" -> OperatorType.EQUALS
    "!=" -> OperatorType.NOT_EQUALS
    "!" -> OperatorType.FACTORIAL
    "•" -> OperatorType.MULT
    "×" -> OperatorType.MULT
    "÷" -> OperatorType.DIV
    ":" -> OperatorType.DIV
    "°" -> OperatorType.DEGREES
    "||" -> OperatorType.OR
    "&&" -> OperatorType.AND
    "%" -> OperatorType.MOD
    "mod" -> OperatorType.MOD
    "**" -> OperatorType.POW
    "sin" -> OperatorType.SIN
    "cos" -> OperatorType.COS
    "tg" -> OperatorType.TAN
    "tan" -> OperatorType.TAN
    "ln" -> OperatorType.LN
    "lg" -> OperatorType.LG
    "int" -> OperatorType.INT
    "rand" -> OperatorType.RAND
    else -> null
}