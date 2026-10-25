package com.independent.f1tvupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.util.Log;

public final class InstallResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent result) {
        int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        boolean self = SelfUpdater.INSTALL_ACTION.equals(result.getAction());
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation;
            if (Build.VERSION.SDK_INT >= 33) {
                confirmation = result.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            } else {
                confirmation = result.getParcelableExtra(Intent.EXTRA_INTENT);
            }
            if (confirmation == null) {
                if (self) SelfUpdater.confirmationFailed(context, new IllegalStateException("Android confirmation is unavailable"));
                else Updater.setState(context, "Installer requested confirmation without an intent");
                return;
            }
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(confirmation);
                if (self) SelfUpdater.setState(context, "Awaiting Android updater confirmation");
                else Updater.setState(context, "Awaiting installation confirmation");
            } catch (Exception e) {
                if (self) SelfUpdater.confirmationFailed(context, e);
                else Updater.recordError(context, e);
            }
            return;
        }
        if (self) {
            SelfUpdater.installationResult(context, status, result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) {
            Updater.installationSucceeded(context);
            return;
        }
        String error = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Updater.setState(context, "Installation did not finish. Download is saved; retry installation.");
        Updater.prefs(context).edit()
                .remove("installing_release_id").remove("installing_version").remove("installing_session_id")
                .putString("last_error", "Code " + status + ": " +
                (error == null ? "Android installer rejected installation" : error)).apply();
        Log.w("F1TVUpdater", "Package installer failed code=" + status);
    }
}
