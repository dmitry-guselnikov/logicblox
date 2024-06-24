package net.guselnikov.logicblox.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

fun BigDecimal.isInteger() = signum() == 0 || scale() <= 0 || stripTrailingZeros().scale() <= 0

fun pow(x: BigDecimal, y: BigDecimal): BigDecimal {
    if (x == BigDecimal.ZERO) return BigDecimal.ZERO
    if (x.isInteger() && y.isInteger()) {
        return if (y >= BigDecimal.ZERO) x.pow(y.toInt()) else BigDecimal.ONE.divide(
            y,
            MathContext.DECIMAL64
        ).pow(abs(y.toInt()))
    }

    val doubleValue = x.toDouble().pow(y.toDouble())
    return BigDecimal(doubleValue, MathContext.DECIMAL64)
}

val TWO_PI = BigDecimal("3.1415926535897932384626433832795") * BigDecimal(2)

fun sinBigDecimal(x: BigDecimal): BigDecimal = round(
    BigDecimal(sin(x.remainder(TWO_PI).toDouble()))
)

fun cosBigDecimal(x: BigDecimal): BigDecimal = round(
    BigDecimal(cos(x.remainder(TWO_PI).toDouble()))
)

fun tanBigDecimal(x: BigDecimal): BigDecimal = round(
    BigDecimal(tan(x.remainder(TWO_PI).toDouble()))
)

fun lnBigDecimal(x: BigDecimal): BigDecimal = round(
    BigDecimal(ln(x.toDouble()))
)

fun lgBigDecimal(x: BigDecimal): BigDecimal = round(
    BigDecimal(log10(x.toDouble()))
)

fun round(x: BigDecimal): BigDecimal {
    if (x.abs() < BigDecimal.ONE) {
        return (x + BigDecimal.ONE).setScale(14, RoundingMode.HALF_UP) - BigDecimal.ONE
    }

    return x.setScale(14, RoundingMode.HALF_UP)
}

fun roundToInt(x: BigDecimal): BigDecimal {
    if (x.abs() < BigDecimal.ONE) {
        return (x + BigDecimal.ONE).setScale(0, RoundingMode.FLOOR) - BigDecimal.ONE
    }

    return x.setScale(0, RoundingMode.FLOOR)
}