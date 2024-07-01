package net.guselnikov.logicblox.block.parser


data class ConditionalFormula(
    val condition: String,
    val trueStatementFormula: String,
    val falseStatementFormula: String
)

enum class ReadingMode {
    INIT,
    IF_SKIPPED,
    CONDITION,
    TRUE_STATEMENT,
    TRUE_STATEMENT_COMPLETED,
    ELSE_SKIPPED,
    FALSE_STATEMENT,
    COMPLETED
}

private fun breakConditionalFormula(formula: String): ConditionalFormula? {
    val conditionStringBuilder = StringBuilder()
    val trueStatementStringBuilder = StringBuilder()
    val falseStatementStringBuilder = StringBuilder()

    var mode: ReadingMode = ReadingMode.INIT
    var skipChars = 0
    formula.forEachIndexed { index, char ->
        if (skipChars > 0) {
            skipChars--
            return@forEachIndexed
        }

        when (mode) {
            ReadingMode.INIT -> {
                if (formula.slice(index..formula.lastIndex).startsWith("if")) {
                    mode = ReadingMode.IF_SKIPPED
                    skipChars = 1
                }
            }

            ReadingMode.IF_SKIPPED -> {
                if (char == '(') {
                    mode = ReadingMode.CONDITION
                    conditionStringBuilder.append(char)
                }
            }

            ReadingMode.CONDITION -> {
                when (char) {
                    '{' -> mode = ReadingMode.TRUE_STATEMENT
                    else -> conditionStringBuilder.append(char)
                }
            }

            ReadingMode.TRUE_STATEMENT -> {
                when (char) {
                    '}' -> mode = ReadingMode.TRUE_STATEMENT_COMPLETED
                    else -> trueStatementStringBuilder.append(char)
                }
            }

            ReadingMode.TRUE_STATEMENT_COMPLETED -> {
                if (formula.slice(index..formula.lastIndex).startsWith("else")) {
                    mode = ReadingMode.ELSE_SKIPPED
                    skipChars = 3
                }
            }

            ReadingMode.ELSE_SKIPPED -> {
                if (char == '{') mode = ReadingMode.FALSE_STATEMENT
            }

            ReadingMode.FALSE_STATEMENT -> {
                when (char) {
                    '}' -> mode = ReadingMode.COMPLETED
                    else -> falseStatementStringBuilder.append(char)
                }
            }

            ReadingMode.COMPLETED -> return@forEachIndexed
        }
    }

    val conditionStr = conditionStringBuilder.toString()
    val trueStatementFormula = trueStatementStringBuilder.toString()
    val falseStatementFormula = falseStatementStringBuilder.toString()

    if (conditionStr.isNotBlank() && trueStatementFormula.isNotBlank()) return ConditionalFormula(
        conditionStr,
        trueStatementFormula,
        falseStatementFormula
    )
    return null
}