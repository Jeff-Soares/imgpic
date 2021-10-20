package dev.jx.imgpic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jx.imgpic.databinding.ItemPhotoBinding

class ExternalPhotoAdapter(private val itemClick: (SharedPhoto) -> Unit) :
    ListAdapter<SharedPhoto, ExternalPhotoAdapter.PhotoViewHolder>(Companion) {

    companion object : DiffUtil.ItemCallback<SharedPhoto>() {
        override fun areItemsTheSame(oldItem: SharedPhoto, newItem: SharedPhoto) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: SharedPhoto, newItem: SharedPhoto) =
            oldItem == newItem
    }

    inner class PhotoViewHolder(val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = currentList[position]
        val aspectRatio = photo.width.toFloat() / photo.height.toFloat()

        with(holder.binding) {
            itemPhoto.setImageURI(photo.contentUri)
            itemPhoto.setOnClickListener { itemClick(photo) }

            ConstraintSet().apply {
                clone(root)
                setDimensionRatio(itemPhoto.id, aspectRatio.toString())
                applyTo(root)
            }
        }
    }

}
