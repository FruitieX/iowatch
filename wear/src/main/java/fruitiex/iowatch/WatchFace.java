package fruitiex.iowatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFace extends CanvasWatchFaceService {
    static Paint redPaint;
    static Paint whitePaint;
    static Paint bgPaint;
    static Paint outerPaint;
    static int outerOuterBgColor;

    static float thickStroke = 8.0f;
    static float thinStroke = 4.0f;
    static float circleOffs = 24.0f;

    static float minHrOverflow = 10.0f;
    static float secOverflow = 16.0f;

    // device screen details
    boolean mLowBitAmbient;
    boolean mBurnInProtection;

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        Calendar mCalendar;
        static final float TWO_PI = (float) Math.PI * 2f;

                /** Handler to update the time once a second in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (inAmbientMode) {
                whitePaint.setStrokeWidth(thinStroke);
            } else {
                whitePaint.setStrokeWidth(thickStroke);
            }
            invalidate();

            updateTimer();
        }
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            whitePaint = new Paint();
            whitePaint.setARGB(255, 152, 164, 163);
            whitePaint.setStyle(Paint.Style.STROKE);
            whitePaint.setStrokeWidth(thickStroke);
            whitePaint.setAntiAlias(true);

            redPaint = new Paint();
            redPaint.setARGB(255, 170, 91, 52);
            redPaint.setStyle(Paint.Style.STROKE);
            redPaint.setStrokeWidth(thinStroke);
            redPaint.setAntiAlias(true);

            bgPaint = new Paint();
            bgPaint.setARGB(255, 30, 35, 39);
            bgPaint.setAntiAlias(true);

            outerPaint = new Paint();
            outerPaint.setARGB(255, 32, 39, 42);
            outerPaint.setAntiAlias(true);

            outerOuterBgColor = Color.rgb(40, 49, 56);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setShowSystemUiTime(false)
                    .build());

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawColor(Color.BLACK);

            int width = bounds.width();
            int height = bounds.height();

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float innerX, innerY, outerX, outerY;

            // draw the clock pointers
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float secRot = seconds / 60f * TWO_PI;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float minRot = minutes / 60f * TWO_PI;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            float hrRot = hours / 12f * TWO_PI;

            float secLength = centerX - 60;
            float minLength = centerX - 65;
            float hrLength = centerX - 95;

            if (!isInAmbientMode()) {
                canvas.drawColor(outerOuterBgColor);
                canvas.drawCircle(centerX, centerY, width / 2, outerPaint);
                canvas.drawCircle(centerX, centerY, width / 2 - circleOffs - 20.0f, bgPaint);
                float secX = (float) Math.sin(secRot);
                float secY = (float) -Math.cos(secRot);
                canvas.drawLine(centerX - secX * secOverflow, centerY - secY * secOverflow, centerX + secX * secLength, centerY + secY * secLength, redPaint);
            }

            float minX = (float) Math.sin(minRot);
            float minY = (float) -Math.cos(minRot);
            canvas.drawLine(centerX - minX * minHrOverflow, centerY - minY * minHrOverflow, centerX + minX * minLength, centerY + minY * minLength, whitePaint);

            float hrX = (float) Math.sin(hrRot);
            float hrY = (float) -Math.cos(hrRot);
            canvas.drawLine(centerX - hrX * minHrOverflow, centerY - hrY * minHrOverflow, centerX + hrX * hrLength, centerY + hrY * hrLength, whitePaint);

            // draw the ticks/dials
            float innerTickRadius = centerX - circleOffs - 14;
            float outerTickRadius = centerX - circleOffs - 2;
            for (int tickIndex = 0; tickIndex < 4; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 4);
                innerX = (float) Math.sin(tickRot) * innerTickRadius;
                innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                outerX = (float) Math.sin(tickRot) * outerTickRadius;
                outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, whitePaint);
            }

            // draw outer circle
            canvas.drawArc(circleOffs, circleOffs, width - circleOffs, height - circleOffs, 0, 360, false, whitePaint);
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
