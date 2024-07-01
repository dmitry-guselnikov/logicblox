package net.guselnikov.logicblox.block.parser

sealed class TokenGroup
class FormulaGroup(tokens: List<Token>): TokenGroup()
class BlockGroup(expressions: List<TokenGroup>): TokenGroup()

class ConditionGroup(
    condition: FormulaGroup,
    onTrueBlock: BlockGroup,
    onFalseBlock: BlockGroup
)

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