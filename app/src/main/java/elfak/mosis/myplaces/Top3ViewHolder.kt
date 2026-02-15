package elfak.mosis.myplaces

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class Top3ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val goldAvatar: ImageView = itemView.findViewById(R.id.goldAvatar)
    val goldUsername: TextView = itemView.findViewById(R.id.goldUserName)
    val goldWins: TextView = itemView.findViewById(R.id.goldWins)

    val silverAvatar: ImageView = itemView.findViewById(R.id.silverAvatar)
    val silverUsername: TextView = itemView.findViewById(R.id.silverUserName)
    val silverWins: TextView = itemView.findViewById(R.id.silverWins)

    val bronzeAvatar: ImageView = itemView.findViewById(R.id.bronzeAvatar)
    val bronzeUsername: TextView = itemView.findViewById(R.id.bronzeUserName)
    val bronzeWins: TextView = itemView.findViewById(R.id.bronzeWins)

    fun bind(
        topUsers: List<Pair<String, Int>>,
        avatarMap: Map<String, String?>,
        onUserClick: (String) -> Unit
    ) {
        if (topUsers.isEmpty()) return

        fun loadAvatar(username: String, imageView: ImageView) {
            val path = avatarMap[username]
            if (!path.isNullOrEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(R.drawable.ic_pokemon_placeholder)
                }
            } else {
                imageView.setImageResource(R.drawable.ic_pokemon_placeholder)
            }
        }

        // gold
        if (topUsers.size > 0) {
            val (goldName, goldWinsCount) = topUsers[0]
            goldUsername.text = goldName
            goldWins.text = "🏆 $goldWinsCount"
            loadAvatar(goldName, goldAvatar)

            goldAvatar.setOnClickListener { onUserClick(goldName) }
            goldUsername.setOnClickListener { onUserClick(goldName) }
        }

        // silver
        if (topUsers.size > 1) {
            val (silverName, silverWinsCount) = topUsers[1]
            silverUsername.text = silverName
            silverWins.text = "🏆 $silverWinsCount"
            loadAvatar(silverName, silverAvatar)

            silverAvatar.setOnClickListener { onUserClick(silverName) }
            silverUsername.setOnClickListener { onUserClick(silverName) }
        }

        // bronze
        if (topUsers.size > 2) {
            val (bronzeName, bronzeWinsCount) = topUsers[2]
            bronzeUsername.text = bronzeName
            bronzeWins.text = "🏆 $bronzeWinsCount"
            loadAvatar(bronzeName, bronzeAvatar)

            bronzeAvatar.setOnClickListener { onUserClick(bronzeName) }
            bronzeUsername.setOnClickListener { onUserClick(bronzeName) }
        }
    }
}