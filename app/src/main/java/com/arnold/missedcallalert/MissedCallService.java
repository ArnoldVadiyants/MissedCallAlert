package com.arnold.missedcallalert;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * 10.11.2015.
 */
public class MissedCallService extends Service {
    private static int NOTIFICATION_ID = 1;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getApplicationContext().getContentResolver().
                registerContentObserver(CallLog.Calls.CONTENT_URI, true,
                        new CallContentObserver(new Handler(), getApplicationContext()));
        addNotification();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)

    private void addNotification() {
        // create the notification
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name) + " on")
                .setContentIntent(pendingIntent)
                        //At most three action buttons can be added
                .setOngoing(true).build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        this.stopForeground(true);

    }

}

class CallContentObserver extends ContentObserver {

    Context context;

    public CallContentObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {

        if (null == uri) {
            onChange(selfChange);
            return;
        }

        super.onChange(selfChange, uri);

        final Cursor c = context.getContentResolver().query(uri, null, null, null, null);
       int dircode = 0;
        if (c != null && c.moveToFirst()){
        do {
             int type = c.getColumnIndex(CallLog.Calls.TYPE);
            dircode = c.getInt(type);
        } while (c.moveToNext());
    }


        switch (dircode) {
            case CallLog.Calls.MISSED_TYPE:
                Log.d("TAG", "*****Missed Call*****");
                break;
        }
    }

}