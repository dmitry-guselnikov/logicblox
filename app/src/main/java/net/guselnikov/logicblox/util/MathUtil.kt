package net.guselnikov.logicblox.util

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.pow

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