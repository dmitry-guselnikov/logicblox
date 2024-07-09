package net.guselnikov.logicblox.block.parser

import kotlinx.coroutines.delay
import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
import net.guselnikov.logicblox.block.ValueText
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

sealed class Token
sealed class Operator: Token() {
    abstract val precedence: Int
    abstract val isRightHand: Boolean
    abstract val argumentsNumber: Int
    abstract val symbols: List<String>
    abstract suspend fun calculate(vararg args: Value): Value
    abstract fun doesPrint(): Boolean
}

data object Or: Operator() {
    override val precedence: Int = 0
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("||")
    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toBoolean() || rhs.toBoolean())
    }

    override fun doesPrint(): Boolean = false
}
data object And: Operator() {
    override val precedence: Int = 0
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("&&")
    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toBoolean() && rhs.toBoolean())
    }
    override fun doesPrint(): Boolean = false
}
data object Plus: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("+")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ZERO)
        if (lhs.isText() || rhs.isText()) return Literal(lhs.toText() + rhs.toText())
        return Number(lhs.toDecimal() + rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object Minus: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("-")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ZERO)

        return Number(lhs.toDecimal() - rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object Div: Operator() {
    override val precedence: Int = 3
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("/", "÷", ":")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ONE)

        val mc = MathContext(14, RoundingMode.HALF_UP)
        return try {
            Number(BigDecimal(lhs.toDouble() / rhs.toDouble(), mc))
        } catch (e: Exception) {
            Bool(false)
        }
    }
    override fun doesPrint(): Boolean = false
}

data object Mult: Operator() {
    override val precedence: Int = 3
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("*", "•", "×")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Number(lhs.toDecimal() * rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object Sqrt: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("√", "sqrt")

    override suspend fun calculate(vararg args: Value): Value {
        val x = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(pow(x.toDecimal(), BigDecimal("0.5")))
    }
    override fun doesPrint(): Boolean = false
}

data object Pow: Operator() {
    override val precedence: Int = 4
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("^", "**")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ONE)

        return Number(pow(lhs.toDecimal(), rhs.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
data object Less: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("<")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() < rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}

data object LessOrEqual: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("<=", "≤")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() <= rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}

data object Greater: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf(">")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() > rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object GreaterOrEqual: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf(">=", "≥")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() >= rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object Equals: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("==")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() == rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object NotEquals: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("!=", "≠")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() != rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object Mod: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("%", "mod")
    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)
        return Number(lhs.toDecimal() % rhs.toDecimal())
    }
    override fun doesPrint(): Boolean = false
}
data object UnaryMinus: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("-")

    override suspend fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(lhs.toDecimal().multiply(BigDecimal(-1)))
    }
    override fun doesPrint(): Boolean = false
}
data object Factorial: Operator() {
    override val precedence: Int = 6
    override val isRightHand: Boolean = true
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("!")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(factorial(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
/*
    OperatorType.DEGREES -> Number(
            BigDecimal(
                (lhs.toDecimal() * BigDecimal("3.1415926535897932384626433832795")).toDouble() / 180.0,
                MathContext.DECIMAL128
            )
        )
 */

data object Sin: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("sin")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(sinBigDecimal(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
data object Cos: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("cos")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(cosBigDecimal(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
data object Tan: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("tg", "tan")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(tanBigDecimal(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
data object Abs: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("abs")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(arg.toDecimal().abs())
    }
    override fun doesPrint(): Boolean = false
}
data object Ln: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("ln")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(lnBigDecimal(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}
data object Lg: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("lg")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(lgBigDecimal(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}

data object ToInt: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("int")
    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(roundToInt(arg.toDecimal()))
    }
    override fun doesPrint(): Boolean = false
}

data object Print: Operator() {
    override val precedence: Int = -1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("print")
    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Literal("")
        return arg
    }
    override fun doesPrint(): Boolean = true
}

data object Println: Operator() {
    override val precedence: Int = -1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("println")
    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Literal("\n")
        return Literal(arg.toText() + "\n")
    }
    override fun doesPrint(): Boolean = true
}

data object Sleep: Operator() {
    override val precedence: Int = -1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("sleep")

    override suspend fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        delay((arg.toDecimal() * BigDecimal("1000")).toLong())
        return arg
    }

    override fun doesPrint(): Boolean = false
}

data object Rand: Operator() {
    override val precedence: Int = 7
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 0
    override val symbols: List<String> = listOf("rand")
    override suspend fun calculate(vararg args: Value): Value {
        return Number(BigDecimal(Math.random()))
    }
    override fun doesPrint(): Boolean = false
}

sealed class Value : Token() {
    abstract fun toValueNumber(): ValueNumber
    abstract fun toValueText(): ValueText
    abstract fun toDecimal(): BigDecimal
    abstract fun toDouble(): Double
    abstract fun toBoolean(): Boolean
    abstract fun toText(): String

    abstract fun isText(): Boolean
}

class Number(private val decimal: BigDecimal) : Value() {
    override fun toValueNumber() = ValueDecimal(decimal)
    override fun toValueText() = ValueText(decimal.toPlainString())
    override fun toDecimal(): BigDecimal = decimal
    override fun toDouble(): Double = decimal.toDouble()
    override fun toBoolean(): Boolean = decimal != BigDecimal.ZERO
    override fun toText(): String = decimal.toPlainString()
    override fun isText(): Boolean = false
}

class Bool(private val bool: Boolean) : Value() {
    override fun toValueNumber(): ValueNumber = ValueBoolean(bool)
    override fun toValueText(): ValueText = ValueText(toText())
    override fun toDecimal(): BigDecimal = if (bool) BigDecimal.ONE else BigDecimal.ZERO
    override fun toDouble(): Double = if (bool) 1.0 else 0.0
    override fun toBoolean(): Boolean = bool
    override fun toText(): String = if (bool) "true" else "false"
    override fun isText(): Boolean = false
}

class Literal(val string: String): Value() {
    override fun toValueNumber() = try {
        ValueDecimal(BigDecimal(string.toDouble()))
    } catch (e: Exception) {
        ValueBoolean(false)
    }

    override fun toValueText(): ValueText = ValueText(string)

    override fun toDecimal(): BigDecimal = try {
        BigDecimal(string.toDouble())
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    override fun toDouble(): Double = try {
        string.toDouble()
    } catch (e: Exception) {
        Double.NaN
    }

    override fun toBoolean(): Boolean = false

    override fun toText(): String = string

    override fun isText(): Boolean = true
}

class Word(val string: String) : Token()

data object LeftBracket : Token()
data object RightBracket : Token()
data object Assign : Token()
data object BlockStart: Token()
data object BlockEnd: Token()
data object If: Token()
data object Else: Token()
data object Return: Token()
data object NewLine: Token()
data object While: Token()
data object Break: Token()
data object Continue: Token()

private fun factorial(x: BigDecimal): BigDecimal {
    var res = BigDecimal.ONE
    for (i in 2..x.toInt()) {
        res *= BigDecimal(i)
    }

    return res
}