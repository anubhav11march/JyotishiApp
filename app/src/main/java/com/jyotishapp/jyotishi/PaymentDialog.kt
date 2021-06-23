package com.jyotishapp.jyotishi

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.gift_dialog.*
import java.lang.Exception

class PaymentDialog: AppCompatDialogFragment() {

    private var amountInput: TextInputLayout? = null
    private var listener: DialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)

        val layoutInflater = activity?.layoutInflater
        val view = layoutInflater?.inflate(R.layout.gift_dialog,null)

        builder.setView(view)
            .setTitle("Send a Gift to Jyotish Ji!")
            .setNegativeButton("Return to livestream") { _, _ -> }
            .setPositiveButton("Proceed") { dialogInterface , _ ->

                val amountEntered = amountInput?.editText?.text.toString()

                val amount = amountEntered.toIntOrNull()

                if(amount == null) {
                    Toast.makeText(activity?.applicationContext,"Unable to send gift",Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                else {

                    amount?.let { amountToProceed ->

                        listener?.getAmount(amount)
                    }
                }
            }

        amountInput = view?.findViewById(R.id.dialog_amount)

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as DialogListener
        } catch (e: Exception) {
            Log.d("error ",e.message.toString())
        }
    }

    interface DialogListener {

        fun getAmount(amount: Int)
    }
}