package com.jyotishapp.jyotishi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jyotishapp.jyotishi.Adapter.StreamActiveUsersAdapter
import com.jyotishapp.jyotishi.Adapter.StreamChatAdapter
import com.jyotishapp.jyotishi.Common.Constant.ADMIN_ID
import com.jyotishapp.jyotishi.Common.Constant.ADMIN_UNIQUE_AGORA_ID
import com.jyotishapp.jyotishi.Common.Constant.DEFAULT_USER_URL
import com.jyotishapp.jyotishi.LiveStreamViewModel.RecyclerViewState.Initial
import com.jyotishapp.jyotishi.LiveStreamViewModel.RecyclerViewState.StopScroll
import com.jyotishapp.jyotishi.Models.ActiveUser
import com.jyotishapp.jyotishi.Models.ChatUser
import com.jyotishapp.jyotishi.Models.UserStreamMetaData
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultListener
import com.razorpay.PaymentResultWithDataListener
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_live_stream.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class LiveStreamActivity : AppCompatActivity(), PaymentDialog.DialogListener, PaymentResultWithDataListener{

    private var mRtcEngine: RtcEngine? = null
    private lateinit var iRtcEngineEventHandler: IRtcEngineEventHandler
    private var token: String? = ""
    private val handler = Handler(Looper.getMainLooper())
    private var optionalUid = 0
    private val PERMISSION_CODE = 22
    private lateinit var adminFrame: RelativeLayout
    private var remoteView: SurfaceView? = null
    private val liveStreamViewModel by viewModels<LiveStreamViewModel>()
    private var alertDialogBuilder: AlertDialog.Builder? = null
    private val GOOGLE_PAY_REQUEST_CODE = 123
    private val GOOGLE_PAY_PACKAGE_NAME =
        "com.google.android.apps.nbu.paisa.user"

    override fun getAmount(amount: Int) {

        val job = lifecycleScope.launchWhenCreated {

            liveStreamViewModel.currentUserName.collect { username ->

                val upiID = resources.getString(R.string.upi_id)
                val transactionNote = "gift from $username for Jyotish Ji"

                if(amount > 0) {

                    liveStreamViewModel.setGiftAmount(amount)
                    val checkout = Checkout()

                    try {

                        val options = JSONObject()
                        options.put("name", "Jyotish Ji")
                        options.put("description", "Gift")

                        options.put("image", "https://rzp-mobile.s3.amazonaws.com/images/rzp.png")
                        options.put("currency", "INR")

                        // amount is in paise so please multiple it by 100

                        var total = amount
                        total *= 100
                        options.put("amount", "$total")
                        val preFill = JSONObject()
                        preFill.put("email", "accounts@kyloapps.com")
                        preFill.put("contact", "8826359249")
                        options.put("prefill", preFill)
                        checkout.open(this@LiveStreamActivity, options)
                    } catch (e: Exception) {
                        Toast.makeText(this@LiveStreamActivity,e.message.toString(),Toast.LENGTH_SHORT).show()
                    }

//                    val uri = Uri.Builder()
//                        .scheme("upi")
//                        .authority("pay")
//                        .appendQueryParameter("pa", upiID)
//                        .appendQueryParameter("pn", "Jyotish Ji")
//                        //.appendQueryParameter("mc", "your-merchant-code")
//                        //.appendQueryParameter("tr", "your-transaction-ref-id")
//                        .appendQueryParameter("tn", transactionNote)
//                        .appendQueryParameter("am", "$amount")
//                        .appendQueryParameter("cu", "INR")
//                        //.appendQueryParameter("url", "your-transaction-url")
//                        .build()
//
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = uri
//                    intent.setPackage(GOOGLE_PAY_PACKAGE_NAME)
//                    try {
//                        startActivityForResult(intent, GOOGLE_PAY_REQUEST_CODE)
//                    } catch (e: java.lang.Exception) {
//                        Toast.makeText(this@LiveStreamActivity, "Google Pay is not installed", Toast.LENGTH_LONG).show()
//                    }
                } else {

                    Toast.makeText(this@LiveStreamActivity,"Enter amount",Toast.LENGTH_SHORT).show()
                }
            }
        }

        job.cancel()
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {

//        try {
//            amount = p1?.data?.get("amount") as Int
//        } catch (e: Exception){
//            Toast.makeText(this,e.message.toString(),Toast.LENGTH_SHORT).show()
//        }

        val data = liveStreamViewModel.currentUserName.asLiveData()

        data.observe(this, Observer { userName ->

            liveStreamViewModel.giftAmount.observe(this, Observer { giftAmount ->

                val date = Date(System.currentTimeMillis())
                val sdf = SimpleDateFormat("HH:mm")
                val time = sdf.format(date)

                val amount = giftAmount ?: -1

                val chatUser = ChatUser(
                    userName,
                    "$userName sent ₹ $amount as a gift to Jyotish Ji!",
                    DEFAULT_USER_URL,
                    time,
                    true
                )

                FirebaseDatabase.getInstance().getReference("StreamChat")
                    .push()
                    .setValue(chatUser)
            })
        })
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        Toast.makeText(this,"Payment failed or cancelled!",Toast.LENGTH_LONG).show()
    }

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
            if (value == "off") {

                Toast.makeText(this@LiveStreamActivity, "Stream has ended", Toast.LENGTH_LONG)
                    .show()
                val intent = Intent(this@LiveStreamActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private val chatListener = object : ChildEventListener {

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val chat = snapshot.getValue(ChatUser::class.java)
            chat?.let {
                liveStreamViewModel.addChat(it)
            }
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(p0: DataSnapshot) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onCancelled(p0: DatabaseError) {}
    }

    private val activeUsersListener = object : ChildEventListener {

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val activeUser = snapshot.getValue(ActiveUser::class.java)
            activeUser?.let {
                liveStreamViewModel.addActiveUser(it)
            }
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val activeUser = snapshot.getValue(ActiveUser::class.java)
            activeUser?.let {
                liveStreamViewModel.removeActiveUser(it)
            }
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onCancelled(p0: DatabaseError) {}
    }

    private fun permissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            ), PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
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

            if (uid == ADMIN_UNIQUE_AGORA_ID) {

                remoteView = RtcEngine.CreateRendererView(baseContext)
                adminFrame.addView(remoteView)
                mRtcEngine?.setupRemoteVideo(
                    VideoCanvas(
                        remoteView,
                        VideoCanvas.RENDER_MODE_HIDDEN,
                        uid
                    )
                )
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)

            mRtcEngine?.muteRemoteVideoStream(uid, true)
            if (uid != ADMIN_UNIQUE_AGORA_ID) {

                liveStreamViewModel.addUser(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)

            if (uid != ADMIN_UNIQUE_AGORA_ID) {

                liveStreamViewModel.removeUser(uid)

                handler.post {

                    mRtcEngine?.setupRemoteVideo(
                        VideoCanvas(
                            null,
                            VideoCanvas.RENDER_MODE_HIDDEN,
                            uid
                        )
                    )

                }
            }
        }

    }

    private fun leaveStream() {

        alertDialogBuilder
            ?.setTitle("Leave this Stream?")
            ?.setMessage("There will be a short delay before you can rejoin this livestream. Are you sure you want to leave?")
            ?.setCancelable(false)
            ?.setPositiveButton("Yes") { dialog, i ->

                liveStreamViewModel.setInActive()

                mRtcEngine?.muteAllRemoteAudioStreams(true)

                Toast.makeText(this@LiveStreamActivity, "You left the stream", Toast.LENGTH_LONG).show()
                val intent = Intent(this@LiveStreamActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            ?.setNegativeButton("No") { _, _ ->

            }
            ?.create()
            ?.show()
    }

    private fun initializeApplication() {

        liveStreamViewModel.apply {
            reset()
            setAppInitialised()
            setProfile()
        }

        alertDialogBuilder = AlertDialog.Builder(this)

        lifecycleScope.launchWhenStarted {

            liveStreamViewModel.usersList.collect { list ->

                withContext(Dispatchers.Main) {

                    participants_info.text = when {

                        list.isEmpty() -> {
                            "No Other\nParticipants"
                        }

                        list.size == 1 -> {
                            "1 Other \nParticipant"
                        }

                        else -> {
                            "${list.size} Other \nParticipants"
                        }
                    }
                }
            }
        }

        send_to_stream.setOnClickListener {

            if (!TextUtils.isEmpty(chat.text.toString())) {

                val message = chat.text.toString()

                lifecycleScope.launchWhenStarted {
                    liveStreamViewModel.currentUserName.collect { userName ->

                        val date = Date(System.currentTimeMillis())
                        val sdf = SimpleDateFormat("HH:mm")
                        val time = sdf.format(date)

                        val chatUser = ChatUser(
                            userName,
                            message,
                            DEFAULT_USER_URL,
                            time,
                            false
                        )

                        chat.setText("")
                        chat.hint = "Sending message..."
                        val view = this@LiveStreamActivity.currentFocus
                        view?.let {
                            val imm: InputMethodManager =
                                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(it.windowToken, 0)
                        }

                        FirebaseDatabase.getInstance().getReference("StreamChat")
                            .push()
                            .setValue(chatUser)
                            .addOnCompleteListener {
                                chat.hint = "type a message"
                            }
                    }
                }

            } else {
                Toast.makeText(this, "Type a message first!", Toast.LENGTH_SHORT).show()
            }
        }

        gift.setOnClickListener {

            val dialog = PaymentDialog()
            dialog.show(supportFragmentManager,"Send a gift")
        }

        leave_call.setOnClickListener {

            leaveStream()
        }

        Handler().postDelayed({
            mRtcEngine?.muteRemoteVideoStream(ADMIN_UNIQUE_AGORA_ID, false)
            mRtcEngine?.muteRemoteAudioStream(ADMIN_UNIQUE_AGORA_ID, false)
            remoteView = RtcEngine.CreateRendererView(baseContext)
            adminFrame.addView(remoteView)
            mRtcEngine?.setupRemoteVideo(
                VideoCanvas(
                    remoteView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    ADMIN_UNIQUE_AGORA_ID
                )
            )
            remoteView?.tag = ADMIN_UNIQUE_AGORA_ID
        }, 1500)

        iRtcEngineEventHandler = getHandler()
        FirebaseDatabase.getInstance().getReference("StreamStatus")
            .addValueEventListener(listener)

        FirebaseDatabase.getInstance().getReference("StreamChat")
            .addChildEventListener(chatListener)

        FirebaseDatabase.getInstance().getReference("StreamActiveUsers")
            .addChildEventListener(activeUsersListener)

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
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
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

        if (FirebaseAuth.getInstance().currentUser?.uid != null) {
            val db = FirebaseDatabase.getInstance()

            db.getReference("Users")
                .child("${FirebaseAuth.getInstance().currentUser?.uid}")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(Users::class.java)
                        user?.let {
                            db.getReference("StreamMetaData")
                                .child("$optionalUid")
                                .setValue(
                                    UserStreamMetaData(
                                        id = it.UserId,
                                        name = it.Name
                                    )
                                )
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

        val chatsAdapter = StreamChatAdapter(this)
        val activeUsersAdapter = StreamActiveUsersAdapter(this)

        recyclerview_chats.apply {
            adapter = chatsAdapter
        }
        recyclerview_active_users.apply {
            adapter = activeUsersAdapter
        }

        lifecycleScope.launchWhenStarted {

            async {
                liveStreamViewModel.chatsList.collect { chats ->
                    chatsAdapter.asyncListDiffer.submitList(chats)
                }
            }

            async {
                liveStreamViewModel.activeUsersList.collect { activeUsers ->
                    activeUsersAdapter.asyncListDiffer.submitList(activeUsers)
                }
            }

            async {
                liveStreamViewModel.recyclerViewStatus.collect { state ->

                    withContext(Dispatchers.Main) {

                        when (state) {

                            is Initial -> {

                                scroll.visibility = View.GONE
                            }

                            is StopScroll -> {

                                scroll.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }

        chatsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

//                val messageCount = chatsAdapter.itemCount
//                val chatsLinearLayout = recyclerview_chats.layoutManager as LinearLayoutManager
//                val lastVisiblePosition = chatsLinearLayout.findLastCompletelyVisibleItemPosition()

                val job = lifecycleScope.launchWhenCreated {

                    liveStreamViewModel.recyclerViewStatus.collect { state ->

                        when (state) {

                            is Initial -> {

                                recyclerview_chats.scrollToPosition(positionStart)
                            }

                            is StopScroll -> Unit
                        }
                    }
                }

                job.cancel()

//                if(lastVisiblePosition == -1 ||
//                    (positionStart >= (messageCount - 1) &&
//                            lastVisiblePosition == (positionStart - 1))) {
//                    recyclerview_chats.scrollToPosition(positionStart)
//                    scroll.visibility = View.GONE
//                } else {
//                    scroll.visibility = View.VISIBLE
//                }

            }
        })

        recyclerview_chats.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val chatsLinearLayout = recyclerview_chats.layoutManager as LinearLayoutManager
                val lastVisiblePosition = chatsLinearLayout.findLastCompletelyVisibleItemPosition()

                if (lastVisiblePosition != chatsAdapter.itemCount - 1) {
                    liveStreamViewModel.markChatsAsStopped()
                } else {
                    liveStreamViewModel.markChatsAsInitial()
                }
            }
        })

        scroll.setOnClickListener {

            liveStreamViewModel.markChatsAsInitial()
            recyclerview_chats.smoothScrollToPosition(chatsAdapter.itemCount - 1)
        }
    }

    override fun onBackPressed() {

        leaveStream()
    }

    override fun onStart() {
        super.onStart()

        mRtcEngine?.muteAllRemoteAudioStreams(false)

        liveStreamViewModel.appInitialised.observe(this, Observer {

            if (it) {
                FirebaseDatabase.getInstance().getReference("StreamStatus")
                    .addValueEventListener(listener)
            }

            liveStreamViewModel.setActive()
        })
    }

    override fun onStop() {
        super.onStop()

        mRtcEngine?.muteAllRemoteAudioStreams(true)

        FirebaseDatabase.getInstance().getReference("StreamStatus")
            .removeEventListener(listener)

        liveStreamViewModel.setInActive()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRtcEngine?.leaveChannel()
        handler.post { RtcEngine.destroy() }
        mRtcEngine = null
    }
}