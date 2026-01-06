import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import elfak.mosis.myplaces.R
import elfak.mosis.myplaces.data.Pokemon



class PokemonAdapter(
    private val isBattleMode: Boolean = false,
    private val onClick: ((Pokemon) -> Unit)? = null
): ListAdapter<Pokemon, PokemonAdapter.PokemonViewHolder>(DiffCallback()) {

    inner class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name = itemView.findViewById<TextView>(R.id.pokemonName)
        private val maxHp = itemView.findViewById<TextView>(R.id.pokemonMaxHp)
        private val currHp = itemView.findViewById<TextView>(R.id.pokemonCurrentHp)
        private val atk = itemView.findViewById<TextView>(R.id.pokemonAtk)
        private val hpBar = itemView.findViewById<ProgressBar>(R.id.pokemonHpBar)


        fun bind(pokemon: Pokemon, isBattleMode: Boolean) {
            name.text = "Lv.${pokemon.level} ${pokemon.name}"
            maxHp.text = "/ ${pokemon.maxhp}"
            currHp.text = "❤️ ${pokemon.currenthp}"
            atk.text = "⚔️ ${pokemon.attack}"

            val textcolor = if (isBattleMode) Color.WHITE else Color.BLACK

            name.setTextColor(textcolor)
            maxHp.setTextColor(textcolor)
            currHp.setTextColor(textcolor)
            atk.setTextColor(textcolor)

            // Izračunava koliko je HP procentualno
            val hpPercent = if (pokemon.maxhp > 0) {
                (pokemon.currenthp.toFloat() / pokemon.maxhp * 100).toInt()
            } else 0
            hpBar.progress = hpPercent

            // Menja boju bara po nivou HP
            val color = when {
                hpPercent >= 50 -> android.R.color.holo_green_light
                hpPercent >= 20 -> android.R.color.holo_orange_light
                else -> android.R.color.holo_red_light
            }
            hpBar.progressTintList = itemView.context.getColorStateList(color)

            val isAlive = pokemon.currenthp > 0
            itemView.alpha = if (isAlive) 1f else 0.4f
            itemView.isEnabled = isAlive

            // ⚡ Ovo je ključno:
            itemView.setOnClickListener {
                onClick?.invoke(pokemon)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pokemon>() {
        override fun areItemsTheSame(oldItem: Pokemon, newItem: Pokemon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pokemon, newItem: Pokemon) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pokemon_list, parent, false) // ovde mora da bude tvoj layout fajl za jednog Pokemona
        return PokemonViewHolder(view)
    }


    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        val pokemon = getItem(position)

        holder.bind(pokemon, isBattleMode)

        holder.itemView.setOnClickListener {
            onClick?.invoke(pokemon)
        }
    }


}
