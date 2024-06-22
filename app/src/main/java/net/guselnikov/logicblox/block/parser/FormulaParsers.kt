package net.guselnikov.logicblox.block.parser


import net.guselnikov.logicblox.block.Undefined
import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueType
import net.guselnikov.logicblox.util.pow
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Stack
import kotlin.Exception
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.tan
import kotlin.text.StringBuilder

private fun <T> T.oneOf(vararg candidates: T): Boolean {
    candidates.forEach {
        if (it == this) return true
    }

    return false
}

private fun String.startsWithOneOf(vararg substring: String): String? {
    substring.forEach {
        if (startsWith(it)) return it
    }
    return null
}

enum class OperatorType {
    PLUS,
    MINUS,
    LESS,
    GREATER,
    MULT,
    DIV,
    POW,
    UNARY_MINUS,
    SQRT,
    SIN,
    COS,
    TAN,
    LN,
    LG,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    EQUALS,
    NOT_EQUALS
}

sealed class Token
class Operator(val operation: OperatorType) : Token() {
    val precedence: Int
        get() = when (operation) {
            OperatorType.LESS, OperatorType.GREATER, OperatorType.EQUALS, OperatorType.NOT_EQUALS -> 0
            OperatorType.PLUS, OperatorType.MINUS -> 1
            OperatorType.MULT, OperatorType.DIV -> 2
            OperatorType.POW -> 3
            OperatorType.UNARY_MINUS, OperatorType.SQRT, OperatorType.SIN, OperatorType.COS, OperatorType.TAN, OperatorType.LN, OperatorType.LG -> 4
            else -> -1
        }

    val argumentsNumber: Int
        get() = when (operation) {
            OperatorType.LESS, OperatorType.EQUALS, OperatorType.NOT_EQUALS, OperatorType.GREATER, OperatorType.PLUS, OperatorType.MINUS, OperatorType.MULT, OperatorType.DIV, OperatorType.POW -> 2
            else -> 1
        }
}

sealed class Value : Token() {
    abstract fun toValueNumber(): ValueNumber
    abstract fun toDecimal(): BigDecimal
    abstract fun toDouble(): Double
}

class Number(val decimal: BigDecimal) : Value() {
    override fun toValueNumber() = ValueDecimal(decimal)
    override fun toDecimal(): BigDecimal = decimal
    override fun toDouble(): Double = decimal.toDouble()
}

class Bool(val bool: Boolean) : Value() {
    override fun toValueNumber(): ValueNumber = ValueBoolean(bool)
    override fun toDecimal(): BigDecimal = if (bool) BigDecimal.ONE else BigDecimal.ZERO
    override fun toDouble(): Double = if (bool) 1.0 else 0.0
}

class Word(val string: String) : Token()

/**
 * 1. Сделать поддержку унарных операций
 * 2. Сделать поддержку булевых операций с минимальным прецедентом
 *    В формуле 1+2 == 2+1, оператор == должен выполниться последним и вернуть ValueBoolean
 * 3.
 */
private fun parse(formula: String): List<Token> {
    // Parse the given string to tokens
    val tokens = arrayListOf<Token>()
    var readingNumber = false
    var readingWord = false
    var currentNumber = StringBuilder()
    var currentWord = StringBuilder()

    fun writeWord() {
        readingWord = false
        when (val word = currentWord.toString()) {
            "sin" -> tokens.add(Operator(OperatorType.SIN))
            "cos" -> tokens.add(Operator(OperatorType.COS))
            "tg" -> tokens.add(Operator(OperatorType.TAN))
            "tan" -> tokens.add(Operator(OperatorType.TAN))
            "ln" -> tokens.add(Operator(OperatorType.LN))
            "lg" -> tokens.add(Operator(OperatorType.LG))
            "π" -> tokens.add(Number(BigDecimal("3.1415926535897932384626433832795")))
            "rand" -> tokens.add(Number(BigDecimal(Math.random())))
            "true" -> tokens.add(Bool(true))
            "false" -> tokens.add(Bool(false))
            else -> tokens.add(Word(word))
        }
        currentWord.clear()
    }

    formula.forEachIndexed { index, it ->
        val operatorStr: String? = formula.slice(index..formula.lastIndex).startsWithOneOf(
            "√", "+", "-", "*", "/", "^", "(", ")", "<", ">", "==", "!="
        )

        when {
            operatorStr != null -> {
                if (readingWord) {
                    writeWord()
                }

                if (readingNumber) {
                    readingNumber = false
                    tokens.add(Number(BigDecimal(currentNumber.toString())))
                    currentNumber.clear()
                }

                val lastOperator = tokens.lastOrNull() as? Operator
                if (it == '-' && (tokens.isEmpty() || (lastOperator is Operator && lastOperator.operation != OperatorType.RIGHT_BRACKET))) {
                    tokens.add(Operator(OperatorType.UNARY_MINUS))
                } else {
                    operatorStr.toOperatorType()?.let { operation ->
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
            token is Operator && token.operation.oneOf(
                OperatorType.MULT,
                OperatorType.DIV,
                OperatorType.POW,
                OperatorType.PLUS,
                OperatorType.MINUS,
                OperatorType.LESS,
                OperatorType.GREATER,
                OperatorType.EQUALS,
                OperatorType.NOT_EQUALS,
                OperatorType.UNARY_MINUS,
                OperatorType.SIN,
                OperatorType.SQRT,
                OperatorType.COS,
                OperatorType.TAN,
                OperatorType.LN,
                OperatorType.LG
            ) -> {
                var thereIsHigherPrecedence = true
                while (thereIsHigherPrecedence) {
                    val topStackOperator: Operator? = try {
                        operatorsStack.peek()
                    } catch (e: Exception) {
                        null
                    }
                    thereIsHigherPrecedence = if (topStackOperator == null) false
                    else {
                        if (token.precedence <= 2) topStackOperator.precedence >= token.precedence
                        else topStackOperator.precedence > token.precedence
                    }

                    if (thereIsHigherPrecedence) {
                        outputQueue.add(operatorsStack.pop())
                    }
                }
                operatorsStack.push(token)
            }

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
            if (token is Word) {
                Number(params[token.string]?.toBigDecimal() ?: return Undefined)
            } else token
        }
    )

    while (transformedTokens.size > 1) {
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
        }

        transformedTokens.remove(operator)
    }

    return (transformedTokens.getOrNull(0) as? Value)?.toValueNumber() ?: Undefined
}

private fun calculate(operator: Operator, vararg args: Value): Value {
    val lhs = args.getOrNull(0) ?: Bool(false)
    val rhs = args.getOrNull(1) ?: Bool(false)

    val mc = MathContext(14, RoundingMode.HALF_UP)
    val trigMc = MathContext(7, RoundingMode.HALF_UP)

    return when (operator.operation) {
        OperatorType.PLUS -> Number(lhs.toDecimal() + rhs.toDecimal())
        OperatorType.MINUS -> Number(lhs.toDecimal() - rhs.toDecimal())
        OperatorType.MULT -> Number(lhs.toDecimal() * rhs.toDecimal())
        OperatorType.DIV -> try {
            Number(BigDecimal(lhs.toDouble() / rhs.toDouble(), mc))
        } catch (e: Exception) {
            Bool(false)
        }

        OperatorType.POW -> Number(pow(lhs.toDecimal(), rhs.toDecimal()))
        OperatorType.LESS -> Bool(lhs.toDecimal() < rhs.toDecimal())
        OperatorType.EQUALS -> Bool(lhs.toDecimal() == rhs.toDecimal())
        OperatorType.NOT_EQUALS -> Bool(lhs.toDecimal() != rhs.toDecimal())
        OperatorType.GREATER -> Bool(lhs.toDecimal() > rhs.toDecimal())
        OperatorType.SQRT -> Number(pow(lhs.toDecimal(), BigDecimal("0.5")))
        OperatorType.UNARY_MINUS -> Number(lhs.toDecimal().multiply(BigDecimal(-1)))
        OperatorType.SIN -> {
            val sin = sin(lhs.toDouble())
            val res = BigDecimal(sin)
            Number(res)
        }
        OperatorType.COS -> Number(BigDecimal(cos(lhs.toDouble()), trigMc))
        OperatorType.TAN -> Number(BigDecimal(tan(lhs.toDouble()), trigMc))
        OperatorType.LN -> Number(BigDecimal(ln(lhs.toDouble()), mc))
        OperatorType.LG -> Number(BigDecimal(log10(lhs.toDouble()), mc))
        else -> Bool(false)
    }
}

//private fun printTokens(tokens: List<Token>): String {
//    val builder = StringBuilder()
//    tokens.forEach {
//        when (it) {
//            is Operator -> builder.append(it.char)
//            is Value -> builder.append(it.toDouble())
//            is Word -> builder.append(it.string)
//        }
//
//        builder.append(" ")
//    }
//
//    return builder.toString()
//}

fun calculateFormula(formula: String, params: Map<String, ValueNumber>): ValueType = try {
    val tokens = parse(formula)
    val sortedTokens = sortTokens(tokens)
    calculateTokens(sortedTokens, params)
} catch (e: Exception) {
    Undefined
}

fun String.toOperatorType(): OperatorType? = when (this) {
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
    else -> null
}