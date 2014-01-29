package ab.android.wanderwiki.activity; 
  
  
import wanderwiki.android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
public class AdvancedResearchTrack extends Activity { 
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
        LinearLayout l = new LinearLayout(this); 
        
        setContentView(R.layout.advanced_research); 
        final Button SearchButton = (Button) findViewById(R.id.search_button); 
        //final Button DownloadTrackButton = (Button) findViewById(R.id.download_track_button); 
        //final Button TracksButton = (Button) findViewById(R.id.tracks_button); 
        final EditText KeywordEditText = (EditText) findViewById(R.id.trackName); 
        final EditText DistanceMaxEditText = (EditText) findViewById(R.id.search_track_length_max); 
        final EditText DistanceMinEditText = (EditText) findViewById(R.id.search_track_length_min); 
        final EditText DurationMaxEditText = (EditText) findViewById(R.id.search_track_duration_min); 
        final EditText DurationMinEditText = (EditText) findViewById(R.id.search_track_duration_max); 
        //Référence à définir dans le layout 
          SearchButton.setOnClickListener(new OnClickListener() { 
                      
          @Override
          public void onClick(View v) { 
            Intent intent = new Intent(AdvancedResearchTrack.this, SearchTrackResults.class); 
            intent.putExtra("FROM",0); 
            intent.putExtra("KEYWORD",KeywordEditText.getText().toString()); 
            intent.putExtra("LENGTH_MAX",DistanceMaxEditText.getText().toString()); 
            intent.putExtra("LENGTH_MIN",DistanceMinEditText.getText().toString()); 
            intent.putExtra("DURATION_MAX",DurationMaxEditText.getText().toString()); 
            intent.putExtra("DURATION_MIN",DurationMinEditText.getText().toString()); 
            startActivity(intent); 
            } 
        }); 
      
        //final Button AdvancedResearchButton = (Button) findViewById(R.id.advanced_research_button); 
    //  AdvancedResearchButton.setOnClickListener(new OnClickListener() {            
    //        @Override 
    //        public void onClick(View v) { 
    //          Intent intent = new Intent(SearchTrack.this, AdvancedResearchTrack.class); 
    //          startActivity(intent); 
    //          } 
    //$         }); 
  
    } 
}