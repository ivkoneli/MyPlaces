package elfak.mosis.myplaces

import elfak.mosis.myplaces.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserLeaderboardAdapter(
    private var users: List<Pair<String, Int>> // Pair<username, wins>
) : RecyclerView.Adapter<UserLeaderboardAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.userAvatar)
        val info: TextView = itemView.findViewById(R.id.userInfo)
        val rank: TextView = itemView.findViewById(R.id.userRank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.userleaderboard_list, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val (username, wins) = users[position]
        holder.info.text = "$username - 🏆 Wins: $wins"
        holder.rank.text = "#${position + 1}"
        holder.avatar.setImageResource(R.drawable.ic_pokemon_placeholder)
    }

    fun updateData(newUsers: List<Pair<String, Int>>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
