package me.guillaumin.android.osmtracker.listener;

import android.view.View;
import android.view.View.OnClickListener;

import me.guillaumin.android.osmtracker.activity.TrackLogger;

/**
 * Manages voice recording.
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class VoiceRecOnClickListener implements OnClickListener{

	/**
	 * Parent activity
	 */
	private TrackLogger tl;
	public VoiceRecOnClickListener(TrackLogger trackLogger) {
		tl = trackLogger;
	}

	@Override
	public void onClick(View v) {
		tl.showDialog(TrackLogger.DIALOG_VOICE_RECORDING);
	}

}
