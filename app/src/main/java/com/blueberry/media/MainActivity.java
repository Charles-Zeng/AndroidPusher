package com.blueberry.media;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;
import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_COLOR_FORMAT;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;
import static android.media.MediaFormat.KEY_MAX_INPUT_SIZE;

// 视频角度问题：
//http://blog.csdn.net/veilling/article/details/52421930
//
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback2 {

    static final int NAL_SLICE = 1;
    static final int NAL_SLICE_DPA = 2;
    static final int NAL_SLICE_DPB = 3;
    static final int NAL_SLICE_DPC = 4;
    static final int NAL_SLICE_IDR = 5;
    static final int NAL_SEI = 6;
    static final int NAL_SPS = 7;
    static final int NAL_PPS = 8;
    static final int NAL_AUD = 9;
    static final int NAL_FILLER = 12;
    private static final String TAG = "MainActivity";
    private TextView StatusView;
    private SurfaceView mSurfaceView;
    private Button btnToggle;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Camera.Size previewSize;
    private long presentationTimeUs;
    private MediaCodec vencoder;
    private Thread recordThread;
    private boolean aLoop;
    private AudioRecord mAudioRecord;
    private byte[] aBuffer;
    private MediaCodec aencoder;
    private int aSampleRate;
    private int aChanelCount;
    private int colorFormat;
    private MediaCodec.BufferInfo aBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private boolean isPublished = false;
    private RtmpPublisher mRtmpPublisher = new RtmpPublisher();
    ////////////////////以下获取摄像头权限////////////
    private static final int REQUEST_CODE = 0; // 请求码
    ///////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SysApplication.getInstance().addActivity(this);
        Log.i(TAG, "onCreate: ");
        initView();
    }

    private void initView() {
        btnToggle = (Button) findViewById(R.id.btn_toggle);
        StatusView = (TextView) findViewById(R.id.statusView);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检查是否授予了摄像头的权限
                int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
                //如果拥有该权限,permission值此时为PackageManager.PERMISSION_GRANTED(granted翻译过来就是准许)
                //如果没有这个权限,permission值为PackageManager.PERMISSION_DENIED(denied翻译过来就是拒签)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    //没有授予摄像头的权限,那么就去动态地向用户申请该权限
                    requestCameraPermission();
                } else {
                    //用户已经授权了该权限就直接启动摄像头
                    switchPublish();
                }
            }
        });
        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        //接收stopSecTime值
        int stopSecTime = bundle.getInt("stopSec");
        Log.i(TAG, "initView: " + stopSecTime);
        handler.postDelayed(runnable, stopSecTime);//StopSecond秒后执行一次runnable.
    }
    public void switchPublish() {
        if (isPublished) {
            stop();
        } else {
            start();
        }
        btnToggle.setText(isPublished ? "停止" : "开始");
    }

    private void start() {
        Log.i(TAG, "开始采集");
        //初始化
        String testStr = "rtmp://";
        testStr += GlobalContextValue.VideoServiceIP;
        testStr += ":1935/hls/";
        testStr += GlobalContextValue.ServiceName;
        int ret = mRtmpPublisher.init(testStr, GlobalContextValue.width, GlobalContextValue.height, 5);

        if (ret < 0) {
            Log.e(TAG, "连接失败");
            return;
        }

        //isPublished = true;
        initAudioDevice();

        try {
            vencoder = initVideoEncoder();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("video encoder init fail");
        }

        try {
            aencoder = initAudioEncoder();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("audio encoder init fail");
        }

        //开启录音
        aLoop = true;
        recordThread = new Thread(fetchAudioRunnable());

        presentationTimeUs = new Date().getTime() * 1000;
        mAudioRecord.startRecording();
        recordThread.start();
        if (aencoder != null) {
            aencoder.start();
        }
        if (vencoder != null) {
            vencoder.start();
        }
        isPublished = true;
        StatusView.setTextColor(this.getResources().getColor(R.color.colorAccent));
        StatusView.setText("正在推流中。。。");
        btnToggle.setText(isPublished ? "停止" : "开始");
        String clientStatus = "";
        try {
            clientStatus = BuildClientPushStatus("started");
        }catch (JSONException e)
        {
            e.printStackTrace();
        }
        //客户端停止推流告诉服务器开始推送状态
        SessionManager.getInstance().writeToServer(clientStatus);
        Log.i(TAG, "started: 开始推送时告诉服务器客户端推送的状态" + clientStatus);
    }

    private void stop() {
        Log.i(TAG, "停止采集");
        StatusView.setText("停止推流中。。。");
        isPublished = false;
        mRtmpPublisher.stop();
        aLoop = false;
        if (recordThread != null) {
            recordThread.interrupt();
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        vencoder.stop();
        vencoder.release();
        aencoder.stop();
        aencoder.release();
        StatusView.setTextColor(this.getResources().getColor(R.color.colorAccent));
        btnToggle.setText(isPublished ? "停止" : "开始");
        //关闭定时器，只需执行一次
        handler.removeCallbacks(runnable);
        String clientStatus = "";
        try {
            clientStatus = BuildClientPushStatus("stoped");
        }catch (JSONException e)
        {
            e.printStackTrace();
        }
        //客户端停止推流告诉服务器已经停止状态
        SessionManager.getInstance().writeToServer(clientStatus);
        Log.i(TAG, "stoped: 停止后告诉服务器客户端推送的状态" + clientStatus);
    }


    private Runnable fetchAudioRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                fetchPcmFromDevice();
            }
        };
    }

    private void fetchPcmFromDevice() {
        Log.d(TAG, "录音线程开始");
        while (aLoop && mAudioRecord != null && !Thread.interrupted()) {
            int size = mAudioRecord.read(aBuffer, 0, aBuffer.length);
            if (size < 0) {
                Log.i(TAG, "audio ignore ,no data to read");
                break;
            }
            if (aLoop) {
                byte[] audio = new byte[size];
                System.arraycopy(aBuffer, 0, audio, 0, size);
                onGetPcmFrame(audio);
            }
        }
    }

    private void initAudioDevice() {
        int[] sampleRates = {44100, 22050, 16000, 11025};
        for (int sampleRate :
                sampleRates) {
            //编码制式
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            // stereo 立体声，
            int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
            int buffsize = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
                    audioFormat, buffsize);
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                continue;
            }
            aSampleRate = sampleRate;
            aChanelCount = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2 : 1;
            aBuffer = new byte[Math.min(4096, buffsize)];
        }
    }


    private MediaCodec initAudioEncoder() throws IOException {
        MediaCodec aencoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                aSampleRate, aChanelCount);
        format.setInteger(KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(KEY_BIT_RATE, 1000 * 30);
        aencoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return aencoder;
    }

    private MediaCodec initVideoEncoder() throws IOException {
        // 初始化
        MediaCodecInfo mediaCodecInfo = getMediaCodecInfoByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        colorFormat = getColorFormat(mediaCodecInfo);
        MediaCodec vencoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                GlobalContextValue.width, GlobalContextValue.height);
        format.setInteger(KEY_MAX_INPUT_SIZE, 0);
        format.setInteger(KEY_BIT_RATE, 700 * 1000);
        //匹配华为手机和荣耀手机的厂商名字
        if(GlobalContextValue.DeviceBrand.equalsIgnoreCase("huawei")
                ||    GlobalContextValue.DeviceBrand.equalsIgnoreCase("honor"))
        {
            format.setInteger(KEY_COLOR_FORMAT, 21);
            Log.i(KEY_COLOR_FORMAT, String.format("KEY_COLOR_FORMAT DeviceBrand=%s", GlobalContextValue.DeviceBrand));
        }else
        {
            format.setInteger(KEY_COLOR_FORMAT, colorFormat);
            Log.i(KEY_COLOR_FORMAT, String.format("KEY_COLOR_FORMAT DeviceBrand=%s", GlobalContextValue.DeviceBrand));
        }
        format.setInteger(KEY_FRAME_RATE, 20);
        format.setInteger(KEY_I_FRAME_INTERVAL, 5);
        vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        return vencoder;
    }


    public static MediaCodecInfo getMediaCodecInfoByType(String mimeType) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public static int getColorFormat(MediaCodecInfo mediaCodecInfo) {
        int matchedForamt = 0;
        MediaCodecInfo.CodecCapabilities codecCapabilities =
                mediaCodecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        for (int i = 0; i < codecCapabilities.colorFormats.length; i++) {
            int format = codecCapabilities.colorFormats[i];
            if (format >= codecCapabilities.COLOR_FormatYUV420Planar &&
                    format <= codecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
                    ) {
                if (format >= matchedForamt) {
                    matchedForamt = format;
                }
            }
        }

        return matchedForamt;
    }


    private void initCamera() {
        onPause();
        openCamera();
        setCameraParameters();
        setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(getPreviewCallback());
        mCamera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
        mCamera.startPreview();
    }

    private int calculateFrameSize(int format) {
        return GlobalContextValue.width * GlobalContextValue.height * ImageFormat.getBitsPerPixel(format) / 8;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
            Log.i(TAG, "setCameraDisplayOrientation: " + result);
        }
        //20180105 因为摄像头摄像需要调整
        camera.setDisplayOrientation(result);
    }

    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size size : supportedPreviewSizes
                ) {
            if (size.width >= 320 && size.width <= 720) {
                previewSize = size;
                Log.i(TAG, String.format("find preview size width=%d,height=%d", previewSize.width,
                        previewSize.height));
                break;
            }
        }

        int[] destRange = {25 * 1000, 45 * 1000};
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        for (int[] range : supportedPreviewFpsRange
                ) {
            if (range[PREVIEW_FPS_MIN_INDEX] <= 45 * 1000 && range[PREVIEW_FPS_MAX_INDEX] >= 25 * 1000) {
                destRange = range;
                Log.d(TAG, String.format("find fps range :%s", Arrays.toString(destRange)));
                break;
            }
        }

        parameters.setPreviewSize(GlobalContextValue.width, GlobalContextValue.height);
        parameters.setPreviewFpsRange(destRange[PREVIEW_FPS_MIN_INDEX],
                destRange[PREVIEW_FPS_MAX_INDEX]);
        parameters.setFocusMode(FOCUS_MODE_AUTO);
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setRotation(onOrientationChanged(0));
        mCamera.setParameters(parameters);
    }

    public int onOrientationChanged(int orientation) {

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        orientation = (orientation + 45) / 90 * 90;
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }
        return rotation;
    }

    private void openCamera() {
        if (mCamera == null) {
            try {
                    mCamera = Camera.open();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "打开摄像头失败", Toast.LENGTH_SHORT).show();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                throw new RuntimeException("打开摄像头失败", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isPublished) {
            stop();
        }

        onPause();
        initCamera();
        DataSource.getInstance().setActivity(this);
        start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initCamera();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public Camera.PreviewCallback getPreviewCallback() {
        return new Camera.PreviewCallback() {
            byte[] dstByte = new byte[calculateFrameSize(ImageFormat.NV21)];
            //byte[] dstByteSend = new byte[calculateFrameSize(ImageFormat.NV21)];
            //byte[] dstByteSendDegree = new byte[calculateFrameSize(ImageFormat.NV21)];
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (data != null) {
                    if (isPublished) {
                        // data 是Nv21
                        if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                            Yuv420Util.Nv21ToYuv420SP(data, dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            //dstByteSend = Yuv420Util.rotateYUV420Degree90(dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            //Yuv420Util.YUV420spRotateNegative90(dstByteSendDegree, dstByteSend,GlobalContextValue.width, GlobalContextValue.height);
                            //Log.d(TAG, "colorFormat: COLOR_FormatYUV420SemiPlanar");
                            //Log.d(TAG, String.format("colorFormatNv21ToYuv420SP-1=%s", colorFormat));
                        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                            Yuv420Util.Nv21ToI420(data, dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            //Log.d(TAG, "colorFormat: COLOR_FormatYUV420Planar");
                            Log.d(TAG, String.format("colorFormatNv21ToI420-2=%s", colorFormat));
                        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) {
                            // Yuv420_888
                            Log.d(TAG, String.format("colorFormat=%s", colorFormat));
                        } else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
                            //http://blog.csdn.net/jumper511/article/details/21719313
                            //这样处理的话颜色核能会有些失真。
                            Yuv420Util.Nv21ToYuv420SP(data, dstByte, GlobalContextValue.height, GlobalContextValue.width);
                            Log.d(TAG, String.format("colorFormatNv21ToYuv420SP-3=%s", colorFormat));
                            //Log.d(TAG, "colorFormat: COLOR_FormatYUV420PackedPlanar");
                        }  else if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar){
                            //华为colorFormat=39
                            Yuv420Util.Nv21ToYuv420SP(data, dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            //dstByteSend = Yuv420Util.rotateYUV420Degree90(dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            //Yuv420Util.YUV420spRotateNegative90(dstByteSendDegree, dstByteSend,GlobalContextValue.width, GlobalContextValue.height);
                            //Yuv420Util.rotateYUV420Degree90(dstByte, dstByteSend, GlobalContextValue.width, GlobalContextValue.height);
                            Log.d(TAG, String.format("colorFormatNv21ToYuv420SP-4=%s", colorFormat));
                        }
                        else {
                            //其他颜色，直接拷贝，不用转华为高版本因为U和V顺序替换了，所以交换下U和V顺序
                            Yuv420Util.Nv21ToYuv420SPHigher(data, dstByte, GlobalContextValue.width, GlobalContextValue.height);
                            Log.d(TAG, String.format("colorFormatNv21ToYuv420SP-5=%s", colorFormat));
                            //System.arraycopy(data, 0, dstByte, 0, data.length);
                        }
                        /*if (90 == onOrientationChanged(0))
                        {
                            onGetVideoFrame(dstByteSend);
                        }else
                        {
                            onGetVideoFrame(dstByteSendDegree);
                        }*/
                        onGetVideoFrame(dstByte);
                    }
                    camera.addCallbackBuffer(data);
                } else {
                    camera.addCallbackBuffer(new byte[calculateFrameSize(ImageFormat.NV21)]);
                }

            }
        };
    }

    private void onGetVideoFrame(byte[] dstByte) {
        ByteBuffer[] inputBuffers = vencoder.getInputBuffers();
        ByteBuffer[] outputBuffers = vencoder.getOutputBuffers();

        int inputBufferId = vencoder.dequeueInputBuffer(-1);
        if (inputBufferId >= 0) {
            // fill inputBuffers[inputBufferId] with valid data
            ByteBuffer bb = inputBuffers[inputBufferId];
            bb.clear();
            bb.put(dstByte, 0, dstByte.length);
            long pts = new Date().getTime() * 1000 - presentationTimeUs;
            vencoder.queueInputBuffer(inputBufferId, 0, dstByte.length, pts, 0);
        }

        for (; ; ) {
            int outputBufferId = vencoder.dequeueOutputBuffer(vBufferInfo, 0);
            if (outputBufferId >= 0) {
                // outputBuffers[outputBufferId] is ready to be processed or rendered.
                ByteBuffer bb = outputBuffers[outputBufferId];
                onEncodedAvcFrame(bb, vBufferInfo);
                vencoder.releaseOutputBuffer(outputBufferId, false);
            }
            if (outputBufferId < 0) {
                break;
            }
        }
    }


    private void onGetPcmFrame(byte[] data) {
        ByteBuffer[] inputBuffers = aencoder.getInputBuffers();
        ByteBuffer[] outputBuffers = aencoder.getOutputBuffers();
        int inputBufferId = aencoder.dequeueInputBuffer(-1);
        if (inputBufferId >= 0) {
            ByteBuffer bb = inputBuffers[inputBufferId];
            bb.clear();
            bb.put(data, 0, data.length);
            long pts = new Date().getTime() * 1000 - presentationTimeUs;
            aencoder.queueInputBuffer(inputBufferId, 0, data.length, pts, 0);
        }

        for (; ; ) {
            int outputBufferId = aencoder.dequeueOutputBuffer(aBufferInfo, 0);
            if (outputBufferId >= 0) {
                // outputBuffers[outputBufferId] is ready to be processed or rendered.
                ByteBuffer bb = outputBuffers[outputBufferId];
                onEncodeAacFrame(bb, aBufferInfo);
                aencoder.releaseOutputBuffer(outputBufferId, false);
            }
            if (outputBufferId < 0) {
                break;
            }
        }
    }

    private void onEncodedAvcFrame(ByteBuffer bb, MediaCodec.BufferInfo vBufferInfo) {
        int offset = 4;
        //判断帧的类型
        if (bb.get(2) == 0x01) {
            offset = 3;
        }

        int type = bb.get(offset) & 0x1f;
        if (type == NAL_SPS) {
            //[0, 0, 0, 1, 103, 66, -64, 13, -38, 5, -126, 90, 1, -31, 16, -115, 64, 0, 0, 0, 1, 104, -50, 6, -30]
            //打印发现这里将 SPS帧和 PPS帧合在了一起发送
            // SPS为 [4，len-8]
            // PPS为后4个字节
            //so .
            byte[] pps = new byte[4];
            byte[] sps = new byte[vBufferInfo.size - 12];
            bb.getInt();// 抛弃 0,0,0,1
            bb.get(sps, 0, sps.length);
            bb.getInt();
            bb.get(pps, 0, pps.length);
            Log.d(TAG, "解析得到 sps:" + Arrays.toString(sps) + ",PPS=" + Arrays.toString(pps));
            mRtmpPublisher.sendSpsAndPps(sps, sps.length, pps, pps.length,
                    vBufferInfo.presentationTimeUs / 1000);
        } else {
            byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            mRtmpPublisher.sendVideoData(bytes, bytes.length,
                    vBufferInfo.presentationTimeUs / 1000);
        }

    }

    private void onEncodeAacFrame(ByteBuffer bb, MediaCodec.BufferInfo aBufferInfo) {

        // 1.界定符 FF F1
        // 2.加上界定符的前7个字节是帧描述信息
        // 3.AudioDecoderSpecificInfo 长度为2个字节如果是44100 改值为0x1210
        //http://blog.csdn.net/avsuper/article/details/24661533
        //http://www.tuicool.com/articles/aYvmua
        if (aBufferInfo.size == 2) {
            // 我打印发现，这里应该已经是吧关键帧计算好了，所以我们直接发送
            byte[] bytes = new byte[2];
            bb.get(bytes);
            mRtmpPublisher.sendAacSpec(bytes, 2);

        } else {
            byte[] bytes = new byte[aBufferInfo.size];
            bb.get(bytes);
            mRtmpPublisher.sendAacData(bytes, bytes.length, aBufferInfo.presentationTimeUs / 1000);
        }

    }
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("确认退出吗？")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        //关闭整个程序
                        SysApplication.getInstance().exit();
                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作
                    }
                }).show();
    }
    private void requestCameraPermission() {
        //requestPermissions弹出一个系统的Dialog,说明当前要申请什么权限
        //requestPermissions需要一个String数组和一个请求码用于回调
        //String数组可以填写多个你需要申请的权限
        //需要一个请求码用于处理申请权限的结果
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.INTERNET,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //依据请求码来做处理
        if (requestCode == REQUEST_CODE) {
            //检查申请的权限是否标准了
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //..............
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("你已经拒绝了授权摄像头的权限,可以到手机设置->应用管理->permission->权限里面来进行手动授权才能开启摄像头的功能。\n" +
                                "也可以点击ok按键直接跳到应用权限设置的页面" )
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switchPublish();
                            }
                        })
                        .setNegativeButton("Cancel", null);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    //实现X分钟如果没收到服务器的停止命令，就开始停止发送视频流
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //定时5分钟调用stop方法
            stop();
        }
    };
    //构建客户端推流状态包
    public String BuildClientPushStatus(String Status) throws JSONException
    {
        JSONObject PushStatusPacket = new JSONObject();
        PushStatusPacket.put("Type","StatusPush");
        if (Status.equals("started"))
        {
            PushStatusPacket.put("Status","Started");
        }
        else
        {
            PushStatusPacket.put("Status","Stoped");
        }
        System.out.print(PushStatusPacket);
        return PushStatusPacket.toString();
    }
}
