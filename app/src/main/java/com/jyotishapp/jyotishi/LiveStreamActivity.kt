package com.jyotishapp.jyotishi

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jyotishapp.jyotishi.Adapter.RemoteViewAdapterTest
import com.jyotishapp.jyotishi.Adapter.UsersAdapter
import com.jyotishapp.jyotishi.Common.Constant.ADMIN_ID
import com.jyotishapp.jyotishi.Common.Constant.ADMIN_UNIQUE_AGORA_ID
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_live_stream.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

class LiveStreamActivity : AppCompatActivity() {

    private var mRtcEngine: RtcEngine? = null
    private lateinit var iRtcEngineEventHandler: IRtcEngineEventHandler
    private lateinit var remoteViewAdapterTest: RemoteViewAdapterTest
    private var usersAdapter: UsersAdapter? = null
    private var uidList = ArrayList<Int>()
    private var token: String? = ""
    private val handler = Handler(Looper.getMainLooper())
    private val lock = ReentrantLock()
    private var isSelfMuted: Boolean = true
    private var isCamEnabled: Boolean = true
    private var optionalUid = 0
    private val PERMISSION_CODE = 22
    private lateinit var adminFrame: RelativeLayout
    private var remoteView: SurfaceView? = null
    private val liveStreamViewModel by viewModels<LiveStreamViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)

        supportActionBar?.hide()

        if (permissionsGranted()) {
            initializeApplication()
        } else {
            requestPermissions()
        }
    }

    private val listener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {}

        override fun onDataChange(snapshot: DataSnapshot) {
            val value = snapshot.getValue(String::class.java)
            if(value == "off") {
                Toast.makeText(this@LiveStreamActivity,"Stream has ended", Toast.LENGTH_LONG).show()
                val intent = Intent(this@LiveStreamActivity,MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun permissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.CAMERA
                ), PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    initializeApplication()
                } else {
                    finish()
                }
            }
        }
    }

    private fun getHandler() = object : IRtcEngineEventHandler() {

        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed)

            if(uid == ADMIN_UNIQUE_AGORA_ID) {

                remoteView = RtcEngine.CreateRendererView(baseContext)
                adminFrame.addView(remoteView)
                mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN,uid))
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)

            mRtcEngine?.muteRemoteVideoStream(uid, true)
            if(uid != ADMIN_UNIQUE_AGORA_ID) {

                liveStreamViewModel.addUser(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)

            if(uid != ADMIN_UNIQUE_AGORA_ID) {

                liveStreamViewModel.removeUser(uid)

                handler.post {

                    mRtcEngine?.setupRemoteVideo(VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid))

                }
            }
        }

    }

    private fun initializeApplication() {

        liveStreamViewModel.reset()

        Handler().postDelayed({
            mRtcEngine?.muteRemoteVideoStream(ADMIN_UNIQUE_AGORA_ID,false)
            mRtcEngine?.muteRemoteAudioStream(ADMIN_UNIQUE_AGORA_ID,false)
            remoteView = RtcEngine.CreateRendererView(baseContext)
            adminFrame.addView(remoteView)
            mRtcEngine?.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, ADMIN_UNIQUE_AGORA_ID))
            remoteView?.tag = ADMIN_UNIQUE_AGORA_ID
        },1500)

        iRtcEngineEventHandler = getHandler()
        FirebaseDatabase.getInstance().getReference("StreamStatus")
                .addValueEventListener(listener)

        adminFrame = findViewById(R.id.admin)

        try {
            mRtcEngine = RtcEngine.create(
                    baseContext,
                    resources.getString(R.string.agora_app_id),
                    iRtcEngineEventHandler
            )

        } catch (e: Exception) {
            Log.e("exception ", Log.getStackTraceString(e))
        }

        mRtcEngine!!.enableVideo()

        mRtcEngine!!
                .setVideoEncoderConfiguration(
                        VideoEncoderConfiguration(
                                VideoEncoderConfiguration.VD_640x360,
                                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                                VideoEncoderConfiguration.STANDARD_BITRATE,
                                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT)
                )

        val localContainer = findViewById<FrameLayout>(R.id.local_container_test)
        val localFrame = RtcEngine.CreateRendererView(baseContext)
        localContainer
                .addView(
                        localFrame,
                        FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                )
        mRtcEngine!!.setupLocalVideo(VideoCanvas(localFrame, VideoCanvas.RENDER_MODE_HIDDEN, 0))

        optionalUid = 0

        optionalUid = FirebaseAuth.getInstance().currentUser?.uid.hashCode()

        if(FirebaseAuth.getInstance().currentUser?.uid != null) {
            val db = FirebaseDatabase.getInstance()

            db.getReference("Users")
                    .child("${FirebaseAuth.getInstance().currentUser?.uid}")
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onCancelled(p0: DatabaseError) {

                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(Users::class.java)
                            user?.let {
                                db.getReference("StreamMetaData")
                                        .child("$optionalUid")
                                        .setValue(it.Name)
                            }
                        }
                    })
        }

        token = getString(R.string.access_token)

        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN")) {
            token = null
        }

        mRtcEngine!!.joinChannel(
                token,
                ADMIN_ID,
                "",
                optionalUid
        )

        mRtcEngine?.muteLocalAudioStream(true)

        mRtcEngine!!.enableDualStreamMode(true)

        val remoteViewManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        )

        usersAdapter = UsersAdapter(mRtcEngine)

        recyclerview_test.apply {
            layoutManager = remoteViewManager
            adapter = usersAdapter
            setHasFixedSize(true)
        }

        lifecycleScope.launchWhenStarted {
            liveStreamViewModel.usersList.collect {
                usersAdapter?.asyncListDiffer?.submitList(it)

                if (uidList.size == 3) {
                    mRtcEngine?.setRemoteDefaultVideoStreamType(Constants.VIDEO_STREAM_HIGH)
                    //Go back to High Video Stream
                }
                if (it.size == 4) {
                    mRtcEngine?.setRemoteDefaultVideoStreamType(Constants.VIDEO_STREAM_LOW)
                    //Fallback to Low Video Stream
                }

                Log.d("liveStreamVM list","""
                    $it
                """.trimIndent())
            }
        }

        btn_switch_camera_test.setOnClickListener {
            mRtcEngine!!.switchCamera()
        }

        btn_mute_test.setOnClickListener {
            isSelfMuted = !isSelfMuted
            mRtcEngine!!.muteLocalAudioStream(isSelfMuted)
            if (isSelfMuted) {
                btn_mute_test.setImageDrawable(resources.getDrawable(R.drawable.ic_mic_off))
            } else {
                btn_mute_test.setImageDrawable(resources.getDrawable(R.drawable.ic_mic))
            }
        }

        btn_toggle_camera_test.setOnClickListener {
            isCamEnabled = !isCamEnabled
            if(!isCamEnabled) {
                mRtcEngine!!.enableLocalVideo(isCamEnabled)
                btn_toggle_camera_test.setImageDrawable(resources.getDrawable(R.drawable.ic_cam_off))
            }
            else {
                mRtcEngine!!.enableLocalVideo(isCamEnabled)
                btn_toggle_camera_test.setImageDrawable(resources.getDrawable(R.drawable.ic_cam))
            }
        }

        btn_call_test.setOnClickListener {
            finish()
        }

        sendGift.setOnClickListener {

        }
    }

    override fun onPause() {
        super.onPause()
        FirebaseDatabase.getInstance().getReference("StreamStatus")
                .removeEventListener(listener)
    }

    override fun onDestroy() {
        super.onDestroy()

        mRtcEngine?.leaveChannel()
        handler.post { RtcEngine.destroy() }
        mRtcEngine = null
    }
}