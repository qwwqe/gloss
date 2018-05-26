package com.longlivemydog.rosie.gloss;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingDictService extends Service {
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;
    private View mFloatingView;
    private GestureDetector mGestureDetector;

    public FloatingDictService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_dict_layout, null);
        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, mParams);

        mGestureDetector = new GestureDetector(getApplicationContext(), new FloatingGestureListener());

        mFloatingView.findViewById(R.id.dict_button).setOnTouchListener(new View.OnTouchListener() {
           @Override
           public boolean onTouch(View v, MotionEvent e) {
               return mGestureDetector.onTouchEvent(e);
           }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mFloatingView != null) {
            mWindowManager.removeView(mFloatingView);
        }
    }

    class FloatingGestureListener extends GestureDetector.SimpleOnGestureListener {
        int initialX, initialY;
        float initialRawX, initialRawY;

        @Override
        public boolean onDown(MotionEvent event) {
            initialX = mParams.x;
            initialY = mParams.y;
            initialRawX = event.getRawX();
            initialRawY = event.getRawY();

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            Toast.makeText(getApplicationContext(),
                    "This should convert the screen to selectable text.",
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            Toast.makeText(getApplicationContext(),
                    "This should bring up Gloss options (either through a dropdown or an application window).",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mParams.x = initialX + (int) (e2.getRawX() - initialRawX);
            mParams.y = initialY + (int) (e2.getRawY() - initialRawY);
            mWindowManager.updateViewLayout(mFloatingView, mParams);

            return true;
        }
    }
}
