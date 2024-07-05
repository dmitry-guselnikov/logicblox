package net.guselnikov.logicblox.block.parser

import net.guselnikov.logicblox.block.base.GroupBlock
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