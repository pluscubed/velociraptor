package com.pluscubed.velociraptor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;

import com.pluscubed.velociraptor.ui.LimitService;


@RequiresApi(api = Build.VERSION_CODES.N)
public class LimitTileService extends TileService {

    @Override
    public void onStartListening() {
        updateTile(isServiceRunning());
    }

    private void updateTile(boolean active) {
        Tile tile = getQsTile();

        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        boolean serviceRunning = isServiceRunning();
        updateTile(!serviceRunning);

        enableService(!serviceRunning);
    }

    public void enableService(boolean start) {
        Intent intent = new Intent(this, LimitService.class);
        intent.putExtra(start ? LimitService.EXTRA_NOTIF_START : LimitService.EXTRA_NOTIF_CLOSE, true);
        startService(intent);
    }

    @Override
    public void onTileRemoved() {
        enableService(false);
    }

    private boolean isServiceRunning() {
        final boolean[] isRunning = new boolean[1];

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isRunning[0] = true;
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("pong"));
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent("ping"));
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

        return isRunning[0];
    }
}
