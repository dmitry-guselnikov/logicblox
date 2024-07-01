package net.guselnikov.logicblox.block.parser

import net.guselnikov.logicblox.block.ValueBoolean
import net.guselnikov.logicblox.block.ValueDecimal
import net.guselnikov.logicblox.block.ValueNumber
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
    abstract fun calculate(vararg args: Value): Value
}

object Or: Operator() {
    override val precedence: Int = 0
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("||")
    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toBoolean() || rhs.toBoolean())
    }
}
object And: Operator() {
    override val precedence: Int = 0
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("&&")
    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toBoolean() && rhs.toBoolean())
    }
}
object Plus: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("+")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ZERO)

        return Number(lhs.toDecimal() + rhs.toDecimal())
    }
}
object Minus: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("-")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ZERO)

        return Number(lhs.toDecimal() - rhs.toDecimal())
    }
}
object Div: Operator() {
    override val precedence: Int = 3
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("/", "÷", ":")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ONE)

        val mc = MathContext(14, RoundingMode.HALF_UP)
        return try {
            Number(BigDecimal(lhs.toDouble() / rhs.toDouble(), mc))
        } catch (e: Exception) {
            Bool(false)
        }
    }

}
object Mult: Operator() {
    override val precedence: Int = 3
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("*", "•", "×")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Number(lhs.toDecimal() * rhs.toDecimal())
    }

}
object Sqrt: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("√", "sqrt")

    override fun calculate(vararg args: Value): Value {
        val x = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(pow(x.toDecimal(), BigDecimal("0.5")))
    }

}
object Pow: Operator() {
    override val precedence: Int = 4
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("^", "**")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        val rhs = args.getOrNull(1) ?: Number(BigDecimal.ONE)

        return Number(pow(lhs.toDecimal(), rhs.toDecimal()))
    }
}
object Less: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("<")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() < rhs.toDecimal())
    }
}
object LessOrEqual: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("<=", "≤")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() <= rhs.toDecimal())
    }

}
object Greater: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf(">")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() > rhs.toDecimal())
    }
}
object GreaterOrEqual: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf(">=", "≥")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() >= rhs.toDecimal())
    }
}
object Equals: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("==")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() == rhs.toDecimal())
    }
}
object NotEquals: Operator() {
    override val precedence: Int = 1
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("!=", "≠")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)

        return Bool(lhs.toDecimal() != rhs.toDecimal())
    }
}
object Mod: Operator() {
    override val precedence: Int = 2
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 2
    override val symbols: List<String> = listOf("%", "mod")
    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Bool(false)
        val rhs = args.getOrNull(1) ?: Bool(false)
        return Number(lhs.toDecimal() % rhs.toDecimal())
    }
}
object UnaryMinus: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("-")

    override fun calculate(vararg args: Value): Value {
        val lhs = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(lhs.toDecimal().multiply(BigDecimal(-1)))
    }

}
object Factorial: Operator() {
    override val precedence: Int = 6
    override val isRightHand: Boolean = true
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("!")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(factorial(arg.toDecimal()))
    }
}
/*
    "rand" -> OperatorType.RAND

    OperatorType.DEGREES -> Number(
            BigDecimal(
                (lhs.toDecimal() * BigDecimal("3.1415926535897932384626433832795")).toDouble() / 180.0,
                MathContext.DECIMAL128
            )
        )
 */

object Sin: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("sin")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(sinBigDecimal(arg.toDecimal()))
    }
}
object Cos: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("cos")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(cosBigDecimal(arg.toDecimal()))
    }
}
object Tan: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("tg", "tan")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(tanBigDecimal(arg.toDecimal()))
    }
}
object Abs: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("abs")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ZERO)
        return Number(arg.toDecimal().abs())
    }
}
object Ln: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("ln")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(lnBigDecimal(arg.toDecimal()))
    }
}
object Lg: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("lg")

    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(lgBigDecimal(arg.toDecimal()))
    }
}
object ToInt: Operator() {
    override val precedence: Int = 5
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 1
    override val symbols: List<String> = listOf("int")
    override fun calculate(vararg args: Value): Value {
        val arg = args.getOrNull(0) ?: Number(BigDecimal.ONE)
        return Number(roundToInt(arg.toDecimal()))
    }
}
object Rand: Operator() {
    override val precedence: Int = 7
    override val isRightHand: Boolean = false
    override val argumentsNumber: Int = 0
    override val symbols: List<String> = listOf("rand")
    override fun calculate(vararg args: Value): Value {
        return Number(BigDecimal(Math.random()))
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

class Literal(val string: String): Token()

data object LeftBracket : Token()
data object RightBracket : Token()
data object Assign : Token()
data object BlockStart: Token()
data object BlockEnd: Token()
data object If: Token()
data object Else: Token()
data object Return: Token()
data object NewLine: Token()
data object Print: Token()

private fun factorial(x: BigDecimal): BigDecimal {
    var res = BigDecimal.ONE
    for (i in 2..x.toInt()) {
        res *= BigDecimal(i)
    }

    return res
}