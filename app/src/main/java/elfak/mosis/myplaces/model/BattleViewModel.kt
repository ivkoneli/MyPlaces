package elfak.mosis.myplaces.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import elfak.mosis.myplaces.data.Pokemon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BattleViewModel : ViewModel() {

    val selectedPlayerPokemon = MutableLiveData<Pokemon>()
    val enemyPokemon = MutableLiveData<Pokemon>()
    val battleLog = MutableLiveData<List<String>>()
    val battleResult = MutableLiveData<Boolean>() // true = win

    fun setPlayerPokemon(pokemon: Pokemon) {
        selectedPlayerPokemon.value = pokemon
    }

    fun setEnemyPokemon(pokemon: Pokemon) {
        enemyPokemon.value = pokemon
    }
    fun startBattle(
        onWin: (Pokemon) -> Unit,
        onLose: () -> Unit
    ) {
        val player = selectedPlayerPokemon.value ?: return
        val enemy = enemyPokemon.value ?: return

        val logLines = mutableListOf<String>()

        viewModelScope.launch {

            while (player.currenthp > 0 && enemy.currenthp > 0) {

                // PLAYER HITS
                enemy.currenthp =
                    (enemy.currenthp - player.attack).coerceAtLeast(0)

                logLines.add("${player.name} hits ${enemy.name} for ${player.attack}")
                battleLog.postValue(logLines.toList())
                enemyPokemon.postValue(enemy)

                delay(700)

                if (enemy.currenthp <= 0) break

                // ENEMY HITS
                player.currenthp =
                    (player.currenthp - enemy.attack).coerceAtLeast(0)

                logLines.add("${enemy.name} hits back for ${enemy.attack}")
                battleLog.postValue(logLines.toList())
                selectedPlayerPokemon.postValue(player)

                delay(700)
            }

            val playerWon = player.currenthp > 0

            logLines.add(if (playerWon) "You won!" else "You lost!")
            if (playerWon) logLines.add("Gained 100 XP!")
            if (playerWon) enemy.alive = false

            battleLog.postValue(logLines.toList())

            delay(800)

            if (playerWon) {
                onWin(enemy)
            } else {
                onLose()
            }
        }
    }

}

