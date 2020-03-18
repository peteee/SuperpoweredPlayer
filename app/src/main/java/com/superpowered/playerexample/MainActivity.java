package com.superpowered.playerexample;

import android.support.v7.app.AppCompatActivity;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.content.Context;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.View;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the device's sample rate and buffer size to enable
        // low-latency Android audio output, if available.
        String samplerateString = null, buffersizeString = null;
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            }
        }
        if (samplerateString == null) samplerateString = "48000";
        if (buffersizeString == null) buffersizeString = "480";
        int samplerate = Integer.parseInt(samplerateString);
        int buffersize = Integer.parseInt(buffersizeString);

        // Files under res/raw are not zipped, just copied into the APK.
        // Get the offset and length to know where our file is located.
        AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.track);
        int fileOffset = (int)fd.getStartOffset();
        int fileLength = (int)fd.getLength();
        try {
            fd.getParcelFileDescriptor().close();
        } catch (IOException e) {
            Log.e("PlayerExample", "Close error.");
        }
        String path = getPackageResourcePath();         // get path to APK package
        System.loadLibrary("PlayerExample");    // load native library
        StartAudio(samplerate, buffersize);             // start audio engine
        OpenFile(path, fileOffset, fileLength);         // open audio file from APK
        // If the application crashes, please disable Instant Run under Build, Execution, Deployment in preferences.

        // Setup TimeStretching events
        final SeekBar speedBar = findViewById(R.id.speedBar);
        if (speedBar != null) speedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar speedBar, int progress, boolean fromUser) {
                TextView t = findViewById(R.id.tsTextVal);
                String text;
                if (progress == 10000) text = "original tempo";
                else if (progress < 10000) text = "-" + (100 - progress / 100) + "%";
                else text = "+" + (progress / 100 - 100) + "%";
                t.setText(""+(text)+"");
                onSpeedSlider(progress);
            }
            public void onStartTrackingTouch(SeekBar speedBar) {}
            public void onStopTrackingTouch(SeekBar speedBar) {}
        });
        // Setup PitchShifting events
        final SeekBar pitchBar = findViewById(R.id.pitchBar);
        if (pitchBar != null) pitchBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar pitchBar, int progress, boolean fromUser) {
                TextView p = findViewById(R.id.psTextVal);
                if (progress < -12) progress = -12;
                else if (progress > 12) progress = 12;
                if (p != null)
                    p.setText(""+progress+"");
                onPitchSlider(progress);
            }
            public void onStartTrackingTouch(SeekBar pitchBar) {}
            public void onStopTrackingTouch(SeekBar pitchBar) {}
        });
    }

    // Handle Play/Pause button toggle.
    public void PlayerExample_PlayPause (View button) {
        TogglePlayback();
        playing = !playing;
        Button b = findViewById(R.id.playPause);
        b.setText(playing ? "Pause" : "Play");
    }


    @Override
    public void onPause() {
        super.onPause();
        onBackground();
    }

    @Override
    public void onResume() {
        super.onResume();
        onForeground();
    }

    protected void onDestroy() {
        super.onDestroy();
        Cleanup();
    }

    // Functions implemented in the native library.
    private native void StartAudio(int samplerate, int buffersize);
    private native void OpenFile(String path, int offset, int length);
    private native void TogglePlayback();
    private native void onSpeedSlider(int value);
    private native void onPitchSlider(int value);
    private native void onForeground();
    private native void onBackground();
    private native void Cleanup();

    private boolean playing = false;
}
