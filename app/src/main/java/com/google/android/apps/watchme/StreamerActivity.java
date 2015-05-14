/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.apps.watchme.util.YouTubeApi;
import com.holtaf.testandroidapplication.NativeBridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         StreamerActivity class which previews the camera and streams via StreamerService.
 */
public class StreamerActivity extends Activity {
	private static final String TAG = StreamerActivity.class.getName();
	private static final int REQUEST_CODE = 100;
	private MediaProjectionManager projectionManager;
	private ImageReader imageReader;
	private Handler handler;
	private int width = 1152;
	private int height = 1600;

	private byte[] yuvData;
//	private byte[] rgbData;

	private String rtmpUrl;
	private String broadcastId;

	private VirtualDisplay virtualDisplay;

	private VideoStreamingConnection videoStreamingConnection;

	private MediaProjection mediaProjection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.streamer);

		projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

		broadcastId = getIntent().getStringExtra(YouTubeApi.BROADCAST_ID_KEY);

		rtmpUrl = getIntent().getStringExtra(YouTubeApi.RTMP_URL_KEY);

		imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
		imageReader.setOnImageAvailableListener(new ImageAvailableListener(), handler);

		startProjection();

		videoStreamingConnection = new VideoStreamingConnection();

		new Thread() {
			@Override
			public void run() {
				Looper.prepare();
				handler = new Handler();
				Looper.loop();
			}
		}.start();
	}

	private void startStreaming() {
		videoStreamingConnection.open(rtmpUrl, width, height);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		videoStreamingConnection.close();
		mediaProjection.stop();
	}

	public void endEvent(View view) {
		Intent data = new Intent();
		data.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);
		if (getParent() == null) {
			setResult(Activity.RESULT_OK, data);
		} else {
			getParent().setResult(Activity.RESULT_OK, data);
		}
		finish();
	}

	private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
		@Override
		public void onImageAvailable(ImageReader reader) {
			Image image = reader.acquireLatestImage();

			if (image != null) {
				Log.d("frame", "Frame acquired");

				ByteBuffer buffer = image.getPlanes()[0].getBuffer();

				if (yuvData == null) {
					yuvData = new byte[width * height * 3];
				}
//
//				if (rgbData == null) {
//					rgbData = new byte[width * height * 4];
//				}
//
//				buffer.get(rgbData);

				NativeBridge.rgbToYuv(buffer, yuvData, width, height);
//				encodeYUV420SP(yuvData, rgbData, width, height);

				videoStreamingConnection.sendVideoFrame(yuvData);

				image.close();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE) {
			mediaProjection = projectionManager.getMediaProjection(resultCode, data);
			virtualDisplay = mediaProjection.createVirtualDisplay("asd", width, height, getResources().getDisplayMetrics().densityDpi,
					DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.getSurface(), null, handler);

			startStreaming();
		}
	}


	private void startProjection() {
		startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
	}

	private void encodeYUV420SP(byte[] yuv420sp, byte[] rgbaData, int width, int height) {
		final int frameSize = width * height;

		int yIndex = 0;
		int uvIndex = frameSize;

		int R, G, B, Y, U, V;
		int index = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				R = rgbaData[(j * width + i) * 4];
				G = rgbaData[(j * width + i) * 4 + 1];
				B = rgbaData[(j * width + i) * 4 + 2];

				// well known RGB to YUV algorithm
				Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
				U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
				V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

				// NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
				//    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
				//    pixel AND every other scanline.
				yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
				if (j % 2 == 0 && index % 2 == 0) {
					yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
					yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
				}

				index ++;
			}
		}
	}
}
