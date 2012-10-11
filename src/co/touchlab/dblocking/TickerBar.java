package co.touchlab.dblocking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: qui
 * Date: 10/10/12
 * Time: 12:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class TickerBar extends View {

    public static final int THIN = 0;
    public static final int MEDIUM = THIN + 1;
    public static final int THICK = MEDIUM + 1;

    protected static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;
    protected static final int DEFAULT_TICK_COLOR = Color.GREEN;

    protected int mTickColor;
    protected float mTickWidth;

    protected float mTotalWidth;

    private Paint mBackgroundPaint;
    private Paint mBarPaint;

    protected final float DP;

    protected final float TICK_1_WIDTH;
    protected final float TICK_2_WIDTH;
    protected final float TICK_3_WIDTH;

    private List<Integer> mColorList;
    private List<Float> mWidthList;

    public TickerBar(Context context) {
        this(context, null);
    }

    public TickerBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TickerBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        DP = context.getResources().getDisplayMetrics().density;

        TICK_1_WIDTH = DP;
        TICK_2_WIDTH = DP * 2;
        TICK_3_WIDTH = DP * 3;

        init(context, attrs, defStyle);
    }

    protected void init(Context context, AttributeSet attrs, int defStyle) {

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(DEFAULT_BACKGROUND_COLOR);

        mBarPaint = new Paint();
        mBarPaint.setStyle(Paint.Style.FILL);

        mColorList = new ArrayList<Integer>();
        mWidthList = new ArrayList<Float>();

        setDefaults();

    }

    private void setDefaults() {

        mColorList.clear();
        mWidthList.clear();

        mTotalWidth = 0f;

        mTickWidth = TICK_1_WIDTH;
        mTickColor = DEFAULT_TICK_COLOR;
        mBarPaint.setColor(mTickColor);
        mBarPaint.setStrokeWidth(mTickWidth);

    }

    /**
     * Clears tick data redraws
     */
    public void clear() {
        setDefaults();
        invalidate();
    }

    /*
     * All {@code tick} methods do not update the view
     * All {@code drawTick} methods do update the view
     */
    public void tick() {
        tick(mTickColor);
    }

    public void tick(int color) {
        tick(color, THIN);
    }

    public void tick(int color, int widthEnum) {

        float width = getWidthFromEnum(widthEnum);

        if(color != mTickColor) {

            mTickColor = color;
            mBarPaint.setColor(color);

            mColorList.add(color);
            mWidthList.add(width);

        } else {

            int lastIndex = mWidthList.size() - 1;
            mWidthList.set(lastIndex, mWidthList.get(lastIndex) + width);

        }

        if(width != mTickWidth) {
            mTickWidth = width;
        }

        mTotalWidth += width;

    }

    public void drawTick() {
        tick();
        invalidate();
    }

    public void drawTick(int color) {
        tick(color);
        invalidate();
    }

    public void drawTick(int color, int widthEnum) {
        tick(color, widthEnum);
        invalidate();
    }

    protected float getWidthFromEnum(int widthEnum) {
        switch (widthEnum) {
            case THIN: return TICK_1_WIDTH;
            case MEDIUM: return TICK_2_WIDTH;
            case THICK: return TICK_3_WIDTH;
            default: return TICK_1_WIDTH;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int viewWidth = getWidth();
        final int viewHeight = getHeight();

        if(mBackgroundPaint.getColor() != Color.TRANSPARENT) {
            canvas.drawRect(0, 0, viewWidth, viewHeight, mBackgroundPaint);
        }

        final int S = mColorList.size();

        if(S > 0) {

            float x = 0;
            float barWidth;
            float normalizedWidth;

            // TODO Exit on full width drawn if not normalizing
            // TODO Left align tickbar if not normalizing
            for(int i=0; i<S; i++) {

                barWidth = mWidthList.get(i);
                normalizedWidth = barWidth / mTotalWidth * viewWidth;

                mBarPaint.setColor(mColorList.get(i));

                canvas.drawRect(x, 0, x + normalizedWidth, viewHeight, mBarPaint);

                x += normalizedWidth;

            }

        }

    }

}
