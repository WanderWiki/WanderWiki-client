package me.guillaumin.android.osmtracker.db;

import wanderwiki.android.R;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import me.guillaumin.android.osmtracker.db.TrackContentProvider.Schema;
import me.guillaumin.android.osmtracker.db.model.Track;
/**
 * Adapter for track list in {@link me.guillaumin.android.osmtracker.activity.TrackManager Track Manager}.
 * For each row's contents, see <tt>tracklist_item.xml</tt>.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class TracklistAdapter extends CursorAdapter {

	public TracklistAdapter(Context context, Cursor c) {
		super(context, c);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		bind(cursor, view, context);	
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup vg) {
		View view = LayoutInflater.from(vg.getContext()).inflate(R.layout.tracklist_item,
				vg, false);
		return view;
	}
	
	/**
	 * Do the binding between data and item view.
	 * 
	 * @param cursor
	 *            Cursor to pull data
	 * @param v
	 *            RelativeView representing one item
	 * @param context
	 *            Context, to get resources
	 * @return The relative view with data bound.
	 */
	private View bind(Cursor cursor, View v, Context context) {
		TextView vId = (TextView) v.findViewById(R.id.trackmgr_item_id);
		TextView vNameOrStartDate = (TextView) v.findViewById(R.id.trackmgr_item_nameordate);
		TextView vLength = (TextView) v.findViewById(R.id.trackmgr_item_length);		
		TextView vYesVotes =(TextView)v.findViewById(R.id.trackmgr_item_vote_positive);
		TextView vNoVotes =(TextView)v.findViewById(R.id.trackmgr_item_vote_negative);
		// Bind id
		long trackId = cursor.getLong(cursor.getColumnIndex(Schema.COL_ID));
		String strTrackId = Long.toString(trackId);
		vId.setText("#" + strTrackId);
		// Bind WP count, TP count, name
		Track t = Track.build(trackId, cursor, context.getContentResolver(), false);
		vLength.setText(String.valueOf(cursor.getLong(cursor.getColumnIndex(Schema.COL_LENGTH))));
		vYesVotes.setText(String.valueOf(cursor.getLong(cursor.getColumnIndex(Schema.COL_VOTE_POS))));
		vNoVotes.setText(String.valueOf(cursor.getLong(cursor.getColumnIndex(Schema.COL_VOTE_NEG))));
		vNameOrStartDate.setText(t.getName());

		return v;
	}

}
