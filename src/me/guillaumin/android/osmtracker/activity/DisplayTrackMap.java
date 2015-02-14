package me.guillaumin.android.osmtracker.activity;

import wanderwiki.android.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.bonuspack.overlays.ItemizedOverlayWithBubble;
import org.osmdroid.contributor.util.constants.OpenStreetMapContributorConstants;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import com.android.wanderwiki.OSMTracker;

import ab.android.wanderwiki.activity.Home;
import ab.android.wanderwiki.overlay.WikiArticlesInfoWindow;
import ab.android.wanderwiki.overlay.WikiArticlesOverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.DatabaseHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.listener.StillImageOnClickListener;
import me.guillaumin.android.osmtracker.listener.TextNoteOnClickListener;
import me.guillaumin.android.osmtracker.listener.VoiceRecOnClickListener;
import me.guillaumin.android.osmtracker.overlay.WayPointsOverlay;
import me.guillaumin.android.osmtracker.service.gps.GPSLogger;
import me.guillaumin.android.osmtracker.service.gps.GPSLoggerServiceConnection;
import me.guillaumin.android.osmtracker.view.TextNoteDialog;
import me.guillaumin.android.osmtracker.view.VoiceRecDialog;
/**
 * Display current track over an OSM map.
 * Based on osmdroid code http://osmdroid.googlecode.com/
 *<P>
 * Used only if {@link OSMTracker.Preferences#KEY_UI_DISPLAYTRACK_OSM} is set.
 * Otherwise {@link DisplayTrack} is used (track only, no OSM background tiles).
 * 
 * @author Viesturs Zarins
 *
 */
public class DisplayTrackMap extends Activity implements OpenStreetMapContributorConstants{
	/**
	 * Request code for callback after the camera application had taken a
	 * picture for us.
	 */
	private static final int REQCODE_IMAGE_CAPTURE = 0;
	@SuppressWarnings("unused")
	private static final String TAG = DisplayTrackMap.class.getSimpleName();
	
	/**
	 * Key for keeping the zoom level in the saved instance bundle
	 */
	private static final String CURRENT_ZOOM = "currentZoom";

	/**
	 *  Key for keeping scrolled left position of OSM view activity re-creation
	 * 
	 */
	private static final String CURRENT_SCROLL_X = "currentScrollX";

	/** 
	 * Key for keeping scrolled top position of OSM view across activity re-creation
	 * 
	 */ 
	private static final String CURRENT_SCROLL_Y = "currentScrollY";

	/**
	 *  Key for keeping whether the map display should be centered to the gps location 
	 * 
	 */
	private static final String CURRENT_CENTER_TO_GPS_POS = "currentCenterToGpsPos";

	/**
	 *  Key for keeping whether the map display was zoomed and centered
	 *  on an old track id loaded from the database (boolean {@link #zoomedToTrackAlready})
	 */
	private static final String CURRENT_ZOOMED_TO_TRACK = "currentZoomedToTrack";

	/**
	 * Key for keeping the last zoom level across app. restart
	 */
	private static final String LAST_ZOOM = "lastZoomLevel";

	/**
	 * Default zoom level
	 */
	private static final int DEFAULT_ZOOM  = 16;
	
	/**
	 * Bundle state key for tracking flag.
	 */
	public static final String STATE_IS_TRACKING = "isTracking";

	/**
	 * Keeps track of the image file when taking a picture.
	 */
	private File currentImageFile;
	
	/**
	 * Main OSM view
	 */
	private MapView osmView;
	
	/** Constant used if no track is active (-1)*/
	private static final long TRACK_ID_NO_TRACK = -1;
	
	/**
	 * Controller to interact with view
	 */
	private MapController osmViewController;
	
	/**
	 * OSM view overlay that displays current location
	 */
	private SimpleLocationOverlay myLocationOverlay;
	
	/**
	 * OSM view overlay that displays current path
	 */
	private PathOverlay pathOverlay;

	/**
	 * OSM view overlay that displays waypoints 
	 */
	private WayPointsOverlay wayPointsOverlay;	
	/**
	 * OSM view overlay that displays links to wikipedia articles 
	 */
	private WikiArticlesOverlay wikiArticlesOverlay;	
	
	/**
	 * Current track id
	 */
	private long currentTrackId;
	
	/**
	 * whether the map display should be centered to the gps location 
	 */
	private boolean centerToGpsPos = true;
	
	/**
	 * whether the map display was already zoomed and centered
	 * on an old track loaded from the database (should be done only once).
	 */
	private boolean zoomedToTrackAlready = false;
	
	/**
	 * the last position we know
	 */
	private GeoPoint currentPosition;

	/**
	 * The row id of the last location read from the database that has been added to the
	 * list of layout points. Using this we to reduce DB load by only reading new points.
	 * Initially null, to indicate that no data has yet been read.  
	 */
	private Integer lastTrackPointIdProcessed = null;
	
	/**
	 * Observes changes on trackpoints
	 */
	private ContentObserver trackpointContentObserver;

	/**
	 * Keeps the SharedPreferences
	 */
	private SharedPreferences prefs = null;
	/**
	 * Listener bound to text note buttons
	 */
	private TextNoteOnClickListener textNoteOnClickListener;
	
	/**
	 * Listener bound to voice record buttons
	 */
	private VoiceRecOnClickListener voiceRecordOnClickListener;
	
	/**
	 * Lister bound to picture buttons
	 */
	private StillImageOnClickListener stillImageOnClickListener;
	/**
	 * TrackLogger for media listeners
	 */
	
	
    private int createArticlesOverlayStatus =0;
    

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
	
	/**
	 * Flag to check GPS status at startup. Is cleared after the first
	 * displaying of GPS status dialog, to prevent the dialog to display if user
	 * goes to settings/about/other screen.
	 */
	private boolean checkGPSFlag = true;
	
	/**
	 * GPS coordinates of the map's corner
	 */
	String supLat ="";
	String supLon ="";
	String infLat ="";
	String infLon ="";
	/**
	 * The result from server after voting, this variable will be used to make sure that vote is saved in database
	 */
    String resultVote = null;
    /**
     * The result from server after requesting Wikipedia articles : a JSON
     */
	String resultGettingWikiArticles = "";
	
	
	/**
	 * GPS Logger service, to receive events and be able to update UI.
	 */
	private GPSLogger gpsLogger;

	/**
	 * GPS Logger service intent, to be used in start/stopService();
	 */
	private Intent gpsLoggerServiceIntent;

	/**
	 * Handles the bind to the GPS Logger service
	 */
	
	private ServiceConnection gpsLoggerConnection = new GPSLoggerServiceConnection(this);
	
	/**
	 * This variable contain true if the user is tracking, false if is not tracking
	 */
	public Boolean isTracking = false;
	
	public String resultVoteUrl="";
	protected DatabaseHelper dbHelper;
	/**The handled is used to manage the dialogs of voting*/
	private Handler handler = new Handler(){
			public void handleMessage(android.os.Message msg){
			     dbHelper = new DatabaseHelper(getBaseContext());
			    //
			    if (msg.what==1)
			    {
	    		    Toast.makeText(getBaseContext(),resultVoteUrl+"s", Toast.LENGTH_LONG).show();	
			    	  ContentValues values = new ContentValues();
			    	  
				     	if (resultVote == "success") 
		    		    	{
			    			values.put(Schema.COL_IS_VOTED, 1);
				    		long updateId =dbHelper.getWritableDatabase().update(Schema.TBL_TRACK,values, "_id "+"="+String.valueOf(currentTrackId), null);
					    	goToHome_aux(updateId);
			    		}
				    	else
	    				{
		    			    values.put(Schema.COL_IS_VOTED, 0);
			    			long updateId =dbHelper.getWritableDatabase().update(Schema.TBL_TRACK,values, "_id "+"="+String.valueOf(currentTrackId), null);
				    		goToHome_aux(updateId);
			    		}	
			    }
			    if (msg.what == 0)
				{
			    	JSONArray jArray=null;
			    try{
			           resultGettingWikiArticles=resultGettingWikiArticles.substring(0, resultGettingWikiArticles.length()-1);
						try {
				        	jArray = new JSONArray(resultGettingWikiArticles);
				        	createArticlesOverlayStatus = 1;
		          		} catch (JSONException e) {
				    	// TODO Auto-generated catch block
				    	  e.printStackTrace();
				    	  createArticlesOverlayStatus = 0;
				      }
		            JSONObject json_data=null;
		   		wikiArticlesOverlay = new WikiArticlesOverlay(getBaseContext(),currentTrackId,jArray,osmView);
		   		ItemizedOverlayWithBubble<ExtendedOverlayItem> wikiArticlesItemizedOverlay = null;
		   		try {
		   			ArrayList<ExtendedOverlayItem> listItems = new ArrayList<ExtendedOverlayItem>();
		   			wikiArticlesItemizedOverlay = new  ItemizedOverlayWithBubble<ExtendedOverlayItem>(getBaseContext(),wikiArticlesOverlay.wikiArticlesItems,osmView,new WikiArticlesInfoWindow(osmView,getBaseContext()));
		   		}
		   		catch (Exception e){
		   		}
		    	 	osmView.getOverlays().remove(osmView.getOverlays().size()-1);
		    		osmView.getOverlays().add(wikiArticlesItemizedOverlay);
			    	}
				    catch (Exception e)
				    {		    
				     createArticlesOverlayStatus = 0;
				    }
				}
			};
			};
	public int isReady = 0; 
	public long startTime;
	private Runnable updateTimerThread = new Runnable() {
		        public void run() {
		        	if (isReady==0)
		        	{
		              //	wayPointsOverlay.refresh();	
		    	    	ImageView refreshButton = (ImageView) 	findViewById(R.id.displaytrackmap_refresh);
		    	    	if (osmView.getWidth()!=0 && (osmView.getHeight()!=0))
	 	    	    	{
	 	    	        	refreshButton.setVisibility(View.VISIBLE);
	 		            	updateArticlesoverlay();
	 		            	isReady = 1;	
	 		            	
	 		        	}
		                handler.postDelayed(this,500);
		           		        	}
		        }
		    };
	public int vote = 0 ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);        
        setContentView(R.layout.displaytrackmap);
        startTime = SystemClock.uptimeMillis();
		if (getIntent().getExtras().containsKey(TrackLogger.STATE_IS_TRACKING)) isTracking= getIntent().getExtras().getBoolean(TrackLogger.STATE_IS_TRACKING);
        currentTrackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);
        setTitle(getTitle() + ": #" + currentTrackId);
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
        
        // Initialize OSM view
        osmView = (MapView) findViewById(R.id.displaytrackmap_osmView);
        osmView.setMultiTouchControls(true);  // pinch to zoom
        // we'll use osmView to define if the screen is always on or not
        osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
        osmViewController = osmView.getController();
        //  stopActiveTrack();
        // Check if there is a saved zoom level
        if(savedInstanceState != null) {
        	osmViewController.setZoom(savedInstanceState.getInt(CURRENT_ZOOM, DEFAULT_ZOOM));
        	osmView.scrollTo(savedInstanceState.getInt(CURRENT_SCROLL_X, 0),
        			savedInstanceState.getInt(CURRENT_SCROLL_Y, 0));
        	centerToGpsPos = savedInstanceState.getBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos);
        	zoomedToTrackAlready = savedInstanceState.getBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready);
        } else {
        	// Try to get last zoom Level from Shared Preferences
        	SharedPreferences settings = getPreferences(MODE_PRIVATE);
        	osmViewController.setZoom(settings.getInt(LAST_ZOOM, DEFAULT_ZOOM));
        }
		createOverlays();
        // Create content observer for trackpoints
        trackpointContentObserver = new ContentObserver(new Handler()) {
    		@Override
    		public void onChange(boolean selfChange) {		
    			pathChanged();		
    		}
    	};

        // Register listeners for zoom buttons
        findViewById(R.id.displaytrackmap_imgZoomIn).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomIn();
				updateArticlesoverlay();
			}
        });
        findViewById(R.id.displaytrackmap_out).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				goToHome();
			}
        });
        final ImageView stop_start_button = (ImageView) findViewById(R.id.displaytrackmap_stop_start_tracking);
        stop_start_button.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isTracking){
			    	try{
			    		 stop_start_button.setImageDrawable(getResources().getDrawable(R.drawable.stop_tracking));
	    				 setActiveTrack();
	    				 isTracking = true;
	    				 
	    			}
		    		catch(Exception e)
		    		{
		    		    Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_LONG).show();	
		    		}
				}else{
			    	try{
			     	   	 stop_start_button.setImageDrawable(getResources().getDrawable(R.drawable.start_tracking));
	    				 stopActiveTrack();
	    				 isTracking = false;
	    			}
		    		catch(Exception e)
		    		{
		    		    Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_LONG).show();	
		    		}
				}
			}
        });
        findViewById(R.id.displaytrackmap_imgZoomOut).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				osmViewController.zoomOut();
				updateArticlesoverlay();
			}
        });
        findViewById(R.id.displaytrackmap_position).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				centerToGpsPos = true;
				if(currentPosition != null){
					osmViewController.animateTo(currentPosition);
				}
			}
        });
        findViewById(R.id.displaytrackmap_textNote).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {				
				DisplayTrackMap.this.showDialog(TrackLogger.DIALOG_TEXT_NOTE);
			}
			});
        findViewById(R.id.displaytrackmap_stillImage).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (gpsLogger.isTracking()) {
					requestStillImage();
				}
			}
			});
        findViewById(R.id.displaytrackmap_refresh).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateArticlesoverlay();
			}
			});

        handler.postDelayed(updateTimerThread,20000);
   }
    protected void goToHome_aux(Long var){
    	startActivity(new Intent(this, Home.class));
    	finish();
    }
    
	protected void goToHome() {	
		if (isTracking) {
	    	// we send a broadcast to inform all registered services to stop tracking 
	    	Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
	    	sendBroadcast(intent);			
	    	// need to get sure, that the database is up to date
	    	DataHelper dataHelper = new DataHelper(this);
	    	dataHelper.stopTracking(currentTrackId);
			final DatabaseHelper dbHelper;
		    dbHelper = new DatabaseHelper(getBaseContext());
	    	LayoutInflater layoutInflater = LayoutInflater.from(this);
 			View promptView = layoutInflater.inflate(R.layout.edit_description, null);
		    final EditText descriptionInput = (EditText) promptView.findViewById(R.id.descriptionInput);
		    
 			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
 			alertDialogBuilder.setView(promptView);
        	 alertDialogBuilder
			 .setCancelable(false)
			 .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface dialog, int id) {
				 String track_description = (descriptionInput.getText()).toString();
			     ContentValues values = new ContentValues();
			     values.put(Schema.COL_DESCRIPTION, track_description);
				 long updateId =dbHelper.getWritableDatabase().update(Schema.TBL_TRACK,values, "_id "+"="+String.valueOf(currentTrackId), null);
				 goToHome_aux(updateId);
			 //    cursor.close();
			   }
			 })
			 .setNegativeButton("Cancel",
			         new DialogInterface.OnClickListener() {
			         public void onClick(DialogInterface dialog, int id) {
			         dialog.cancel();
			     }
			 });
			AlertDialog alertD = alertDialogBuilder.create();
			  alertD.show();
		}
		else {
		     final DatabaseHelper dbHelper;

			 dbHelper = new DatabaseHelper(getBaseContext());
			 String[] allColumns = {Schema.COL_ID,Schema.COL_IS_VOTED};
		  	 final Cursor cursor = dbHelper.getWritableDatabase().query(Schema.TBL_TRACK,
				        allColumns, null, null,
				        null, null, null);
			 cursor.moveToFirst();
	         if (cursor.getCount()!=0){
	        	 
	        	 if (cursor.getInt(cursor.getColumnIndex(Schema.COL_IS_VOTED))==0)
	        	 {
		    	try{
			    	CustomDialogClass cdd=new CustomDialogClass(DisplayTrackMap.this);
    				cdd.show();  
	   
	       		  
		    	}
		    	catch (Exception e){
			    	Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_LONG).show();		
	    		}
	    	}
	         }
	         cursor.close();
		}
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_ZOOM, osmView.getZoomLevel());
		outState.putInt(CURRENT_SCROLL_X, osmView.getScrollX());
		outState.putInt(CURRENT_SCROLL_Y, osmView.getScrollY());
		outState.putBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos);
		outState.putBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready);
			// Save the fact that we are currently tracking or not
		if(gpsLogger != null){
			outState.putBoolean(STATE_IS_TRACKING, gpsLogger.isTracking());
		}
		super.onSaveInstanceState(outState);
	}


	@Override
	protected void onResume() {		
		// setKeepScreenOn depending on user's preferences
		osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
		
		// Register content observer for any trackpoint changes
		getContentResolver().registerContentObserver(
				TrackContentProvider.trackPointsUri(currentTrackId),
				true, trackpointContentObserver);
		

		createOverlays();
			// Check GPS status
			if (checkGPSFlag
					&& prefs.getBoolean(OSMTracker.Preferences.KEY_GPS_CHECKSTARTUP,
							OSMTracker.Preferences.VAL_GPS_CHECKSTARTUP)) {
				checkGPSProvider();
			}

			if(!isTracking) {
				Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
		    	sendBroadcast(intent);
		    	}
	    // Forget the last waypoint read from the DB
		// This ensures that all waypoints for the track will be reloaded 
        // from the database to populate the path layout
        lastTrackPointIdProcessed = null;
		
        // Reload path
        pathChanged();
        //create or refresh the overlay of Articles
		updateArticlesoverlay();

        // Refresh way points
         wayPointsOverlay.refresh();
         

			
		super.onResume();
		
	}
	
	@Override
	protected void onPause() {
		// Unregister content observer
		getContentResolver().unregisterContentObserver(trackpointContentObserver);
		isReady=0;
		// Clear the points list.
		pathOverlay.clearPath();
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		// Save zoom level in shared preferences
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(LAST_ZOOM, osmView.getZoomLevel());
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.displaytrackmap_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.displaytrackmap_menu_center_to_gps).setEnabled( (!centerToGpsPos && currentPosition != null ) );
		return super.onPrepareOptionsMenu(menu);
	}

	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.displaytrackmap_menu_center_to_gps:
			centerToGpsPos = true;
			if(currentPosition != null){
				osmViewController.animateTo(currentPosition);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
			case MotionEvent.ACTION_MOVE:
				if (currentPosition != null)
					centerToGpsPos = false;
				break;
		}
		return super.onTouchEvent(event);
	}


	/**
	 * Creates overlays over the OSM view
	 */
	private void createOverlays() {

		pathOverlay = new PathOverlay(Color.BLUE, this);
	//	pathOverlay.
		osmView.getOverlays().add(pathOverlay);

    //    osmView.set
		myLocationOverlay = new SimpleLocationOverlay(this);
		osmView.getOverlays().add(myLocationOverlay);

		
		wayPointsOverlay = new WayPointsOverlay(this, currentTrackId);
		osmView.getOverlays().add(wayPointsOverlay);

		
	}
	/**
	 * Create Wikipedia articles icons overlay over the OSM view
	 */
	private void updateArticlesoverlay() {
			//It's not sure that osmView.getWidth() and osmView.getHeight() are not null
	    	if (osmView.getWidth()!=0 && (osmView.getHeight()!=0))
	    	{
	    	//get the GPS coordinates of map's corners	
			supLat = String.valueOf((osmView.getProjection().fromPixels(0,0)).getLatitudeE6()/1E6);
			supLon = String.valueOf((osmView.getProjection().fromPixels(0,0)).getLongitudeE6()/1E6);
			infLat = String.valueOf((osmView.getProjection().fromPixels(osmView.getWidth(),osmView.getHeight())).getLatitudeE6()/1E6);
			infLon = String.valueOf((osmView.getProjection().fromPixels(osmView.getWidth(),osmView.getHeight())).getLongitudeE6()/1E6);
	        //The thread is used to get the JSON that contain Wikipedia	articles
			new Thread(new Runnable() {
	   	    		@Override
	   	    		public void run(){
	   			     String resultUrl = "http://tools.wmflabs.org/wikijourney/getArticles.php"+"?lat_sup="+supLat+"&lon_sup="+supLon+"&lat_inf="+infLat+"&lon_inf="+infLon+"&lang="+Locale.getDefault().getLanguage()+"&maxRows=10";
	   			     InputStream is = null;
	   			     StringBuilder sb=null;
	   			         try{
	   			             HttpClient httpclient = new DefaultHttpClient();
	   			             HttpGet httpget = new HttpGet(resultUrl);
	   			             HttpResponse response = httpclient.execute(httpget);  			             
	   			             HttpEntity entity = response.getEntity();
	   			             is = entity.getContent();
	   			 	            try{
	   			 	            	
	   			 	                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8);
	   			 	                sb = new StringBuilder();
	   			 	                String line=null;
	   			 	                while ((line = reader.readLine()) != null) {
	   			 	                    sb.append(line + "\n");
	   			 		               
	   			 	                resultGettingWikiArticles=sb.toString();
	   			 	                }
	   			 	                is.close();   			 	       
	   			 	            }catch(Exception e){
	   			 	                Log.e("log_tag", "Error converting result "+e.toString());
	   			 	            }
	   			            
	   			         }catch(Exception e){
	   			             Log.e("log_tag", "Error in http connection"+e.toString());

	   			         }

	   			        	   handler.sendEmptyMessage(0);

	       			}
	   	    	}).start(); 
		     }
	}

	/**
	 * On track path changed, update the two overlays and repaint view.
	 * If {@link #lastTrackPointIdProcessed} is null, this is the initial call
	 * from {@link #onResume()}, and not the periodic call from
	 * {@link ContentObserver#onChange(boolean) trackpointContentObserver.onChange(boolean)}
	 * while recording.
	 */
	private void pathChanged() {
		if (isFinishing()) {
			return;
		}
		
		// See if the track is active.
		// If not, we'll calculate initial track bounds
		// while retrieving from the database.
		// (the first point will overwrite these lat/lon bounds.)
		boolean doInitialBoundsCalc = false;
		double minLat = 91.0, minLon = 181.0;
		 double maxLat = -91.0;
	    double maxLon = -181.0;
		if ((! zoomedToTrackAlready) && (lastTrackPointIdProcessed == null)) {
			final String[] proj_active = {Schema.COL_ACTIVE};
			Cursor cursor = getContentResolver().query(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, currentTrackId),
				proj_active, null, null, null);
			if (cursor.moveToFirst()) {
				doInitialBoundsCalc =
					(cursor.getInt(cursor.getColumnIndex(Schema.COL_ACTIVE)) == Schema.VAL_TRACK_INACTIVE);
			}
			cursor.close();
		}
		
		// Projection: The columns to retrieve. Here, we want the latitude, 
		// longitude and primary key only
		String[] projection = {Schema.COL_LATITUDE, Schema.COL_LONGITUDE, Schema.COL_ID};
		// Selection: The where clause to use
		String selection = null;
		// SelectionArgs: The parameter replacements to use for the '?' in the selection		
		String[] selectionArgs = null;
		
        // Only request the track points that we have not seen yet
		// If we have processed any track points in this session then
		// lastTrackPointIdProcessed will not be null. We only want 
		// to see data from rows with a primary key greater than lastTrackPointIdProcessed  
		if (lastTrackPointIdProcessed != null) {
		    selection = TrackContentProvider.Schema.COL_ID + " > ?";
		    List<String> selectionArgsList  = new ArrayList<String>();
		    selectionArgsList.add(lastTrackPointIdProcessed.toString());
		    
		    selectionArgs = selectionArgsList.toArray(new String[1]); 
		}

		// Retrieve any points we have not yet seen
		Cursor c = getContentResolver().query(
				TrackContentProvider.trackPointsUri(currentTrackId),
				projection, selection, selectionArgs, Schema.COL_ID + " asc");
		
		int numberOfPointsRetrieved = c.getCount();		
        if (numberOfPointsRetrieved > 0 ) {        
            c.moveToFirst();
			double lastLat = 0;
			double lastLon = 0;
	        int primaryKeyColumnIndex = c.getColumnIndex(Schema.COL_ID);
	        int latitudeColumnIndex = c.getColumnIndex(Schema.COL_LATITUDE);
	        int longitudeColumnIndex = c.getColumnIndex(Schema.COL_LONGITUDE);
		
			// Add each new point to the track
			while(!c.isAfterLast()) {			
				lastLat = c.getDouble(latitudeColumnIndex);
				lastLon = c.getDouble(longitudeColumnIndex);
				lastTrackPointIdProcessed = c.getInt(primaryKeyColumnIndex);
				pathOverlay.addPoint((int)(lastLat * 1e6), (int)(lastLon * 1e6));
				if (doInitialBoundsCalc) {
					if (lastLat < minLat)  minLat = lastLat;
					if (lastLon < minLon)  minLon = lastLon;
					if (lastLat > maxLat)  maxLat = lastLat;
					if (lastLon > maxLon)  maxLon = lastLon;

				}

				c.moveToNext();
			}		

			// Last point is current position.
			currentPosition = new GeoPoint(lastLat, lastLon); 
			myLocationOverlay.setLocation(currentPosition);		
			if(centerToGpsPos) {
				osmViewController.setCenter(currentPosition);
			}
		
			// Repaint
			osmView.invalidate();
			if (doInitialBoundsCalc && (numberOfPointsRetrieved > 1)) {
				// osmdroid-3.0.8 hangs if we directly call zoomToSpan during initial onResume,
				// so post a Runnable instead for after it's done initializing.
		    	final double north = maxLat, east = maxLon, south = minLat, west = minLon;

				
		    	osmView.post(new Runnable() {
					@Override
					public void run() {
						osmViewController.zoomToSpan(new BoundingBoxE6(north, east, south, west));
						osmViewController.setCenter(new GeoPoint((north + south) / 2, (east + west) / 2));
						zoomedToTrackAlready = true;

					}
				});
			}
		}
		c.close();
		
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
/**
 * Sets the active track
 * calls {@link stopActiveTrack()} to stop all currently 
 * @param trackId ID of the track to activate
 */
private void setActiveTrack(){


	
	// set the track active
	ContentValues values = new ContentValues();
	values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);
	getContentResolver().update(TrackContentProvider.CONTENT_URI_TRACK, values, Schema.COL_ID + " = ?", new String[] {Long.toString(currentTrackId)});
}

/**
 * Stops the active track
 * Sends a broadcast to be received by GPSLogger to stop logging
 * and forces the DataHelper to stop tracking.
 */
private void stopActiveTrack(){
		// we send a broadcast to inform all registered services to stop tracking 
		Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
		sendBroadcast(intent);
		
		// need to get sure, that the database is up to date
		DataHelper dataHelper = new DataHelper(this);
		dataHelper.stopTracking(currentTrackId);

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
 * Getter for gpsLogger
 * 
 * @return Activity {@link GPSLogger}
 */
public GPSLogger getGpsLogger() {
	return gpsLogger;
}


public long getCurrentTrackId() {
	return this.currentTrackId;
}
@Override
protected void onNewIntent(Intent newIntent) {
	if (newIntent.getExtras() != null && newIntent.getExtras().containsKey(Schema.COL_TRACK_ID)) {
		currentTrackId = newIntent.getExtras().getLong(Schema.COL_TRACK_ID);
		setIntent(newIntent);
	}
	super.onNewIntent(newIntent);
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
			startActivityForResult(cameraIntent, this.REQCODE_IMAGE_CAPTURE);
		} else {
			Toast.makeText(getBaseContext(), 
					getResources().getString(R.string.error_externalstorage_not_writable),
					Toast.LENGTH_SHORT).show();
		}
	}
	
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



private class CustomDialogClass extends Dialog implements
android.view.View.OnClickListener {

	public Activity c;
	public Dialog d;
	ImageButton yes;
	public ImageButton no;

	public CustomDialogClass(Activity a) {
		super(a);
		// TODO Auto-generated constructor stub
		this.c = a;
	}

	@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
requestWindowFeature(Window.FEATURE_NO_TITLE);
setContentView(R.layout.vote);
yes = (ImageButton) findViewById(R.id.yesVoteButton);
no = (ImageButton) findViewById(R.id.noVoteButton);
yes.setOnClickListener(this);
no.setOnClickListener(this);

}
public void setVoteUrl(int x)
{
		final DatabaseHelper dbHelper = new DatabaseHelper(getBaseContext());
		    String[] allColumns = {Schema.COL_ID,Schema.COL_DATABASE_ID};
		  	final Cursor cursor = dbHelper.getWritableDatabase().query(Schema.TBL_USER,
				        allColumns, null, null,
				        null, null, null);
			cursor.moveToFirst();
			String idAuthor = String.valueOf(cursor.getLong(cursor.getColumnIndex(Schema.COL_DATABASE_ID)));
			cursor.close();
			ContentResolver cr = getContentResolver();
			final Cursor cursor2 = cr.query(
					ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, currentTrackId),
					null, null, null, null);
			cursor2.moveToFirst();
			String traceId =String.valueOf(cursor2.getLong(cursor2.getColumnIndex(Schema.COL_DATABASE_ID)));
			cursor2.close();
	
	     	   resultVoteUrl ="http://tools.wmflabs.org/wikijourney/vote.php?voteType="+String.valueOf(x)+"&traceid="+traceId+"&userid="+ idAuthor;
			
			
}
@Override
public void onClick(View v) {
	switch (v.getId()) {
		case R.id.yesVoteButton:
				vote = 1;
	 			
	        	   new Thread(new Runnable() {
		   	    		@Override
		   	    		public void run(){
		   	    			resultVoteUrl="url"; 
		   	    			setVoteUrl(vote);

		   	    			// 		  private String result;

		   	    	      try{
		   	    	          HttpClient httpclient = new DefaultHttpClient();
		   	    	          HttpGet httpget = new HttpGet(resultVoteUrl);
		   	    	          HttpResponse response = httpclient.execute(httpget);
		   	    	          
		   	    	          HttpEntity entity = response.getEntity();
		   	    	          InputStream is = entity.getContent();
		   	    		            //convert response to string
		   	    		            try{
		   	    		            	
		   	    		                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8);
		   	    		                StringBuilder sb = new StringBuilder();
		   	    		              //  sb.append(reader.readLine() + "\n");
		   	    		                String line=null;
		   	    		                while ((line = reader.readLine()) != null) {
		   	    		                    sb.append(line + "\n");
		   	    			                
		   	    			                resultVote=sb.toString();
		   	    		                }
		   	    		                is.close();

		   	    		       
		   	    		            }catch(Exception e){
		   	    		                Log.e("log_tag", "Error converting result "+e.toString());
		   	    		    			resultVote=e.getMessage();
		   	    		            }
		   	    	         
		   	    	      }catch(Exception e){
		   	    	          Log.e("log_tag", "Error in http connection"+e.toString());
		   	    	          resultVote="error2";
		   	    	      }
		   	    	  }
		   	    		
	        	   }).start();
	        	   handler.sendEmptyMessage(1);
				break;
		case R.id.noVoteButton:
			    vote =0 ;
	 			
	        	   new Thread(new Runnable() {
		   	    		@Override
		   	    		public void run(){
		   	    			setVoteUrl(vote);
		   	    		  // 		  private String result;

		   	    	      try{
		   	    	          HttpClient httpclient = new DefaultHttpClient();
		   	    	          HttpGet httpget = new HttpGet(resultVoteUrl);
		   	    	          HttpResponse response = httpclient.execute(httpget);
		   	    	          
		   	    	          HttpEntity entity = response.getEntity();
		   	    	          InputStream is = entity.getContent();
		   	    		            //convert response to string
		   	    		            try{
		   	    		            	
		   	    		                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8);
		   	    		                StringBuilder sb = new StringBuilder();
		   	    		              //  sb.append(reader.readLine() + "\n");
		   	    		                String line=null;
		   	    		                while ((line = reader.readLine()) != null) {
		   	    		                    sb.append(line + "\n");
		   	    			                
		   	    			                resultVote=sb.toString();
		   	    		                }
		   	    		                is.close();

		   	    		       
		   	    		            }catch(Exception e){
		   	    		                Log.e("log_tag", "Error converting result "+e.toString());
		   	    		    			resultVote=e.getMessage();
		   	    		            }
		   	    	         
		   	    	      }catch(Exception e){
		   	    	          Log.e("log_tag", "Error in http connection"+e.toString());
		   	    	          resultVote="error2";
		   	    	      }
		   	    	  }
		   	    		
	        	   }).start();
	        	   handler.sendEmptyMessage(1);
			    break;
		default:
			break;
	}
    dismiss();
}

}


}