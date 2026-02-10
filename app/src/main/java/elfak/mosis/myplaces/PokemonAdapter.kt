import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import elfak.mosis.myplaces.R
import elfak.mosis.myplaces.data.Pokemon

class PokemonAdapter(
    private val isBattleMode: Boolean = false,
    private val showDisown: Boolean = false,
    private val onClick: ((Pokemon) -> Unit)? = null,
    private val onDisownClick: ((Pokemon) -> Unit)? = null
) : ListAdapter<Pokemon, PokemonAdapter.PokemonViewHolder>(DiffCallback()) {

    inner class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnDisown: ImageButton? = itemView.findViewById(R.id.btnDisown)
        private val name: TextView = itemView.findViewById(R.id.pokemonName)
        private val maxHp: TextView = itemView.findViewById(R.id.pokemonMaxHp)
        private val currHp: TextView = itemView.findViewById(R.id.pokemonCurrentHp)
        private val atk: TextView = itemView.findViewById(R.id.pokemonAtk)
        private val hpBar: ProgressBar = itemView.findViewById(R.id.pokemonHpBar)

        fun bind(pokemon: Pokemon) {
            val textColor = if (isBattleMode) Color.WHITE else Color.BLACK

            name.text = "Lv.${pokemon.level} ${pokemon.name}"
            name.setTextColor(textColor)
            maxHp.text = "/ ${pokemon.maxhp}"
            maxHp.setTextColor(textColor)
            currHp.text = "❤️ ${pokemon.currenthp}"
            currHp.setTextColor(textColor)
            atk.text = "⚔️ ${pokemon.attack}"
            atk.setTextColor(textColor)

            val hpPercent = if (pokemon.maxhp > 0) (pokemon.currenthp.toFloat() / pokemon.maxhp * 100).toInt() else 0
            hpBar.progress = hpPercent
            val color = when {
                hpPercent >= 50 -> android.R.color.holo_green_light
                hpPercent >= 20 -> android.R.color.holo_orange_light
                else -> android.R.color.holo_red_light
            }
            hpBar.progressTintList = itemView.context.getColorStateList(color)

            val isAlive = pokemon.isAlive
            itemView.alpha = if (isAlive) 1f else 0.4f

            // ⚡ Osiguraj da item može da reaguje
            itemView.isClickable = true
            itemView.isEnabled = true

            // 🔥 Ovo je klik itema
            itemView.setOnClickListener {
                Log.d("PokemonAdapter", "Clicked on: ${pokemon.name} (Lv.${pokemon.level})")
                onClick?.invoke(pokemon)
            }

            // 🔥 Dugme za disown samo ako je showDisown true
            btnDisown?.let { btn ->
                if (showDisown) {
                    btn.visibility = View.VISIBLE
                    btn.setOnClickListener { onDisownClick?.invoke(pokemon) }
                } else {
                    btn.visibility = View.GONE
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pokemon>() {
        override fun areItemsTheSame(oldItem: Pokemon, newItem: Pokemon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean {
            // uključi sva polja koja utiču na UI
            return oldItem.level == newItem.level &&
                    oldItem.name == newItem.name &&
                    oldItem.currenthp == newItem.currenthp &&
                    oldItem.maxhp == newItem.maxhp &&
                    oldItem.attack == newItem.attack &&
                    oldItem.isAlive == newItem.isAlive
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pokemon_list, parent, false)
        return PokemonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) {
        holder.bind(getItem(position)) // poziva bind sa itemom, lambda je u bind()
    }
}
