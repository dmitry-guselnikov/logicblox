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
import net.guselnikov.logicblox.R
import net.guselnikov.logicblox.block.parser.lines
import net.guselnikov.logicblox.block.parser.parseCode
import net.guselnikov.logicblox.block.parser.printTokens
import net.guselnikov.logicblox.block.parser.tokens

class EditCodeActivity : AppCompatActivity() {

    private val viewModel: EditCodeViewModel by viewModel()
    private lateinit var codeInput: EditText
    private lateinit var savedLabel: TextView
    private lateinit var saveButton: View
    private lateinit var runButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_code)
        codeInput = findViewById(R.id.code_input)
        savedLabel = findViewById(R.id.saved_label)
        saveButton = findViewById(R.id.btn_save)
        runButton = findViewById(R.id.btn_run)

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
    }

    private fun initClicks() {
        saveButton.setOnClickListener {
            viewModel.saveCode(codeInput.text.toString())
        }
        runButton.setOnClickListener {
            val code = codeInput.text.toString()
            val blockGroup = parseCode(code)
            Log.d("Tokens", blockGroup.toString())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // DO nothing
    }
}

