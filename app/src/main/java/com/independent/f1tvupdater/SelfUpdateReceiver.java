package com.independent.f1tvupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Reconcile after replacing our own process, without launching UI or downloading in the background. */
public final class SelfUpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        PendingResult result = goAsync();
        new Thread(() -> {
            try { SelfUpdater.reconcile(context.getApplicationContext()); Updater.schedule(context); }
            catch (Exception e) { android.util.Log.w("F1TVUpdater", "Updater cleanup will retry on launch", e); }
            finally { result.finish(); }
        }, "Updater-replaced-cleanup").start();
    }
}
