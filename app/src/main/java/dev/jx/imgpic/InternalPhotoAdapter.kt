package dev.jx.imgpic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jx.imgpic.databinding.ItemPhotoBinding

class InternalPhotoAdapter(private val itemClick: (Photo) -> Unit) :
    ListAdapter<Photo, InternalPhotoAdapter.PhotoViewHolder>(Companion) {

    companion object : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(oldItem: Photo, newItem: Photo) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Photo, newItem: Photo) = oldItem == newItem
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
        val aspectRatio = photo.bmp.width.toFloat() / photo.bmp.height.toFloat()

        with (holder.binding) {
            itemPhoto.setImageBitmap(photo.bmp)
            itemPhoto.setOnClickListener { itemClick(photo) }

            ConstraintSet().apply {
                clone(root)
                setDimensionRatio(itemPhoto.id, aspectRatio.toString())
                applyTo(root)
            }
        }
    }

}
