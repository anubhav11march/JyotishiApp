package com.jyotishapp.jyotishi.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.jyotishapp.jyotishi.Models.ActiveUser
import com.jyotishapp.jyotishi.R

class StreamActiveUsersAdapter(
        val context: Context
): RecyclerView.Adapter<StreamActiveUsersAdapter.StreamActiveUsersViewHolder>() {

    inner class StreamActiveUsersViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val userProfile: ShapeableImageView = itemView.findViewById(R.id.user_profile)
        val userProfileName: TextView = itemView.findViewById(R.id.user_profile_name)
    }

    private val differCallback = object : DiffUtil.ItemCallback<ActiveUser>() {
        override fun areItemsTheSame(oldItem: ActiveUser, newItem: ActiveUser) =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: ActiveUser, newItem: ActiveUser) =
                oldItem == newItem
    }

    val asyncListDiffer = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamActiveUsersViewHolder {
        val viewHolder = StreamActiveUsersViewHolder(
                LayoutInflater.from(context)
                        .inflate(R.layout.stream_user_list_item,parent,false)
        )
        return viewHolder
    }

    override fun onBindViewHolder(holder: StreamActiveUsersViewHolder, position: Int) {

        val user = asyncListDiffer.currentList[position]

        val radius: Float = context.resources.getDimension(R.dimen.small_image_corner_radius)
        val shapeAppearanceModel: ShapeAppearanceModel =
            holder.userProfile.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()

        holder.userProfile.shapeAppearanceModel = shapeAppearanceModel

        Glide.with(context)
            .load(user.profileUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.userProfile)

        holder.userProfileName.text = user.username
    }

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }
}