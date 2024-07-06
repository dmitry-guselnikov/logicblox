package net.guselnikov.logicblox.presentation

import android.annotation.SuppressLint
import android.widget.TextView
import net.guselnikov.logicblox.block.parser.Value
import net.guselnikov.logicblox.block.runner.Console

class TextViewConsole(val textView: TextView): Console() {
    override fun print(str: String) {
        textView.append(str)
    }

    override fun print(vararg values: Value) {
        values.forEach { value ->
            textView.append(value.toText())
        }
    }

    override fun println(str: String) {
        textView.append(str)
        textView.append("\n")
    }

    @SuppressLint("SetTextI18n")
    override fun clear() {
        textView.text = ""
    }
}