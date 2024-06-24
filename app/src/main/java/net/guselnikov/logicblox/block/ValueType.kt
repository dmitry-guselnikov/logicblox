package net.guselnikov.logicblox.block

import java.math.BigDecimal

sealed class ValueType {
    abstract fun toText(): String
}

sealed class ValueNumber : ValueType() {
    abstract fun toDouble(): Double
    abstract fun toLong(): Long
    abstract fun toBoolean(): Boolean
    abstract fun isInteger(): Boolean
    abstract fun negative(): ValueNumber
    abstract fun reversed(): ValueType?
    abstract fun toBigDecimal(): BigDecimal
}

data object Undefined : ValueType() {
    override fun toText(): String = "undefined"
}

class ValueText(val text: String) : ValueType() {
    override fun toText(): String = text
}

class ValueDecimal(val decimal: BigDecimal) : ValueNumber() {
    override fun toDouble(): Double = decimal.toDouble()
    override fun toLong(): Long = decimal.toLong()
    override fun toBoolean(): Boolean = decimal != BigDecimal.ZERO
    override fun isInteger(): Boolean =
        decimal.signum() == 0 || decimal.scale() <= 0 || decimal.stripTrailingZeros().scale() <= 0
    override fun negative() = ValueDecimal(decimal.negate())
    override fun reversed() = try {
        ValueDecimal(BigDecimal.ONE.negate().divide(decimal))
    } catch (e: Exception) {
        null
    }
    override fun toBigDecimal(): BigDecimal = decimal
    override fun toText(): String = decimal.stripTrailingZeros().toPlainString()

}

class ValueBoolean(val bool: Boolean) : ValueNumber() {
    override fun toDouble() = if (bool) 1.0 else 0.0
    override fun toLong() = if (bool) 1L else 0L
    override fun toBoolean(): Boolean = bool
    override fun isInteger(): Boolean = true
    override fun negative(): ValueNumber = ValueBoolean(bool.not())
    override fun reversed() = negative()
    override fun toBigDecimal(): BigDecimal = if (bool) BigDecimal.ONE else BigDecimal.ZERO
    override fun toText() = if (bool) "true" else "false"
}
