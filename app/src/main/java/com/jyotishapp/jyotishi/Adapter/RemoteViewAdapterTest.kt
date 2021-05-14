package com.jyotishapp.jyotishi.Adapter

import android.graphics.Typeface
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jyotishapp.jyotishi.R
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

class RemoteViewAdapterTest(
    private val list: ArrayList<Int>,
    private val mRtcEngine: RtcEngine?
): RecyclerView.Adapter<RemoteViewAdapterTest.RemoteViewHolderTest>() {

    class RemoteViewHolderTest(val view: ConstraintLayout): RecyclerView.ViewHolder(view) {

        private val username: TextView = view.findViewById(R.id.myText)

        fun bind(name: String) {
            username.text = name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteViewHolderTest {

        val root = ConstraintLayout(parent.context)

        root.layoutParams = RecyclerView.LayoutParams(
            parent.measuredWidth / 2 ,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
        root.id = R.id.constraintLayout
        root.setPadding(12,12,12,12)

        val set = ConstraintSet()
        val view = View(parent.context)
        val textView = TextView(parent.context)
        textView.id = R.id.myText
        view.id = R.id.view
        textView.setTextColor(parent.context.resources.getColor(R.color.white))

        val layoutParams = ConstraintLayout.LayoutParams(0,0)
        view.elevation = 1F
        view.layoutParams = layoutParams
        view.background = parent.resources.getDrawable(R.drawable.shadow_gradient)

        val layoutParams2 = ConstraintLayout.LayoutParams(WRAP_CONTENT,WRAP_CONTENT)
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.elevation = 4F
        textView.text = "Admin"
        textView.textSize = 22F
        textView.layoutParams = layoutParams2

        root.addView(textView)
        root.addView(view)

        set.clone(root)

        set.connect(textView.id,ConstraintSet.LEFT,root.id,ConstraintSet.LEFT,12)
        set.connect(textView.id,ConstraintSet.BOTTOM,root.id,ConstraintSet.BOTTOM,10)

        set.connect(view.id,ConstraintSet.LEFT,root.id,ConstraintSet.LEFT)
        set.connect(view.id,ConstraintSet.RIGHT,root.id,ConstraintSet.RIGHT)
        set.connect(view.id,ConstraintSet.BOTTOM,root.id,ConstraintSet.BOTTOM)
        set.connect(view.id,ConstraintSet.TOP,textView.id,ConstraintSet.TOP)

        set.applyTo(root)

        return RemoteViewHolderTest(root)
    }

    override fun onBindViewHolder(holder: RemoteViewHolderTest, position: Int) {

        mRtcEngine?.muteRemoteVideoStream(list[position], false)

        val surface = RtcEngine.CreateRendererView(holder.view.context)

        surface.tag = list[position]

        mRtcEngine!!.setupRemoteVideo(VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, list[position]))

        holder.view.addView(
            surface,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        )

        FirebaseDatabase.getInstance().getReference("StreamMetaData")
                .child("${list[position]}")
                .addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val name = snapshot.getValue(String::class.java)

                        name?.let {
                            holder.bind(it)
                        }
                    }
                })
    }

    override fun getItemCount(): Int =
        list.size

    override fun onViewRecycled(holder: RemoteViewHolderTest) {

        val childCount = holder.view.childCount
        for (i in 0..childCount) {
            if(holder.view.getChildAt(i) is ConstraintLayout) {
                val uid = (holder.view.getChildAt(i) as SurfaceView).tag as Int

                mRtcEngine?.muteRemoteVideoStream(uid, true)
                break
            }
        }
    }
}