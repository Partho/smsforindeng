/* ====================================================================
 * Copyright (c) 1999-2010 Carnegie Mellon University.  All rights
 * reserved.
 *
 * @author David Huggins-Daines <dhuggins@cs.cmu.edu>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * This work was supported in part by funding from the Defense Advanced 
 * Research Projects Agency and the National Science Foundation of the 
 * United States of America, and the CMU Sphinx Speech Consortium.
 *
 * THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND 
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 */

package edu.cmu.pocketsphinx.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PocketSphinxAndroidDemo extends Activity implements OnTouchListener, RecognitionListener {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}
	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	/**
	 * Editable text view.
	 */
	EditText edit_text;
	
	Button buttonSend;
	EditText textPhoneNo;
	
	
	/**
	 * Respond to touch events on the Speak button.
	 * 
	 * This allows the Speak button to function as a "push and hold" button, by
	 * triggering the start of recognition when it is first pushed, and the end
	 * of recognition when it is released.
	 * 
	 * @param v
	 *            View on which this event is called
	 * @param event
	 *            Event that was triggered.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			start_date = new Date();
			this.listening = true;
			this.rec.start();
			break;
		case MotionEvent.ACTION_UP:
			Date end_date = new Date();
			long nmsec = end_date.getTime() - start_date.getTime();
			this.speech_dur = (float)nmsec / 1000;
			if (this.listening) {
				Log.d(getClass().getName(), "Showing Dialog");
				this.rec_dialog = ProgressDialog.show(PocketSphinxAndroidDemo.this, "", "Recognizing speech...", true);
				this.rec_dialog.setCancelable(false);
				this.listening = false;
			}
			this.rec.stop();
			break;
		default:
			;
		}
		/* Let the button handle its own state */
		return false;
	}

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		copyAsset("pocketsphinxmodels");
		this.rec = new RecognizerTask(new File(getExternalFilesDir(null), "pocketsphinxmodels"));
		this.rec_thread = new Thread(this.rec);
		this.listening = false;
		Button b = (Button) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		this.edit_text = (EditText) findViewById(R.id.EditText01);
		this.rec.setRecognitionListener(this);
		this.rec_thread.start();
		buttonSend = (Button) findViewById(R.id.buttonSend);
		textPhoneNo = (EditText) findViewById(R.id.editTextPhoneNo);
		buttonSend.setOnClickListener(new OnClickListener() {
			 
			@Override
			public void onClick(View v) {
 
			  String phoneNo = textPhoneNo.getText().toString();
			  String sms = edit_text.getText().toString();
 
			  try {
				SmsManager smsManager = SmsManager.getDefault();
				smsManager.sendTextMessage(phoneNo, null, sms, null, null);
				Toast.makeText(getApplicationContext(), "SMS Sent!",
							Toast.LENGTH_LONG).show();
			  } catch (Exception e) {
				Toast.makeText(getApplicationContext(),
					"SMS failed, please try again later!",
					Toast.LENGTH_LONG).show();
				e.printStackTrace();
			  }
 
			}
		});
	}
	
	public void copyAsset(String path) {
		  AssetManager manager = getAssets();
		  
		  File f = new File(getExternalFilesDir(null), path);
		  if(!f.isDirectory()){

			  // If we have a directory, we make it and recurse. If a file, we copy its
			  // contents.
			  try {
			    String[] contents = manager.list(path);
	
			    // The documentation suggests that list throws an IOException, but doesn't
			    // say under what conditions. It'd be nice if it did so when the path was
			    // to a file. That doesn't appear to be the case. If the returned array is
			    // null or has 0 length, we assume the path is to a file. This means empty
			    // directories will get turned into files.
			    if (contents == null || contents.length == 0)
			      throw new IOException();
	
			    // Make the directory.
			    File dir = new File(getExternalFilesDir(null), path);
			    dir.mkdirs();
	
			    // Recurse on the contents.
			    for (String entry : contents) {
			      copyAsset(path + "/" + entry);
			    }
			  } catch (IOException e) {
			    copyFileAsset(path);
			  }
		  }
		}

		/**
		 * Copy the asset file specified by path to app's data directory. Assumes
		 * parent directories have already been created.
		 * 
		 * @param path
		 * Path to asset, relative to app's assets directory.
		 */
		public void copyFileAsset(String path) {
		  File file = new File(getExternalFilesDir(null), path);
		  try {
		    InputStream in = getAssets().open(path);
		    OutputStream out = new FileOutputStream(file);
		    byte[] buffer = new byte[1024];
		    int read = in.read(buffer);
		    while (read != -1) {
		      out.write(buffer, 0, read);
		      read = in.read(buffer);
		    }
		    out.close();
		    in.close();
		  } catch (IOException e) {
		    //Log.d(path, path, e);
		  }
		}
	

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final PocketSphinxAndroidDemo that = this;
		final String hyp = b.getString("hyp");
		that.edit_text.post(new Runnable() {
			public void run() {
				that.edit_text.setText(hyp);
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		final PocketSphinxAndroidDemo that = this;
		this.edit_text.post(new Runnable() {
			public void run() {
				that.edit_text.setText(hyp);
				Date end_date = new Date();
				long nmsec = end_date.getTime() - that.start_date.getTime();
				float rec_dur = (float)nmsec / 1000;
				that.performance_text.setText(String.format("%.2f seconds %.2f xRT",
															that.speech_dur,
															rec_dur / that.speech_dur));
				Log.d(getClass().getName(), "Hiding Dialog");
				that.rec_dialog.dismiss();
			}
		});
	}

	public void onError(int err) {
		final PocketSphinxAndroidDemo that = this;
		that.edit_text.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
	}
}
