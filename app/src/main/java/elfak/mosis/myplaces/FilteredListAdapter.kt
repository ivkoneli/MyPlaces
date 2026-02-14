package elfak.mosis.myplaces

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import elfak.mosis.myplaces.data.MyPlace
import java.text.SimpleDateFormat
import java.util.*

class FilteredListAdapter(
    private val onItemClick: (MyPlace) -> Unit
) : ListAdapter<MyPlace, FilteredListAdapter.PlaceViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MyPlace>() {
            override fun areItemsTheSame(oldItem: MyPlace, newItem: MyPlace) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MyPlace, newItem: MyPlace) = oldItem == newItem
        }
    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.placeName)
        private val type: TextView = itemView.findViewById(R.id.placeType)
        private val dateText: TextView = itemView.findViewById(R.id.placeDate)
        private val image: ImageView = itemView.findViewById(R.id.placeImage)

        fun bind(place: MyPlace) {
            // 1️⃣ Ime mesta
            name.text = place.name

            // 2️⃣ Level + type
            type.text = "Lv.${place.level} • ${place.type}"

            // 3️⃣ Datum
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(place.date))
            dateText.text = formattedDate

            // 4️⃣ Slika po tipu
            image.setImageResource(
                when (place.type) {
                    "Pokemon" -> R.drawable.poke_stop
                    "Pokestop" -> R.drawable.poke_xp
                    "Healing" -> R.drawable.poke_heal
                    else -> R.drawable.poke_stop
                }
            )

            // 5️⃣ Klik listener
            itemView.setOnClickListener { onItemClick(place) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PlaceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.place_item, parent, false))

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
