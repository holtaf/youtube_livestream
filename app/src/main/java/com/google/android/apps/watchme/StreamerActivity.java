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
	private byte[] rgbData;

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

				if (rgbData == null) {
					rgbData = new byte[width * height * 4];
				}

				buffer.get(rgbData);

				NativeBridge.rgbToYuv(rgbData, yuvData, width, height);

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
					DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, imageReader.getSurface(), null, null);

			startStreaming();
		}
	}


	private void startProjection() {
		startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
	}
}
