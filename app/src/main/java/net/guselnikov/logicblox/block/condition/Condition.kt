package net.guselnikov.logicblox.block.condition

import net.guselnikov.logicblox.block.ValueType

abstract class Condition {
    abstract operator fun invoke(vararg input: ValueType): Boolean
}