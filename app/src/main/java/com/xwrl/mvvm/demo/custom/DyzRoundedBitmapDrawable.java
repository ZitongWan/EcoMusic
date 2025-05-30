package com.xwrl.mvvm.demo.custom;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;

@RequiresApi(21)
public final class DyzRoundedBitmapDrawable extends Drawable {

    private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG;
    final Bitmap mBitmap;
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private int mGravity = Gravity.FILL;
    private final Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
    private final BitmapShader mBitmapShader;
    private final Matrix mShaderMatrix = new Matrix();
    private float mCornerRadius;

    final Rect mDstRect = new Rect();   // Gravity.apply() sets this
    private final RectF mDstRectF = new RectF();

    private boolean mApplyGravity = true;
    private boolean mIsCircular;

    // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;
    private final int mRoundAngel;

    /**
     * Returns the paint used to render this drawable.
     */
    @NonNull
    public final Paint getPaint() {
        return mPaint;
    }

    /**
     * Returns the bitmap used by this drawable to render. May be null.
     */
    @Nullable
    public final Bitmap getBitmap() {
        return mBitmap;
    }

    private void computeBitmapSize() {
        mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
        mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    }

    /**
     * Set the density scale at which this drawable will be rendered. This
     * method assumes the drawable will be rendered at the same density as the
     * specified canvas.
     *
     * @param canvas The Canvas from which the density scale must be obtained.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public void setTargetDensity(@NonNull Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public void setTargetDensity(@NonNull DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    /**
     * Get the gravity used to position/stretch the bitmap within its bounds.
     *
     * @return the gravity applied to the bitmap
     *
     * @see Gravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Set the gravity used to position/stretch the bitmap within its bounds.
     *
     * @param gravity the gravity
     *
     * @see Gravity
     */
    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            mApplyGravity = true;
            invalidateSelf();
        }
    }

    /**
     * Enables or disables the mipmap hint for this drawable's bitmap.
     * See {@link Bitmap#setHasMipMap(boolean)} for more information.
     *
     * If the bitmap is null, or the current API version does not support setting a mipmap hint,
     * calling this method has no effect.
     *
     * @param mipMap True if the bitmap should use mipmaps, false otherwise.
     *
     * @see #hasMipMap()
     */
    public void setMipMap(boolean mipMap) {
        if (mBitmap != null) {
            mBitmap.setHasMipMap(mipMap);
            invalidateSelf();
        }
        //throw new UnsupportedOperationException(); // must be overridden in subclasses
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's bitmap.
     *
     * @return True if the mipmap hint is set, false otherwise. If the bitmap
     *         is null, this method always returns false.
     *
     * @see #setMipMap(boolean)
     */
    public boolean hasMipMap() {
        return mBitmap != null && mBitmap.hasMipMap();
    }

    /**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     *
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     *
     * @see #hasAntiAlias()
     */
    public void setAntiAlias(boolean aa) {
        mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    /**
     * Indicates whether anti-aliasing is enabled for this drawable.
     *
     * @return True if anti-aliasing is enabled, false otherwise.
     *
     * @see #setAntiAlias(boolean)
     */
    public boolean hasAntiAlias() {
        return mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        mPaint.setDither(dither);
        invalidateSelf();
    }

    void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight,
                            Rect bounds, Rect outRect) {
        Gravity.apply(gravity, bitmapWidth, bitmapHeight,
                bounds, outRect, View.LAYOUT_DIRECTION_LTR);
    }

    //TODO:
    void updateDstRect() {
        if (mApplyGravity) {
            if (mIsCircular) {
                final int minDimen = Math.min(mBitmapWidth, mBitmapHeight);
                gravityCompatApply(mGravity, minDimen, minDimen, getBounds(), mDstRect);

                // inset the drawing rectangle to the largest contained square,
                // so that a circle will be drawn
                final int minDrawDimen = Math.min(mDstRect.width(), mDstRect.height());
                final int insetX = Math.max(0, (mDstRect.width() - minDrawDimen) / 2);
                final int insetY = Math.max(0, (mDstRect.height() - minDrawDimen) / 2);
                mDstRect.inset(insetX, insetY);
                mCornerRadius = 0.5f * minDrawDimen;
            } else {
                //drawRoundAngel(mRoundAngel);
                gravityCompatApply(mGravity, mBitmapWidth, mBitmapHeight, getBounds(), mDstRect);
            }
            mDstRectF.set(mDstRect);

            if (mBitmapShader != null) {
                // setup shader matrix
                mShaderMatrix.setTranslate(mDstRectF.left,mDstRectF.top);
                mShaderMatrix.preScale(
                        mDstRectF.width() / mBitmap.getWidth(),
                        mDstRectF.height() / mBitmap.getHeight());
                mBitmapShader.setLocalMatrix(mShaderMatrix);
                mPaint.setShader(mBitmapShader);
            }

            mApplyGravity = false;
        }
    }

    @Override
    //TODO:
    public void draw(@NonNull Canvas canvas) {
        final Bitmap bitmap = mBitmap;
        if (bitmap == null) {
            return;
        }
        updateDstRect();
        if (mPaint.getShader() == null) {
            canvas.drawBitmap(bitmap, null, mDstRect, mPaint);
        } else {
            canvas.drawRoundRect(drawRoundAngel(mRoundAngel), mCornerRadius, mCornerRadius, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    /**
     * Sets the image shape to circular.
     * <p>This overwrites any calls made to {@link #setCornerRadius(float)} so far.</p>
     */
    public void setCircular(boolean circular) {
        mIsCircular = circular;
        mApplyGravity = true;
        if (circular) {
            updateCircularCornerRadius();
            mPaint.setShader(mBitmapShader);
            invalidateSelf();
        } else {
            setCornerRadius(0);
        }
    }

    private void updateCircularCornerRadius() {
        final int minCircularSize = Math.min(mBitmapHeight, mBitmapWidth);
        mCornerRadius = minCircularSize >> 1;
    }

    /**
     * @return <code>true</code> if the image is circular, else <code>false</code>.
     */
    public boolean isCircular() {
        return mIsCircular;
    }

    /**
     * TODO:
     * Sets the corner radius to be applied when drawing the bitmap.
     */
    public void setCornerRadius(float cornerRadius) {
        if (mCornerRadius == cornerRadius) return;

        mIsCircular = false;
        if (isGreaterThanZero(cornerRadius)) {
            mPaint.setShader(mBitmapShader);
        } else {
            mPaint.setShader(null);
        }

        mCornerRadius = cornerRadius;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (mIsCircular) {
            updateCircularCornerRadius();
        }
        mApplyGravity = true;
    }

    /**
     * @return The corner radius applied when drawing the bitmap.
     */
    public float getCornerRadius() {
        return mCornerRadius;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        if (mGravity != Gravity.FILL || mIsCircular) {
            return PixelFormat.TRANSLUCENT;
        }
        Bitmap bm = mBitmap;
        return (bm == null
                || bm.hasAlpha()
                || mPaint.getAlpha() < 255
                || isGreaterThanZero(mCornerRadius))
                ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        updateDstRect();
        outline.setRoundRect(mDstRect, getCornerRadius());
    }

    //TODO:初始化构造方法
    private DyzRoundedBitmapDrawable(Resources res, Bitmap bitmap, int angel) {
        mRoundAngel = angel;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        }

        mBitmap = bitmap;
        if (mBitmap != null) {
            computeBitmapSize();
            mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        } else {
            mBitmapWidth = mBitmapHeight = -1;
            mBitmapShader = null;
        }
    }
    @NonNull
    public static DyzRoundedBitmapDrawable create(@NonNull Resources res,
                                                  @Nullable Bitmap bitmap,
                                                  int angelModel) {
        return new DyzRoundedBitmapDrawable(res, bitmap,angelModel);
    }

    @NonNull
    public static DyzRoundedBitmapDrawable create(@NonNull Resources res,
                                                  @ColorInt int color,
                                                  int angelModel) {

        return create(res,
                createColorDrawable(
                res,color,230,230).get(),
                angelModel);
    }

    @NonNull
    public static DyzRoundedBitmapDrawable create(int resId,
                                                  @NonNull Resources res,
                                                  int angelModel) {

        return create(res,
                BitmapFactory.decodeResource(res,resId),
                angelModel);
    }

    private static boolean isGreaterThanZero(float toCompare) {
        return toCompare > 0.05f;
    }

    /**
     * 从左至右四个角：
     *               top
     *           -------------
     *     left  |1         2| right
     *           |3         4|
     *           -------------
     *              bottom
     * @param angel 圆角显示模式，Switch开关语句控制 哪些角 圆角处理
     * @return RectF 返回一个矩形对象，籍此{@link Canvas}才能获得绘制的范围
     * */
    private RectF drawRoundAngel(int angel){
        if (angel < 0) return mDstRectF;
        //Log.e(TAG, "draw: ");
        float left = mDstRectF.left, right = mDstRectF.right,
                top = mDstRectF.top, bottom = mDstRectF.bottom;
        switch (angel) {
            //默认，四个角为圆角
            case 0:
                break;
            //默认，第1 角为圆角
            case 1:
                right += mCornerRadius;
                bottom += mCornerRadius;
                break;
            //默认，第2 角为圆角
            case 2:
                left -= mCornerRadius;
                bottom += mCornerRadius;
                break;
            //默认，第3 角为圆角
            case 3:
                top -= mCornerRadius;
                right += mCornerRadius;
                break;
            //默认，第4 角为圆角
            case 4:
                top -= mCornerRadius;
                left -= mCornerRadius;
                break;
            //默认，1，2 两个角为圆角
            case 12:
                bottom += mCornerRadius;
                break;
            //默认，1，3 两个角为圆角
            case 13:
                right += mCornerRadius;
                break;
            //默认，2，4 两个角为圆角
            case 24:
                left -= mCornerRadius;
                break;
            //默认，3，4 两个角为圆角
            case 34:
                top -= mCornerRadius;
                break;
            default:
                break;
        }
        mDstRectF.left = left;
        mDstRectF.bottom = bottom;
        mDstRectF.right = right;
        mDstRectF.top = top;
        return mDstRectF;
        //Log.e(TAG, "draw: 左 = "+left+", 右 = "+right+", 上 = "+top+", 下 = "+bottom);
    }
    /** 创建一个圆形的纯色drawable
     * @param resources 访问源文件的对象
     * @param color int型的颜色，使用源文件values/color.xml 或者 {@link Color#argb(int, int, int, int)} 获得
     * @param bitmapWidth 需要创建多大的Bitmap，设置其宽度
     * @param bitmapHeight 需要创建多大的Bitmap，设置其高度
     * @return 返回一个圆形、纯色的RoundedBitmapDrawable
     * <a href>https://blog.csdn.net/u010054982/article/details/52487599?utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control&dist_request_id=1619603308189_75864&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromMachineLearnPai2%7Edefault-1.control</a>
     * */
    public static WeakReference<Bitmap> createColorDrawable(Resources resources,
                                                            @ColorInt int color,
                                                            int bitmapWidth,
                                                            int bitmapHeight){
        if (resources == null) return null;

        if(color == 0){ color = Color.parseColor("#E6E6E6"); }
        Bitmap colorBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight, Bitmap.Config.ARGB_8888);
        colorBitmap.eraseColor(color);
        return new WeakReference<>(colorBitmap);
    }

}
