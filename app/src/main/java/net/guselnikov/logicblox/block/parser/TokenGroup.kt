package net.guselnikov.logicblox.block.parser

sealed class TokenGroup {
    abstract fun isEmpty(): Boolean
}
class FormulaGroup(val tokens: List<Token>): TokenGroup() {
    override fun isEmpty(): Boolean = tokens.isEmpty()
}
class BlockGroup(val expressions: List<TokenGroup>): TokenGroup() {
    override fun isEmpty(): Boolean = expressions.isEmpty()
}
class EmptyGroup(): TokenGroup() {
    override fun isEmpty(): Boolean = true
}

class ConditionGroup(
    val condition: FormulaGroup,
    val onTrueBlock: BlockGroup,
    val onFalseBlock: BlockGroup
): TokenGroup() {
    override fun isEmpty(): Boolean = condition.isEmpty()
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