package me.guillaumin.android.osmtracker.activity;

import wanderwiki.android.R;

import java.io.File;
import java.util.Date;

import com.android.wanderwiki.OSMTracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.layout.GpsStatusRecord;
import me.guillaumin.android.osmtracker.layout.UserDefinedLayout;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import me.guillaumin.android.osmtracker.service.gps.GPSLoggerServiceConnection;
import me.guillaumin.android.osmtracker.util.ThemeValidator;
import me.guillaumin.android.osmtracker.view.TextNoteDialog;
import me.guillaumin.android.osmtracker.view.VoiceRecDialog;
/**
 * Main track logger activity. Communicate with the GPS service to display GPS
 * status, and allow user to record waypoints.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class TrackLogger extends Activity {

	private static final String TAG = TrackLogger.class.getSimpleName();

	/**
	 * Request code for callback after the camera application had taken a
	 * picture for us.
	 */
	private static final int REQCODE_IMAGE_CAPTURE = 0;

	/**
	 * Bundle state key for tracking flag.
	 */
	public static final String STATE_IS_TRACKING = "isTracking";
    
	/**
	 * Bundle state key button state.
	 */
	public static final String STATE_BUTTONS_ENABLED = "buttonsEnabled";

	/**
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

	/**
	 * GPS Logger service intent, to be used in start/stopService();
	 */
	private Intent gpsLoggerServiceIntent;

	/**
	 * Main button layout
	 */
	private UserDefinedLayout mainLayout;

	/**
	 * Flag to check GPS status at startup. Is cleared after the first
	 * displaying of GPS status dialog, to prevent the dialog to display if user
	 * goes to settings/about/other screen.
	 */
	private boolean checkGPSFlag = true;
	
	/**
	 * Keeps track of the image file when taking a picture.
	 */
	private File currentImageFile;
	
	/**
	 * Keeps track of the current track id.
	 */
	private long currentTrackId;

	/**
	 * Handles the bind to the GPS Logger service
	 */
	private ServiceConnection gpsLoggerConnection = new GPSLoggerServiceConnection(this);
	
	/**
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences prefs = null;
	
	/**
	 * keeps track of current button status
	 */
	private boolean buttonsEnabled = false;
	
	/**
	 * constant for text note dialog
	 */
	public static final int DIALOG_TEXT_NOTE = 1;
	/**
	 * constant for voice recording dialog
	 */
	public static final int DIALOG_VOICE_RECORDING = 2;
	/**
	 * constant for DisplayTrackMap request
	 */
	public static final int DISPLAY_TRACK_MAP = 3;

	
	public Boolean isTracking = false;
	
	
	private Boolean gpsEnabled = false;
	
	private ProgressDialog	gpsWaitingDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Get the track id to work with
		currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		if (getIntent().getExtras().containsKey(TrackLogger.STATE_IS_TRACKING)) isTracking= getIntent().getExtras().getBoolean(TrackLogger.STATE_IS_TRACKING);
			
		Log.v(TAG, "Starting for track id " + currentTrackId);
			
		gpsLoggerServiceIntent = new Intent(this, GPSLogger.class);
		gpsLoggerServiceIntent.putExtra(Schema.COL_TRACK_ID, currentTrackId);

		if (isTracking){
		// Start GPS Logger service
		startService(gpsLoggerServiceIntent);

		// Bind to GPS service.
		// We can't use BIND_AUTO_CREATE here, because when we'll ubound
		// later, we want to keep the service alive in background
		bindService(gpsLoggerServiceIntent, gpsLoggerConnection, 0);
		}
		

	//	Toast.makeText(this, String.valueOf(isTracking), Toast.LENGTH_LONG).show();
		// Populate default preference values
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// get shared preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Set application theme according to user settings
		setTheme(getResources().getIdentifier(ThemeValidator.getValidTheme(prefs, getResources()), null, null));

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tracklogger);
		final Button DisplayButton = (Button) findViewById(R.id.displaytrack);
		 DisplayButton.setOnClickListener(new OnClickListener() {
				
			  @Override
			  public void onClick(View v) {

				   displayTrack();  
				}
			});
		// set trackLogger to keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
	//	gpsWaitingDialog = ProgressDialog.show(TrackLogger.this, "",  getResources().getString(R.string.various_waiting_gps_fix), true);

		// we'll restore previous button state, GPSStatusRecord will enable all buttons, as soon as there's a gps fix
		if(savedInstanceState != null){
			buttonsEnabled = savedInstanceState.getBoolean(STATE_BUTTONS_ENABLED, false);
		}
	}

	@Override
	protected void onResume() {

		setTitle(getResources().getString(R.string.tracklogger) + ": #" + currentTrackId);
		
		// set trackLogger to  keepScreenOn depending on the user's preference
		View trackLoggerView = findViewById(R.id.tracklogger_root);
		trackLoggerView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
	

		
		// Check GPS status
		if (checkGPSFlag
				&& prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_CHECKSTARTUP,
						OSMTracker.Preferences.VAL_GPS_CHECKSTARTUP)) {
			checkGPSProvider();
		}

		// Register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(true);

		if(!isTracking) {
			Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
	    	sendBroadcast(intent);
	    	}

		setEnabledActionButtons(buttonsEnabled);
		if(!buttonsEnabled){
		//	Toast.makeText(this, R.string.tracklogger_waiting_gps, Toast.LENGTH_LONG).show();
		}


		
		super.onResume();
	}

	private void checkGPSProvider() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			// GPS isn't enabled. Offer user to go enable it
			new AlertDialog.Builder(this)
					.setTitle(R.string.tracklogger_gps_disabled)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(getResources().getString(R.string.tracklogger_gps_disabled_hint))
					.setCancelable(true).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).create().show();
			checkGPSFlag = false;
		}
	}

	@Override
	protected void onPause() {
		
		// Un-register GPS status update for upper controls
		((GpsStatusRecord) findViewById(R.id.gpsStatus)).requestLocationUpdates(false);



		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the fact that we are currently tracking or not
		if(gpsLogger != null){
			outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		}
		outState.putBoolean(STATE_BUTTONS_ENABLED, buttonsEnabled);

		super.onSaveInstanceState(outState);
	}

	/**
	 * Called when GPS is disabled
	 */
	public void onGpsDisabled() {
		// GPS disabled. Grey all.
		setEnabledActionButtons(false);

	}

	/**
	 * Called when GPS is enabled
	 */
	public void onGpsEnabled() {
		// Buttons can be enabled
		
		if (gpsLogger != null) {

			setEnabledActionButtons(true);
			gpsEnabled = true;
			displayTrack();
		}
	}

	/**
	 * Enable buttons associated to tracking
	 * 
	 * @param enabled
	 *            true to enable, false to disable
	 */
	public void setEnabledActionButtons(boolean enabled) {
		if (mainLayout != null) {
			buttonsEnabled = enabled;
			mainLayout.setEnabled(enabled);
		}
	}

	// Create options menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracklogger_menu, menu);
		return true;
	}

	// Manage options menu selections
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.tracklogger_menu_displaytrack:
			// Start display track activity, with or without OSM background
			boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
			if (useOpenStreetMapBackground) {
				i = new Intent(this, DisplayTrackMap.class);
				i.putExtra(TrackLogger.STATE_IS_TRACKING, isTracking);
			} else {
				i = new Intent(this, DisplayTrack.class);
				i.putExtra(TrackLogger.STATE_IS_TRACKING, isTracking);
			}
			i.putExtra(Schema.COL_TRACK_ID, currentTrackId);
			startActivityForResult(i,3);
			String result=i.getStringExtra("result");     
			if (result=="voiceRecord")
			{
				this.showDialog(TrackLogger.DIALOG_VOICE_RECORDING);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// Manage back button if we are on a sub-page
			if (event.getRepeatCount() == 0) {
				if (mainLayout != null && mainLayout.getStackSize() > 1) {
					mainLayout.pop();
					return true;
				}
			}
			break;
		case KeyEvent.KEYCODE_CAMERA:
			if (gpsLogger.isTracking()) {
				requestStillImage();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// API Level 3 doesn't support long presses, so we need to do this manually
			if((gpsLogger != null && gpsLogger.isTracking()) && (event.getEventTime() - event.getDownTime()) > OSMTracker.LONG_PRESS_TIME){
				// new long press of dpad center detected, start voice recording dialog
				this.showDialog(DIALOG_VOICE_RECORDING);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_HEADSETHOOK:
			if (gpsLogger != null && gpsLogger.isTracking()){
				this.showDialog(DIALOG_VOICE_RECORDING);
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Request a still picture from the camera application, saving the file in
	 * the current track directory
	 */
	public void requestStillImage() {
		if (gpsLogger.isTracking()) {
			File imageFile = pushImageFile();
			if (null != imageFile) {
				Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
				startActivityForResult(cameraIntent, TrackLogger.REQCODE_IMAGE_CAPTURE);
			} else {
				Toast.makeText(getBaseContext(), 
						getResources().getString(R.string.error_externalstorage_not_writable),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(TAG, "Activity result: " + requestCode + ", resultCode=" + resultCode + ", Intent=" + data);
		switch (requestCode) {
		case REQCODE_IMAGE_CAPTURE:
			if (resultCode == RESULT_OK) {
				// A still image has been captured, track the corresponding waypoint
				// Send an intent to inform service to track the waypoint.
				File imageFile = popImageFile();
				if (imageFile != null) {
					Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
					intent.putExtra(Schema.COL_TRACK_ID, currentTrackId);
					intent.putExtra(OSMTracker.INTENT_KEY_NAME, getResources().getString(R.string.wpt_stillimage));
					intent.putExtra(OSMTracker.INTENT_KEY_LINK, imageFile.getName());
					sendBroadcast(intent);
				}			
			}

		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * Getter for gpsLogger
	 * 
	 * @return Activity {@link GPSLogger}
	 */
	public GPSLogger getGpsLogger() {
		return gpsLogger;
	}

	/**
	 * Setter for gpsLogger
	 * 
	 * @param l
	 *            {@link GPSLogger} to set.
	 */
	public void setGpsLogger(GPSLogger l) {
		this.gpsLogger = l;
	}
	
	/**
	 * Gets a File for storing an image in the current track dir
	 * and stores it in a class variable.
	 * 
	 * @return A File pointing to an image file inside the current track directory
	 */
	public File pushImageFile() {
		currentImageFile = null;

		// Query for current track directory
		File trackDir = DataHelper.getTrackDirectory(currentTrackId);

		// Create the track storage directory if it does not yet exist
		if (!trackDir.exists()) {
			if ( !trackDir.mkdirs() ) {
				Log.w(TAG, "Directory [" + trackDir.getAbsolutePath() + "] does not exist and cannot be created");
			}
		}

		// Ensure that this location can be written to 
		if (trackDir.exists() && trackDir.canWrite()) {
			currentImageFile = new File(trackDir, 
					DataHelper.FILENAME_FORMATTER.format(new Date()) + DataHelper.EXTENSION_JPG);			
		} else {
			Log.w(TAG, "The directory [" + trackDir.getAbsolutePath() + "] will not allow files to be created");
		}
		
		return currentImageFile;
	}
	
	/**
	 * @return The current image file, and clear the internal variable.
	 */
	public File popImageFile() {
		File imageFile = currentImageFile;
		currentImageFile = null;
		return imageFile;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case DIALOG_TEXT_NOTE:
			// create a new TextNoteDialog
			return new TextNoteDialog(this, currentTrackId);
		case DIALOG_VOICE_RECORDING:
			// create a new VoiceRegDialog
			return new VoiceRecDialog(this, currentTrackId);
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id){
		case DIALOG_TEXT_NOTE:
			// we need to reset Values like uuid of the dialog,
			// otherwise we would overwrite an existing waypoint
			((TextNoteDialog)dialog).resetValues();
			break;
		}
		super.onPrepareDialog(id, dialog);
	}
	
	@Override
	protected void onNewIntent(Intent newIntent) {
		if (newIntent.getExtras() != null && newIntent.getExtras().containsKey(Schema.COL_TRACK_ID)) {
			currentTrackId = newIntent.getExtras().getLong(Schema.COL_TRACK_ID);
			setIntent(newIntent);
		}
		super.onNewIntent(newIntent);
	}

	public long getCurrentTrackId() {
		return this.currentTrackId;
	}
	

	private void displayTrack()
	{
		Intent i;
		// Start display track activity, with or without OSM background
		boolean useOpenStreetMapBackground = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				OSMTracker.Preferences.KEY_UI_DISPLAYTRACK_OSM, OSMTracker.Preferences.VAL_UI_DISPLAYTRACK_OSM);
		if (useOpenStreetMapBackground) {
			i = new Intent(this, DisplayTrackMap.class);
			i.putExtra(TrackLogger.STATE_IS_TRACKING, isTracking);
		} else {
			i = new Intent(this, DisplayTrack.class);
			i.putExtra(TrackLogger.STATE_IS_TRACKING, isTracking);
		}
		i.putExtra(Schema.COL_TRACK_ID, currentTrackId);
		
		startActivity(i);
	}
	
	public void gpsDialogDismiss()
	{
	//	Toast.makeText(this, "jj", Toast.LENGTH_LONG).show();
	//	gpsWaitingDialog.dismiss();
	}
	
}
