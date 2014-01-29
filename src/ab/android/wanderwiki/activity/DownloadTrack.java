package ab.android.wanderwiki.activity;


import wanderwiki.android.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.exception.CreateTrackException;
import me.guillaumin.android.osmtracker.util.FileSystemUtils;
/**
 * The activity of track downloading
 * @author Ayoub Belhadji
 *
 */
public class DownloadTrack extends Activity {
	/**
	 * Objects to use in downloading file from server*/
	private String trackStorageDirectory="";
	private String trackFileName="";
	private String downloadTaskResult= null;
	/**
	 * Objects to use in creating an entry in application database*/
	private DataHelper dataHelper;
	private long trackId;
	private String trackName;
	private long startTime;
	/**
	 * Objects to use in storing gpx data in application database*/
	public DownloadTrack activity;
	private SAXBuilder builder = new SAXBuilder();
	private Document document;
	private Element root;
	private File xmlFile;
	private Button AddTrackButton;
	private int numberTrkpt =1;
	private int j=0;
    /**
     * Object to use in going out from DownloadTrack activity */
	public Intent intent;
	/**
	 * Object to use in showing  progress dialog*/

	private ProgressDialog dialog = null;
	
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg){
		if (msg.what == 0)
		 {
			  
	         
	          downloadTask2();
	          
		 }
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		activity=this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_track);
		//Give the same name to the gpx that figure in server database
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			trackFileName = extras.getString("TRACK_ID_IN_DATABASE")+".gpx";
			}
		dataHelper= new DataHelper(getApplicationContext());
		trackStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wanderwiki/download_tracks/" + trackFileName;
		intent = new Intent(DownloadTrack.this, SearchTrack.class);
	}
	
	
	/**
	 * Creates a new track, in DB and on SD card
	 * @returns The ID of the new track
	 * @throws CreateTrackException
	 */
	@Override
	protected void onResume() {
		super.onResume();

			AddTrackButton = (Button) findViewById(R.id.add_track_button);
			AddTrackButton.setOnClickListener(new OnClickListener() {		
				  @Override
				  public void onClick(View v) {
						    	  //Start downloading the track from the server and show a progress dialog in a new thread
								  final DownloadTrackTask downloadTask =new DownloadTrackTask(DownloadTrack.this,trackFileName);
								  dialog = ProgressDialog.show(DownloadTrack.this, "", getResources().getString(R.string.msg_downloading_file), true);
						          new Thread(new Runnable() {
						              public void run() {                
						       			try {
											downloadTaskResult = (String) downloadTask.execute("http://tools.wmflabs.org/wanderwiki/tracks/"+trackFileName).get();
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (ExecutionException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
						              }                          
						              }).start();
						          	
									try {
										trackId = createNewTrack();
									} catch (CreateTrackException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									xmlFile = new File(trackStorageDirectory);	
									

												
						
				  }
				});
	}
	
	@SuppressWarnings("deprecation")
	private long createNewTrack() throws CreateTrackException {
		Date startDate = new Date();
		
		// Create entry in TRACK table

		 
		ContentValues values = new ContentValues();
		Bundle extras = getIntent().getExtras();
		values.put(Schema.COL_NAME, extras.getString("TRACK_NAME"));
		values.put(Schema.COL_AUTHOR, extras.getString("AUTHOR"));		
		values.put(Schema.COL_LENGTH, extras.getString("LENGTH"));	

		values.put(Schema.COL_DATABASE_ID, extras.getString("TRACK_ID_IN_DATABASE"));
		values.put(Schema.COL_DESCRIPTION, extras.getString("DESCRIPTION"));	
		values.put(Schema.COL_VOTE_POS,extras.getString("VOTE_POS"));
		values.put(Schema.COL_VOTE_NEG,extras.getString("VOTE_NEG"));
		values.put(Schema.COL_START_DATE, startDate.getTime());
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
		values.put(Schema.COL_IS_DOWNLOAD, 1);
		values.put(Schema.COL_DIR,trackStorageDirectory);
		Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);
		return trackId;
	}


    /**The next step is to store gpx data in application database*/
	public void downloadTask2(){

		try {
			document =(Document) builder.build(xmlFile);
			root = document.getRootElement();
			List<Element> listTrks = root.getChildren("trk");
			Element rootTrk = (Element) listTrks.get(0);
			List<Element> listTrksegs = rootTrk.getChildren("trkseg");
			Element rootTrkseg = (Element) listTrksegs.get(0);
			List<Element> listTrkpts = rootTrkseg.getChildren("trkpt");
			numberTrkpt=listTrkpts.size();
					
			Location trackLocation =new Location("passive");
			int t=0;
		    for (int i=0;i<numberTrkpt;i++) {
		    	j++;
			//	progressBarStatus= i*100/numberTrkpt;
				Element trkpt = (Element) listTrkpts.get(i); 
			    Location location = new Location("passive");
			    location.setLatitude(Double.valueOf(trkpt.getAttributes().get(0).getValue()));
			    location.setLongitude(Double.valueOf(trkpt.getAttributes().get(1).getValue()));
			    if (t==0)
			    {
			    	trackLocation = location;
			    }

				dataHelper.track(trackId,location); 
				t++;
			   }
	           dataHelper.wayPoint(trackId, trackLocation, 0, "", "", "");
				//The file is downloaded,
	            dialog.dismiss();
			    Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_download_success), Toast.LENGTH_LONG).show();	
				//Show the track's details
			    Intent intent = new Intent(DownloadTrack.this, DisplayTrackDetail.class);
				intent.putExtra(Schema.COL_TRACK_ID, trackId);
				startActivity(intent);
				finish();
			 
		} catch (Exception e) {
		// TODO Auto-generated catch block
			dialog.dismiss();
		e.printStackTrace();
		Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_LONG).show();		
		//delete the track from application database in the case of the failure in downloading
		deleteTrack(trackId);
		}
		
		j=0;
	}

    /**The next step is to increment the number of downloads in server*/
	public void downloadTask3(){

	}
		/**
		 * Deletes the track with the specified id from DB and SD card
		 * @param The ID of the track to be deleted
		 */
		private void deleteTrack(long id) {
			getContentResolver().delete(
					ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, id),
					null, null);

			// Delete any data stored for the track we're deleting
			File trackStorageDirectory = DataHelper.getTrackDirectory(id);
			if (trackStorageDirectory.exists()) {
				FileSystemUtils.delete(trackStorageDirectory, true);
			}
		}

		private class DownloadTrackTask extends AsyncTask<String, Integer, String>  {
			  public String   trackStorageDirectory;
			  private Context context;

			    public DownloadTrackTask(Context context,String fileName) {
			        this.context = context;
			        trackStorageDirectory= Environment.getExternalStorageDirectory().getAbsolutePath() + "/wanderwiki/download_tracks/" + fileName;
			    }

			    @Override
			    protected String doInBackground(String... sUrl) {
			        // take CPU lock to prevent CPU from going off if the user 
			        // presses the power button during download
			        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			             getClass().getName());
			        wl.acquire();

			        try {
			            InputStream input = null;
			            OutputStream output = null;
			            HttpURLConnection connection = null;
			            try {
			                URL url = new URL(sUrl[0]);
			                connection = (HttpURLConnection) url.openConnection();
			                connection.connect();

			                // expect HTTP 200 OK, so we don't mistakenly save error report 
			                // instead of the file
			                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
			                     return "Server returned HTTP " + connection.getResponseCode() 
			                         + " " + connection.getResponseMessage();
			                // this will be useful to display download percentage
			                // might be -1: server did not report the length
			                int fileLength = connection.getContentLength();
			                // download the file
			                input = connection.getInputStream();
			                output = new FileOutputStream(trackStorageDirectory);

			                byte data[] = new byte[4096];
			                long total = 0;
			                int count;
			                while ((count = input.read(data)) != -1) {
			                    // allow canceling with back button
			                    if (isCancelled())
			                        return "1";
			                    total += count;
			                    // publishing the progress....
			                    if (fileLength > 0) // only if total length is known
			                        publishProgress((int) (total * 100 / fileLength));
			                    output.write(data, 0, count);
			                }
			            } catch (Exception e) {
			                return e.toString();
			            } finally {
			                try {
			                    if (output != null)
			                        output.close();
			                    if (input != null)
			                        input.close();
			                } 
			                catch (IOException ignored) { }

			                if (connection != null)
			                    connection.disconnect();
			            }
			        } finally {
			            wl.release();
			        }
			        
			        return "1";
			    }
			    @Override
			    protected void onPreExecute() {
			        super.onPreExecute();
			 //       mProgressDialog.show();
			    }

			    @Override
			    protected void onProgressUpdate(Integer... progress) {
			        super.onProgressUpdate(progress);
			        // if we get here, length is known, now set indeterminate to false

			    }

			    @Override
			    protected void onPostExecute(String result) {

			    	handler.sendEmptyMessage(0);
			    }
		}

}
