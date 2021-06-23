package com.jyotishapp.jyotishi.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.jyotishapp.jyotishi.Models.ChatUser
import com.jyotishapp.jyotishi.R

class StreamChatAdapter(
        val context: Context
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val Admin = 0
    private val Participant = 1
    private val Gift = 2

    inner class StreamGiftViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val giftImage: ShapeableImageView = itemView.findViewById(R.id.gift_image)
        val giftName: TextView = itemView.findViewById(R.id.gift_name)
        val giftMessage: TextView = itemView.findViewById(R.id.gift_message)
        val giftTime: TextView = itemView.findViewById(R.id.gift_time)
    }

    inner class StreamViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val userImage: ShapeableImageView = itemView.findViewById(R.id.user_image)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userMessage: TextView = itemView.findViewById(R.id.user_message)
        val userTime: TextView = itemView.findViewById(R.id.user_time)
    }

    inner class StreamAdminViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val adminImage: ShapeableImageView = itemView.findViewById(R.id.admin_image)
        val adminName: TextView = itemView.findViewById(R.id.admin_name)
        val adminMessage: TextView = itemView.findViewById(R.id.admin_message)
        val adminTime: TextView = itemView.findViewById(R.id.admin_time)
    }

    private val differCallback = object : DiffUtil.ItemCallback<ChatUser>() {
        override fun areItemsTheSame(oldItem: ChatUser, newItem: ChatUser) =
                oldItem.hashCode() == newItem.hashCode()

        override fun areContentsTheSame(oldItem: ChatUser, newItem: ChatUser) =
                oldItem == newItem
    }

    val asyncListDiffer = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when(viewType) {

            Gift -> {
                StreamGiftViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.stream_gift_viewholder,parent,false)
                )
            }
            Admin -> {
                StreamAdminViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.stream_admin_viewholder,parent,false)
                )
            }
            Participant -> {
                StreamViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.stream_viewholder,parent,false)
                )
            }
            else -> {
                StreamViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.stream_viewholder,parent,false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType) {
            Gift -> {

                val user = asyncListDiffer.currentList[position]
                val giftHolder = holder as StreamGiftViewHolder

                val radius: Float = context.resources.getDimension(R.dimen.image_corner_radius)
                val shapeAppearanceModel: ShapeAppearanceModel =
                    giftHolder.giftImage.shapeAppearanceModel
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, radius)
                        .build()

                giftHolder.giftImage.shapeAppearanceModel = shapeAppearanceModel

                Glide.with(context)
                    .load(user.profileUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(giftHolder.giftImage)

                giftHolder.giftName.text = user.username
                giftHolder.giftMessage.text = user.message
                giftHolder.giftTime.text = user.time

            }
            Admin -> {

                val user = asyncListDiffer.currentList[position]
                val adminHolder = holder as StreamAdminViewHolder

                val radius: Float = context.resources.getDimension(R.dimen.image_corner_radius)
                val shapeAppearanceModel: ShapeAppearanceModel =
                    adminHolder.adminImage.shapeAppearanceModel
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, radius)
                        .build()

                adminHolder.adminImage.shapeAppearanceModel = shapeAppearanceModel

                Glide.with(context)
                    .load(user.profileUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(adminHolder.adminImage)

                adminHolder.adminName.text = user.username
                adminHolder.adminMessage.text = user.message
                adminHolder.adminTime.text = user.time

            }
            Participant -> {

                val user = asyncListDiffer.currentList[position]
                val userHolder = holder as StreamViewHolder

                val radius: Float = context.resources.getDimension(R.dimen.image_corner_radius)
                val shapeAppearanceModel: ShapeAppearanceModel =
                    userHolder.userImage.shapeAppearanceModel
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, radius)
                        .build()

                userHolder.userImage.shapeAppearanceModel = shapeAppearanceModel

                Glide.with(context)
                    .load(user.profileUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(userHolder.userImage)

                userHolder.userName.text = user.username
                userHolder.userMessage.text = user.message
                userHolder.userTime.text = user.time
            }
        }
    }

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }

    override fun getItemViewType(position: Int): Int {

        return when {
            asyncListDiffer.currentList[position].isGift -> {
                Gift
            }
            asyncListDiffer.currentList[position].username == "Jyotish Ji" -> {
                Admin
            }
            else -> {
                Participant
            }
        }
    }
}