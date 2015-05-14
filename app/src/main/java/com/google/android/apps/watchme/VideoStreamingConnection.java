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

import android.util.Log;


public class VideoStreamingConnection {
	// CONSTANTS.
	private static final int AUDIO_SAMPLE_RATE = 44100;

	// Member variables.
	private VideoFrameGrabber videoFrameGrabber;
	private AudioFrameGrabber audioFrameGrabber;
	private boolean encoding;

	private final Object lock = new Object();

	public void open(String url, int width, int height) {
		Log.d(MainActivity.APP_NAME, "open");

        videoFrameGrabber = new VideoFrameGrabber();
        videoFrameGrabber.setFrameCallback(new VideoFrameGrabber.FrameCallback() {
			@Override
			public void handleFrame(byte[] yuv_image) {
				if (encoding) {
					synchronized (lock) {
						int encoded_size = Ffmpeg.encodeVideoFrame(yuv_image);

						// Logging.Verbose("Encoded video! Size = " + encoded_size);
					}
				}
			}
		});

        audioFrameGrabber = new AudioFrameGrabber();
        audioFrameGrabber.setFrameCallback(new AudioFrameGrabber.FrameCallback() {
			@Override
			public void handleFrame(short[] audioData, int length) {
				if (encoding) {
					synchronized (lock) {
						Ffmpeg.encodeAudioFrame(audioData, length);
					}
				}
			}
		});

		synchronized (lock) {
			encoding = Ffmpeg.init(width, height, AUDIO_SAMPLE_RATE, url);
		}

		videoFrameGrabber.start(new byte[width * height * 3]);
		audioFrameGrabber.start(AUDIO_SAMPLE_RATE);

		Log.i(MainActivity.APP_NAME, "Ffmpeg.init() returned " + encoding);
	}

	public void close() {
		Log.i(MainActivity.APP_NAME, "close");

//		videoFrameGrabber.stop();
		audioFrameGrabber.stop();

		if (encoding) {
			synchronized (lock) {
				Ffmpeg.shutdown();
				encoding = false;
			}
		}
	}

	public void sendVideoFrame(byte[] bytes) {
		if (encoding) {
			synchronized (lock) {
				long start = System.currentTimeMillis();
				Ffmpeg.encodeVideoFrame(bytes);
				Log.d("frame", "send frame real - " + (System.currentTimeMillis() - start));
			}
		}
	}
}
