package com.blueberry.media;

/**
 * Created by blueberry on 1/5/2017.
 */

public class Yuv420Util {
    /**
     * Nv21:
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * VUVU
     * VUVU
     * VUVU
     * VUVU
     * <p>
     * I420:
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * YYYYYYYY
     * UUUU
     * UUUU
     * VVVV
     * VVVV
     *
     * @param data
     * @param dstData
     * @param w
     * @param h
     */
    public static void Nv21ToI420(byte[] data, byte[] dstData, int w, int h) {

        int size = w * h;
        // Y
        System.arraycopy(data, 0, dstData, 0, size);
        for (int i = 0; i < size / 4; i++) {
            dstData[size + i] = data[size + i * 2 + 1]; //U
            dstData[size + size / 4 + i] = data[size + i * 2]; //V
        }
    }

    public static void Nv21ToYuv420SP(byte[] data, byte[] dstData, int w, int h) {
        int size = w * h;
        // Y
        System.arraycopy(data, 0, dstData, 0, size);

        for (int i = 0; i < size / 4; i++) {
            dstData[size + i * 2] = data[size + i * 2 + 1]; //U
            dstData[size + i * 2 + 1] = data[size + i * 2 ]; //V
        }
    }
    //针对华为高版本颜色对换的情况下，做U和V对换
    public static void Nv21ToYuv420SPHigher(byte[] data, byte[] dstData, int w, int h) {
        int size = w * h;
        // Y
        System.arraycopy(data, 0, dstData, 0, size);

        for (int i = 0; i < size / 4; i++) {
            dstData[size + i * 2 + 1] = data[size + i * 2 ]; //U
            dstData[size + i * 2 ] = data[size + i * 2 + 1]; //V
        }
    }
    //旋转180度
    public static byte[] rotateYUV420Degree180(byte[] data, int w, int h) {
        int imgSize = w * h;
        int len = imgSize * 3 / 2;//yuv数组长度是图片尺寸的1.5倍
        byte[] yuv = new byte[len];
        int i = 0;
        int count = 0;
        //y
        for (i = imgSize - 1; i >= 0; i--) {
            yuv[count++] = data[i];
        }
        //u,v
        for (i = len - 1; i >= imgSize; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }
    /*** 视频顺时针旋转90* */
    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth,
                                        int imageHeight) {

        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                        + (x - 1)];
                i--;
            }
        }
        return yuv;
    }
    /*** 视频逆时针旋转90* */
    public static void YUV420spRotateNegative90(byte[] dst, byte[] src, int srcWidth, int height)
    {
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if (srcWidth != nWidth || height != nHeight) {
            nWidth = srcWidth;
            nHeight = height;
            wh = srcWidth * height;
            uvHeight = height / 2;
        }
        // 旋转Y
        int k = 0;
        for (int i = 0; i < srcWidth; i++) {
            int nPos = srcWidth - 1;
            for (int j = 0; j < height; j++) {
                dst[k] = src[nPos - i];
                k++;
                nPos += srcWidth;
            }
        }
        for (int i = 0; i < srcWidth; i += 2) {
            int nPos = wh + srcWidth - 1;
            for (int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos - i - 1];
                dst[k + 1] = src[nPos - i];
                k += 2;
                nPos += srcWidth;
            }
        }
        return;
    }
}
