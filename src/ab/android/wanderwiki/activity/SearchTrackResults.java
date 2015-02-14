package ab.android.wanderwiki.activity; 
  
import wanderwiki.android.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
public class SearchTrackResults extends Activity{ 
      
    Context c; 
    String result ="error"; 
    InputStream is = null; 
    StringBuilder sb=null; 
    String keyword = ""; 
    String lengthMax=""; 
    String lengthMin=""; 
    String durationMax=""; 
    String durationMin=""; 
    String resultUrl ="http://tools.wmflabs.org/wikijourney/"; 
    Double lat; 
    Double lon; 
    private ProgressDialog waitingDialog1; 
    private StableArrayAdapter adapter; 
    private Handler handler = new Handler(){ 
        public void handleMessage(android.os.Message msg){ 
  
            if (msg.what == 0) 
            { 
                  
                  try{ 
                      final JSONArray jArray = new JSONArray(result); 
                      JSONObject json_data=null; 
                      final ListView listview = (ListView) findViewById(R.id.listview); 
                      final ArrayList<String> list = new ArrayList<String>(); 
                        
                      for(int i=0;i<jArray.length();i++){ 
                              json_data = jArray.getJSONObject(i); 
                              list.add(json_data.getString("title"));        
                      } 
                          setArrayAdapter(list); 
                          listview.setAdapter(adapter); 
                          listview.setOnItemClickListener(new AdapterView.OnItemClickListener() { 
  
                              @Override
                              public void onItemClick(AdapterView<?> parent,  final View view, int position, long id) { 
                                  
                            Intent intent = new Intent(getApplicationContext(), DownloadTrack.class); 
                                try { 
                                    intent.putExtra("TRACK_ID_IN_DATABASE",jArray.getJSONObject(position).getString("id")); 
                                } catch (JSONException e1) { 
                                    // TODO Auto-generated catch block 
                                Toast.makeText(getBaseContext(), e1.getMessage(), Toast.LENGTH_LONG).show(); 
                                } 
                                try { 
                                    intent.putExtra("TRACK_NAME",jArray.getJSONObject(position).getString("title")); 
                                    intent.putExtra("AUTHOR",jArray.getJSONObject(position).getString("pseudo")); 
                                    intent.putExtra("LENGTH",jArray.getJSONObject(position).getString("length"));        
                                    intent.putExtra("DESCRIPTION",jArray.getJSONObject(position).getString("description"));  
                                    intent.putExtra("VOTE_POS", jArray.getJSONObject(position).getString("votePos"));
                                    intent.putExtra("VOTE_NEG", jArray.getJSONObject(position).getString("voteNeg"));
                                    
                                    
                                } catch (JSONException e) { 
                                    // TODO Auto-generated catch block 
                                    e.printStackTrace(); 
                                      
                                } 
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                            startActivity(intent); 
                            //openDownloadTrack(); 
                            /// finish(); 
                              } 
  
                            }); 
  
                      }catch(JSONException e1){ 
                      //    Toast.makeText(getBaseContext(), "No Track Found", Toast.LENGTH_LONG).show(); 
                            Toast.makeText(getBaseContext(), getResources().getString(R.string.msg_no_track_found), Toast.LENGTH_LONG).show();   
                    //   Toast.makeText(getBaseContext(), result + keyword, Toast.LENGTH_LONG).show(); 
                      }catch (ParseException e1){ 
                          e1.printStackTrace(); 
                      } 
                      final Button download_helping_button = (Button) findViewById(R.id.download_helping_button); 
                      download_helping_button.setOnClickListener(new OnClickListener() { 
                          
                          @Override
                          public void onClick(View v) { 
                            Intent intent = new Intent(SearchTrackResults.this, DownloadTrack.class); 
                            startActivity(intent); 
                            } 
                        }); 
              waitingDialog1.dismiss(); 
            } 
        }; 
    }; 
    @SuppressWarnings("unchecked") 
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.search_track_results);   
          
        c=this; 
        Bundle extras = getIntent().getExtras(); 
        if (extras != null) { 
            keyword = extras.getString("KEYWORD"); 
            lengthMax= extras.getString("LENGTH_MAX"); 
          //  lengthMin= extras.getString("LENGTH_MIN"); 
         //   durationMax= extras.getString("DURATION_MAX"); 
         //   durationMin= extras.getString("DURATION_MIN"); 
        } 
        //_getLocation(); 
		resultUrl = "http://tools.wmflabs.org/wikijourney/search.php"
		+"?content=" + keyword 
		+"&limdist="+lengthMax
		+"&sort=1"
		+"&searchType=4";
		
    //  getListView().setEmptyView(findViewById(R.id.trackmgr_empty)); 
    //  registerForContextMenu(getListView()); 
    //  if (savedInstanceState != null) { 
    //      prevItemVisible = savedInstanceState.getInt(PREV_VISIBLE, -1); 
    //  } 
      
     //   Toast.makeText(getBaseContext(),resultUrl  , Toast.LENGTH_LONG).show(); 
        waitingDialog1 = ProgressDialog.show(SearchTrackResults.this, "", getResources().getString(R.string.searching_tracks), true); 
        new Thread(new Runnable() { 
            @Override
            public void run(){ 
  
                    try{ 
                          HttpClient httpclient = new DefaultHttpClient(); 
                          HttpGet httpget = new HttpGet(resultUrl); 
                          HttpResponse response = httpclient.execute(httpget); 
                            
                          HttpEntity entity = response.getEntity(); 
                          is = entity.getContent(); 
                            //convert response to string 
                            try{ 
                                  
                                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8); 
                                sb = new StringBuilder(); 
                              //  sb.append(reader.readLine() + "\n"); 
                                String line=null; 
                                while ((line = reader.readLine()) != null) { 
                                    sb.append(line + "\n"); 
                                      
                                    result=sb.toString(); 
                                } 
                                is.close(); 
  
                         
                            }catch(Exception e){ 
                                Log.e("log_tag", "Error converting result "+e.toString()); 
                                result=e.getMessage(); 
                            } 
                           
                      }catch(Exception e){ 
                          Log.e("log_tag", "Error in http connection"+e.toString()); 
                          result="error2"; 
                      } 
                    handler.sendEmptyMessage(0); 
            } 
        }).start(); 
  
     //   Toast.makeText(getBaseContext(),result  , Toast.LENGTH_LONG).show(); 
  
        //paring data 
    
    } 
  
      
  
      protected void setArrayAdapter(ArrayList<String> _list) { 
         adapter = new StableArrayAdapter(this,android.R.layout.simple_list_item_1, _list); 
          
    } 
  
  
  
    private class StableArrayAdapter extends ArrayAdapter<String> { 
  
            HashMap<String, Integer> mIdMap = new HashMap<String, Integer>(); 
  
            public StableArrayAdapter(Context context, int textViewResourceId, 
                List<String> objects) { 
              super(context, textViewResourceId, objects); 
              for (int i = 0; i < objects.size(); ++i) { 
                mIdMap.put(objects.get(i), i); 
              } 
            } 
  
            @Override
            public long getItemId(int position) { 
              String item = getItem(position); 
              return mIdMap.get(item); 
            } 
  
            @Override
            public boolean hasStableIds() { 
              return true; 
            } 
  
          } 
      private class Connection extends AsyncTask { 
            //http post 
                
  
                      @Override
                      protected String doInBackground(Object... arg0) { 
                        
                        return result; 
                      } 
                  } 
  
      private void _getLocation() { 
            // Get the location manager 
            LocationManager locationManager = (LocationManager)  
                    getSystemService(LOCATION_SERVICE); 
            Criteria criteria = new Criteria(); 
            String bestProvider = locationManager.getBestProvider(criteria, false); 
            Location location = locationManager.getLastKnownLocation(bestProvider); 
            LocationListener loc_listener = new LocationListener() { 
  
                public void onLocationChanged(Location l) {} 
  
                public void onProviderEnabled(String p) {} 
  
                public void onProviderDisabled(String p) {} 
  
                public void onStatusChanged(String p, int status, Bundle extras) {} 
            }; 
            locationManager 
                    .requestLocationUpdates(bestProvider, 0, 0, loc_listener); 
            location = locationManager.getLastKnownLocation(bestProvider); 
            try { 
                lat = location.getLatitude(); 
                lon = location.getLongitude(); 
            } catch (NullPointerException e) { 
                lat = -1.0; 
                lon = -1.0; 
            } 
        } 
  
} 