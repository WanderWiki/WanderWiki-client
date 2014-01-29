package me.guillaumin.android.osmtracker.service.gps;

import com.android.wanderwiki.OSMTracker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import me.guillaumin.android.osmtracker.activity.DisplayTrackMap;
import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
/**
 * Handles the bind to the GPS Logger service
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPSLoggerServiceConnection implements ServiceConnection {
	/**
	 * 
	 */
	private static final int DISPLAYTRACKMAP = 0;
	private static final int TRACKLOGGER= 1;
	/**
	 * Reference to TrackLogger activity
	 */
	private TrackLogger activity;
	private DisplayTrackMap activity2;
	int selectedActivity;
	public GPSLoggerServiceConnection(TrackLogger tl) {
		activity = tl;
		selectedActivity = TRACKLOGGER;
	}
	
	public GPSLoggerServiceConnection(DisplayTrackMap d1) {
		activity2 = d1;
		selectedActivity = DISPLAYTRACKMAP;
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (selectedActivity == TRACKLOGGER){
			activity.setEnabledActionButtons(false);
			activity.setGpsLogger(null);
			activity.gpsDialogDismiss();
		}else{
			activity2.setGpsLogger(null);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	if (selectedActivity == TRACKLOGGER){	
		    activity.gpsDialogDismiss();
			activity.setGpsLogger( ((GPSLogger.GPSLoggerBinder) service).getService());
			
		// Update record status regarding of current tracking state
	//	GpsStatusRecord gpsStatusRecord = (GpsStatusRecord) activity.findViewById(R.id.gpsStatus);
	//	if (gpsStatusRecord != null) {
	//		gpsStatusRecord.manageRecordingIndicator(activity.getGpsLogger().isTracking());
	//	}
		
		// If not already tracking, start tracking
			if (!activity.getGpsLogger().isTracking()) {
				activity.setEnabledActionButtons(false);
				Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
				intent.putExtra(Schema.COL_TRACK_ID, activity.getCurrentTrackId());
				activity.sendBroadcast(intent);
			}
	}
	else{	
		activity2.setGpsLogger( ((GPSLogger.GPSLoggerBinder) service).getService());

	// Update record status regarding of current tracking state
//	GpsStatusRecord gpsStatusRecord = (GpsStatusRecord) activity.findViewById(R.id.gpsStatus);
//	if (gpsStatusRecord != null) {
//		gpsStatusRecord.manageRecordingIndicator(activity.getGpsLogger().isTracking());
//	}
	
	// If not already tracking, start tracking
	//	if (!activity2.getGpsLogger().isTracking()) {
	//		Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
	//		intent.putExtra(Schema.COL_TRACK_ID, activity2.getCurrentTrackId());
	//		activity2.sendBroadcast(intent);
	//	}
}
	}
}
