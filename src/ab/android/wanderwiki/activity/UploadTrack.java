package ab.android.wanderwiki.activity;

import wanderwiki.android.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Pattern;

import com.android.wanderwiki.OSMTracker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import me.guillaumin.android.osmtracker.db.DataHelper;
import me.guillaumin.android.osmtracker.db.DatabaseHelper;
import me.guillaumin.android.osmtracker.db.TrackContentProvider;
import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.gpx.ExportToStorageTask;
/**
 * The activity of track uploading
 * @author Ayoub Belhadji
 *
 */
public class UploadTrack extends Activity {

    private int serverResponseCode = 0;
    private ProgressDialog dialog = null;  
    String upLoadServerUri = null;
	/**
	 * Characters to replace in track filename, for use by {@link #buildGPXFilename(Cursor)}. <BR>
	 * The characters are: (space) ' " / \ * ? ~ @ &lt; &gt; <BR>
	 * In addition, ':' will be replaced by ';', before calling this pattern.
	 */
	private final static Pattern FILENAME_CHARS_BLACKLIST_PATTERN =
		Pattern.compile("[ '\"/\\\\*?~@<>]");  // must double-escape \
	

	
    /**File Path */
    private String uploadFilePath;
    /**File Name*/
    private String uploadFileName;
   
    private long trackId;
    private ExportToStorageTask exportToStorageTask;
    private DatabaseHelper dbHelper;
    @Override
    public void onCreate(Bundle savedInstanceState) {
         
        super.onCreate(savedInstanceState);
	    dbHelper = new DatabaseHelper(getBaseContext());
        setContentView(R.layout.upload_track);
        trackId = getIntent().getExtras().getLong(Schema.COL_TRACK_ID);

		ContentResolver cr = getContentResolver();
		final Cursor cursor = cr.query(
			ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
			null, null, null, null);
		Uri uriss = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		cursor.moveToFirst();
		
		
		exportToStorage();
        upLoadServerUri = "http://tools.wmflabs.org/wikijourney/uploadTrack.php";
                dialog = ProgressDialog.show(UploadTrack.this, "", "Uploading file...", true);
                new Thread(new Runnable() {
                    public void run() {

                         uploadFile(uploadFilePath +'/'+ uploadFileName);
                                                  
                    }
                  }).start();   
                
                
          
    }
    /**
     * First, create a gpx file that contains track's data */  
    protected void exportToStorage() {
		Cursor c = this.getContentResolver().query(ContentUris.withAppendedId(
				TrackContentProvider.CONTENT_URI_TRACK, trackId), null, null,
				null, null);
		Date startDate = new Date();
		if (null != c && 1 <= c.getCount()) {
			c.moveToFirst();
			long startDateInMilliseconds = c.getLong(c.getColumnIndex(Schema.COL_START_DATE));
			startDate.setTime(startDateInMilliseconds);
		}

		String filenameBase = buildGPXFilename(c);
		c.close();
    	exportToStorageTask = new ExportToStorageTask(this, trackId);
    	exportToStorageTask.execute();
    	try{
    	uploadFilePath=Environment.getExternalStorageDirectory().getAbsolutePath() + "/wikijourney/download_tracks/"+filenameBase;
    	uploadFileName=filenameBase+DataHelper.EXTENSION_GPX;
    	}
    	catch(Exception e)
    	{
    	Toast.makeText(this,e.getMessage(), Toast.LENGTH_SHORT).show();
    	}
	}
   /**
    * Next step is uploading the gpx file to server*/
	public int uploadFile(String sourceFileUri) {
          String fileName = sourceFileUri;  
          HttpURLConnection conn = null;
          DataOutputStream dos = null;  
          String lineEnd = "\r\n";
          String twoHyphens = "--";
          String boundary = "*****";
          int bytesRead, bytesAvailable, bufferSize;
          byte[] buffer;
          int maxBufferSize = 1 * 1024 * 1024; 
          File sourceFile = new File(sourceFileUri); 
           
          if (!sourceFile.isFile()) {
               dialog.dismiss();  
               Log.e("uploadFile", "Source File not exist :"
                                   +uploadFilePath + "" + uploadFileName);
               return 0;
          }
          else
          {
               try { 
                    
                     // open a URL connection to the Servlet
                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
                   URL url = new URL(upLoadServerUri);
                    
                   // Open a HTTP  connection to  the URL
                   conn = (HttpURLConnection) url.openConnection(); 
                   conn.setDoInput(true); // Allow Inputs
                   conn.setDoOutput(true); // Allow Outputs
                   conn.setUseCaches(false); // Don't use a Cached Copy
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Connection", "Keep-Alive");
                   conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                   conn.setRequestProperty("uploaded_file", fileName); 
                   
                   dos = new DataOutputStream(conn.getOutputStream());      
                   dos.writeBytes(twoHyphens + boundary + lineEnd); 
                   dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                             + fileName + "" + lineEnd);
                    
                   dos.writeBytes(lineEnd);
       
                   // create a buffer of  maximum size
                   bytesAvailable = fileInputStream.available(); 
          
                   bufferSize = Math.min(bytesAvailable, maxBufferSize);
                   buffer = new byte[bufferSize];
          
                   // read file and write it into form...
                   bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
                      
                   while (bytesRead > 0) {
                        
                     dos.write(buffer, 0, bufferSize);
                     bytesAvailable = fileInputStream.available();
                     bufferSize = Math.min(bytesAvailable, maxBufferSize);
                     bytesRead = fileInputStream.read(buffer, 0, bufferSize);   
                      
                    }
          
                   // send multipart form data necesssary after file data...
                   dos.writeBytes(lineEnd);
                   dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
          
                   // Responses from the server (code and message)
                   serverResponseCode = conn.getResponseCode();
                   final String serverResponseMessage = conn.getResponseMessage();
                     
                   Log.i("uploadFile", "HTTP Response is : "
                           + serverResponseMessage + ": " + serverResponseCode);
                   final InputStream is = conn.getInputStream();
                   if(serverResponseCode == 200){
                        
                       runOnUiThread(new Runnable() {
                            public void run() {
                                
                                String msg = "error";
								try {
									msg = streamToString(is);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								String[] msgArr;
								if (msg !="error"){
									if (msg.contains("success")) 
									 {
								    	msgArr = msg.split("/");
                                        ContentValues values = new ContentValues();
               			                values.put(Schema.COL_IS_UPLOADED, 1);
               			                values.put(Schema.COL_DATABASE_ID, msgArr[1]);
                                        long insertId =dbHelper.getWritableDatabase().update(Schema.TBL_TRACK,values, "_id "+"="+String.valueOf(trackId), null);
                					    Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_upload_success), Toast.LENGTH_LONG).show();	
                						Intent intent = new Intent(UploadTrack.this, Home.class);
                						startActivity(intent);
                						finish();
									 }
								}
                            }
                        });                
                   }    
                    
                   //close the streams //
                   fileInputStream.close();
                   dos.flush();
                   dos.close();
                     
              } catch (MalformedURLException ex) {
                   
                  dialog.dismiss();  
                  ex.printStackTrace();
                   
                  runOnUiThread(new Runnable() {
                      public void run() {
                          Toast.makeText(UploadTrack.this, "MalformedURLException", 
                                                              Toast.LENGTH_SHORT).show();
                      }
                  });
                   
                  Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
              } catch (final Exception e) {
                   
                  dialog.dismiss();  
                  e.printStackTrace();
                   
                  runOnUiThread(new Runnable() {
                      public void run() {     
                          Toast.makeText(UploadTrack.this, e.getMessage(), 
                                  Toast.LENGTH_SHORT).show();
                      }
                  });
                  Log.e("Upload file to server Exception", "Exception : "
                                                   + e.getMessage(), e);  
              }
              dialog.dismiss();       
              return serverResponseCode; 
               
           } 
         } 
	public String buildGPXFilename(Cursor c) {
		// Build GPX filename from track info & preferences
		final String filenameOutput = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(
				OSMTracker.Preferences.KEY_OUTPUT_FILENAME,
				OSMTracker.Preferences.VAL_OUTPUT_FILENAME);
		StringBuffer filenameBase = new StringBuffer();
		final int colName = c.getColumnIndex(Schema.COL_NAME);
		if ((! c.isNull(colName))
			&& (! filenameOutput.equals(OSMTracker.Preferences.VAL_OUTPUT_FILENAME_DATE)))
		{
			final String tname_raw =
				c.getString(colName).trim().replace(':', ';');
			final String sanitized =
				FILENAME_CHARS_BLACKLIST_PATTERN.matcher(tname_raw).replaceAll("_");
			filenameBase.append(sanitized);
		}
		if ((filenameBase.length() == 0)
			|| ! filenameOutput.equals(OSMTracker.Preferences.VAL_OUTPUT_FILENAME_NAME))
		{
			final long startDate = c.getLong(c.getColumnIndex(Schema.COL_START_DATE));
			if (filenameBase.length() > 0)
				filenameBase.append('_');
			filenameBase.append(DataHelper.FILENAME_FORMATTER.format(new Date(startDate)));
		}
		//filenameBase.append(DataHelper.EXTENSION_GPX);
		return filenameBase.toString();
	}
	public  String streamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
