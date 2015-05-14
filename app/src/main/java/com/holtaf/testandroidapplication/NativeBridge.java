package com.holtaf.testandroidapplication;

import java.nio.ByteBuffer;

public class NativeBridge {
	static {
		System.loadLibrary("convertyuv");
	}

	public static native void rgbToYuv(ByteBuffer rgbaData, byte[] yuvData, int width, int height);
}
