package com.jyotishapp.jyotishi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.onesignal.OneSignal
import io.github.yavski.fabspeeddial.FabSpeedDial
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter
import kotlinx.android.synthetic.main.activity_main_screen.*
import java.text.SimpleDateFormat
import java.util.*

class MainScreen : BaseClass() {
    var bmb: BoomMenuButton? = null
    var fabsd: FabSpeedDial? = null
    var imageBorder: LinearLayout? = null
    var dataBase: FirebaseDatabase? = null
    var mReff: DatabaseReference? = null
    var dialog: AlertDialog.Builder? = null
    var bg: ConstraintLayout? = null
    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var usersName: TextView? = null
    var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        loadLocale()
        super.onCreate(savedInstanceState)
        //        startActivity(new Intent(MainScreen.this, SplashScreen.class));
        val MAuth = FirebaseAuth.getInstance()
        if (MAuth!!.currentUser == null) {
            startActivity(Intent(this@MainScreen, LanguageActivity::class.java))
            finish()
            Toast.makeText(this@MainScreen, getString(R.string.login_again), Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance().getReference("StreamStatus")
                .addValueEventListener(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isStarted = snapshot.getValue(String::class.java)
                        joinStream.visibility = if(isStarted.equals("on"))
                            View.VISIBLE
                        else
                            View.GONE
                    }
                })



        uid = MAuth.currentUser!!.uid
        for (i in 0..1) {
            checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)
        }
        FirebaseDatabase.getInstance().reference.child("CurrentVidCall").removeValue()
        val sdf = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.US)
        val date = Date(System.currentTimeMillis())
        try {
            dataBase = FirebaseDatabase.getInstance()
            mReff = dataBase!!.reference.child("Users")
            mReff = mReff!!.child(MAuth!!.currentUser!!.uid)
        } catch (e: Exception) {
            startActivity(Intent(this@MainScreen, LanguageActivity::class.java))
            finish()
            Toast.makeText(this@MainScreen, getString(R.string.login_again), Toast.LENGTH_SHORT).show()
            return
        }

        if(MAuth.currentUser?.phoneNumber != "")
            mReff!!.child("Phone").setValue(MAuth.currentUser?.phoneNumber)
        else
            mReff!!.child("Phone").setValue("N/A")

        mReff!!.child("UserId").setValue(MAuth!!.currentUser!!.uid)
        mReff!!.child("Last Active").setValue(sdf.format(date))
        mReff!!.child("Calling").setValue(false)
        mReff!!.child("Engaged").setValue(false)
        mReff!!.child("InCallWith").setValue("")
        mReff!!.child("IncomingCall").setValue(false)
        mReff!!.child("IncomingVideoCall").setValue(false)


        //TODO: onboarding new user default values
        setContentView(R.layout.activity_main_screen)
        supportActionBar!!.hide()
        initializeViews()
        onIncomingCall()
        onIncomingVideoCall()
        bmb = findViewById<View>(R.id.mainPic) as BoomMenuButton
        fabsd = findViewById<View>(R.id.tool) as FabSpeedDial
        imageBorder = findViewById<View>(R.id.imageBorder) as LinearLayout
        dialog = AlertDialog.Builder(this)
        bg = findViewById<View>(R.id.bg) as ConstraintLayout
        drawerLayout = findViewById<View>(R.id.drawerLayout) as DrawerLayout
        navigationView = findViewById<View>(R.id.nView) as NavigationView
        val headerLayout : View = navigationView!!.getHeaderView(0)
        usersName = headerLayout.findViewById<View>(R.id.usersName) as TextView

//        val animator = ObjectAnimator.ofInt(offer,"backgroundColor", Color.YELLOW,Color.RED)
//        animator.duration = 500
//        animator.setEvaluator(ArgbEvaluator())
//        animator.repeatMode = ValueAnimator.REVERSE
//        animator.repeatCount = Animation.INFINITE
//        animator.start()

        mReff!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                usersName!!.text = dataSnapshot.child("Name").value.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        navigationView!!.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            when (id) {
                R.id.AudioCalls -> {
                    //                        Toast.makeText(MainScreen.this, "Voice Calls", Toast.LENGTH_LONG).show();
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                    startActivity(Intent(this@MainScreen, VoiceLogsActivity::class.java))
                }
                R.id.VideoCalls -> {
                    //                         Toast.makeText(MainScreen.this, "Video Calls", Toast.LENGTH_LONG).show();
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                    startActivity(Intent(this@MainScreen, VideoLogsActivity::class.java))
                }
                R.id.settings -> {
                    startActivity(Intent(this@MainScreen, SettingsActivity::class.java))
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                }
                R.id.BuyPremium -> {
                    startActivity(Intent(this@MainScreen, BuyPremiumActivity::class.java))
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                }
                R.id.TermsAndConditions -> {
                    startActivity(Intent(this@MainScreen, TnCActivity::class.java))
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                }
                R.id.appointment -> {
                    startActivity(Intent(this@MainScreen, AppointHostActivity::class.java))
                    drawerLayout!!.closeDrawer(Gravity.LEFT)
                }
                else -> return@OnNavigationItemSelectedListener true
            }
            false
        })
        val builder = TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.mess)
                .normalText(getString(R.string.chat))
                .imagePadding(Rect(15, 15, 15, 15))
                .listener {
                    startActivity(Intent(this@MainScreen, ChatActivity::class.java))
                    //                        overridePendingTransition(R.anim.x_exit, R.anim.x_enter);
                }
        bmb!!.addBuilder(builder)
        val builder1 = TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.vid)
                .imagePadding(Rect(15, 15, 15, 15))
                .normalText(getString(R.string.video))
                .listener { startActivity(Intent(this@MainScreen, VidCallActivity::class.java)) }
        bmb!!.addBuilder(builder1)
        val builder2 = TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.audio_white_icon)
                .imagePadding(Rect(15, 15, 15, 15))
                .normalText(getString(R.string.audio))
                .listener {
                    startActivity(Intent(this@MainScreen, VoiceCallActivity::class.java))
                    Toast.makeText(this@MainScreen, "Audio Call", Toast.LENGTH_LONG).show()
                }
        bmb!!.addBuilder(builder2)
        fabsd!!.setMenuListener(object : SimpleMenuListenerAdapter() {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.profile -> {
                        startActivity(Intent(this@MainScreen, UserProfileActivity::class.java))
                        Log.v("AAA", menuItem.itemId.toString() + " " + R.id.profile)
                    }
                    R.id.signout -> {
                        Toast.makeText(this@MainScreen, getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
                        Log.v("AAA", menuItem.itemId.toString() + "")
                        logout()
                    }
                    R.id.language -> {
                        startActivity(Intent(this@MainScreen, LanguageActivity::class.java))
                        Log.v("AAA", menuItem.itemId.toString() + " " + R.id.language)
                    }
                    else -> Toast.makeText(this@MainScreen, getString(R.string.error_occured), Toast.LENGTH_SHORT).show()
                }
                return true
            }
        })
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(NotificationOpener(this@MainScreen, uid))
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init()
        OneSignal.setSubscription(true)
        OneSignal.idsAvailable { userId, registrationId ->
            FirebaseDatabase.getInstance().reference.child("Users").child(MAuth!!.currentUser!!.uid)
                    .child("NotificationKey").setValue(userId)
        }

        //rotating the border
        val rotateAnimation = RotateAnimation(0F, 360F, Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.duration = 2000
        rotateAnimation.repeatCount = Animation.INFINITE
        imageBorder!!.animation = rotateAnimation
    }

//    override fun onRestart() {
//        super.onRestart()
//        val appointListRef = FirebaseDatabase.getInstance().getReference("Users/$uid")
//        val map = HashMap<String, Any>()
//        map["isListInitialised"] = "false"
//        appointListRef.updateChildren(map)
//    }

    fun bgClicked(view: View?) {
        fabsd!!.closeMenu()
    }

    fun loadLocale() {
        val prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE)
        val language = prefs.getString("My_Lang", "")
        setLocale(language)
    }

    fun setLocale(language: String?) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        val editor = getSharedPreferences("Settings", Context.MODE_PRIVATE).edit()
        editor.putString("My_Lang", language)
        editor.apply()
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            Log.v("AAA", permission)
            return false
        }
        return true
    }

    fun drawerOpen(view: View?) {
        drawerLayout!!.openDrawer(Gravity.LEFT)
        fabsd!!.closeMenu()
    }

    override fun onBackPressed() {
            dialog!!.setMessage(R.string.exit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { dialogInterface, i ->
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .setNegativeButton(R.string.no) { dialogInterface, i -> dialogInterface.cancel() }
            val alertDialog = dialog!!.create()
            alertDialog.setTitle(R.string.exit_question)
            alertDialog.show()

    }

    fun picClick(view: View?) {
        fabsd!!.closeMenu()
        bmb!!.boom()
    }

    fun exploreOptionsClicked(view: View?) {
        fabsd!!.closeMenu()
        bmb!!.boom()
    }

    fun joinLiveStreamClicked(view: View?) {
        val intent = Intent(this,LiveStreamActivity::class.java)
        startActivity(intent)
    }

    fun appointmentsClicked(view: View?){
        fabsd!!.closeMenu()
        val intent = Intent(this,AppointHostActivity::class.java)
        startActivity(intent)
    }

    fun viewJyotishProfilesClicked(view: View?) {
        fabsd!!.closeMenu()
        startActivity(Intent(this@MainScreen, JyotishProfilesActivity::class.java))
    }

    fun logout() {
        try {
            unregisterReceiver(ConnectivityReceiver())
        }catch (e: java.lang.Exception){}
        OneSignal.setSubscription(false)
        FirebaseAuth.getInstance().signOut()
        mAuth?.signOut()


//        try {
//            // clearing app data
//            String packageName = getApplicationContext().getPackageName();
//            Runtime runtime = Runtime.getRuntime();
//            runtime.exec("pm clear "+packageName);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        val i = Intent(this@MainScreen, LanguageActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
//
    }

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}