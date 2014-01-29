package ab.android.wanderwiki.activity;

import wanderwiki.android.R;

import java.text.DateFormat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
/**
 * The activity that show track's details after been downloaded or tracked
 * @author Ayoub Belhadji
 *
 */
public class DisplayTrackDetail extends Activity{

	/** Current track ID */
	protected long trackId;

	/** Text View for the track name */
	protected TextView tvName;

	/** Text View for track description */
	protected TextView tvAuthor;

	/** Text View for track tags */
	protected TextView tvDescription;
	/** Text View for track creation date */
	
	protected TextView tvDate;
	/** Text View for track length */
	protected TextView tvLength;
	/** Button for display track**/
	protected Button btDisplay;
	/** Button for resume tracking**/
	protected Button btResume;
	/** Button for upload track**/
	protected Button btUpload;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display_track_detail);
		this.trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
		ContentResolver cr = getContentResolver();
		
		final Cursor cursor = cr.query(
			ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
			null, null, null, null);
		Uri uriss = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		cursor.moveToFirst();
		tvName = (TextView) findViewById(R.id.displaytrackdetail_trackNameView);
		tvAuthor = (TextView) findViewById(R.id.displaytrackdetail_authorView);
		tvDescription = (TextView) findViewById(R.id.displaytrackdetail_descriptionView);
		tvDate = (TextView) findViewById(R.id.displaytrackdetail_dateView);
		tvLength = (TextView) findViewById(R.id.displaytrackdetail_lengthView);
		btDisplay =(Button) findViewById(R.id.displaytrackdetail_display_button);
		btUpload =(Button) findViewById(R.id.displaytrackdetail_upload_button);
		/**Display the track*/
		btDisplay.setOnClickListener(new OnClickListener() {			
			  @Override
			  public void onClick(View v) {
				  try{
				  displayTrack(trackId);
				  }catch (Exception e)
				  {
						 Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
				  }
				}
			});
		/**Upload the track*/
		btUpload.setOnClickListener(new OnClickListener() {			
			  @Override
			  public void onClick(View v) {
				  try{
				  uploadTrack(trackId);
				  }catch (Exception e)
				  {
						 Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
				  }
				}
			});
		
		try{
		tvName.setText( getResources().getString(R.string.track_name)+cursor.getString(cursor.getColumnIndex(Schema.COL_NAME)));
		tvAuthor.setText( getResources().getString(R.string.author)+cursor.getString(cursor.getColumnIndex(Schema.COL_AUTHOR)));
		tvDescription.setText( getResources().getString(R.string.description) + cursor.getString(cursor.getColumnIndex(Schema.COL_DESCRIPTION)));
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		
		tvDate.setText( getResources().getString(R.string.date)+ dateFormat.format(cursor.getLong(cursor.getColumnIndex(Schema.COL_START_DATE))));
		tvLength.setText( getResources().getString(R.string.length)+ String.valueOf(cursor.getLong(cursor.getColumnIndex(Schema.COL_LENGTH))));

		//If the track is already uploaded don't show the upload's button
		}catch (Exception e)
		{
		    
			Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
		}

		cursor.close();
	}

	
	protected void uploadTrack(long trackId2) {
		try{
		Intent i;
		i = new Intent(this, UploadTrack.class);
		i.putExtra(Schema.COL_TRACK_ID, trackId2);
    	startActivity(i);
		}catch (Exception e)
		{
			 Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
		}
		
	}


	private void displayTrack(long trackId2){
		try{
		Intent i;
		i = new Intent(this, TrackLogger.class);
		i.putExtra(Schema.COL_TRACK_ID, trackId2);
		i.putExtra(TrackLogger.STATE_IS_TRACKING, false);
    	startActivity(i);
		}catch (Exception e)
		{
			 Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
		}
	}

}
