package ani.saikou.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.ItemGenreBinding
import ani.saikou.loadImage
import ani.saikou.px

class GenreAdapter(
    private val type: String,
    private val big: Boolean = false
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {
    var genres = mutableMapOf<String, String>()
    var pos = arrayListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height = 72f.px }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
        if (pos.size > position) {
            val genre = genres[pos[position]]
            binding.genreTitle.text = pos[position]
            binding.genreImage.loadImage(genre)
        }
    }

    override fun getItemCount(): Int = genres.size
    inner class GenreViewHolder(val binding: ItemGenreBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(itemView.context, SearchActivity::class.java)
                        .putExtra("type", type)
                        .putExtra("genre", pos[bindingAdapterPosition])
                        .putExtra("sortBy", "Trending")
                        .putExtra("search", true)
                        .also {
                        if (pos[bindingAdapterPosition].lowercase() == "hentai") {
                            if (!Anilist.adult) Toast.makeText(
                                itemView.context,
                                "Attiva i contenuti +18 nelle impostazione del tuo account Anilist",
                                Toast.LENGTH_SHORT
                            ).show()
                            it.putExtra("hentai", true)
                        }
                    },
                    null
                )
            }
        }
    }

    fun addGenre(genre: Pair<String, String>) {
        genres[genre.first] = genre.second
        pos.add(genre.first)
        notifyItemInserted(pos.size - 1)
    }
}