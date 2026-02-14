package elfak.mosis.myplaces

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment

class StarterDialog(
    private val onPokemonChosen: (String) -> Unit
) : DialogFragment() {

    private lateinit var overlayPikachu: View
    private lateinit var overlayCharmander: View
    private lateinit var overlayBulbasaur: View

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(), // 90% width ekrana
            (resources.displayMetrics.heightPixels * 0.5).toInt() // fiksnih 50% height ekrana
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = layoutInflater.inflate(R.layout.dialog_choose_starter, null)
        dialog.setContentView(view)

        dialog.window?.setGravity(Gravity.CENTER)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val btnPikachu = view.findViewById<Button>(R.id.btnPickPikachu)
        val btnCharmander = view.findViewById<Button>(R.id.btnPickCharmander)
        val btnBulbasaur = view.findViewById<Button>(R.id.btnPickBulbasaur)

        overlayPikachu = view.findViewById(R.id.overlayPikachu)
        overlayCharmander = view.findViewById(R.id.overlayCharmander)
        overlayBulbasaur = view.findViewById(R.id.overlayBulbasaur)

        btnPikachu.setOnClickListener {
            onPokemonChosen("Pikachu")
            dismiss()
        }

        btnCharmander.setOnClickListener {
            onPokemonChosen("Charmander")
            dismiss()
        }

        btnBulbasaur.setOnClickListener {
            onPokemonChosen("Bulbasaur")
            dismiss()
        }

        startBlinkingEffect()

        return dialog
    }

    private fun startBlinkingEffect() {
        val overlays = listOf(overlayPikachu, overlayCharmander, overlayBulbasaur)
        val handler = Handler()
        var currentIndex = 0

        val blinkRunnable = object : Runnable {
            override fun run() {
                // blink trenutni overlay 2-3 puta
                val overlay = overlays[currentIndex]
                val anim = AlphaAnimation(0f, 0.8f).apply {
                    duration = 600
                    repeatMode = Animation.REVERSE
                    repeatCount = 2
                }
                overlay.visibility = View.VISIBLE
                overlay.startAnimation(anim)

                // sakrij overlay posle animacije
                handler.postDelayed({ overlay.visibility = View.GONE }, 2400)

                // idemo na sledeći overlay
                currentIndex = (currentIndex + 1) % overlays.size
                handler.postDelayed(this, 2700)
            }
        }
        handler.post(blinkRunnable)
    }
}
