package net.guselnikov.logicblox.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.amrdeveloper.codeview.CodeView
import net.guselnikov.logicblox.R
import net.guselnikov.logicblox.block.parser.parseCode
import net.guselnikov.logicblox.block.runner.Console
import net.guselnikov.logicblox.block.runner.runGroup
import net.guselnikov.logicblox.presentation.viewutil.setup

class EditCodeActivity : AppCompatActivity() {

    private val viewModel: EditCodeViewModel by viewModel()
    private lateinit var codeInput: CodeView
    private lateinit var savedLabel: TextView
    private lateinit var saveButton: View
    private lateinit var runButton: View
    private lateinit var consoleTV: TextView
    private lateinit var console: Console
    private lateinit var canvas: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_code)
        codeInput = findViewById(R.id.code_input)
        codeInput.setup()
        savedLabel = findViewById(R.id.saved_label)
        saveButton = findViewById(R.id.btn_save)
        runButton = findViewById(R.id.btn_run)
        consoleTV = findViewById(R.id.console)
        console = TextViewConsole(consoleTV)
        canvas = findViewById(R.id.canvas)

        canvas.viewTreeObserver.addOnPreDrawListener {
            viewModel.canvasWidth = canvas.width
            viewModel.canvasHeight = canvas.height
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val code = viewModel.getCode()
        codeInput.setText(code)


        initClicks()
        initObservers()
    }

    private fun initObservers() {
        viewModel.savedFlag.observe(this) { saved ->
            savedLabel.isVisible = saved
        }

        codeInput.doOnTextChanged { text, _, _, _ ->
            viewModel.setNotSaved()
        }
        viewModel.consoleMessage.observe(this) { message ->
            if (message == null) {
                console.clear()
            } else {
                console.print(message)
            }

        }
    }

    private fun initClicks() {
        saveButton.setOnClickListener {
            viewModel.saveCode(codeInput.text.toString())
        }
        runButton.setOnClickListener {
            val code = codeInput.text.toString()

            consoleTV.isVisible = true
            console.clear()

            viewModel.runCode(code)
            consoleTV.postDelayed({ consoleTV.isVisible = false }, 100000L)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // DO nothing
    }
}

