package ab.android.wanderwiki.overlay;
import wanderwiki.android.R;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapView;
import org.osmdroid.bonuspack.overlays.ExtendedOverlayItem;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import me.guillaumin.android.osmtracker.db.TrackContentProvider;

public class WikiArticlesOverlay extends ItemizedOverlay{

	/**
	 * List of waypoints to display on the map.
	 */
	
    public ArrayList<ExtendedOverlayItem> listItems;
    public List<ExtendedOverlayItem> wikiArticlesItems = new ArrayList<ExtendedOverlayItem>();
    private long trackId;
	private Context mContext;
	private JSONArray jArray = null;
	private ContentResolver pContentResolver;
	private int check=0;
	private MapView mapView;
	private ArrayList<GeoPoint> waypoints;

	private MapView map;
	public WikiArticlesOverlay(
			final Drawable pDefaultMarker,
			final Context pContext,
			final long _trackId
			)
	{
		super(pDefaultMarker, new DefaultResourceProxyImpl(pContext));
		this.trackId = _trackId;
		this.pContentResolver = pContext.getContentResolver();
        waypoints = new ArrayList<GeoPoint>();
        listItems = new ArrayList<ExtendedOverlayItem>();
		
	}
	
	public WikiArticlesOverlay(
			final Context pContext,
			final long trackId,
			final JSONArray JA,
			final MapView mapView
			)
	{
		this(pContext.getResources().getDrawable(R.drawable.w), pContext, trackId);
		jArray = new JSONArray();
		if (JA!=null) check = 1000;
		mContext =pContext;
		jArray=JA;
		map = mapView;
		refresh();
	}

	@Override
	public boolean onSnapToItem(final int pX, final int pY, final Point pSnapPoint, final IMapView pMapView) {
		// TODO Implement this!
		return false;
	}
	
	

	
	public void refresh() {

		Cursor c = this.pContentResolver.query(
				TrackContentProvider.waypointsUri(trackId),
				null, null, null, TrackContentProvider.Schema.COL_TIMESTAMP + " asc");
        JSONObject json_data=null;
	        

		ExtendedOverlayItem i;
		GeoPoint gP;
		try {
			for(int k=0;k<jArray.length();k++){
				json_data = jArray.getJSONObject(k);
			if (jArray!= null) check = 2000 ;
			gP=new GeoPoint(Double.valueOf(json_data.getString("lat")),Double.valueOf(json_data.getString("lon")));
			waypoints.add(gP);
			i = new ExtendedOverlayItem(
					"   "+json_data.getString("title"),
					json_data.getString("wikipediaUrl"),
					gP, mContext);
			i.setMarker(mContext.getResources().getDrawable(R.drawable.w));
			i.setRelatedObject(json_data.getString("wikipediaUrl"));
            //4. end
   

		wikiArticlesItems.add(i);
			}

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      
		c.close();
		populate();
	}


	@Override
	protected OverlayItem createItem(final int index) {
		return null;
		// TODO Auto-generated method stub
	
	}
	@Override
	public int size() {
		return wikiArticlesItems.size();
	}

}