package org.nervos.neuron.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.nervos.neuron.R;

public class BaseActivity extends AppCompatActivity {

    private View rootView;
    private View mProgressView;
    private View mProgressCircleView;

    protected Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && getStatusBarColor() == getResources().getColor(R.color.white)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getStatusBarColor());
        }
    }

    /**
     * set statusBarColor
     */
    protected int getStatusBarColor() {
        return getResources().getColor(R.color.white);
    }

    public void onDestroy() {
        super.onDestroy();
        rootView = null;
        mProgressView = null;
        mProgressCircleView = null;
        EventBus.getDefault().unregister(this);
    }

    /**
     * 显示Progress Bar
     */
    protected void showProgressBar() {
        showProgressBar(getString(R.string.loading));
    }

    protected void showProgressBar(@StringRes int message) {
        showProgressBar(getString(message));
    }

    protected void showProgressBar(String message) {
        if (mProgressView == null) {
            mProgressView = LayoutInflater.from(this).inflate(R.layout.progressbar_layout, null);
            TextView messageText = mProgressView.findViewById(R.id.progress_bar_text);
            messageText.setText(message);
            rootView = getWindow().getDecorView();
            FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            fl.gravity = Gravity.CENTER;

            ((ViewGroup) rootView).addView(mProgressView, 1, fl);
        }
    }

    /**
     * 隐藏Progress Bar
     */
    protected void dismissProgressBar() {
        if (rootView != null && mProgressView != null) {
            ((ViewGroup) rootView).removeView(mProgressView);
        }
        mProgressView = null;
        rootView = null;
    }


    /**
     * show Progress circle
     */

    protected void showProgressCircle() {
        if (mProgressCircleView == null) {
            mProgressCircleView = LayoutInflater.from(this).inflate(R.layout.progressbar_circle, null);
            rootView = getWindow().getDecorView();
            FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            fl.gravity = Gravity.CENTER;

            ((ViewGroup) rootView).addView(mProgressCircleView, 1, fl);
        }
    }

    /**
     * hide Progress circle
     */
    protected void dismissProgressCircle() {
        if (rootView != null && mProgressCircleView != null) {
            ((ViewGroup) rootView).removeView(mProgressCircleView);
        }
        mProgressCircleView = null;
        rootView = null;
    }

    @Subscribe
    public void onEvent(Object object) {
    }
}
