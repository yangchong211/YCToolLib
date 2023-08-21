package com.yc.zxingserver.scan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.yc.zxingserver.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 20;

    /**
     * 画笔
     */
    private Paint paint;

    /**
     * 文本画笔
     */
    private TextPaint textPaint;
    /**
     * 扫码框外面遮罩颜色
     */
    private int maskColor;
    /**
     * 扫描区域边框颜色
     */
    private int frameColor;
    /**
     * 扫描线颜色
     */
    private int laserColor;
    /**
     * 扫码框四角颜色
     */
    private int cornerColor;
    /**
     * 结果点颜色
     */
    private int resultPointColor;

    /**
     * 提示文本与扫码框的边距
     */
    private float labelTextPadding;
    /**
     * 提示文本的位置
     */
    private TextLocation labelTextLocation;
    /**
     * 扫描区域提示文本
     */
    private String labelText;
    /**
     * 扫描区域提示文本颜色
     */
    private int labelTextColor;
    /**
     * 提示文本字体大小
     */
    private float labelTextSize;

    /**
     * 扫描线开始位置
     */
    public int scannerStart = 0;
    /**
     * 扫描线结束位置
     */
    public int scannerEnd = 0;
    /**
     * 是否显示结果点
     */
    private boolean isShowResultPoint;

    /**
     * 屏幕宽
     */
    private int screenWidth;
    /**
     * 屏幕高
     */
    private int screenHeight;
    /**
     * 扫码框宽
     */
    private int frameWidth;
    /**
     * 扫码框高
     */
    private int frameHeight;
    /**
     * 扫描激光线风格
     */
    private LaserStyle laserStyle;

    /**
     * 网格列数
     */
    private int gridColumn;
    /**
     * 网格高度
     */
    private int gridHeight;

    /**
     * 扫码框
     */
    private Rect frame;

    /**
     * 扫描区边角的宽
     */
    private int cornerRectWidth;
    /**
     * 扫描区边角的高
     */
    private int cornerRectHeight;
    /**
     * 扫描线每次移动距离
     */
    private int scannerLineMoveDistance;
    /**
     * 扫描线高度
     */
    private int scannerLineHeight;

    /**
     * 边框线宽度
     */
    private int frameLineWidth;

    /**
     * 扫描动画延迟间隔时间 默认15毫秒
     */
    private int scannerAnimationDelay;

    /**
     * 扫码框占比
     */
    private float frameRatio;


    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    public enum LaserStyle{
        NONE(0),LINE(1),GRID(2);
        private int mValue;
        LaserStyle(int value){
            mValue = value;
        }

        private static LaserStyle getFromInt(int value){

            for(LaserStyle style : LaserStyle.values()){
                if(style.mValue == value){
                    return style;
                }
            }

            return LaserStyle.LINE;
        }
    }

    public enum TextLocation {
        TOP(0),BOTTOM(1);

        private int mValue;

        TextLocation(int value){
            mValue = value;
        }

        private static TextLocation getFromInt(int value){

            for(TextLocation location : TextLocation.values()){
                if(location.mValue == value){
                    return location;
                }
            }

            return TextLocation.TOP;
        }


    }

    public ViewfinderView(Context context) {
        this(context,null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        //初始化自定义属性信息
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
        maskColor = array.getColor(R.styleable.ViewfinderView_maskColor, ContextCompat.getColor(context,R.color.viewfinder_mask));
        frameColor = array.getColor(R.styleable.ViewfinderView_frameColor, ContextCompat.getColor(context,R.color.viewfinder_frame));
        cornerColor = array.getColor(R.styleable.ViewfinderView_cornerColor, ContextCompat.getColor(context,R.color.viewfinder_corner));
        laserColor = array.getColor(R.styleable.ViewfinderView_laserColor, ContextCompat.getColor(context,R.color.viewfinder_laser));
        resultPointColor = array.getColor(R.styleable.ViewfinderView_resultPointColor, ContextCompat.getColor(context,R.color.viewfinder_result_point_color));

        labelText = array.getString(R.styleable.ViewfinderView_labelText);
        labelTextColor = array.getColor(R.styleable.ViewfinderView_labelTextColor, ContextCompat.getColor(context,R.color.viewfinder_text_color));
        labelTextSize = array.getDimension(R.styleable.ViewfinderView_labelTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14f,getResources().getDisplayMetrics()));
        labelTextPadding = array.getDimension(R.styleable.ViewfinderView_labelTextPadding,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,24,getResources().getDisplayMetrics()));
        labelTextLocation = TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderView_labelTextLocation,0));

        isShowResultPoint = array.getBoolean(R.styleable.ViewfinderView_showResultPoint,false);

        frameWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameWidth,0);
        frameHeight = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameHeight,0);

        laserStyle = LaserStyle.getFromInt(array.getInt(R.styleable.ViewfinderView_laserStyle, LaserStyle.LINE.mValue));
        gridColumn = array.getInt(R.styleable.ViewfinderView_gridColumn,20);
        gridHeight = (int)array.getDimension(R.styleable.ViewfinderView_gridHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,40,getResources().getDisplayMetrics()));

        cornerRectWidth = (int)array.getDimension(R.styleable.ViewfinderView_cornerRectWidth,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,4,getResources().getDisplayMetrics()));
        cornerRectHeight = (int)array.getDimension(R.styleable.ViewfinderView_cornerRectHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,16,getResources().getDisplayMetrics()));
        scannerLineMoveDistance = (int)array.getDimension(R.styleable.ViewfinderView_scannerLineMoveDistance,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,2,getResources().getDisplayMetrics()));
        scannerLineHeight = (int)array.getDimension(R.styleable.ViewfinderView_scannerLineHeight,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,5,getResources().getDisplayMetrics()));
        frameLineWidth = (int)array.getDimension(R.styleable.ViewfinderView_frameLineWidth,TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1,getResources().getDisplayMetrics()));
        scannerAnimationDelay = array.getInteger(R.styleable.ViewfinderView_scannerAnimationDelay,15);
        frameRatio = array.getFloat(R.styleable.ViewfinderView_frameRatio,0.625f);
        array.recycle();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;

        screenWidth = getDisplayMetrics().widthPixels;
        screenHeight = getDisplayMetrics().heightPixels;

        int size = (int)(Math.min(screenWidth,screenHeight) * frameRatio);

        if(frameWidth<=0 || frameWidth > screenWidth){
            frameWidth = size;
        }

        if(frameHeight<=0 || frameHeight > screenHeight){
            frameHeight = size;
        }

    }

    private DisplayMetrics getDisplayMetrics(){
        return getResources().getDisplayMetrics();
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public void setLabelTextColor(@ColorInt int color) {
        this.labelTextColor = color;
    }

    public void setLabelTextColorResource(@ColorRes int id){
        this.labelTextColor = ContextCompat.getColor(getContext(),id);
    }

    public void setLabelTextSize(float textSize) {
        this.labelTextSize = textSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //扫码框默认居中，支持利用内距偏移扫码框
        int leftOffset = (screenWidth - frameWidth) / 2 + getPaddingLeft() - getPaddingRight();
        int topOffset = (screenHeight - frameHeight) / 2 + getPaddingTop() - getPaddingBottom();
        frame = new Rect(leftOffset, topOffset, leftOffset + frameWidth, topOffset + frameHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (frame == null) {
            return;
        }

        if(scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom - scannerLineHeight;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas,frame,width,height);
        // Draw a red "laser scanner" line through the middle to show decoding is active
        drawLaserScanner(canvas,frame);
        // Draw a two pixel solid black border inside the framing rect
        drawFrame(canvas, frame);
        // 绘制边角
        drawCorner(canvas, frame);
        //绘制提示信息
        drawTextInfo(canvas, frame);
        //绘制扫码结果点
        drawResultPoint(canvas,frame);
        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(scannerAnimationDelay,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    /**
     * 绘制文本
     * @param canvas
     * @param frame
     */
    private void drawTextInfo(Canvas canvas, Rect frame) {
        if(!TextUtils.isEmpty(labelText)){
            textPaint.setColor(labelTextColor);
            textPaint.setTextSize(labelTextSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            StaticLayout staticLayout = new StaticLayout(labelText,textPaint,canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL,1.0f,0.0f,true);
            if(labelTextLocation == TextLocation.BOTTOM){
                canvas.translate(frame.left + frame.width() / 2,frame.bottom + labelTextPadding);
                staticLayout.draw(canvas);
            }else{
                canvas.translate(frame.left + frame.width() / 2,frame.top - labelTextPadding - staticLayout.getHeight());
                staticLayout.draw(canvas);
            }
        }

    }

    /**
     * 绘制边角
     * @param canvas
     * @param frame
     */
    private void drawCorner(Canvas canvas, Rect frame) {
        paint.setColor(cornerColor);
        //左上
        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectWidth, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectHeight, frame.top + cornerRectWidth, paint);
        //右上
        canvas.drawRect(frame.right - cornerRectWidth, frame.top, frame.right, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.top, frame.right, frame.top + cornerRectWidth, paint);
        //左下
        canvas.drawRect(frame.left, frame.bottom - cornerRectWidth, frame.left + cornerRectHeight, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerRectHeight, frame.left + cornerRectWidth, frame.bottom, paint);
        //右下
        canvas.drawRect(frame.right - cornerRectWidth, frame.bottom - cornerRectHeight, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.bottom - cornerRectWidth, frame.right, frame.bottom, paint);
    }

    /**
     * 绘制激光扫描线
     * @param canvas
     * @param frame
     */
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        if(laserStyle!=null){
            paint.setColor(laserColor);
            switch (laserStyle){
                case LINE://线
                    drawLineScanner(canvas,frame);
                    break;
                case GRID://网格
                    drawGridScanner(canvas,frame);
                    break;
            }
            paint.setShader(null);
        }
    }

    /**
     * 绘制线性式扫描
     * @param canvas
     * @param frame
     */
    private void drawLineScanner(Canvas canvas,Rect frame){
        //线性渐变
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + scannerLineHeight,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        paint.setShader(linearGradient);
        if(scannerStart <= scannerEnd) {
            //椭圆
            RectF rectF = new RectF(frame.left + 2 * scannerLineHeight, scannerStart, frame.right - 2 * scannerLineHeight, scannerStart + scannerLineHeight);
            canvas.drawOval(rectF, paint);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }
    }

    /**
     * 绘制网格式扫描
     * @param canvas
     * @param frame
     */
    private void drawGridScanner(Canvas canvas,Rect frame){
        int stroke = 2;
        paint.setStrokeWidth(stroke);
        //计算Y轴开始位置
        int startY = gridHeight > 0 && scannerStart - frame.top > gridHeight ? scannerStart - gridHeight : frame.top;

        LinearGradient linearGradient = new LinearGradient(frame.left + frame.width()/2, startY, frame.left + frame.width()/2, scannerStart, new int[]{shadeColor(laserColor), laserColor}, new float[]{0,1f}, LinearGradient.TileMode.CLAMP);
        //给画笔设置着色器
        paint.setShader(linearGradient);

        float wUnit = frame.width() * 1.0f/ gridColumn;
        float hUnit = wUnit;
        //遍历绘制网格纵线
        for (int i = 1; i < gridColumn; i++) {
            canvas.drawLine(frame.left + i * wUnit, startY,frame.left + i * wUnit, scannerStart,paint);
        }

        int height = gridHeight > 0 && scannerStart - frame.top > gridHeight ? gridHeight : scannerStart - frame.top;

        //遍历绘制网格横线
        for (int i = 0; i <= height/hUnit; i++) {
            canvas.drawLine(frame.left, scannerStart - i * hUnit,frame.right, scannerStart - i * hUnit,paint);
        }

        if(scannerStart<scannerEnd){
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }

    }

    /**
     * 处理颜色模糊
     * @param color
     * @return
     */
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "01"+hax.substring(2);
        return Integer.valueOf(result, 16);
    }

    /**
     * 绘制扫描区边框
     * @param canvas
     * @param frame
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.top + frameLineWidth, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + frameLineWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - frameLineWidth, frame.top, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - frameLineWidth, frame.right, frame.bottom, paint);
    }

    /**
     * 绘制模糊区域
     * @param canvas
     * @param frame
     * @param width
     * @param height
     */
    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
        canvas.drawRect(0, frame.bottom, width, height, paint);
    }

    /**
     * 绘制扫码结果点
     * @param canvas
     * @param frame
     */
    private void drawResultPoint(Canvas canvas,Rect frame){

        if(!isShowResultPoint){
            return;
        }

        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;

        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle( point.getX(),point.getY(), radius, paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle( point.getX(),point.getY(), radius, paint);
                }
            }
        }
    }

    public void drawViewfinder() {
        invalidate();
    }

    public boolean isShowResultPoint() {
        return isShowResultPoint;
    }

    public void setLaserStyle(LaserStyle laserStyle) {
        this.laserStyle = laserStyle;
    }

    /**
     * 设置显示结果点
     * @param showResultPoint 是否显示结果点
     */
    public void setShowResultPoint(boolean showResultPoint) {
        isShowResultPoint = showResultPoint;
    }


    public void addPossibleResultPoint(ResultPoint point) {
        if(isShowResultPoint){
            List<ResultPoint> points = possibleResultPoints;
            synchronized (points) {
                points.add(point);
                int size = points.size();
                if (size > MAX_RESULT_POINTS) {
                    // trim it
                    points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
                }
            }
        }

    }



}