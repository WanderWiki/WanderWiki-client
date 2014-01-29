package ab.android.wanderwiki.activity;

import wanderwiki.android.R;

import ab.android.wanderwiki.webviewclient.MyWebViewClient;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
/**
 * The activity showing the Wikipedia article
 * @author Ayoub Belhadji
 *
 */
public class DisplayArticle extends Activity {
	    private String wikipediaUrl="";
	    private WebView webView; 
		public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
		    wikipediaUrl = extras.getString("WIKIPEDIA_URL");
		}
		setContentView(R.layout.display_article);
		webView = (WebView) findViewById(R.id.webView);
		/**The MyWebViewClient class is created so as to prevent showing the article in the user's default navigator*/
		webView.setWebViewClient(new MyWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl(wikipediaUrl);
		}
		
	}