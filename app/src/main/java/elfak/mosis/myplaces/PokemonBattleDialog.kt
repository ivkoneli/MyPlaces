package elfak.mosis.myplaces

import PokemonAdapter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import elfak.mosis.myplaces.auth.AuthRepository
import elfak.mosis.myplaces.data.AppUser
import elfak.mosis.myplaces.data.MyPlace
import elfak.mosis.myplaces.data.Pokemon
import elfak.mosis.myplaces.model.BattleViewModel
import elfak.mosis.myplaces.model.MyPlacesViewModel
import elfak.mosis.myplaces.model.UserViewModel

class PokemonBattleDialog : DialogFragment() {

    private val battleViewModel: BattleViewModel by viewModels()
    private val myPlacesViewModel: MyPlacesViewModel by activityViewModels()
    private val userViewModel : UserViewModel by activityViewModels()
    private val authRepo = AuthRepository()
    private lateinit var pokemonList: RecyclerView
    private lateinit var battleLog: TextView


    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.5).toInt()
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_pokemon_battle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pokemonList = view.findViewById(R.id.pokemonSelectList)
        battleLog = view.findViewById(R.id.battleLog)
        val startBattleBtn = view.findViewById<Button>(R.id.btnStartBattle)
        val finishBattleBtn = view.findViewById<Button>(R.id.btnFinishBattle)
        val playerPokemonName = view.findViewById<TextView>(R.id.playerPokemonName)
        val playerPokemonAttack = view.findViewById<TextView>(R.id.playerPokemonAtk)
        val playerPokemonHealth = view.findViewById<TextView>(R.id.playerPokemonHpText)
        val playerPokemonHpbar = view.findViewById<ProgressBar>(R.id.playerPokemonHpBar)
        val enemyPokemonName = view.findViewById<TextView>(R.id.enemyPokemonName)
        val enemyPokemonAttack = view.findViewById<TextView>(R.id.enemyPokemonAtk)
        val enemyPokemonHealth = view.findViewById<TextView>(R.id.enemyPokemonHpText)
        val enemyPokemonHpbar = view.findViewById<ProgressBar>(R.id.enemyPokemonHpBar)
        val chooseBtn = view.findViewById<Button>(R.id.btnChoosePokemon)

        var pokemonCount = 0
        val adapter = PokemonAdapter(
            isBattleMode = true
        ) { selected ->
            battleViewModel.setPlayerPokemon(selected)
            startBattleBtn.visibility = View.VISIBLE
            chooseBtn.visibility = View.GONE
            playerPokemonName.text = "Lv.${selected.level}   ${selected.name}"
            playerPokemonAttack.text = "${selected.attack}"
            playerPokemonHealth.text = "❤ ${selected.currenthp} / ${selected.maxhp}"
            showBattleLog()
        }

        pokemonList.adapter = adapter
        pokemonList.layoutManager = LinearLayoutManager(requireContext())

        chooseBtn.setOnClickListener {
            startBattleBtn.visibility = View.GONE
            chooseBtn.visibility = View.VISIBLE
            showPokemonSelection()
        }

        val place = arguments?.getParcelable<MyPlace>("enemy_place")
            ?: return


        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            userViewModel.fetchUserPokemons(user.uid) { pokemons ->
                adapter.submitList(pokemons)
                pokemonCount = pokemons.size
            }
        }
        battleViewModel.selectedPlayerPokemon.observe(viewLifecycleOwner) { pokemon ->
            pokemon ?: return@observe

            playerPokemonHealth.text =
                "❤ ${pokemon.currenthp} / ${pokemon.maxhp}"

            playerPokemonHpbar.max = pokemon.maxhp
            playerPokemonHpbar.progress = pokemon.currenthp
            updateHpColor(playerPokemonHpbar, pokemon.currenthp, pokemon.maxhp)
        }

        battleViewModel.enemyPokemon.observe(viewLifecycleOwner) { pokemon ->
            pokemon ?: return@observe

            enemyPokemonHealth.text =
                "❤ ${pokemon.currenthp} / ${pokemon.maxhp}"

            enemyPokemonHpbar.max = pokemon.maxhp
            enemyPokemonHpbar.progress = pokemon.currenthp
            updateHpColor(enemyPokemonHpbar, pokemon.currenthp, pokemon.maxhp)
        }


        val enemyPokemon = Pokemon(
            id = place.id,
            name = place.name,
            maxhp = place.level * 100,
            currenthp = place.level * 100,
            attack = place.level * 10,
            ownerId = "" ,// prazno dok ne pobedi
            level = place.level

        )

        battleViewModel.setEnemyPokemon(enemyPokemon)
        enemyPokemonName.text = "Lv.${place.level}   ${enemyPokemon.name}"
        enemyPokemonAttack.text = "${enemyPokemon.attack}"
        enemyPokemonHealth.text = "❤ ${enemyPokemon.currenthp} / ${enemyPokemon.maxhp}"

        val scrollView = view.findViewById<ScrollView>(R.id.battleLogScroll)
        battleViewModel.battleLog.observe(viewLifecycleOwner) { log ->
            if (log.isNullOrEmpty()) return@observe

            showBattleLog()

            battleLog.text = formatBattleLog(log)

            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

        startBattleBtn.setOnClickListener {

            battleViewModel.battleLog.value = listOf("Battle starts...")
            battleLog.text = "Battle starts..."
            showBattleLog()


            val user = userViewModel.currentUser.value ?: return@setOnClickListener
            val playerPoke = battleViewModel.selectedPlayerPokemon.value ?: return@setOnClickListener
            val capturable = pokemonCount < user.level + 1

            battleViewModel.startBattle(
                onWin = { defeatedPokemon ->
                    addBattleXP(user)
                    playerPoke.addBattleXP(100)
                    updatePokemonAfterBattle(playerPoke)

                    showCaptureDialog(
                        defeatedPokemon = defeatedPokemon,
                        canCapture = capturable,
                        onCapture = {
                            capturePokemon(defeatedPokemon)
                            showXpPopup(user,playerPoke)
                        },
                        onSkip = {
                            showXpPopup(user,playerPoke)
                        }
                    )

                    startBattleBtn.visibility = View.GONE
                    finishBattleBtn.visibility = View.VISIBLE
                },
                onLose = {
                    AddLoss(user)
                    updatePokemonAfterBattle(playerPoke)
                    Toast.makeText(requireContext(), "You lost the battle 😢", Toast.LENGTH_SHORT).show()

                    startBattleBtn.visibility = View.GONE
                    finishBattleBtn.visibility = View.VISIBLE
                }
            )
        }

        finishBattleBtn.setOnClickListener {
            dismiss()
        }

    }

    companion object {
        fun newInstance(place: MyPlace): PokemonBattleDialog {
            return PokemonBattleDialog().apply {
                arguments = bundleOf("enemy_place" to place)
            }
        }
    }
    private fun showPokemonSelection() {
        pokemonList.visibility = View.VISIBLE
        battleLog.visibility  = View.GONE
    }


    private fun showBattleLog() {
        pokemonList.visibility  = View.GONE
        battleLog.visibility   = View.VISIBLE
    }

    fun updatePokemonAfterBattle(player: Pokemon) {

        val user = userViewModel.currentUser.value ?: return
        userViewModel.currentUser.value = user

        val db = FirebaseFirestore.getInstance()

        val data = mapOf(
            "xp" to player.xp,
            "level" to player.level,
            "maxhp" to player.maxhp,
            "currenthp" to player.currenthp.coerceAtLeast(0),
            "attack" to player.attack
        )

        db.collection("pokemons")
            .document(player.id)
            .update(data)
            .addOnSuccessListener {
                Log.d("Battle", "Pokémon state saved after battle")
            }
            .addOnFailureListener { e ->
                Log.e("Battle", "Failed to save Pokémon: ${e.message}")
            }
    }
    fun updateHpColor(bar: ProgressBar, current: Int, max: Int) {
        val percent = current * 100 / max
        val color = when {
            percent > 50 -> android.R.color.holo_green_light
            percent > 20 -> android.R.color.holo_orange_light
            else -> android.R.color.holo_red_light
        }
        bar.progressTintList =
            ContextCompat.getColorStateList(requireContext(), color)
    }


    private fun addBattleXP(user: AppUser) {
        val db = FirebaseFirestore.getInstance()
        var newXp = user.xp + 100
        var newLevel = user.level
        var xpThreshold = user.level * 100
        var newWins = user.wins + 1

        // Proveravamo level up dokle god XP prelazi threshold
        while (newXp >= xpThreshold && newLevel < 30) {
            newXp -= xpThreshold
            newLevel += 1
            xpThreshold = newLevel * 100
        }

        // Update u Firebase
        db.collection("users").document(user.uid)
            .update(mapOf("xp" to newXp, "level" to newLevel , "wins" to newWins))
            .addOnSuccessListener {
                // Update u UserViewModel
                userViewModel.currentUser.value = user.copy(level = newLevel, xp = newXp, wins = newWins)
                Toast.makeText(requireContext(),
                    "Battle won! XP +100, Level: $newLevel", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Battle won! XP +100, but failed to update level: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun AddLoss(user : AppUser){

        val db = FirebaseFirestore.getInstance()

        var newLoses = user.loses + 1

        // Update u Firebase
        db.collection("users").document(user.uid)
            .update(mapOf("loses" to newLoses))
            .addOnSuccessListener {
                userViewModel.currentUser.value = user.copy(loses = newLoses)
            }
    }


    private fun formatBattleLog(lines: List<String>): CharSequence {
        val builder = SpannableStringBuilder()

        lines.forEach { line ->
            val start = builder.length
            builder.append(line).append("\n")

            // Damage broj (crveno)
            Regex("\\d+").findAll(line).forEach { match ->
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    start + match.range.first,
                    start + match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            // Player pokemon (plavo)
            battleViewModel.selectedPlayerPokemon.value?.name?.let { playerName ->
                val index = line.indexOf(playerName)
                if (index >= 0) {
                    builder.setSpan(
                        ForegroundColorSpan(Color.CYAN),
                        start + index,
                        start + index + playerName.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            // Enemy pokemon (narandžasto)
            battleViewModel.enemyPokemon.value?.name?.let { enemyName ->
                val index = line.indexOf(enemyName)
                if (index >= 0) {
                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#FFA500")),
                        start + index,
                        start + index + enemyName.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            // WIN / LOSE styling
            if (line.contains("You won")) {
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    start + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    ForegroundColorSpan(Color.GREEN),
                    start,
                    start + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (line.contains("You lost")) {
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    start + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    ForegroundColorSpan(Color.RED),
                    start,
                    start + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (line.contains("XP")) {
                builder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#8BC34A")), // light green
                    start,
                    start + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        }

        return builder
    }

    private fun capturePokemon(pokemon: Pokemon) {
        val user = userViewModel.currentUser.value ?: return

        authRepo.addPokemonToUser(
            pokemon = pokemon,
            ownerId = user.uid,
            onSuccess = { addBattleXP(user) },
            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        )
    }
    private fun showCaptureDialog(
        defeatedPokemon: Pokemon,
        canCapture : Boolean,
        onCapture: () -> Unit,
        onSkip: () -> Unit
    ) {

        CaptureDialog(
            defeatedPokemonName = defeatedPokemon.name,
            canCapture = canCapture,
            onCapture = onCapture,
            onSkip = onSkip
        ).show(parentFragmentManager, "capture_dialog")
    }


   private fun showXpPopup(user: AppUser ,player: Pokemon) {
        XpRewardDialog(player, 100)
        .show(parentFragmentManager, "xp_dialog")
   }

}
