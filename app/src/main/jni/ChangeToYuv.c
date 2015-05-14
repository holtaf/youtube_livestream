#include <string.h>
#include <jni.h>

JNIEXPORT void JNICALL Java_com_holtaf_testandroidapplication_NativeBridge_rgbToYuv(
		JNIEnv * env, jobject cls, jobject rgbaBuffer, jbyteArray yuvData, int width, int height) {

		jbyte * rgbaBytes = (*env)->GetDirectBufferAddress(env, rgbaBuffer);
		jbyte * yuvBytes = (*env)->GetByteArrayElements(env, yuvData, 0);

		int frameSize = width * height;

		int yIndex = 0;
		int uvIndex = frameSize;

		int R, G, B, Y, U, V;
		int index = 0;
		int i, j;
		for (j = 0; j < height; j++) {
			for (i = 0; i < width; i++) {
				R = rgbaBytes[(j * width + i) * 4 + 3];
				G = rgbaBytes[(j * width + i) * 4 + 2];
				B = rgbaBytes[(j * width + i) * 4 + 1];

				// well known RGB to YUV algorithm
				Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
				U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
				V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

				// NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
				//    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
				//    pixel AND every other scanline.
				yuvBytes[yIndex++] = (jbyte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
				if (j % 2 == 0 && index % 2 == 0) {
					yuvBytes[uvIndex++] = (jbyte)((V<0) ? 0 : ((V > 255) ? 255 : V));
					yuvBytes[uvIndex++] = (jbyte)((U<0) ? 0 : ((U > 255) ? 255 : U));
				}

				index ++;
			}
		}

		(*env)->ReleaseByteArrayElements(env, yuvData, yuvBytes, 0);
}