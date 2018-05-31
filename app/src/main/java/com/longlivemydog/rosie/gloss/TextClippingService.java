package com.longlivemydog.rosie.gloss;

import android.accessibilityservice.AccessibilityService;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

public class TextClippingService extends AccessibilityService {

    ArrayList<AccessibilityNodeInfo> mTextNodes;

    public TextClippingService() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.v("TCS", "onServiceConnected()");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v("TCS", "onAccessibilityEvent()");

        if(event.getText().size() != 1 || event.getText().get(0) != "GLOSS_REQUEST")
            return;

        mTextNodes = new ArrayList<>();

        List<AccessibilityWindowInfo> windows = getWindows();
        ArrayList<AccessibilityNodeInfo> nodes = new ArrayList<>();
        for(AccessibilityWindowInfo window : windows) {
            nodes.add(window.getRoot());
        }

        for(int i = 0; i < nodes.size(); i++) {
            AccessibilityNodeInfo node = nodes.get(i);
            if(node == null)
                continue;

            CharSequence text = node.getText();
            if(text != null && text.length() > 0 && node.isVisibleToUser()) {
                Log.v("TCS", "--> " + text.toString());
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                Log.v("TCS", "    " + bounds.toShortString());
                Log.v("TCS", "    " + node.toString());
                mTextNodes.add(node);
            }

            for(int j = 0; j < node.getChildCount(); j++) {
                nodes.add(node.getChild(j));
            }
        }

        Intent textBroadcastIntent = new Intent();
        textBroadcastIntent.setAction("com.longlivemydog.rosie.TEXT_CLIP_NOTIFICATION");
        textBroadcastIntent.putParcelableArrayListExtra("nodes", mTextNodes);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(textBroadcastIntent);

    }

    @Override
    public void onInterrupt() {
        Log.v("TCS", "onInterrupt()");
    }

}
