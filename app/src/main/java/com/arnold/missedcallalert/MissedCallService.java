package com.arnold.missedcallalert;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * 10.11.2015.
 */
public class MissedCallService extends Service {
    private static int NOTIFICATION_ID = 1;
    private static String TAG = "MissedCallService";
    CallContentObserver callContentObserver;
    MissedCallManager missedCallManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callContentObserver = new CallContentObserver(new Handler(), getApplicationContext());
        missedCallManager = MissedCallManager.get(this);
        getApplicationContext().getContentResolver().
                registerContentObserver(CallLog.Calls.CONTENT_URI, true, callContentObserver);
        addNotification();


        return super.onStartCommand(intent, flags, startId);
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
                .setOngoing(true).build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(callContentObserver);
        missedCallManager.stopNotify();
        this.stopForeground(true);

    }


}

class CallContentObserver extends ContentObserver {
    Context context;
    MissedCallManager missedCallManager;
    private static String TAG = "CallContentObserver";

    public CallContentObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
        missedCallManager = MissedCallManager.get(context);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri == null) {
            Log.v(TAG, "onChange - uri = null");
            onChange(selfChange);
            return;
        }
        Log.v(TAG, "onChange");
        super.onChange(selfChange, uri);
        final String[] projection = null;
        final String selection = null;
        final String[] selectionArgs = null;
        final String sortOrder = null; //android.provider.CallLog.Calls.DATE + " DESC";
        Cursor cursor = null;
        int dircode = 0;
        String isCallNew = "0";
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    isCallNew = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NEW));
                    int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
                    dircode = cursor.getInt(type);
                } while (cursor.moveToNext());

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (dircode == CallLog.Calls.MISSED_TYPE && Integer.parseInt(isCallNew) > 0) {
            Log.v(TAG, " If_Enable");
            if (!missedCallManager.isEnableNotify()) {
                Log.v(TAG, " setEnableTrue");
                missedCallManager.startNotify();
            }
            Log.v(TAG, " MissedCall");
        } else {
            if (missedCallManager.isEnableNotify()) {
                missedCallManager.stopNotify();
            }
            Log.v(TAG, " no MissedCall");
        }
    }
}

class MissedCallManager {
    private static String TAG = "MissedCallManager";
    private static MissedCallManager sMissedCallManager;
    private Context mContext;
    private boolean mIsEnableNotify;
    private Thread mThread;

    public boolean isEnableNotify() {
        return mIsEnableNotify;
    }

    public void setEnableNotify(boolean isEnableNotify) {
        this.mIsEnableNotify = isEnableNotify;
    }

    private MissedCallManager(Context c) {
        mThread = new Thread();
        mContext = c;
    }

    public static MissedCallManager get(Context c) {
        if (sMissedCallManager == null) {
            sMissedCallManager = new MissedCallManager(c.getApplicationContext());
        }
        return sMissedCallManager;
    }

    public void startNotify() {
        mIsEnableNotify = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mIsEnableNotify) {
                    SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(mContext);

                    if (shared.getBoolean("notifications_new_message_vibrate", true)) {
                        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(1000);
                    }

                    Uri notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    String ringtonValue = (shared.getString("notifications_new_message_ringtone", String.valueOf(notifUri)));
                    Ringtone ringtone = RingtoneManager.getRingtone(mContext, Uri.parse(ringtonValue));
                    try {
                        ringtone.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String intervalValue = (shared.getString("notifications_interval", "1"));
                    int interval = 1;
                    try {
                        interval = Integer.parseInt(intervalValue);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    Log.v(TAG, "level = " + interval);
                    try {
                        Thread.sleep(interval * 60 * 1000);
                    } catch (InterruptedException e) {
                        Log.v(TAG, "Thread was interrupted");
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mThread.start();
    }

    public void stopNotify() {
        mIsEnableNotify = false;
        mThread.interrupt();
    }


}





