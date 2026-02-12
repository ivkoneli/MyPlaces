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
    private val onClick: (Int?, Boolean) -> Unit // Int? = drawable, null = galerija ili kamera, Boolean = isCamera
) : BaseAdapter() {

    override fun getCount() = avatars.size + 2 // +1 galerija, +1 kamera

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


        when {
            position < avatars.size -> imageView.setImageResource(avatars[position])
            position == avatars.size -> imageView.setImageResource(R.drawable.ic_home) // + galerija
            else -> imageView.setImageResource(R.drawable.ic_sort) // + kamera
        }

        imageView.setOnClickListener {
            when {
                position < avatars.size -> onClick(avatars[position], false)
                position == avatars.size -> onClick(null, false)  // galerija
                else -> onClick(null, true) // kamera
            }
        }

        return imageView
    }
}
