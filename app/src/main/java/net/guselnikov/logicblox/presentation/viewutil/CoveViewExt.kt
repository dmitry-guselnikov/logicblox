package net.guselnikov.logicblox.presentation.viewutil

import com.amrdeveloper.codeview.CodeView
import net.guselnikov.logicblox.R
import java.util.regex.Pattern


fun CodeView.setup() {
    setTextColor(0xFFEEEEEE.toInt())


    addSyntaxPattern(Pattern.compile("\\b\\d+\\b"), 0xFFAADDFF.toInt())
    val codeOperators = listOf("if", "else", "return", "while", "break", "continue", "for", "to", "from", "step")
    val codeOperatorsPattern = codeOperators.reduce { acc, s ->
        if (acc.isNotEmpty()) "${acc}|\\b${s}\\b"
        else s
    }
    val booleanPattern = "\\btrue\\b|\\bfalse\\b"
    val functionsPattern = "\\bsin\\b|\\bcos\\b|\\btg\\b|\\btan\\b|\\bmod\\b|\\bint\\b|\\bprintln\\b|\\bprint\\b|\\bsleep\\b|\\bsqrt\\b|\\babs\\b|\\bln\\b|\\blg\\b|\\brand\\b"
    val stringsPattern = "\".*\""
    val commentPattern ="//.*"
    val constantsPattern = "\\bscreenWidth\\b|\\bscreenHeight\\b"

    addSyntaxPattern(Pattern.compile(codeOperatorsPattern), 0xFFFFBBA0.toInt())
    addSyntaxPattern(Pattern.compile(booleanPattern), 0xFFFFFF40.toInt())
    addSyntaxPattern(Pattern.compile(functionsPattern), 0xFFDDAAEE.toInt())
    addSyntaxPattern(Pattern.compile(constantsPattern), 0xFFFFBBDD.toInt())
    addSyntaxPattern(Pattern.compile(stringsPattern), 0xFF80DD80.toInt())
    addSyntaxPattern(Pattern.compile(commentPattern), 0xFFAAAAAA.toInt())

    setEnableLineNumber(true)
    setLineNumberTextSize(context.resources.getDimension(R.dimen.line_number_text_size))
    setLineNumberTextColor(0xFF808080.toInt())
    setEnableAutoIndentation(true)
    setEnableHighlightCurrentLine(true)

    val pairCompleteMap: MutableMap<Char, Char> = HashMap()
    pairCompleteMap['{'] = '}'
    pairCompleteMap['['] = ']'
    pairCompleteMap['('] = ')'
    pairCompleteMap['"'] = '"'

    setPairCompleteMap(pairCompleteMap)
    enablePairComplete(true)
    enablePairCompleteCenterCursor(true)

    //addSyntaxPattern(Pattern.compile(nonWordsPattern), 0xFFDDDDDD.toInt())


    //addSyntaxPattern(Pattern.compile(operatorsPattern), 0xFF80AA80.toInt())
}