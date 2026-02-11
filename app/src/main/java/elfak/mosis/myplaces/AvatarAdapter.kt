import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ImageView
import elfak.mosis.myplaces.R

class AvatarAdapter(
    private val context: Context,
    private val avatars: List<Int>, // drawable res IDs
    private val onClick: (Int?) -> Unit // Int? = drawable id, null = galerija
) : BaseAdapter() {

    override fun getCount() = avatars.size + 1 // +1 za galeriju

    override fun getItem(position: Int): Any? =
        if (position < avatars.size) avatars[position] else null

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = (convertView as? ImageView) ?: ImageView(context).apply {
            layoutParams = AbsListView.LayoutParams(140, 140)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(4, 4, 4, 4)
            setBackgroundResource(R.drawable.avatar_border)
        }


        if (position < avatars.size) {
            imageView.setImageResource(avatars[position])
        } else {
            imageView.setImageResource(R.drawable.ic_home) // "+" ikonca za galeriju
        }

        imageView.setOnClickListener {
            onClick(if (position < avatars.size) avatars[position] else null)
        }

        return imageView
    }
}
