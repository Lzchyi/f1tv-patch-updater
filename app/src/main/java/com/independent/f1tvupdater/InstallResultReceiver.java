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
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation;
            if (Build.VERSION.SDK_INT >= 33) {
                confirmation = result.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            } else {
                confirmation = result.getParcelableExtra(Intent.EXTRA_INTENT);
            }
            if (confirmation == null) {
                Updater.setState(context, "Installer requested confirmation without an intent");
                return;
            }
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(confirmation);
                Updater.setState(context, "Awaiting installation confirmation");
            } catch (Exception e) {
                Updater.recordError(context, e);
            }
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) {
            long targetVersion = Updater.prefs(context).getLong("installing_version", -1);
            long actual = Updater.installedVersion(context);
            if (actual != targetVersion || targetVersion < 0) {
                Updater.setState(context, "Installer reported success, but installed version differs. Verify manually.");
                return;
            }
            Updater.prefs(context).edit()
                    .putLong("installed_release_id", Updater.prefs(context).getLong("installing_release_id", -1))
                    .remove("installing_release_id")
                    .remove("installing_version")
                    .putString("state", "Installed successfully")
                    .putString("last_error", "")
                    .apply();
            Updater.cleanupCandidate(context);
            return;
        }
        String error = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Updater.setState(context, "Installation failed; existing F1 TV was not uninstalled");
        Updater.prefs(context).edit()
                .remove("installing_release_id").remove("installing_version")
                .putString("last_error", "Code " + status + ": " +
                (error == null ? "Android installer rejected installation" : error)).apply();
        Log.w("F1TVUpdater", "Package installer failed code=" + status);
    }
}
