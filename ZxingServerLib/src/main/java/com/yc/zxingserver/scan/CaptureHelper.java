
package com.yc.zxingserver.scan;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import androidx.annotation.FloatRange;
import androidx.fragment.app.Fragment;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.yc.zxingserver.camera.CameraManager;
import com.yc.zxingserver.camera.FrontLightMode;
import com.yc.toolutils.AppLogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class CaptureHelper implements CaptureLifecycle,CaptureTouchEvent,CaptureManager, SurfaceHolder.Callback  {

    private Activity activity;

    private CaptureHandler captureHandler;
    private OnCaptureListener onCaptureListener;

    private CameraManager cameraManager;

    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;


    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private SurfaceHolder surfaceHolder;
    private View ivTorch;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,Object> decodeHints;
    private String characterSet;

    private boolean hasSurface;
    /**
     * 默认触控误差值
     */
    private static final int DEVIATION = 6;
    /**
     * 是否支持缩放（变焦），默认支持
     */
    private boolean isSupportZoom = true;
    private float oldDistance;

    /**
     * 是否支持自动缩放（变焦），默认支持
     */
    private boolean isSupportAutoZoom = true;

    /**
     * 是否支持识别颜色反转色的码，黑白颜色反转，默认不支持
     */
    private boolean isSupportLuminanceInvert = false;

    /**
     * 是否支持连扫，默认不支持
     */
    private boolean isContinuousScan = false;
    /**
     * 连扫时，是否自动重置预览和解码器，默认自动重置
     */
    private boolean isAutoRestartPreviewAndDecode = true;
    /**
     * 是否播放音效
     */
    private boolean isPlayBeep;
    /**
     * 是否震动
     */
    private boolean isVibrate;

    /**
     * 是否支持垂直的条形码
     */
    private boolean isSupportVerticalCode;

    /**
     * 是否返回扫码原图
     */
    private boolean isReturnBitmap;

    /**
     * 是否支持全屏扫码识别
     */
    private boolean isFullScreenScan;

    /**
     * 识别区域比例，范围建议在0.625 ~ 1.0之间，默认0.9
     */
    private float framingRectRatio = 0.9f;
    /**
     * 识别区域垂直方向偏移量
     */
    private int framingRectVerticalOffset;
    /**
     * 识别区域水平方向偏移量
     */
    private int framingRectHorizontalOffset;
    /**
     * 光线太暗，当光线亮度太暗，亮度低于此值时，显示手电筒按钮
     */
    private float tooDarkLux = AmbientLightManager.TOO_DARK_LUX;
    /**
     * 光线足够明亮，当光线亮度足够明亮，亮度高于此值时，隐藏手电筒按钮
     */
    private float brightEnoughLux = AmbientLightManager.BRIGHT_ENOUGH_LUX;
    /**
     * 扫码回调
     */
    private OnCaptureCallback onCaptureCallback;

    private boolean hasCameraFlash;

    /**
     * use {@link #CaptureHelper(Fragment, SurfaceView, ViewfinderView, View)}
     * @param fragment
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated
    public CaptureHelper(Fragment fragment, SurfaceView surfaceView, ViewfinderView viewfinderView){
        this(fragment,surfaceView,viewfinderView,null);
    }

    public CaptureHelper(Fragment fragment, SurfaceView surfaceView, ViewfinderView viewfinderView, View ivTorch){
        this(fragment.getActivity(),surfaceView,viewfinderView,ivTorch);
    }

    /**
     *  use {@link #CaptureHelper(Activity, SurfaceView, ViewfinderView, View)}
     * @param activity
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated
    public CaptureHelper(Activity activity,SurfaceView surfaceView,ViewfinderView viewfinderView){
        this(activity,surfaceView,viewfinderView,null);
    }

    /**
     *
     * @param activity
     * @param surfaceView
     * @param viewfinderView
     * @param ivTorch
     */
    public CaptureHelper(Activity activity,SurfaceView surfaceView,ViewfinderView viewfinderView,View ivTorch){
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.viewfinderView = viewfinderView;
        this.ivTorch = ivTorch;
    }


    @Override
    public void onCreate(){
        surfaceHolder = surfaceView.getHolder();
        hasSurface = false;
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
        ambientLightManager = new AmbientLightManager(activity);

        hasCameraFlash = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        initCameraManager();

        onCaptureListener = (result, barcode, scaleFactor) -> {
            inactivityTimer.onActivity();
            beepManager.playBeepSoundAndVibrate();
            onResult(result,barcode,scaleFactor);
        };
        //设置是否播放音效和震动
        beepManager.setPlayBeep(isPlayBeep);
        beepManager.setVibrate(isVibrate);

        //设置闪光灯的太暗时和足够亮时的照度值
        ambientLightManager.setTooDarkLux(tooDarkLux);
        ambientLightManager.setBrightEnoughLux(brightEnoughLux);

    }



    @Override
    public void onResume(){
        beepManager.updatePrefs();

        inactivityTimer.onResume();

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
        ambientLightManager.start(cameraManager);
    }


    @Override
    public void onPause(){
        if (captureHandler != null) {
            captureHandler.quitSynchronously();
            captureHandler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
        if(ivTorch != null && ivTorch.getVisibility() == View.VISIBLE){
            ivTorch.setSelected(false);
            ivTorch.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onDestroy(){
        inactivityTimer.shutdown();
    }

    /**
     * 支持缩放时，须在{@link Activity#onTouchEvent(MotionEvent)}调用此方法
     * @param event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(isSupportZoom && cameraManager.isOpen()){
            Camera camera = cameraManager.getOpenCamera().getCamera();
            if(camera ==null){
                return false;
            }
            if(event.getPointerCount() > 1) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {//多点触控
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDistance = calcFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newDistance = calcFingerSpacing(event);

                        if (newDistance > oldDistance + DEVIATION) {//
                            handleZoom(true, camera);
                        } else if (newDistance < oldDistance - DEVIATION) {
                            handleZoom(false, camera);
                        }
                        oldDistance = newDistance;
                        break;
                }

                return true;
            }
        }

        return false;
    }

    private void initCameraManager(){
        cameraManager = new CameraManager(activity);
        cameraManager.setFullScreenScan(isFullScreenScan);
        cameraManager.setFramingRectRatio(framingRectRatio);
        cameraManager.setFramingRectVerticalOffset(framingRectVerticalOffset);
        cameraManager.setFramingRectHorizontalOffset(framingRectHorizontalOffset);
        if(ivTorch !=null && hasCameraFlash){
            ivTorch.setOnClickListener(v -> {
                if(cameraManager!=null){
                    cameraManager.setTorch(!ivTorch.isSelected());
                }
            });
            cameraManager.setOnSensorListener((torch, tooDark, ambientLightLux) -> {
                if(tooDark){
                    if(ivTorch.getVisibility() != View.VISIBLE){
                        ivTorch.setVisibility(View.VISIBLE);
                    }
                }else if(!torch){
                    if(ivTorch.getVisibility() == View.VISIBLE){
                        ivTorch.setVisibility(View.INVISIBLE);
                    }
                }
            });
            cameraManager.setOnTorchListener(torch -> ivTorch.setSelected(torch));

        }
    }


    /**
     * 初始化Camera
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            AppLogUtils.w("initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (captureHandler == null) {
                captureHandler = new CaptureHandler(activity,viewfinderView,onCaptureListener, decodeFormats, decodeHints, characterSet, cameraManager);
                captureHandler.setSupportVerticalCode(isSupportVerticalCode);
                captureHandler.setReturnBitmap(isReturnBitmap);
                captureHandler.setSupportAutoZoom(isSupportAutoZoom);
                captureHandler.setSupportLuminanceInvert(isSupportLuminanceInvert);
            }
        } catch (IOException ioe) {
            AppLogUtils.w( ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            AppLogUtils.w( "Unexpected error initializing camera", e);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            AppLogUtils.w( "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     * 处理变焦缩放
     * @param isZoomIn
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            AppLogUtils.i( "zoom not supported");
        }
    }

    /**
     * 聚焦
     * @param event
     * @param camera
     */
    @Deprecated
    private void focusOnTouch(MotionEvent event,Camera camera) {

        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();

        Rect focusRect = calcTapArea(event.getRawX(), event.getRawY(), 1f,previewSize);
        Rect meteringRect = calcTapArea(event.getRawX(), event.getRawY(), 1.5f,previewSize);
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 600));
            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 600));
            parameters.setMeteringAreas(meteringAreas);
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus((success, camera1) -> {
            Camera.Parameters params1 = camera1.getParameters();
            params1.setFocusMode(currentFocusMode);
            camera1.setParameters(params1);
        });

    }


    /**
     * 计算两指间距离
     * @param event
     * @return
     */
    private float calcFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算对焦区域
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private Rect calcTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 200;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) ((x / previewSize.width) * 2000 - 1000);
        int centerY = (int) ((y / previewSize.height) * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
    }

    /**
     * 根据范围限定值
     * @param x
     * @param min 范围最小值
     * @param max 范围最大值
     * @return
     */
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    /**
     * 重新启动扫码和解码器
     */
    public void restartPreviewAndDecode(){
        if(captureHandler!=null){
            captureHandler.restartPreviewAndDecode();
        }
    }

    /**
     * 接收扫码结果
     * @param result
     * @param barcode
     * @param scaleFactor
     */
    public void onResult(Result result, Bitmap barcode, float scaleFactor){
        onResult(result);
    }

    /**';, mnb
     *
     * 接收扫码结果，想支持连扫时，可将{@link #continuousScan(boolean)}设置为{@code true}
     * 如果{@link #isContinuousScan}支持连扫，则默认重启扫码和解码器；当连扫逻辑太复杂时，
     * 请将{@link #autoRestartPreviewAndDecode(boolean)}设置为{@code false}，并手动调用{@link #restartPreviewAndDecode()}
     * @param result 扫码结果
     */
    public void onResult(Result result){
        final String text = result.getText();
        if(isContinuousScan){
            if(onCaptureCallback!=null){
                onCaptureCallback.onResultCallback(text);
            }
            if(isAutoRestartPreviewAndDecode){
                restartPreviewAndDecode();
            }
            return;
        }

        if(isPlayBeep && captureHandler != null){//如果播放音效，则稍微延迟一点，给予播放音效时间
            captureHandler.postDelayed(() -> {
                //如果设置了回调，并且onCallback返回为true，则表示拦截
                if(onCaptureCallback!=null && onCaptureCallback.onResultCallback(text)){
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra(Intents.Scan.RESULT,text);
                activity.setResult(Activity.RESULT_OK,intent);
                activity.finish();
            },100);
            return;
        }

        //如果设置了回调，并且onCallback返回为true，则表示拦截
        if(onCaptureCallback!=null && onCaptureCallback.onResultCallback(text)){
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Intents.Scan.RESULT,text);
        activity.setResult(Activity.RESULT_OK,intent);
        activity.finish();
    }


    /**
     * 设置是否连续扫码，如果想支持连续扫码，则将此方法返回{@code true}并重写{@link #onResult(Result)}
     */
    public CaptureHelper continuousScan(boolean isContinuousScan){
        this.isContinuousScan = isContinuousScan;
        return this;
    }


    /**
     * 设置是否自动重启扫码和解码器，当支持连扫时才起作用。
     * @return 默认返回 true
     */
    public CaptureHelper autoRestartPreviewAndDecode(boolean isAutoRestartPreviewAndDecode){
        this.isAutoRestartPreviewAndDecode = isAutoRestartPreviewAndDecode;
        return this;
    }


    /**
     * 设置是否播放音效
     * @return
     */
    public CaptureHelper playBeep(boolean playBeep){
        this.isPlayBeep = playBeep;
        if(beepManager!=null){
            beepManager.setPlayBeep(playBeep);
        }
        return this;
    }

    /**
     * 设置是否震动
     * @return
     */
    public CaptureHelper vibrate(boolean vibrate){
        this.isVibrate = vibrate;
        if(beepManager!=null){
            beepManager.setVibrate(vibrate);
        }
        return this;
    }


    /**
     * 设置是否支持缩放
     * @param supportZoom
     * @return
     */
    public CaptureHelper supportZoom(boolean supportZoom) {
        isSupportZoom = supportZoom;
        return this;
    }

    /**
     * 设置支持的解码一/二维码格式，默认常规的码都支持
     * @param decodeFormats  可参见{@link DecodeFormatManager}
     * @return
     */
    public CaptureHelper decodeFormats(Collection<BarcodeFormat> decodeFormats) {
        this.decodeFormats = decodeFormats;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param decodeHints
     * @return
     */
    public CaptureHelper decodeHints(Map<DecodeHintType,Object> decodeHints) {
        this.decodeHints = decodeHints;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param key {@link DecodeHintType}
     * @param value {@link }
     * @return
     */
    public CaptureHelper decodeHint(DecodeHintType key,Object value){
        if(decodeHints == null){
            decodeHints = new EnumMap<>(DecodeHintType.class);
        }
        decodeHints.put(key,value);
        return this;
    }

    /**
     *  设置解码时编码字符集
     * @param characterSet
     * @return
     */
    public CaptureHelper characterSet(String characterSet) {
        this.characterSet = characterSet;
        return this;
    }

    /**
     * 设置是否支持扫垂直的条码
     * @param supportVerticalCode 默认为false，想要增强扫条码识别度时可使用，相应的会增加性能消耗。
     * @return
     */
    public CaptureHelper supportVerticalCode(boolean supportVerticalCode) {
        this.isSupportVerticalCode = supportVerticalCode;
        if(captureHandler!=null){
            captureHandler.setSupportVerticalCode(isSupportVerticalCode);
        }
        return this;
    }

    /**
     * 设置闪光灯模式。当设置模式为：{@link FrontLightMode#AUTO}时，如果满意默认的照度值范围，
     * 可通过{@link #tooDarkLux(float)}和{@link #brightEnoughLux(float)}来自定义照度范围，
     * 控制自动触发开启和关闭闪光灯。
     * 当设置模式非{@link FrontLightMode#AUTO}时，传感器不会检测，则不使用手电筒
     *
     * @param mode 默认:{@link FrontLightMode#AUTO}
     * @return
     */
    public CaptureHelper frontLightMode(FrontLightMode mode) {
        FrontLightMode.put(activity,mode);
        if(ivTorch!=null && mode != FrontLightMode.AUTO){
            ivTorch.setVisibility(View.INVISIBLE);
        }
        return this;
    }

    /**
     * 设置光线太暗时，自动显示手电筒按钮
     * @param tooDarkLux  默认：{@link AmbientLightManager#TOO_DARK_LUX}
     * @return
     */
    public CaptureHelper tooDarkLux(float tooDarkLux) {
        this.tooDarkLux = tooDarkLux;
        if(ambientLightManager != null){
            ambientLightManager.setTooDarkLux(tooDarkLux);
        }
        return this;
    }

    /**
     * 设置光线足够明亮时，自动隐藏手电筒按钮
     * @param brightEnoughLux 默认：{@link AmbientLightManager#BRIGHT_ENOUGH_LUX}
     * @return
     */
    public CaptureHelper brightEnoughLux(float brightEnoughLux) {
        this.brightEnoughLux = brightEnoughLux;
        if(ambientLightManager != null){
            ambientLightManager.setTooDarkLux(tooDarkLux);
        }
        return this;
    }

    /**
     * 设置返回扫码原图
     * @param returnBitmap 默认为false，当返回true表示扫码就结果会返回扫码原图，相应的会增加性能消耗。
     * @return
     */
    public CaptureHelper returnBitmap(boolean returnBitmap) {
        isReturnBitmap = returnBitmap;
        if(captureHandler!=null){
            captureHandler.setReturnBitmap(isReturnBitmap);
        }
        return this;
    }


    /**
     * 设置是否支持自动缩放
     * @param supportAutoZoom
     * @return
     */
    public CaptureHelper supportAutoZoom(boolean supportAutoZoom) {
        isSupportAutoZoom = supportAutoZoom;
        if(captureHandler!=null){
            captureHandler.setSupportAutoZoom(isSupportAutoZoom);
        }
        return this;
    }

    /**
     * 是否支持识别反色码，黑白颜色反转
     * @param supportLuminanceInvert 默认为false，当返回true时表示支持，会增加识别率，但相应的也会增加性能消耗。
     * @return
     */
    public CaptureHelper supportLuminanceInvert(boolean supportLuminanceInvert) {
        isSupportLuminanceInvert = supportLuminanceInvert;
        if(captureHandler!=null){
            captureHandler.setSupportLuminanceInvert(isSupportLuminanceInvert);
        }
        return this;
    }

    /**
     * 设置是否支持全屏扫码识别
     * @param fullScreenScan 默认为false
     * @return
     */
    public CaptureHelper fullScreenScan(boolean fullScreenScan) {
        isFullScreenScan = fullScreenScan;
        if(cameraManager!=null){
            cameraManager.setFullScreenScan(isFullScreenScan);
        }
        return this;
    }

    /**
     * 设置识别区域比例，范围建议在0.625 ~ 1.0之间。非全屏识别时才有效
     * 0.625 即与默认推荐显示区域一致，1.0表示与宽度一致
     * @param framingRectRatio 默认0.9
     * @return
     */
    public CaptureHelper framingRectRatio(@FloatRange(from = 0.0f ,to = 1.0f) float framingRectRatio) {
        this.framingRectRatio = framingRectRatio;
        if(cameraManager!=null){
            cameraManager.setFramingRectRatio(framingRectRatio);
        }
        return this;
    }

    /**
     * 设置识别区域垂直方向偏移量，非全屏识别时才有效
     * @param framingRectVerticalOffset 默认0，表示不偏移
     * @return
     */
    public CaptureHelper framingRectVerticalOffset(int framingRectVerticalOffset) {
        this.framingRectVerticalOffset = framingRectVerticalOffset;
        if(cameraManager!=null){
            cameraManager.setFramingRectVerticalOffset(framingRectVerticalOffset);
        }
        return this;
    }

    /**
     * 设置识别区域水平方向偏移量，非全屏识别时才有效
     * @param framingRectHorizontalOffset 默认0，表示不偏移
     * @return
     */
    public CaptureHelper framingRectHorizontalOffset(int framingRectHorizontalOffset) {
        this.framingRectHorizontalOffset = framingRectHorizontalOffset;
        if(cameraManager!=null){
            cameraManager.setFramingRectHorizontalOffset(framingRectHorizontalOffset);
        }
        return this;
    }


    /**
     * 设置扫码回调
     * @param callback
     * @return
     */
    public CaptureHelper setOnCaptureCallback(OnCaptureCallback callback) {
        this.onCaptureCallback = callback;
        return this;
    }

    /**
     * {@link CameraManager}
     * @return {@link #cameraManager}
     */
    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /**
     * {@link BeepManager}
     * @return {@link #beepManager}
     */
    @Override
    public BeepManager getBeepManager() {
        return beepManager;
    }

    /**
     * {@link AmbientLightManager}
     * @return {@link #ambientLightManager}
     */
    @Override
    public AmbientLightManager getAmbientLightManager() {
        return ambientLightManager;
    }

    /**
     * {@link InactivityTimer}
     * @return {@link #inactivityTimer}
     */
    @Override
    public InactivityTimer getInactivityTimer() {
        return inactivityTimer;
    }
}