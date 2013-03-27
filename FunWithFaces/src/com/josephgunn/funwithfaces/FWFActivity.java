package com.josephgunn.funwithfaces;

import com.josephgunn.funwithfaces.util.SystemUiHider;
import com.josephgunn.funwithfaces.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FWFActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    private JavaCameraView mOpenCvCameraView;
    private static final String TAG = "FunWithFaces::Activity"; 

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);

    };


	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	/**
	 * The user preferences for the application.
	 */
	SharedPreferences mPreferences;

	private int mCameraId = 1;
	private CameraInfo mCameraInfo;
	
	/**
	 * private int getCameraId 
	 * Get the id of the first front facing camera, or the one specified in the user preferences.
	 */
	private int getCameraId() {
		int cameraId = 1;
		mCameraInfo = new CameraInfo();
		/**
		 * Search for the front facing camera
		 */
		for ( int i = 0; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo( i, mCameraInfo);
			if ( mCameraInfo.facing == 1) {
				cameraId = i;
				break;
			}
		}

		cameraId = mPreferences.getInt("CameraID", mCameraId);
		Camera.getCameraInfo( cameraId, mCameraInfo);

		
		return cameraId;
	}
/**
 * Set the display rotation given the camera position and the display rotation.
 *
 */
	private void setRotationForDisplay( CameraBridgeViewBase openCvCameraView) {
		int deviceRotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch ( deviceRotation) {
			case Surface.ROTATION_0:
				break;
			case Surface.ROTATION_90:
				degrees = 90; break;
			case Surface.ROTATION_180:
				degrees = 180; break;
			case Surface.ROTATION_270:
				degrees = 270; break;
		}

		int rotation = (mCameraInfo.orientation + degrees) % 360;
		rotation = (360 - rotation) % 360;
		if ( mCameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT ) {
			rotation = (mCameraInfo.orientation - degrees + 360) % 360;
		}
		mOpenCvCameraView.setRotation(rotation);
		
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = getPreferences(MODE_PRIVATE);

		setContentView(R.layout.activity_fwf);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.visualMode);
		mOpenCvCameraView = (JavaCameraView)findViewById(R.id.image_area);
		
		mCameraId = getCameraId();
		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, controlsView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});
		
	    mOpenCvCameraView.setCvCameraViewListener(this);
	    setRotationForDisplay( mOpenCvCameraView);    

		
		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
	@Override
	public void onClick(View view) {
		if (TOGGLE_ON_CLICK) {
			mSystemUiHider.toggle();
		} else {
			mSystemUiHider.show();
		}
	}
});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.visualMode).setOnTouchListener(
				mDelayHideTouchListener);
	}
	 @Override
	 public void onPause()
	 {
	     super.onPause();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	 public void onDestroy() {
	     super.onDestroy();
	     if (mOpenCvCameraView != null)
	         mOpenCvCameraView.disableView();
	 }

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
	    mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

		delayedHide(100);
	};

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
    @Override
    public void onManagerConnected(int status) {

		
        switch (status) {
            case LoaderCallbackInterface.SUCCESS:
            {
                Log.i(FWFActivity.TAG, "OpenCV loaded successfully");

                /* Now enable camera view to start receiving frames */
        	    mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                mOpenCvCameraView.enableView();	// This should not be required?
                mOpenCvCameraView.setOnTouchListener(FWFActivity.this);
            } break;
            default:
            {
                super.onManagerConnected(status);
            } break;

        }
    }
    };
    /**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
//			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub

		
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	 public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		 Mat newMat = inputFrame.rgba();
		 return newMat;
	 }
}
