package onion.blog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class HostService extends Service {

    String TAG = "HostService";
    PowerManager.WakeLock wakeLock;

    public HostService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Server.getInstance(this);
        Tor.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Server.getInstance(this);
        Tor.getInstance(this);

        return START_STICKY;

    }

    @Override
    public void onCreate() {

        Log.i(TAG, "onCreate");

        Server.getInstance(this);
        Tor.getInstance(this);

        PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();

    }

    @Override
    public void onDestroy() {

        Log.i(TAG, "onDestroy");

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        super.onDestroy();

    }

}
