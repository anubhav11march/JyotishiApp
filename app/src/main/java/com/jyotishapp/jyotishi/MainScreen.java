package com.jyotishapp.jyotishi;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.Util;
import com.onesignal.OneSignal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

public class MainScreen extends BaseClass {


    private static final int PERMISSION_REQ_ID = 22;
    BoomMenuButton bmb;
    FirebaseAuth mAuth;
    FabSpeedDial fabsd;
    LinearLayout imageBorder;
    FirebaseDatabase database;
    DatabaseReference mRef;
    AlertDialog.Builder dialog;
    ConstraintLayout bg;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    TextView usersName;
    View headerLayout;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
//        startActivity(new Intent(MainScreen.this, SplashScreen.class));
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainScreen.this, LanguageActivity.class));
            finish();
            Toast.makeText(MainScreen.this, getString(R.string.login_again), Toast.LENGTH_SHORT).show();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();
        for (int i = 0; i < 2; i++) {
            checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID);
        }
        FirebaseDatabase.getInstance().getReference().child("CurrentVidCall").removeValue();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.US);
        Date date = new Date(System.currentTimeMillis());
        try {
            database = FirebaseDatabase.getInstance();
            mRef = database.getReference().child("Users");
            mRef = mRef.child(mAuth.getCurrentUser().getUid());
        } catch (Exception e) {
            startActivity(new Intent(MainScreen.this, LanguageActivity.class));
            finish();
            Toast.makeText(MainScreen.this, getString(R.string.login_again), Toast.LENGTH_SHORT).show();
            return;
        }
        mRef.child("UserId").setValue(mAuth.getCurrentUser().getUid());
        mRef.child("Last Active").setValue(sdf.format(date));
        mRef.child("Calling").setValue(false);
        mRef.child("Engaged").setValue(false);
        mRef.child("InCallWith").setValue("");
        mRef.child("IncomingCall").setValue(false);
        mRef.child("IncomingVideoCall").setValue(false);
        mRef.child("Premium").setValue(false);
        //TODO: onboarding new user default values

        setContentView(R.layout.activity_main_screen);
        getSupportActionBar().hide();
        initializeViews();

        bmb = (BoomMenuButton) findViewById(R.id.mainPic);
        fabsd = (FabSpeedDial) findViewById(R.id.tool);
        imageBorder = (LinearLayout) findViewById(R.id.imageBorder);
        dialog = new AlertDialog.Builder(this);
        bg = (ConstraintLayout) findViewById(R.id.bg);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationView = (NavigationView) findViewById(R.id.nView);
        headerLayout = navigationView.getHeaderView(0);
        usersName = (TextView) headerLayout.findViewById(R.id.usersName);

        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersName.setText(dataSnapshot.child("Name").getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                switch (id){
                    case R.id.AudioCalls:
//                        Toast.makeText(MainScreen.this, "Voice Calls", Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        startActivity(new Intent(MainScreen.this, VoiceLogsActivity.class));
                        break;
                    case R.id.VideoCalls:
//                        Toast.makeText(MainScreen.this, "Video Calls", Toast.LENGTH_LONG).show();
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        startActivity(new Intent(MainScreen.this, VideoLogsActivity.class));
                        break;
                    case R.id.settings:
                        startActivity(new Intent(MainScreen.this, SettingsActivity.class));
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        break;
                    case R.id.BuyPremium:
                        startActivity(new Intent(MainScreen.this, BuyPremiumActivity.class));
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        break;
                    case R.id.TermsAndConditions:
                        startActivity(new Intent(MainScreen.this, TnCActivity.class));
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        break;
                        default:
                            return true;

                }
                return false;
            }
        });

        TextInsideCircleButton.Builder builder = new TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.mess)
                .normalText(getString(R.string.chat))
                .imagePadding(new Rect(15, 15, 15, 15))
                .listener(new OnBMClickListener() {
                    @Override
                    public void onBoomButtonClick(int index) {
                        startActivity(new Intent(MainScreen.this, ChatActivity.class));
//                        overridePendingTransition(R.anim.x_exit, R.anim.x_enter);
                    }
                });
        bmb.addBuilder(builder);
        TextInsideCircleButton.Builder builder1 = new TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.vid)
                .imagePadding(new Rect(15, 15, 15, 15))
                .normalText(getString(R.string.video))
                .listener(new OnBMClickListener() {
                    @Override
                    public void onBoomButtonClick(int index) {
                        startActivity(new Intent(MainScreen.this, VidCallActivity.class));
                    }
                });
        bmb.addBuilder(builder1);
        TextInsideCircleButton.Builder builder2 = new TextInsideCircleButton.Builder()
                .normalImageRes(R.drawable.audio_white_icon)
                .imagePadding(new Rect(15, 15, 15, 15))
                .normalText(getString(R.string.audio))
                .listener(new OnBMClickListener() {
                    @Override
                    public void onBoomButtonClick(int index) {
                        startActivity(new Intent(MainScreen.this, VoiceCallActivity.class));
                        Toast.makeText(MainScreen.this, "Audio Call", Toast.LENGTH_LONG).show();
                    }
                });
        bmb.addBuilder(builder2);
        fabsd.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.profile:
                        startActivity(new Intent(MainScreen.this, UserProfileActivity.class));
                        Log.v("AAA", menuItem.getItemId() + " " + R.id.profile);
                        break;
                    case R.id.signout:
                        Toast.makeText(MainScreen.this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
                        Log.v("AAA", menuItem.getItemId() + "");
                        logout();
                        break;
                    case R.id.language:
                        startActivity(new Intent(MainScreen.this, LanguageActivity.class));
                        Log.v("AAA", menuItem.getItemId() + " " + R.id.language);
                        break;
                    default:
                        Toast.makeText(MainScreen.this, getString(R.string.error_occured), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new NotificationOpener(MainScreen.this, uid))
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        OneSignal.setSubscription(true);
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid())
                        .child("NotificationKey").setValue(userId);
            }
        });

        //rotating the border
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(2000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        imageBorder.setAnimation(rotateAnimation);
    }

    public void bgClicked(View view) {
        fabsd.closeMenu();
    }


    public void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        setLocale(language);
    }

    public void setLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", language);
        editor.apply();
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            Log.v("AAA", permission);
            return false;
        }
        return true;
    }

    public void drawerOpen(View view){
        drawerLayout.openDrawer(Gravity.LEFT);
        fabsd.closeMenu();
    }

    @Override
    public void onBackPressed() {
        dialog.setMessage(R.string.exit_question)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog alertDialog = dialog.create();
        alertDialog.setTitle(R.string.exit_question);
        alertDialog.show();
    }

    public void picClick(View view) {
        fabsd.closeMenu();
        bmb.boom();
    }

    public void exploreOptionsClicked(View view) {
        fabsd.closeMenu();
        bmb.boom();
    }

    public void viewJyotishProfilesClicked(View view) {
        fabsd.closeMenu();
        startActivity(new Intent(MainScreen.this, JyotishProfilesActivity.class));
    }

    public void logout() {
        OneSignal.setSubscription(false);
        FirebaseAuth.getInstance().signOut();
        mAuth.signOut();


//        try {
//            // clearing app data
//            String packageName = getApplicationContext().getPackageName();
//            Runtime runtime = Runtime.getRuntime();
//            runtime.exec("pm clear "+packageName);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        Intent i = new Intent(MainScreen.this, LanguageActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

}
