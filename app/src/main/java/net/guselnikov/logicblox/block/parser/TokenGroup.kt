package net.guselnikov.logicblox.block.parser

import net.guselnikov.logicblox.block.base.GroupBlock
import java.math.BigDecimal
import kotlin.math.exp

sealed class TokenGroup {
    abstract fun isEmpty(): Boolean
}
class FormulaGroup(val tokens: List<Token>): TokenGroup() {
    override fun isEmpty(): Boolean = tokens.isEmpty()
}

class BlockGroup(val expressions: List<TokenGroup>): TokenGroup() {
    override fun isEmpty(): Boolean = expressions.isEmpty()
}

data object EmptyGroup : TokenGroup() {
    override fun isEmpty(): Boolean = true
}

class ConditionGroup(
    val condition: FormulaGroup,
    val onTrueBlock: BlockGroup,
    val onFalseBlock: BlockGroup
): TokenGroup() {
    override fun isEmpty(): Boolean = condition.isEmpty()
}

class WhileLoopGroup(
    val condition: FormulaGroup,
    val loopBlock: BlockGroup
): TokenGroup() {
    override fun isEmpty(): Boolean = condition.isEmpty()
}

// for (i from 1 to 10 step 0.5)
class ForLoopGroup(
    val variable: String,
    val loopBlock: BlockGroup,
    val start: FormulaGroup,
    val end: FormulaGroup,
    val step: FormulaGroup) : TokenGroup() {
    override fun isEmpty(): Boolean = loopBlock.isEmpty()
}

@Suppress("Unused")
fun printGroup(tokenGroup: TokenGroup, nesting:Int = 0): String {
    val stringBuilder = StringBuilder()
    val spacesBuilder = StringBuilder()
    for (i in 0 until nesting * 3) {
        spacesBuilder.append(" ")
    }
    val spaces = spacesBuilder.toString()

    when (tokenGroup) {
        is FormulaGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append(printTokens(tokenGroup.tokens))
        }
        is EmptyGroup -> stringBuilder.append("{}")
        is BlockGroup -> {
            stringBuilder.append("{\n")
            tokenGroup.expressions.forEach { expression ->
                stringBuilder.append(printGroup(expression, nesting + 1))
                stringBuilder.append("\n")
            }
            stringBuilder.append(spaces)
            stringBuilder.append("}")
        }
        is ConditionGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("if ")
            stringBuilder.append(printTokens(tokenGroup.condition.tokens))
            stringBuilder.append(printGroup(tokenGroup.onTrueBlock, nesting + 1))
            stringBuilder.append(" else ")
            stringBuilder.append(printGroup(tokenGroup.onFalseBlock, nesting + 1))
        }

        is WhileLoopGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("while")
            stringBuilder.append(printTokens(tokenGroup.condition.tokens))
            stringBuilder.append(printGroup(tokenGroup.loopBlock), nesting + 1)
        }

        is ForLoopGroup -> {
            stringBuilder.append(spaces)
            stringBuilder.append("for ")
            stringBuilder.append(tokenGroup.variable)
            stringBuilder.append(" from ")
            stringBuilder.append(tokenGroup.start.toString())
            stringBuilder.append(" to ")
            stringBuilder.append(tokenGroup.end.toString())
            stringBuilder.append(" step ")
            stringBuilder.append(tokenGroup.step.toString())
            stringBuilder.append(printGroup(tokenGroup.loopBlock))
        }
    }

    return stringBuilder.toString()
}

//        var formulaToCalculate: String = formula
//        val conditionalFormula = breakConditionalFormula(formula)
//        if (conditionalFormula != null) {
//            // Calculate condition
//            val conditionTokens = parse(conditionalFormula.condition)
//            val sortedConditionTokens = sortTokens(conditionTokens)
//            val conditionResult = calculateTokens(sortedConditionTokens, params)
//            if (conditionResult is ValueNumber) {
//                formulaToCalculate =
//                    if (conditionResult.toBoolean()) conditionalFormula.trueStatementFormula else conditionalFormula.falseStatementFormula
//            }
//        }