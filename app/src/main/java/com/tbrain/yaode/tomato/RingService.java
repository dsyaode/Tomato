package com.tbrain.yaode.tomato;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;

/**
 * Created by Administrator on 2016/6/25 0025.
 */
public class RingService extends Service {

    public final static String ACTION_START_RING = "action_start_ring";
    public final static String ACTION_CLOSE_RING = "action_close_ring";
    public final static String KEY_RING_TIME = "key_ring_time";

    public final static String BROADCAST_RING = "broadcast_ring";
    public final static String BROADCAST_UPDATE_TIME = "broadcast_update_time";

    public static int TOMATO_TIME = 25*60*1000;
    public long ringTime = 0;
    public long startTime = 0;

    private MediaPlayer mp;

    private Handler handler;

    public class MyHandler extends Handler{
        @Override
        public void handleMessage(Message message){
            if(message.what == 1){

                long remainTime = ringTime-(System.currentTimeMillis()-startTime);
                remainTime = remainTime >= 0 ?remainTime:0;

                Intent intent = new Intent();
                intent.setAction(BROADCAST_UPDATE_TIME);
                intent.putExtra(KEY_RING_TIME, ""+remainTime);
                RingService.this.sendBroadcast(intent);

                if(remainTime > 0){
                    this.sendEmptyMessageDelayed(1, 200);
                }else{
                    this.sendEmptyMessage(2);
                }

                putServiceToForeground(remainTime/1000);

            }else if(message.what == 2){
                Intent intent = new Intent();
                intent.setAction(BROADCAST_RING);
                RingService.this.sendBroadcast(intent);

//                mp.start();
                playRing();
//                stopForeground(true);
                closeRing();
            }

            super.handleMessage(message);
        }
    }

    private void playRing(){

        final int streamMode = AudioManager.STREAM_MUSIC;

        final AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
//        Log.i("yds", ""+am.getStreamVolume(streamMode));
        final int cur = am.getStreamVolume(streamMode);
        int max = am.getStreamMaxVolume(streamMode);

        if(cur <= 0){
            am.setStreamVolume(streamMode, max/2, AudioManager.FLAG_SHOW_UI);
        }

        mp.setAudioStreamType(streamMode);
        mp.start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                am.setStreamVolume(streamMode, cur, AudioManager.FLAG_SHOW_UI);
//                Log.i("yds2", ""+am.getStreamVolume(streamMode));
            }
        }, 5000);
    }

    public void onCreate(){
        handler = new MyHandler();
        mp = MediaPlayer.create(this,R.raw.three_push);
    }

    public static void startService(Context context, int time){
        Intent intent = new Intent();
        intent.setClass(context, RingService.class);
        intent.setAction(ACTION_START_RING);
        intent.putExtra(KEY_RING_TIME, time);
        context.startService(intent);
    }

    public static void closeService(Context context){
        Intent intent = new Intent();
        intent.setClass(context, RingService.class);
        intent.setAction(ACTION_CLOSE_RING);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent != null){
            String action = intent.getAction();
            if(action.equals(ACTION_START_RING)){
                int time = intent.getIntExtra(KEY_RING_TIME, TOMATO_TIME);
                ringTime = time;
                startTime = System.currentTimeMillis();
                startRing();
            }else if(action.equals(ACTION_CLOSE_RING)){
                closeRing();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startRing(){
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent = new Intent();
//                intent.setAction(BROADCAST_RING);
//                RingService.this.sendBroadcast(intent);
//
//                mp.start();
//                RingService.this.stopSelf();
//            }
//        }, TOMATO_TIME);
        handler.removeCallbacksAndMessages(null);
        handler.sendEmptyMessage(1);

    }

    private void closeRing(){
        handler.removeCallbacksAndMessages(null);
        NotificationManager nm = (NotificationManager) RingService.this.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(myID);
        stopSelf();
    }

    final static int myID = 1;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void putServiceToForeground(long remainTime) {
        String str = "剩下 ";
        if(remainTime >= 60){
            str = str + remainTime/60 + "m";
            remainTime = remainTime%60;
        }
        str = str + remainTime%60 + "s";

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder mBuilder =
                new Notification.Builder(this.getApplicationContext())
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(str)
                        .setSmallIcon(R.mipmap.tomato_alpha)
                        .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(myID, mBuilder.build());
    }
}
