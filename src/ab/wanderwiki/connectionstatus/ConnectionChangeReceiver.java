package ab.wanderwiki.connectionstatus;

import wanderwiki.android.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ConnectionChangeReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(final Context context, final Intent intent) {

		String status = NetworkUtil.getConnectivityStatusString(context);
        if (status =="Not connected to Internet")
        {
        	Toast.makeText(context, context.getResources().getString(R.string.msg_error_no_connection), Toast.LENGTH_LONG).show();
        }
        else
        {
        //	if (waitingDialog.isShowing()) waitingDialog.dismiss();
        	
        Toast.makeText(context, context.getResources().getString(R.string.msg_connection_enabled), Toast.LENGTH_LONG).show();
        }
		
	}
}