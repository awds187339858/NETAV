package com.hz.quickdock;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 只在「点按侧栏语音按钮」后的 15 秒窗口内工作：
 * 在百度地图界面里找到小度语音入口并点一下，然后立即失效。
 * 其余时间完全不做任何事，不读界面、不拦截事件。
 */
public class VoiceClickService extends AccessibilityService {

    /** DockService 点语音按钮时置 true。 */
    public static volatile boolean pending = false;
    public static volatile long pendingAt = 0L;

    private static final long WINDOW_MS = 15000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static void arm() {
        pendingAt = System.currentTimeMillis();
        pending = true;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // 轮询只在 pending 期间做事，平时只是判断一个布尔值
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (pending && System.currentTimeMillis() - pendingAt <= WINDOW_MS) {
                    tryClick();
                } else if (pending) {
                    pending = false;
                }
                handler.postDelayed(this, 500L);
            }
        }, 500L);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (pending && System.currentTimeMillis() - pendingAt <= WINDOW_MS) {
            tryClick();
        }
    }

    private synchronized void tryClick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        AccessibilityNodeInfo target = findByViewId(root, "voice_xd_page_container");
        if (target == null) {
            target = findByViewId(root, "userHeadIcon");
        }
        if (target == null) {
            return;
        }
        boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!ok) {
            AccessibilityNodeInfo p = target.getParent();
            if (p != null) {
                ok = p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        if (ok) {
            pending = false;
        }
    }

    private AccessibilityNodeInfo findByViewId(AccessibilityNodeInfo node, String part) {
        if (node == null) {
            return null;
        }
        CharSequence id = node.getViewIdResourceName();
        if (id != null && id.toString().contains(part)) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo hit = findByViewId(child, part);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        pending = false;
    }
}
