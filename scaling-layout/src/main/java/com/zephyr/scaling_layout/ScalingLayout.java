package com.zephyr.scaling_layout;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * Created by mertsimsek on 30/09/2017.
 */
public class ScalingLayout extends FrameLayout {

    /**
     * Settings
     */
    ScalingLayoutSettings settings;

    /**
     * Current radius
     */
    private float currentRadius;

    /**
     * Width is dependent value. It depends on
     * radius. If radius gets updated,
     * layout width will be updated according to this change.
     */
    private int currentWidth;

    /**
     * If layout has margins, margin has to be change
     * according to radius.
     */
    private float[] maxMargins;
    private float[] currentMargins;

    /**
     * State for layout.
     */
    private State state;

    /**
     * Values to draw rounded on layout
     */
    private Path path;
    private Path outlinePath;
    private RectF rectF;
    private Paint maskPaint;

    /**
     * Animator to expand and collapse
     */
    private ValueAnimator valueAnimator;

    /**
     * Listener to notify observer about
     * progress and collapse/expand
     */
    private ScalingLayoutListener scalingLayoutListener;

    /**
     * CustomOutline for elevation shadows
     */
    @Nullable
    private ScalingLayoutOutlineProvider viewOutline;


    public ScalingLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ScalingLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ScalingLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("NewApi")
    public ScalingLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    /**
     * Initialize layout
     */
    public void init(Context context, AttributeSet attributeSet) {
        settings = new ScalingLayoutSettings(context, attributeSet);
        settings.setElevation(ViewCompat.getElevation(this));
        state = State.COLLAPSED;

        path = new Path();
        outlinePath = new Path();
        rectF = new RectF(0, 0, 0, 0);

        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        setLayerType(LAYER_TYPE_HARDWARE, null);

        valueAnimator = ValueAnimator.ofFloat(0, 0);
        valueAnimator.setDuration(200);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                setRadius((Float) valueAnimator.getAnimatedValue());
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (maxMargins == null) {
            maxMargins = new float[4];
            currentMargins = new float[4];
            MarginLayoutParams marginLayoutParams = ((MarginLayoutParams) getLayoutParams());
            currentMargins[0] = maxMargins[0] = marginLayoutParams.leftMargin;
            currentMargins[1] = maxMargins[1] = marginLayoutParams.topMargin;
            currentMargins[2] = maxMargins[2] = marginLayoutParams.rightMargin;
            currentMargins[3] = maxMargins[3] = marginLayoutParams.bottomMargin;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (!settings.isInitialized()) {
            settings.initialize(w, h);
            currentWidth = w;
            currentRadius = settings.getMaxRadius();
            viewOutline = new ScalingLayoutOutlineProvider(w, h, currentRadius);
        }

        rectF.set(0, 0, w, h);
        updateViewOutline(h, currentWidth, currentRadius);
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);

        path.reset();
        path.addRoundRect(rectF, currentRadius, currentRadius, Path.Direction.CCW);
        canvas.drawPath(path, maskPaint);
    }

    /**
     * Provides current {@link ScalingLayoutSettings}
     */
    public ScalingLayoutSettings getSettings() {
        return settings;
    }

    /**
     * get current state of layout
     */
    public State getState() {
        return state;
    }

    /**
     * Expand layout to screen
     */
    public void expand() {
        valueAnimator.setFloatValues(settings.getMaxRadius(), 0);
        valueAnimator.start();
    }

    /**
     * Collapse layout to initial position
     */
    public void collapse() {
        valueAnimator.setFloatValues(0, settings.getMaxRadius());
        valueAnimator.start();
    }

    /**
     * This method takes a progress parameter value
     * between 0.0f and 1.0f. And apply this
     * progress value to layout.
     */
    public void setProgress(float progress) {
        if (progress > 1.0f || progress < 0.0f) {
            return;
        }
        setRadius(settings.getMaxRadius() - settings.getMaxRadius() * progress);
    }

    public void setListener(ScalingLayoutListener scalingLayoutListener) {
        this.scalingLayoutListener = scalingLayoutListener;
    }

    /**
     * Updates view outline borders and radius
     */
    private void updateViewOutline(int height, int width, float radius) {
        if (ViewCompat.getElevation(this) > 0f) {
            try {
                assert viewOutline != null;
                viewOutline.setHeight(height);
                viewOutline.setWidth(width);
                viewOutline.setRadius(radius);
                setOutlineProvider(viewOutline);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Set radius will update layout radius
     * Also layouts margins and width depend on
     * radius. So If you update radius, your layout's width
     * and margins will be updated.
     */
    private void setRadius(float radius) {
        if (radius < 0) {
            return;
        }

        updateCurrentRadius(radius);
        updateCurrentWidth(currentRadius);
        updateCurrentMargins(currentRadius);
        updateState(currentRadius);
        updateCurrentElevation();

        getLayoutParams().width = currentWidth;
        ((MarginLayoutParams) getLayoutParams())
                .setMargins((int) currentMargins[0],
                        (int) currentMargins[1],
                        (int) currentMargins[2],
                        (int) currentMargins[3]);
        requestLayout();
    }

    /**
     * Update current radius
     */
    private void updateCurrentRadius(float radius) {
        currentRadius = Math.min(radius, settings.getMaxRadius());
    }

    /**
     * Update layout width with given radius value
     */
    private void updateCurrentWidth(float currentRadius) {
        int diffPixel = settings.getMaxWidth() - settings.getInitialWidth();
        float calculatedWidth = (diffPixel - (currentRadius * diffPixel / settings.getMaxRadius())) + settings.getInitialWidth();
        if (calculatedWidth > settings.getMaxWidth()) {
            currentWidth = settings.getMaxWidth();
        } else if (calculatedWidth < settings.getInitialWidth()) {
            currentWidth = settings.getInitialWidth();
        } else {
            currentWidth = (int) calculatedWidth;
        }
    }

    /**
     * Update layout margins with given radius value
     */
    private void updateCurrentMargins(float currentRadius) {
        currentMargins[0] = maxMargins[0] * currentRadius / settings.getMaxRadius();
        currentMargins[1] = maxMargins[1] * currentRadius / settings.getMaxRadius();
        currentMargins[2] = maxMargins[2] * currentRadius / settings.getMaxRadius();
        currentMargins[3] = maxMargins[3] * currentRadius / settings.getMaxRadius();
    }

    /**
     * Updates layout state
     */
    private void updateState(float currentRadius) {
        if (currentRadius == 0) {
            state = State.EXPANDED;
        } else if (currentRadius == settings.getMaxRadius()) {
            state = State.COLLAPSED;
        } else {
            state = State.PROGRESSING;
        }
        notifyListener();
    }

    private void updateCurrentElevation() {
        ViewCompat.setElevation(this, settings.getElevation());
        if (ViewCompat.getElevation(this) > 0f) {
            try {
                setOutlineProvider(getOutlineProvider());
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Notify observers about change
     */
    private void notifyListener() {
        if (scalingLayoutListener != null) {
            if (state == State.COLLAPSED) {
                scalingLayoutListener.onCollapsed();
            } else if (state == State.EXPANDED) {
                scalingLayoutListener.onExpanded();
            } else {
                scalingLayoutListener.onProgress(currentRadius / settings.getMaxRadius());
            }
        }
    }

    @Override
    public ViewOutlineProvider getOutlineProvider() {
        return new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setConvexPath(path);
            }
        };
    }
}