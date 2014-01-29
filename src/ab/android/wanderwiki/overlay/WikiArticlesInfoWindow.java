package ab.android.wanderwiki.overlay;

import org.osmdroid.bonuspack.overlays.DefaultInfoWindow;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.views.MapView;

import wanderwiki.android.R;

import ab.android.wanderwiki.activity.DisplayArticle;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
public class WikiArticlesInfoWindow extends DefaultInfoWindow {
	Button btn;
	String urlWikipedia;	
	protected Context mContext;
	public WikiArticlesInfoWindow(MapView mapView,Context context) {
		// TODO Auto-generated constructor stub
		super(R.layout.bonuspack_bubble,mapView);
		// TODO Auto-generated constructor stub
		mContext=context;
		btn = (Button)(mView.findViewById(R.id.bubble_moreinfo));
		btn.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View view) {
                //if (selectedPoi.mUrl != null){
                   Intent intent = new Intent(view.getContext(), DisplayArticle.class);
            	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	    intent.putExtra("WIKIPEDIA_URL",urlWikipedia);
            	    view.getContext().startActivity(intent);
                //	 Toast.makeText(view.getContext(),selectedPoi, Toast.LENGTH_LONG).show();
          //  }
         }
	});
	}
	@Override public void onOpen(Object item){
        super.onOpen(item);
        ExtendedOverlayItem eItem = (ExtendedOverlayItem)item;
        urlWikipedia = (String)eItem.getRelatedObject();
	}

}
