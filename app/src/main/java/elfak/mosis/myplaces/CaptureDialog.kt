package elfak.mosis.myplaces

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class CaptureDialog(
    private val defeatedPokemonName: String,
    private val canCapture: Boolean,
    private val onCapture: () -> Unit,
    private val onSkip: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_capture, container, false)

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.5).toInt()
        )
        
        val captureBtn = view.findViewById<Button>(R.id.btnCapture)
        val skipBtn = view.findViewById<Button>(R.id.btnSkip)
        val titleText = view.findViewById<TextView>(R.id.captureTitle)
        titleText.text = "\uD83C\uDF89 You defeated ${defeatedPokemonName}! \uD83C\uDF89"

        if (!canCapture) {
            captureBtn.isEnabled = false
            captureBtn.text = "No slots"
        } else {
            captureBtn.setOnClickListener {
                onCapture()
                dismiss()
            }
        }

        skipBtn.setOnClickListener {
            onSkip()
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            (resources.displayMetrics.heightPixels * 0.5).toInt()
        )
        dialog?.window?.setGravity(Gravity.CENTER)
    }
}