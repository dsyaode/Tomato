package com.tbrain.yaode.tomato;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements View.OnClickListener{

    private static final int deltaTime = 1*1000;

    private SeekBar bar;
    private Button btn;
    private TextView curTxt;
    private ListView listView;

    private int startTime = 0;
    private int endTime = 60;
    private int defaultTime = 25;
    private int curTime = 0;

    int left = 0;
    int right = 0;

    boolean isStart = false;

    private void reset(){
        btn.setText("开始番茄时间");
        btn.setBackgroundResource(R.color.orangle);
        isStart = false;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            handler.sendMessage(handler.obtainMessage(2));
            if(intent.getAction().equals(RingService.BROADCAST_RING)){
                reset();
            }else if(intent.getAction().equals(RingService.BROADCAST_UPDATE_TIME)){
                StringBuilder str = new StringBuilder();
                str.append("番茄+");
                long millionSec = Long.valueOf(intent.getStringExtra(RingService.KEY_RING_TIME));
                int min = (int) (millionSec/(1000*60));
                int sec = (int) (millionSec%(1000*60))/1000;
                str.append(String.format("%02d:%02d", min, sec));
                btn.setText(str.toString());
                btn.setBackgroundResource(R.color.green);

                isStart = true;
            }
        }
    };

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        btn = (Button) view.findViewById(R.id.btn_start);
        btn.setOnClickListener(this);

        curTxt = (TextView) view.findViewById(R.id.textView);

        bar = (SeekBar) view.findViewById(R.id.seekBar2);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                curTime = (int)((float)(endTime - startTime)*progress/100);
                curTxt.setText("" + curTime );
                int x = left+(int)((float)(right - left)*progress/100);
                if(x < left){
                    x = left;
                }else if(x >right){
                    x = right;
                }
                Log.i("yds","" + progress + "," + x + "," + right);
                curTxt.setX(x);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView txt0 = (TextView) view.findViewById(R.id.textView2);
        final TextView txt60 = (TextView) view.findViewById(R.id.textView3);

        final ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                left = (int) txt0.getX();
                right = (int)txt60.getX();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        final TextAdapter textAdapter = new TextAdapter();
        listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(textAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivityFragment.this.setTime((Integer) textAdapter.getItem(position));
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RingService.BROADCAST_RING);
        intentFilter.addAction(RingService.BROADCAST_UPDATE_TIME);
        this.getActivity().registerReceiver(receiver, intentFilter);

        setDefaultTime();
        return view;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        this.getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void onClick(View v) {
        if(isStart){
            RingService.closeService(this.getActivity());
            reset();
        }else{
            RingService.startService(this.getActivity(), curTime*60*1000);
        }

    }

    public void setTime(int time ){
        bar.setProgress((int) Math.ceil((float)time/endTime*100));
    }

    public void setDefaultTime(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setTime(defaultTime);
            }
        }, 100);
    }

    public class TextAdapter extends BaseAdapter{

        List<Integer> times;

        public TextAdapter(){
            times = new ArrayList<Integer>();
            times.add(25);
            times.add(10);
            times.add(35);
        }

        @Override
        public int getCount() {
            return times.size();
        }

        @Override
        public Object getItem(int position) {
            return times.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = new TextView(MainActivityFragment.this.getContext());
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            text.setTextColor(MainActivityFragment.this.getResources().getColor(R.color.orangle));
            int pad = 10;
            text.setPadding(pad,pad,pad,pad);
            text.setText("" + (Integer) getItem(position));
            return text;
        }
    }

}
