package elfak.mosis.myplaces

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.Pokemon
import elfak.mosis.myplaces.model.UserViewModel

class XpRewardDialog(
    private val pokemon: Pokemon? = null, // pokemon optional
    private val gainedXp: Int = 100
) : DialogFragment() {

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_xp, null)
        dialog.setContentView(view)

        val user = userViewModel.currentUser.value
        if (user == null) {
            dismiss()
            return dialog
        }

        val playerXpBar = view.findViewById<ProgressBar>(R.id.playerXpBar)
        val playerXpTitle = view.findViewById<TextView>(R.id.playerXpTitle)
        val pokemonXpBar = view.findViewById<ProgressBar>(R.id.pokemonXpBar)
        val pokemonXpTitle = view.findViewById<TextView>(R.id.pokemonXpTitle)


        fun xpForLevel(level: Int): Int {
            // XP potrebna za taj nivo
            return level * 100
        }

        // --------------- PLAYER XP -----------------
        val currentLevel = user.level
        val currentXp = user.xp
        val gainedXp = this.gainedXp

        val startPlayerLevel: Int
        val startPlayerXpPercent: Int
        val endPlayerXpPercent: Int

        if (currentXp < gainedXp && currentLevel > 1) {

            startPlayerLevel = currentLevel - 1
            val xpPrevLevel = xpForLevel(startPlayerLevel)
            startPlayerXpPercent = ( (xpPrevLevel - gainedXp ) * 100 / xpPrevLevel).coerceIn(0, 100)
            endPlayerXpPercent = 100
        } else {

            startPlayerLevel = currentLevel
            val xpThisLevel = xpForLevel(currentLevel)
            startPlayerXpPercent = ((currentXp - gainedXp) * 100 / xpThisLevel).coerceIn(0,100)
            endPlayerXpPercent = (currentXp * 100 / xpThisLevel).coerceIn(0,100)
        }

        playerXpTitle.text = "Level $startPlayerLevel"

        val playerAnimator = ObjectAnimator.ofInt(playerXpBar, "progress", startPlayerXpPercent, endPlayerXpPercent)
        playerAnimator.duration = 1000
        playerAnimator.start()

        playerAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {


                // ---------------- POKEMON XP ----------------
                if (pokemon == null) {
                    pokemonXpBar.visibility = View.GONE
                    pokemonXpTitle.visibility = View.GONE
                }
                else{
                    val pokeLevel = pokemon.level
                    val pokeXp = pokemon.xp
                    val gainedXp = 100

                    val startPokeLevel: Int
                    val startPokeXpPercent: Int
                    val endPokeXpPercent: Int

                    if (pokeXp < gainedXp && pokeLevel > 1) {
                        // Pokémon se upravo level-upovao
                        startPokeLevel = pokeLevel - 1
                        val xpPrevLevel = xpForLevel(startPokeLevel)

                        val startXp = xpPrevLevel - gainedXp   // npr 500 - 100 = 400

                        startPokeXpPercent =
                            (startXp * 100 / xpPrevLevel).coerceIn(0, 100)

                        endPokeXpPercent = 100
                    } else {
                        // Normalna situacija
                        startPokeLevel = pokeLevel
                        val xpThisLevel = xpForLevel(pokeLevel)

                        startPokeXpPercent =
                            ((pokeXp - gainedXp) * 100 / xpThisLevel).coerceIn(0, 100)

                        endPokeXpPercent =
                            (pokeXp * 100 / xpThisLevel).coerceIn(0, 100)
                    }

                    pokemonXpTitle.text = "${pokemon.name} Level $startPokeLevel"

                    val pokeAnimator = ObjectAnimator.ofInt(
                        pokemonXpBar,
                        "progress",
                        startPokeXpPercent,
                        endPokeXpPercent
                    )

                    pokeAnimator.duration = 1000
                    pokeAnimator.start()
                }


            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        playerAnimator.start()

        return dialog
    }
}
