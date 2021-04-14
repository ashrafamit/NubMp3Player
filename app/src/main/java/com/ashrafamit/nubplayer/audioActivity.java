package com.ashrafamit.nubplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gauravk.audiovisualizer.visualizer.BarVisualizer;

import java.io.File;
import java.util.ArrayList;

public class audioActivity extends AppCompatActivity {

    ImageButton btnplay,btnnext,btnprev,btnff,btnfr;
    ImageView imageView;
    TextView txtName,txtStart,txtStop;
    SeekBar seekAudio;
    BarVisualizer visualizer;

    String sname;
    public  static final String EXTRA_NAME= "audio_name";
    public static MediaPlayer mediaPlayer;
    int position;
    ArrayList<File> myAudios;

    Thread updateSeekbar;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()== android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (visualizer != null){
            visualizer.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        getSupportActionBar().setTitle("Now Playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initialization();

        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Intent i =getIntent();
        Bundle bundle=i.getExtras();
        myAudios=(ArrayList)bundle.getParcelableArrayList("audios");
        String audioName= i.getStringExtra("audioName");
        position=bundle.getInt("position",0);

        txtName.setSelected(true);

        Uri uri=Uri.parse((myAudios.get(position).toString()));
        sname=myAudios.get(position).getName();
        txtName.setText(sname);


        mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
        mediaPlayer.start();

        updatingSeekbar();  //update seekbar

        //manually update seekbar
        seekAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress()); //automatically seek to that position which the listener wants
            }
        });

        //end time
        String endTime= createTime(mediaPlayer.getDuration());
        txtStop.setText(endTime);

        final Handler handler=new Handler();
        final int delay=1000;  //update time in every second, 1000ms=1s

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime= createTime(mediaPlayer.getCurrentPosition());
                txtStart.setText(currentTime);
                handler.postDelayed(this, delay);
            }
        },delay);   //this will update out current time, and show in start



        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer.isPlaying())
                {
                    btnplay.setBackgroundResource(R.drawable.ic_play);
                    mediaPlayer.pause();
                }
                else
                {
                    btnplay.setBackgroundResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                }
            }
        });


        //next listner, after completing a song
        afterCompletingAnAudioPlayNextAudio();


        int audioSessionId= mediaPlayer.getAudioSessionId();
        if (audioSessionId != -1){
            visualizer.setAudioSessionId(audioSessionId);
        }


        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();;
                mediaPlayer.release();
                position=((position+1)%myAudios.size());
                Uri u=Uri.parse(myAudios.get(position).toString());
                mediaPlayer=MediaPlayer.create(getApplicationContext(), u);

                sname=myAudios.get(position).getName();
                txtName.setText(sname);

                mediaPlayer.start();

                btnplay.setBackgroundResource(R.drawable.ic_pause);

                startAnimation(imageView,1);

                updatingSeekbar();  //update seekbar
                String endTime= createTime(mediaPlayer.getDuration());
                txtStop.setText(endTime);


                afterCompletingAnAudioPlayNextAudio();
                int audioSessionId= mediaPlayer.getAudioSessionId();
                if (audioSessionId != -1){
                    visualizer.setAudioSessionId(audioSessionId);
                }
            }
        });

        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position= ((position-1)<0)?(myAudios.size()-1):(position-1);

                Uri u=Uri.parse(myAudios.get(position).toString());
                mediaPlayer=MediaPlayer.create(getApplicationContext(),u);

                sname=myAudios.get(position).getName();
                txtName.setText(sname);

                mediaPlayer.start();

                btnplay.setBackgroundResource(R.drawable.ic_pause);

                startAnimation(imageView,-1);

                updatingSeekbar();  //update seekbar
                String endTime= createTime(mediaPlayer.getDuration());
                txtStop.setText(endTime);

                afterCompletingAnAudioPlayNextAudio();
                int audioSessionId= mediaPlayer.getAudioSessionId();
                if (audioSessionId != -1){
                    visualizer.setAudioSessionId(audioSessionId);
                }
            }
        });

        btnff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()+10000);  //by clicking forward button, song will move 10 sec
                }
            }
        });

        btnfr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition()-10000);  //by clicking backward button, song will move 10 sec
                }
            }
        });

    }

    private void afterCompletingAnAudioPlayNextAudio() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btnnext.performClick();
            }
        });
    }

    private void updatingSeekbar() {
        updateSeekbar=new Thread(){
            @Override
            public void run() {
                int totalDuration = mediaPlayer.getDuration();
                int currentPosition= 0;
                ///seekbar will be updated in every 500 mSec
                while(currentPosition < totalDuration){
                    try {
                        sleep(500);
                        currentPosition= mediaPlayer.getCurrentPosition();
                        seekAudio.setProgress(currentPosition);
                    }
                    catch (InterruptedException | IllegalStateException e){
                        e.printStackTrace();
                    }
                }
            }
        };

        seekAudio.setMax(mediaPlayer.getDuration());
        updateSeekbar.start();

        seekAudio.getProgressDrawable().setColorFilter(getResources().getColor(R.color.redish), PorterDuff.Mode.MULTIPLY);
        seekAudio.getThumb().setColorFilter(getResources().getColor(R.color.redish),PorterDuff.Mode.SRC_IN);

    }

    private void initialization() {
        btnplay=(ImageButton)findViewById(R.id.btnPlay);
        btnnext=(ImageButton)findViewById(R.id.btnNext);
        btnprev=(ImageButton)findViewById(R.id.btnPrev);
        btnff=(ImageButton)findViewById(R.id.btnFastForward);
        btnfr=(ImageButton)findViewById(R.id.btnFastRewind);

        txtName=(TextView)findViewById(R.id.tvSn);
        txtStart=(TextView)findViewById(R.id.tvStart);
        txtStop=(TextView)findViewById(R.id.tvStop);

        seekAudio=findViewById(R.id.seekBar);
        visualizer=findViewById(R.id.blast);

        imageView=findViewById(R.id.imageView);
    }

    public void  startAnimation(View view, int r){
        ObjectAnimator animator;
        if (r==1) {
            animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
        }else{
            animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, -360f);
        }
        animator.setDuration(1000);    ///1 second
        AnimatorSet animatorSet=new AnimatorSet();
        animatorSet.playTogether(animator);
        animator.start();
    }

    public String createTime(int duration){
        String time= "";
        int min = duration/1000/60;
        int sec= duration/1000%60;
        time+=min+":";

        if(sec<10){
            time+="0";
        }
        time+=sec;

        return time;
    }



}