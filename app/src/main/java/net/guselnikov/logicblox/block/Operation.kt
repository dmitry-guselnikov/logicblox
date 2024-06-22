package net.guselnikov.logicblox.block

import java.lang.Math.pow
import java.math.BigDecimal
import kotlin.math.pow

abstract class Operation {
    abstract operator fun invoke(): ValueType
}

class Addition(val inputs: List<ValueType>) : Operation() {
    override fun invoke(): ValueNumber {

        var acc: BigDecimal = BigDecimal.ZERO

        inputs.filterIsInstance<ValueNumber>().forEach {
            val inc = when {
                it is ValueDecimal -> it.decimal
                it is ValueBoolean && it.bool -> BigDecimal.ONE
                else -> BigDecimal.ZERO
            }
            acc += inc
        }

        return ValueDecimal(acc)
    }
}

class Multiplication(val inputs: List<ValueType>) : Operation() {
    override fun invoke(): ValueNumber {
        var acc = BigDecimal.ONE

        inputs.filterIsInstance<ValueNumber>().forEach {
            val factor = when {
                it is ValueDecimal -> it.decimal
                it is ValueBoolean && !it.bool -> return ValueDecimal(BigDecimal.ZERO)
                else -> BigDecimal.ONE
            }

            acc *= factor
        }

        return ValueDecimal(acc)
    }
}

class Concatenation(val inputs: List<ValueType>) : Operation() {
    override fun invoke(): ValueText {
        val stringBuilder = StringBuilder()
        inputs.forEach {
            stringBuilder.append(it.toText())
        }

        return ValueText(stringBuilder.toString())
    }
}

class Power(val inputs: List<ValueType>) : Operation() {
    override fun invoke(): ValueType {
        val base = inputs.getOrNull(0) ?: Undefined
        val power = inputs.getOrNull(1) ?: Undefined

        if (base is ValueDecimal && power is ValueDecimal) {
            val result = if (power.isInteger()) base.decimal.pow(power.toLong().toInt())
            else try {
                net.guselnikov.logicblox.util.pow(base.decimal, power.decimal)
            } catch (e: Exception) {
                null
            }

            return if (result != null) ValueDecimal(result) else Undefined
        }

        return Undefined
    }

}