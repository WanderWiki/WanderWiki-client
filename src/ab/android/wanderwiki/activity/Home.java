package ab.android.wanderwiki.activity;


import wanderwiki.android.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.activity.TrackLogger;
import me.guillaumin.android.osmtracker.activity.TrackManager;
import me.guillaumin.android.osmtracker.db.DatabaseHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.exception.CreateTrackException;

/**
 * The first activity to appear. 
 * @author Ayoub Belhadji
 *
 */
public class Home extends Activity {
	/**
	 * Variables used in first time use to create an account or check Gmail.
	 */
	private String pseudonym;
	private String gmail;
	private LayoutInflater layoutInflater;
	private LayoutInflater layoutInflater2;
	private View promptView;
	private View promptView2;
	private EditText checkGmailInput;
	private AlertDialog.Builder alertDialogBuilder;
	private AlertDialog.Builder alertDialogBuilder2;
	protected DatabaseHelper dbHelper;
	private ProgressDialog waitingDialog1;
	private ProgressDialog waitingDialog2;
	private AlertDialog alertD;
	/** Variables that will contain results from server during the creation of a new account or checking Gmail.*/
	private String resultGetId ="";
	private String resultAddUser ="";
	/** Variables used in creating new track*/
	private String newTrackName;
	private String authorName;

	/** Constant used if no track is active (-1)*/
	private static final long TRACK_ID_NO_TRACK = -1;
	
	/** The active track being recorded, if any, or {@link TRACK_ID_NO_TRACK}; value is updated in {@link #onResume()} */
	private long currentTrackId = TRACK_ID_NO_TRACK;
	/** The handler is used to manage the appearance of creating/checking account dialogs during the first use */
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg){
		     dbHelper = new DatabaseHelper(getBaseContext());
		     /**A dialog will appear in the case of new inscription, in witch the user will choose a pseudonyme */
		    if (msg.what == 1)
			{
			    final EditText pseudonymInput = (EditText) promptView2.findViewById(R.id.pseudonymInput);
			     pseudonym = (pseudonymInput.getText()).toString();
				     final ContentValues values = new ContentValues();
				     values.put(Schema.COL_PSEUDONYM, pseudonym);
			   	     values.put(Schema.COL_GMAIL, gmail);
						if (resultAddUser.contains("success"))
						{
						 String[] msgArr;
						 msgArr = resultAddUser.split("/");
						 values.put(Schema.COL_DATABASE_ID, msgArr[1]);
					     long insertId =dbHelper.getWritableDatabase().insert(Schema.TBL_USER, null,values);
						 Toast.makeText(getBaseContext(),getResources().getString(R.string.msg_account_created), Toast.LENGTH_LONG).show();	
						 waitingDialog2.dismiss();
					    }
						else if (resultAddUser.contains("error"))
						{
					     Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_error_server), Toast.LENGTH_LONG).show();	
						}
			}
		     /**A dialog will appear in the case of first use, so as to verify the Gmail address of the user */
			if (msg.what == 0)
			{
				try{
		   		  if (resultGetId.contains("Need to create new account"))
	 			  {
					    waitingDialog2 = ProgressDialog.show(Home.this, "", getResources().getString(R.string.creating_acount), true);
						alertDialogBuilder2.setView(promptView2);
						 alertDialogBuilder2
						 .setCancelable(false)
						 .setPositiveButton("OK", new DialogInterface.OnClickListener() {
							 public void onClick(DialogInterface dialog, int id) {
				   	     doSlowlyTask(1);
					 }   
				    });
					AlertDialog alertD2 = alertDialogBuilder2.create();
					  alertD2.show();
			      }
		   		  else if (resultGetId.contains("success"))
				  {
					    String[] msgArr;
					    msgArr = resultGetId.split("/");
					    final ContentValues values = new ContentValues();
					    values.put(Schema.COL_DATABASE_ID, msgArr[1]);		  
					    pseudonym = msgArr[2];
					    values.put(Schema.COL_PSEUDONYM, pseudonym);
					    Date startDate = new Date();
					    values.put(Schema.COL_GMAIL, gmail);
					    long insertId =dbHelper.getWritableDatabase().insert(Schema.TBL_USER, null,values);
						Toast.makeText(getBaseContext(),getResources().getString(R.string.msg_account_checked), Toast.LENGTH_LONG).show();	
						 waitingDialog1.dismiss();
				  }
		   		  else 
				  {
					   Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_error_server), Toast.LENGTH_LONG).show();	
				 			  alertD.show();
				  }
			}catch(Exception e)
			{
				Toast.makeText(getBaseContext(),e.getMessage(), Toast.LENGTH_LONG).show();
			}
			}
		};
	};
	private void doSlowlyTask (int i){
		/**A task to check if Gmail address of the user exists already in the database*/
		if (i==0)
		{
	    	new Thread(new Runnable() {
	    		@Override
	    		public void run(){
			         String resultUrl = "http://tools.wmflabs.org/wanderwiki/idUser.php?email_adr="+gmail;
				     InputStream is;
			         StringBuilder sb;
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
		                       resultGetId=sb.toString();
		                   }
		                 is.close();
		 	    	    }catch(Exception e){
		 	             Log.e("log_tag", "Error converting result "+e.toString());
		 	            resultGetId=e.getMessage();
		 		     }            
		 		    }catch(Exception e){
		 	             Log.e("log_tag", "Error in http connection"+e.toString());
		 	            resultGetId="error2";
		 		    }
		        	  handler.sendEmptyMessage(0);
	    		}
	    	}).start();

		}
		if (i==1)
		{
        /**A task to create new account in database */
			 new Thread(new Runnable() {
		     @Override
	    		public void run(){
				    String resultUrl =  "http://tools.wmflabs.org/wanderwiki/createUser.php?email_adr="+gmail+"&pseudo="+pseudonym+"&security="+"0";
					InputStream is;
			        StringBuilder sb;
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
				               resultAddUser=sb.toString();
		                   }
			               is.close();
		 	    	    }catch(Exception e){
		 	    	    	Log.e("log_tag", "Error converting result "+e.toString());
					        resultAddUser=e.getMessage();
			 		     }            
			 		    }catch(Exception e){
			 		    	Log.e("log_tag", "Error in http connection"+e.toString());
					        resultAddUser="error2";
			 		    }
		            handler.sendEmptyMessage(1);
				    }
			}).start();
		}
				
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		final Button SearchButton = (Button) findViewById(R.id.search_button);
		final Button CreationButton = (Button) findViewById(R.id.creation_button);
		final Button TracksButton = (Button) findViewById(R.id.tracks_button);
		final Button AboutButton = (Button) findViewById(R.id.about);
		layoutInflater = LayoutInflater.from(this);
		layoutInflater2 = LayoutInflater.from(this);
		alertDialogBuilder2 = new AlertDialog.Builder(this);
		promptView2 = layoutInflater2.inflate(R.layout.new_user, null);
    	alertDialogBuilder = new AlertDialog.Builder(this);
		promptView = layoutInflater.inflate(R.layout.update_gmail, null);
		checkGmailInput = (EditText) promptView.findViewById(R.id.checkGmailInput);
		/** Get the Gmail address of user*/
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(this).getAccounts();
		int k =0;
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		    	if (k==0){
		    		gmail = account.name;
		    		checkGmailInput.setText(gmail);
		    	}
		        k=1;
		    }
		}
		/**Go to the activity of search*/
		SearchButton.setOnClickListener(new OnClickListener() {			
		  @Override
		  public void onClick(View v) {
			Intent intent = new Intent(Home.this, SearchTrack.class);
			startActivity(intent);
			}
		});
		/**Go to the activity that contain list of all tracks downloaded or created*/ 
		  TracksButton.setOnClickListener(new OnClickListener() {
				
			  @Override
			  public void onClick(View v) {
				Intent intent = new Intent(Home.this, TrackManager.class);
				startActivity(intent);
				}
			});
		  
		  
		  AboutButton.setOnClickListener(new OnClickListener() {
				
			  @Override
			  public void onClick(View v) {
				Intent intent = new Intent(Home.this, me.guillaumin.android.osmtracker.activity.About.class);
				startActivity(intent);
				}
			});
		  
		  /**Create a new Track*/
		  CreationButton.setOnClickListener(new OnClickListener() {
				
			  @Override
			  public void onClick(View v) {
				  createNewTrack();
				}
			});
	       /**Create Wanderwiki folder in external memory if doesn't exist*/ 	 
     	 File folder = new File(Environment.getExternalStorageDirectory() + "/wanderwiki");
     	 boolean success = true;
     	 if (!folder.exists()) {
     	     success = folder.mkdir();
     	     
         	 File folder2 = new File(Environment.getExternalStorageDirectory() + "/wanderwiki/download_tracks");
         	 boolean success2 = true;
         	 if (!folder2.exists()) {
         	     success2 = folder2.mkdir();
         	 }
         	 if (!success2) {
    			  Toast.makeText(getBaseContext(),getResources().getString(R.string.msg_error_dir_creation), Toast.LENGTH_LONG).show();	
         	 } 
     	 }
     	 if (!success) {
			  Toast.makeText(getBaseContext(),getResources().getString(R.string.msg_error_dir_creation), Toast.LENGTH_LONG).show();	
     	 } 

		  /**After setting button's listeners check if it's the first use*/
		  
			 final DatabaseHelper dbHelper = new DatabaseHelper(getBaseContext());
		     String[] allColumns = {Schema.COL_ID,Schema.COL_PSEUDONYM};
		  	 final Cursor cursor = dbHelper.getWritableDatabase().query(Schema.TBL_USER,
				        allColumns, null, null,
				        null, null, null);
			 cursor.moveToFirst();
	         if (false && cursor.getCount()==0){

	        	waitingDialog1 = ProgressDialog.show(Home.this, "",  getResources().getString(R.string.checking_gmail), true);
	  			alertDialogBuilder.setView(promptView);
	         	 alertDialogBuilder
	 			 .setCancelable(false)
	 			 .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	 				 public void onClick(DialogInterface dialog, int id) {
								        	 doSlowlyTask(0);
	 			   }
	 			 });
	 			 alertD = alertDialogBuilder.create();
	 			  alertD.show();
	 	
	         }
	         else {
	          authorName = "test";//cursor.getString(cursor.getColumnIndex(Schema.COL_PSEUDONYM));
	         }
	         cursor.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	/**This method is used to create a new track in the database of the application*/
	public void createNewTrack()
	{
		final Intent i = new Intent(this, TrackLogger.class);
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		View promptView = layoutInflater.inflate(R.layout.prompts, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(promptView);
		final EditText input = (EditText) promptView.findViewById(R.id.userInput);
		alertDialogBuilder
		 .setCancelable(false)
		 .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int id) {
			 newTrackName = (input.getText()).toString();
			 try {
				currentTrackId = createNewTrack_aux();
			} catch (CreateTrackException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
				// New track
				i.putExtra(Schema.COL_TRACK_ID, currentTrackId);
				i.putExtra(TrackLogger.STATE_IS_TRACKING, true);
				startActivity(i);
			 }
		 });
		AlertDialog alertD = alertDialogBuilder.create();
		  alertD.show();
	}
	/**
	 * Creates a new track, in DB and on SD card
	 * @returns The ID of the new track
	 * @throws CreateTrackException
	 */
	private long createNewTrack_aux() throws CreateTrackException {
		Date startDate = new Date();
		
		// Create entry in TRACK table
		final ContentValues values = new ContentValues();
	
		values.put(Schema.COL_NAME, newTrackName);
		values.put(Schema.COL_AUTHOR, authorName);
		values.put(Schema.COL_START_DATE, startDate.getTime());
		values.put(Schema.COL_ACTIVE, Schema.VAL_TRACK_ACTIVE);

		Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
		long trackId = ContentUris.parseId(trackUri);
		
		
		return trackId;

	}

}