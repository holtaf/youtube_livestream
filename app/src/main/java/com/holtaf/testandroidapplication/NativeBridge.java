package com.holtaf.testandroidapplication;

import java.nio.ByteBuffer;

public class NativeBridge {
	static {
		System.loadLibrary("convertyuv");
	}

	public static native void rgbToYuv(byte[] rgbData, byte[] yuvData, int width, int height);
}
