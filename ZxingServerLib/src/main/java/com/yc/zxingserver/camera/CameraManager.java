package com.yc.zxingserver.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import androidx.annotation.FloatRange;
import android.view.SurfaceHolder;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.yc.zxingserver.camera.open.OpenCamera;
import com.yc.zxingserver.camera.open.OpenCameraInterface;
import com.yc.toolutils.AppLogUtils;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraManager {

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    private static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080

    private final Context context;
    private final CameraConfigurationManager configManager;
    private OpenCamera camera;
    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private boolean isFullScreenScan;

    private float framingRectRatio;
    private int framingRectVerticalOffset;
    private int framingRectHorizontalOffset;

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;

    private OnTorchListener onTorchListener;
    private OnSensorListener onSensorListener;

    private boolean isTorch;

    public CameraManager(Context context) {
        this.context = context.getApplicationContext();
        this.configManager = new CameraConfigurationManager(context);
        previewCallback = new PreviewCallback(configManager);
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                requestedFramingRectWidth = 0;
                requestedFramingRectHeight = 0;
            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            AppLogUtils.w( "Camera rejected parameters. Setting only minimal safe-mode parameters");
            AppLogUtils.i( "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    AppLogUtils.w( "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);

    }

    public synchronized boolean isOpen() {
        return camera != null;
    }

    public OpenCamera getOpenCamera() {
        return camera;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public void closeDriver() {
        if (camera != null) {
            camera.getCamera().release();
            camera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
            framingRectInPreview = null;
        }
        isTorch = false;
        if(onTorchListener!=null){
            onTorchListener.onTorchChanged(false);
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public void startPreview() {
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }

    }

    public synchronized void setTorch(boolean newSetting) {
        OpenCamera theCamera = camera;
        if (theCamera != null && newSetting != configManager.getTorchState(theCamera.getCamera())) {
            boolean wasAutoFocusManager = autoFocusManager != null;
            if (wasAutoFocusManager) {
                autoFocusManager.stop();
                autoFocusManager = null;
            }
            this.isTorch = newSetting;
            configManager.setTorch(theCamera.getCamera(), newSetting);
            if (wasAutoFocusManager) {
                autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
                autoFocusManager.start();
            }

            if(onTorchListener!=null){
                onTorchListener.onTorchChanged(newSetting);
            }

        }

    }


    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point point = configManager.getCameraResolution();
            if (point == null) {
                // Called early, before init even finished
                return null;
            }

            int width = point.x;
            int height = point.y;

            if(isFullScreenScan){
                framingRect = new Rect(0,0,width,height);
            }else{
                int size = (int)(Math.min(width,height) * framingRectRatio);

                int leftOffset = (width - size) / 2 + framingRectHorizontalOffset;
                int topOffset = (height - size) / 2 + framingRectVerticalOffset;
                framingRect = new Rect(leftOffset, topOffset, leftOffset + size, topOffset + size);
            }

        }
        return framingRect;
    }


    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     *
     * @return {@link Rect} expressing barcode scan area in terms of the preview size
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

//            rect.left = rect.left * cameraResolution.x / screenResolution.x;
//            rect.right = rect.right * cameraResolution.x / screenResolution.x;
//            rect.top = rect.top * cameraResolution.y / screenResolution.y;
//            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;


            framingRectInPreview = rect;
        }

        return framingRectInPreview;
    }

    public void setFullScreenScan(boolean fullScreenScan) {
        isFullScreenScan = fullScreenScan;
    }

    public void setFramingRectRatio(@FloatRange(from = 0.0f ,to = 1.0f) float framingRectRatio) {
        this.framingRectRatio = framingRectRatio;
    }

    public void setFramingRectVerticalOffset(int framingRectVerticalOffset) {
        this.framingRectVerticalOffset = framingRectVerticalOffset;
    }

    public void setFramingRectHorizontalOffset(int framingRectHorizontalOffset) {
        this.framingRectHorizontalOffset = framingRectHorizontalOffset;
    }

    public Point getCameraResolution() {
        return configManager.getCameraResolution();
    }

    public Point getScreenResolution() {
        return configManager.getScreenResolution();
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Allows third party apps to specify the scanning rectangle dimensions, rather than determine
     * them automatically based on screen resolution.
     *
     * @param width The width in pixels to scan.
     * @param height The height in pixels to scan.
     */
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            AppLogUtils.d( "Calculated manual framing rect: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }

        if(isFullScreenScan){
            return new PlanarYUVLuminanceSource(data,width,height,0,0,width,height,false);
        }
        int size = (int)(Math.min(width,height) * framingRectRatio);
        int left = (width-size)/2 + framingRectHorizontalOffset;
        int top = (height-size)/2 + framingRectVerticalOffset;
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, left, top,
                size, size, false);
    }

    /**
     * 提供闪光灯监听
     * @param listener
     */
    public void setOnTorchListener(OnTorchListener listener){
        this.onTorchListener = listener;
    }

    /**
     * 传感器光线照度监听
     * @param listener
     */
    public void setOnSensorListener(OnSensorListener listener){
        this.onSensorListener = listener;
    }

    public void sensorChanged(boolean tooDark,float ambientLightLux){
        if(onSensorListener!=null){
            onSensorListener.onSensorChanged(isTorch,tooDark,ambientLightLux);
        }
    }

    public interface OnTorchListener{
        /**
         * 当闪光灯状态改变时触发
         * @param torch true表示开启、false表示关闭
         */
        void onTorchChanged(boolean torch);
    }


    /**
     * 传感器灯光亮度监听
     */
    public interface OnSensorListener{
        /**
         *
         * @param torch 闪光灯是否开启
         * @param tooDark  传感器检测到的光线亮度，是否太暗
         * @param ambientLightLux 光线照度
         */
        void onSensorChanged(boolean torch, boolean tooDark, float ambientLightLux);
    }


}