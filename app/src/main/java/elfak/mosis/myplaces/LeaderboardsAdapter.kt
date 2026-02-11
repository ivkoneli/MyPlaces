package elfak.mosis.myplaces

import android.graphics.BitmapFactory
import elfak.mosis.myplaces.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

private const val VIEW_TYPE_TOP3 = 0
private const val VIEW_TYPE_NORMAL = 1

class UserLeaderboardAdapter(
    private var users: List<Pair<String, Int>>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 👈 promenjeno ovde

    private var avatarMap: Map<String, String?> = emptyMap()

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.userAvatar)
        val rank: TextView = itemView.findViewById(R.id.userRank)
        val username: TextView = itemView.findViewById(R.id.userName)
        val wins: TextView = itemView.findViewById(R.id.userWins)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TOP3) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.userleaderboards_top3, parent, false)
            Top3ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.userleaderboard_list, parent, false)
            UserViewHolder(view)
        }
    }

    // Top 3 usera je 1 item count
    override fun getItemCount(): Int {
        return if (users.size <= 3) {
            1 // samo TOP3 view sa koliko god ima korisnika (1,2 ili 3)
        } else {
            1 + (users.size - 3) // 1 TOP3 + ostali normalni
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (users.isEmpty()) return

        if (holder is Top3ViewHolder) {
            holder.bind(users.take(3), avatarMap, onUserClick)
        } else if (holder is UserViewHolder) {
            val dataIndex = position - 1 + 3
            if (dataIndex >= users.size) return
            val (username, wins) = users[dataIndex]

            holder.rank.text = "#${dataIndex + 1}"
            holder.username.text = username
            holder.wins.text = "🏆  $wins"

            // load avatar ako postoji
            val avatarPath = avatarMap[username]
            if (!avatarPath.isNullOrEmpty()) {
                if (avatarPath.startsWith("drawable://")) {
                    val resId = avatarPath.removePrefix("drawable://").toInt()
                    holder.avatar.setImageResource(resId)
                } else {
                    val file = File(avatarPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        holder.avatar.setImageBitmap(bitmap)
                    } else {
                        holder.avatar.setImageResource(R.drawable.ic_pokemon_placeholder)
                    }
                }
            } else {
                holder.avatar.setImageResource(R.drawable.ic_pokemon_placeholder)
            }


            holder.itemView.setOnClickListener { onUserClick(username) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_TOP3 else VIEW_TYPE_NORMAL
    }

    fun updateData(newUsers: List<Pair<String, Int>>) {
        users = newUsers
        notifyDataSetChanged()
    }

    fun updateAvatars(newAvatarMap: Map<String, String?>) {
        avatarMap = newAvatarMap
        notifyDataSetChanged()
    }
}
