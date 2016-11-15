package com.taobao.weex.analyzer.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.taobao.weex.analyzer.core.AbstractLoopTask;
import com.taobao.weex.analyzer.R;
import com.taobao.weex.analyzer.core.FPSSampler;
import com.taobao.weex.analyzer.utils.SDKUtils;
import com.taobao.weex.analyzer.utils.ViewUtils;
import com.taobao.weex.analyzer.view.chart.TimestampLabelFormatter;

/**
 * Description:
 * <p>
 * Created by rowandjj(chuyi)<br/>
 * Date: 16/10/18<br/>
 * Time: 下午3:29<br/>
 */

public class FpsSampleView extends DragSupportOverlayView {

    private SampleFPSTask mTask;
    private DynamicChartViewController mChartViewController;

    private OnCloseListener mOnCloseListener;

    public FpsSampleView(Context application) {
        super(application);

        mWidth = WindowManager.LayoutParams.MATCH_PARENT;
        mHeight = (int) ViewUtils.dp2px(application, 150);
    }

    public void setOnCloseListener(@Nullable OnCloseListener listener) {
        this.mOnCloseListener = listener;
    }

    @NonNull
    @Override
    protected View onCreateView() {
        //prepare chart view
        int expectFPS = 60;
        mChartViewController = new DynamicChartViewController.Builder(mContext)
                .title(mContext.getResources().getString(R.string.wxt_fps))
                .titleOfAxisX(null)
                .titleOfAxisY("fps")
                .labelColor(Color.WHITE)
                .backgroundColor(Color.parseColor("#ba000000"))
                .lineColor(Color.parseColor("#CDDC39"))
                .isFill(true)
                .fillColor(Color.parseColor("#CDDC39"))
                .numXLabels(5)
                .minX(0)
                .maxX(20)
                .numYLabels(5)
                .minY(0)
                .maxY(expectFPS)
                .labelFormatter(new TimestampLabelFormatter())
                .maxDataPoints(20 + 2)
                .build();

        FrameLayout frameLayout = new FrameLayout(mContext);
        View chartView = mChartViewController.getChartView();
        frameLayout.addView(chartView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        //add close btn. it's ugly here,we can expend chart view to support close btn.
        TextView closeBtn = new TextView(mContext);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setText(mContext.getResources().getString(R.string.wxt_close));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnCloseListener != null && isViewAttached) {
                    mOnCloseListener.close(FpsSampleView.this);
                    dismiss();
                }
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ViewUtils.dp2px(mContext, 50), (int) ViewUtils.dp2px(mContext, 30));
        params.gravity = Gravity.RIGHT;
        frameLayout.addView(closeBtn, params);

        return frameLayout;
    }

    @Override
    protected void onShown() {
        if(mTask != null){
            mTask.stop();
            mTask = null;
        }
        mTask = new SampleFPSTask(mChartViewController, SDKUtils.isDebugMode(mContext));
        mTask.start();
    }

    @Override
    protected void onDismiss() {
        if (mTask != null) {
            mTask.stop();
            mTask = null;
        }
    }


    private static class SampleFPSTask extends AbstractLoopTask {
        private DynamicChartViewController mController;
        private FPSSampler mFpsChecker;
        private int mAxisXValue = -1;
        private boolean isDebug;

        SampleFPSTask(@NonNull DynamicChartViewController controller, boolean isDebug) {
            super(false,1000);
            this.mController = controller;
            this.mFpsChecker = new FPSSampler();
            this.isDebug = isDebug;
        }

        @Override
        protected void onStart() {
            super.onStart();
            if (mFpsChecker == null) {
                mFpsChecker = new FPSSampler();
            }
            mFpsChecker.reset();
            mFpsChecker.start();
        }

        @Override
        protected void onRun() {
            if (mController == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 16) {
                //check fps
                mAxisXValue++;
                final double fps = mFpsChecker.getFPS();
                if(isDebug) {
                    Log.d("weex-analyzer", "current fps : "+ fps);
                }
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        mController.appendPointAndInvalidate(mAxisXValue, fps);
                        mFpsChecker.reset();
                    }
                });
            }
        }

        @Override
        protected void onStop() {
            mController = null;
            mFpsChecker.stop();
            mFpsChecker = null;
        }
    }
}