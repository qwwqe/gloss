package com.longlivemydog.rosie.gloss;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// TODO: implement Messenger/Handler interface between this and TextClipping Service
// TODO: investigate merging this and TextClippingService to obviate the above
public class FloatingDictService extends Service {
    private static final int STATE_FLOATING = 0x0001;
    private static final int STATE_GLOSSING = 0x0002;

    private int mState;

    private WindowManager mWindowManager;
    private int mWindowType;

    private WindowManager.LayoutParams mFloatingViewParams;
    private WindowManager.LayoutParams mGlossingViewParams;

    private View mFloatingView;
    private View mGlossingView;
    private ArrayList<View> mManagedViews;

    private GestureDetector mGestureDetector;
    private TextBroadcastReceiver mTextBroadcastReceiver;
    private LocalBroadcastManager mTextBroadcastManager;

    private ArrayList<AccessibilityNodeInfo> mTextNodes;

    private boolean mGlossingEnabled;
    private long mGlossingRefreshTime;

    public FloatingDictService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /** prepare floating widget **/
        mWindowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mWindowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_dict_layout, null);
        mFloatingViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                mWindowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mGlossingViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                mWindowType,
        //                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSPARENT);
        mGlossingViewParams.gravity = Gravity.TOP | Gravity.LEFT;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        addViewToWindowManager(mFloatingView, mFloatingViewParams);

        mGestureDetector = new GestureDetector(getApplicationContext(), new FloatingGestureListener());

        mFloatingView.findViewById(R.id.dict_button).setOnTouchListener(new View.OnTouchListener() {
           @Override
           public boolean onTouch(View v, MotionEvent e) {
               return mGestureDetector.onTouchEvent(e);
           }
        });

        setState(STATE_FLOATING);

        /** register for TEXT_CLIP_NOTIFICATION broadcasts from the accessibility service **/
        mTextBroadcastReceiver = new TextBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.longlivemydog.rosie.TEXT_CLIP_NOTIFICATION");

        mTextBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        mTextBroadcastManager.registerReceiver(mTextBroadcastReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mFloatingView != null) {
            mWindowManager.removeView(mFloatingView);
        }

        if(mTextBroadcastManager != null) {
            mTextBroadcastManager.unregisterReceiver(mTextBroadcastReceiver);
        }
    }

    // this must only be called when mState = STATE_GLOSSING
    public void refreshTextOverlay() {
        if(mTextNodes == null || mTextNodes.size() == 0)
            return;

        resetState();

        mGlossingView = LayoutInflater.from(FloatingDictService.this).inflate(R.layout.gloss_overlay, null, false);

        FrameLayout containerView = mGlossingView.findViewById(R.id.glossing_layout_container);

        containerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setState(STATE_FLOATING);
            }
        });

        for(AccessibilityNodeInfo nodeInfo : mTextNodes) {
            View view = LayoutInflater.from(FloatingDictService.this).inflate(R.layout.text_line_overlay, null);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            //TextView textView = view.findViewById(R.id.text_line);
            //textView.setText(nodeInfo.getText());

            Rect nodeBounds = new Rect();
            nodeInfo.getBoundsInScreen(nodeBounds);

            FrameLayout.LayoutParams textParams =
                new FrameLayout.LayoutParams(nodeBounds.width(), nodeBounds.height());

            textParams.leftMargin = nodeBounds.left;
            textParams.topMargin = nodeBounds.top;

            containerView.addView(view, textParams);
        }

        addViewToWindowManager(mGlossingView, mGlossingViewParams);
    }

    private void addViewToWindowManager(View view, WindowManager.LayoutParams params) {
        if(mManagedViews == null)
            mManagedViews = new ArrayList<>();

        mWindowManager.addView(view, params);
        mManagedViews.add(view);
    }

    private void clearManagedWindows() {
        for(View managedView : mManagedViews) {
            mWindowManager.removeView(managedView);
        }

        mManagedViews = new ArrayList<>();
    }

    private void setState(int state) {
        clearManagedWindows();

        switch(state) {
            case STATE_FLOATING:
                addViewToWindowManager(mFloatingView, mFloatingViewParams);
                break;
            case STATE_GLOSSING:
                break;
        }

        mState = state;
    }

    private void resetState() {
        setState(mState);
    }

    class FloatingGestureListener extends GestureDetector.SimpleOnGestureListener {
        int initialX, initialY;
        float initialRawX, initialRawY;

        @Override
        public boolean onDown(MotionEvent event) {
            initialX = mFloatingViewParams.x;
            initialY = mFloatingViewParams.y;
            initialRawX = event.getRawX();
            initialRawY = event.getRawY();

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            setState(STATE_GLOSSING);
            refreshTextOverlay();
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
            mFloatingViewParams.x = initialX + (int) (e2.getRawX() - initialRawX);
            mFloatingViewParams.y = initialY + (int) (e2.getRawY() - initialRawY);
            mWindowManager.updateViewLayout(mFloatingView, mFloatingViewParams);

            return true;
        }
    }

    // TODO: revise this so that messages aren't always being broadcast and unpacked
    //       (maybe try explicitly generating an accessibility event for TextClippingService?)
    class TextBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTextNodes = intent.getParcelableArrayListExtra("nodes");

            if(mGlossingEnabled) {
//                if(System.nanoTime() - mGlossingRefreshTime > 2.0e9) {
//                    refreshTextOverlay();
//                }
            }

            for(AccessibilityNodeInfo node : mTextNodes)
                Log.v("TBR", "---> " + node.getText());

        }
    }
}
