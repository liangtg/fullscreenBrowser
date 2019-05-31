package com.liangtg.fullscreenbrowser;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TOOLBAR_ACTION = BuildConfig.APPLICATION_ID + ".TOOLBAR_ACTION";
    private static final String TAG = "web";
    private final String HOME_URL = "http://m.baidu.com/error.jsp";
    private ViewHolder viewHolder;
    private ToolbarReceiver toolbarReceiver = new ToolbarReceiver();

    private static void d(Object object) {
        Log.d(TAG, String.valueOf(object));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullscreen();
        setContentView(R.layout.activity_main);
        viewHolder = new ViewHolder();
        initWebSettings();
        getWindow().getDecorView().postOnAnimation(new Runnable() {
            @Override public void run() {
                if (isFinishing()) return;
                loadUrl();
                //viewHolder.hideToolbar();
            }
        });
    }

    private void fullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initWebSettings() {
        WebSettings settings = viewHolder.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
    }

    private int getStatusBarHeight() {
        int statusBarHeight1 = -1;
        //获取status_bar_height资源的ID
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight1 = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight1;
    }

    private void loadUrl() {
        String url = getIntent().getDataString();
        if (TextUtils.isEmpty(url)) url = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(url) && url.startsWith("http")) {
            viewHolder.webView.loadUrl(url);
        } else {
            viewHolder.webView.loadUrl(HOME_URL);
        }
        d(getIntent().toString());
        Bundle extras = getIntent().getExtras();
        if (null == extras) return;
        Set<String> set = extras.keySet();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            String key = it.next();
            d(key + " -- " + extras.get(key) + "");
        }
    }

    @Override public void onBackPressed() {
        if (viewHolder.webView.canGoBack()) {
            viewHolder.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadUrl();
    }

    private void createShotcut() {
        Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Intent target = new Intent(this, MainActivity.class);
        target.setData(Uri.parse(viewHolder.webView.getUrl()));
        intent.putExtra("duplicate", false);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, viewHolder.webView.getTitle());
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, viewHolder.webView.getFavicon());
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);
        sendBroadcast(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "app");
        builder.setAutoCancel(false);
        builder.setContentIntent(
            PendingIntent.getBroadcast(this, 0, new Intent(TOOLBAR_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setContentText("open toolbar");
        builder.setSmallIcon(R.drawable.ic_toolbar);
        NotificationManagerCompat.from(this).notify(10, builder.build());
        registerReceiver(toolbarReceiver, new IntentFilter(TOOLBAR_ACTION));
    }

    @Override protected void onPause() {
        super.onPause();
        NotificationManagerCompat.from(this).cancel(10);
        unregisterReceiver(toolbarReceiver);
    }

    private class ToolbarReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            viewHolder.showToolbar();
        }
    }

    private class ViewHolder implements View.OnClickListener, View.OnTouchListener,
        Toolbar.OnMenuItemClickListener {
        private WebView webView;
        private View progressBar;
        private Toolbar toolbar;
        private LayerDrawable progressDrawable;
        private View toolbarTouch;

        public ViewHolder() {
            toolbarTouch = findViewById(R.id.toolbar_touch);
            toolbarTouch.setOnTouchListener(this);
            toolbar = findViewById(R.id.toolbar);
            toolbar.inflateMenu(R.menu.main);
            toolbar.setOnMenuItemClickListener(this);
            webView = findViewById(R.id.webview);
            progressBar = findViewById(R.id.progress_horizontal);
            progressBar.getLayoutParams().height = getStatusBarHeight();
            progressBar.setOnClickListener(this);
            progressDrawable = (LayerDrawable) progressBar.getBackground();
            webView.setWebChromeClient(new WebChromeClient() {
                @Override public void onProgressChanged(WebView view, int newProgress) {
                    progressDrawable.getDrawable(1).setLevel(newProgress * 100);
                }
            });
            webView.setWebViewClient(new WebViewClient() {
                @Override public void onPageFinished(WebView view, String url) {
                }

                @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    //d("start: " + url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    d(request.getUrl());
                    if (!request.getUrl().getScheme().startsWith("http")) {
                        return true;
                    }
                    return false;
                }
            });
        }

        private void showToolbar() {
            toolbar.setVisibility(View.VISIBLE);
            toolbarTouch.setVisibility(View.VISIBLE);
        }

        private void hideToolbar() {
            toolbar.startAnimation(
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.hide_bottom_top));
            toolbar.setVisibility(View.GONE);
            toolbarTouch.setVisibility(View.GONE);
        }

        @Override public void onClick(View v) {
            int id = v.getId();
            if (R.id.progress_horizontal == id) {
                showToolbar();
            }
        }

        @Override public boolean onTouch(View v, MotionEvent event) {
            hideToolbar();
            return false;
        }

        @Override public boolean onMenuItemClick(MenuItem menuItem) {
            hideToolbar();
            int id = menuItem.getItemId();
            if (R.id.mn_home == id) {
                webView.loadUrl(HOME_URL);
            } else if (R.id.mn_shortcut == id) {
                createShotcut();
            }
            return true;
        }
    }
}
