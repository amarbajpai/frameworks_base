package com.android.systemui.ambientmusic;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.R;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.StatusBar;

import com.android.systemui.ambientmusic.AmbientIndicationInflateListener;

import java.util.Locale;

public class AmbientIndicationContainer extends AutoReinflateContainer {
    private View mAmbientIndication;
    private CharSequence mIndication;
    private StatusBar mStatusBar;
    private AnimatedVectorDrawable mAnimatedIcon;
    private TextView mText;
    private Context mContext;
    private String mMediaText;
    private boolean mForcedMediaDoze;
    private Handler mHandler;
    private boolean mInfoAvailable;
    private String mInfoToSet;
    private boolean mKeyguard;
    private String mLastInfo;

    private String mTrackInfoSeparator;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
        final int iconSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.notification_menu_icon_padding);
        mAnimatedIcon = (AnimatedVectorDrawable) mContext.getDrawable(
                R.drawable.audioanim_animation).getConstantState().newDrawable();
        mAnimatedIcon.setBounds(0, 0, iconSize, iconSize);
    }

    public void hideIndication() {
        setIndication(null);
        mAnimatedIcon.stop();
    }

    public void initializeView(StatusBar statusBar, Handler handler) {
        mStatusBar = statusBar;
        addInflateListener(new AmbientIndicationInflateListener(this));
        mHandler = handler;
    }

    public void updateAmbientIndicationView(View view) {
        mAmbientIndication = findViewById(R.id.ambient_indication);
        mText = (TextView)findViewById(R.id.ambient_indication_text);
        setIndication(mMediaText);
    }

    public void updateKeyguardState(boolean keyguard) {
        if (keyguard && mInfoAvailable) {
            mText.setText(mInfoToSet);
            mLastInfo = mInfoToSet;
            mAmbientIndication.setVisibility(View.VISIBLE);
            updatePosition();
        } else {
            setCleanLayout(-1);
            mAmbientIndication.setVisibility(View.INVISIBLE);
            mText.setText(null);
        }
        if (mKeyguard != keyguard) {
            setTickerMarquee(keyguard, false);
        }
        mKeyguard = keyguard;
        // StatusBar.updateKeyguardState will call updateDozingState later
    }

    private void setTickerMarquee(boolean enable, boolean extendPulseOnNewTrack) {
        if (enable) {
            setTickerMarquee(false, false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mText.setEllipsize(TruncateAt.MARQUEE);
                    mText.setMarqueeRepeatLimit(5);
                    boolean rtl = TextUtils.getLayoutDirectionFromLocale(
                            Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
                    mText.setCompoundDrawables(rtl ? null : mAnimatedIcon, null, rtl ?
                            mAnimatedIcon : null, null);
                    mText.setSelected(true);
                    mAnimatedIcon.start();
                    if (extendPulseOnNewTrack && mStatusBar.isPulsing()) {
                        mStatusBar.getDozeScrimController().extendPulseForMusicTicker();
                    }
                }
            }, 1600);
        } else {
            mText.setEllipsize(null);
            mText.setSelected(false);
            mAnimatedIcon.stop();
        }
    }

    public void setOnPulseEvent(int reason, boolean pulsing) {
        setCleanLayout(reason);
        setTickerMarquee(pulsing,
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION);
    }

    public void setCleanLayout(int reason) {
        mForcedMediaDoze =
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION;
        updatePosition();
    }

    public void updatePosition() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.getLayoutParams();
        lp.gravity = mForcedMediaDoze ? Gravity.CENTER : Gravity.BOTTOM;
        this.setLayoutParams(lp);
    }

    public void setIndication(String notificationText) {
        mInfoToSet = null;

        if (mKeyguard) {
            // if we are already showing an Ambient Notification with track info,
            // stop the current scrolling and start it delayed again for the next song
        }

        if (!TextUtils.isEmpty(notificationText)) {
            mInfoToSet = notificationText;
        }

        mInfoAvailable = mInfoToSet != null;
        if (mInfoAvailable) {
            mMediaText = notificationText;
            boolean isAnotherTrack = mInfoAvailable
                    && (TextUtils.isEmpty(mLastInfo) || (!TextUtils.isEmpty(mLastInfo)
                    && !mLastInfo.equals(mInfoToSet)));
            if (!DozeParameters.getInstance(mContext).getAlwaysOn() && mStatusBar != null
                    && isAnotherTrack) {
                mStatusBar.triggerAmbientForMedia();
            }
            if (mKeyguard) {
                mLastInfo = mInfoToSet;
            }
        }
        mText.setText(mInfoToSet);
        mAmbientIndication.setVisibility(mKeyguard && mInfoAvailable ? View.VISIBLE : View.INVISIBLE);
    }

    public View getIndication() {
        return mAmbientIndication;
    }
}
