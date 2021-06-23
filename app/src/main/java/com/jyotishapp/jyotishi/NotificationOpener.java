package com.jyotishapp.jyotishi;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jyotishapp.jyotishi.Common.Constant;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import static com.jyotishapp.jyotishi.Common.Constant.*;
import static com.jyotishapp.jyotishi.Common.Constant.BROADCAST_NOTIFICATION_MESSAGE;

public class NotificationOpener implements OneSignal.NotificationOpenedHandler {
    Context context;
    String uid;
    NotificationOpener(Context context, String uid){
        this.context = context;
        this.uid = uid;
    }
    NotificationOpener(Context context){
        this.context = context;
    }
    @Override
    public void notificationOpened(OSNotificationOpenResult result) {
        OSNotificationAction.ActionType actionType = result.action.type;
        JSONObject data = result.notification.payload.additionalData;
        String customKey;

        Log.v("AAA", "result.notification.payload.toJSONObject().toString(): " +
                result.notification.payload.toJSONObject().toString());

        String x = result.notification.payload.toJSONObject().toString();

        if(x.contains(BROADCAST_NOTIFICATION_MESSAGE)) {
            Intent intent = new Intent(context, LiveStreamActivity.class).
                    setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivities(new Intent[]{intent});
            return;
        }

        int f=0;
        int i=30;

        while(i<x.length()-1){
            if(x.contains("New Message from")){
                f=1;
                break;
            }
            i++;
        }

        if(x.contains("Voice")){
            Intent intent = new Intent(context, MainScreen.class).
                    setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivities(new Intent[]{intent});
            return;
        }

        if(f==1){
            Intent intent = new Intent(context, ChatActivity.class).
                    setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivities(new Intent[]{new Intent(context, MainScreen.class), intent});
            return;
        }

        if(data!=null){
            customKey =data.optString("customKey", null);
            if(customKey!=null)
                Log.v("AAA", "customkey with set value:" + customKey);
        }
        if(actionType == OSNotificationAction.ActionType.ActionTaken)
            Log.v("OneSignalExample", "Button pressed with id: " + result.action.actionID);

        Intent intent = new Intent(context, MainScreen.class).
                setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivities(new Intent[]{intent});
        return;
    }
}
