package com.independent.f1tvupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView status;
    private TextView installed;
    private Button install;
    private Button check;
    private boolean busy;
    private static final int REMOVE_F1 = 42;
    private final SharedPreferences.OnSharedPreferenceChangeListener stateChanged =
            (prefs, key) -> runOnUiThread(() -> { if (!isDestroyed()) render(); });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(15, 19, 27));
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(44),dp(32),dp(44),dp(32));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("F1TV PATCH UPDATER", 28, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        TextView detail = text("Sony / Android TV  •  Stable releases only  •  Daily checks", 15, 0xffaebaca);
        detail.setPadding(0, dp(5), 0, dp(23));
        root.addView(detail);
        installed = text("", 17, Color.WHITE);
        root.addView(installed);
        status = text("", 17, 0xffbdd2e6);
        status.setPadding(0,dp(17),0,dp(22));
        root.addView(status);

        check = button("Check for updates", v -> checkNow());
        install = button("Install downloaded update", v -> installNow());
        Button open = button("Open F1 TV", v -> {
            Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(Updater.TARGET);
            if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(Updater.TARGET);
            if (launch == null) show("F1 TV is not installed or has no launcher.");
            else startActivity(launch);
        });
        root.addView(check); root.addView(install); root.addView(open);
        TextView foot = text("Same-key updates preserve your F1 TV login.\n" +
                "A changed key needs confirmed removal and a fresh login.\n" +
                "Android asks you to approve removal and installation.\n" +
                "Unofficial tool, not affiliated with F1 or the patch author.", 13, 0xff8495a9);
        foot.setPadding(0, dp(24), 0, 0);
        root.addView(foot);
        Updater.prefs(this).registerOnSharedPreferenceChangeListener(stateChanged);
        Updater.schedule(this);
        render();
        // First use should trigger a check without waiting a day.
        if (!Updater.prefs(this).getBoolean("first_check_started", false)) {
            Updater.prefs(this).edit().putBoolean("first_check_started", true).apply();
            checkNow();
        }
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.START);
        return t;
    }
    private Button button(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(56));
        lp.bottomMargin = dp(11);
        b.setLayoutParams(lp);
        b.setOnClickListener(click);
        return b;
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
    private void show(String message) { status.setText(message); }

    private void render() {
        installed.setText("Installed F1 TV: " + Updater.installedVersionName(this));
        String state = Updater.prefs(this).getString("state", "No checks yet");
        String tag = Updater.prefs(this).getString("candidate_tag", "");
        String last = Updater.prefs(this).getString("last_error", "");
        show("Status: " + state + (tag.isEmpty()? "" : "\nRelease: " + tag) +
                (last.isEmpty()?"":"\n"+last));
        install.setText(Updater.prefs(this).getBoolean("candidate_replacement", false) &&
                Updater.installedVersion(this) >= 0
                ? "Replace F1 TV (sign in again)" : "Install downloaded update");
        check.setEnabled(!busy);
        install.setEnabled(!busy && Updater.ready(this));
    }
    private void checkNow() {
        if (busy) return;
        busy = true;
        render();
        show("Checking latest stable release…");
        io.execute(() -> {
            try {
                Updater.checkAndDownload(getApplicationContext(), message ->
                        runOnUiThread(() -> show(message)));
            } catch (Exception e) {
                Updater.recordError(getApplicationContext(), e);
            } finally {
                runOnUiThread(() -> { busy = false; render(); });
            }
        });
    }
    private void installNow() {
        if (busy || !Updater.ready(this)) { render(); return; }
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            show("Permit this updater to install unknown apps in Android settings, then return and try again.");
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        busy = true;
        render();
        show("Validating the downloaded APK bundle before installation…");
        io.execute(() -> {
            try {
                if (Updater.replacementRequired(getApplicationContext())) {
                    runOnUiThread(this::confirmReplacement);
                    return;
                }
                Updater.install(getApplicationContext());
            } catch (Exception e) {
                Updater.recordError(getApplicationContext(), e);
            } finally {
                runOnUiThread(() -> { busy = false; render(); });
            }
        });
    }

    private void confirmReplacement() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Replace F1 TV?")
                .setMessage("The upstream release uses a different signing key. Android cannot update " +
                        "the installed app in place.\n\nThe verified replacement is downloaded. " +
                        "Continuing opens Android's uninstall confirmation. Removing F1 TV erases " +
                        "its login and local settings. You will need to sign in again.\n\n" +
                        "After removal, the updater will open the installation confirmation. " +
                        "If interrupted, reopen this updater and choose Install downloaded update.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue to Android uninstall", (dialog, which) -> removeForReplacement())
                .show();
    }

    private void removeForReplacement() {
        busy = true;
        render();
        io.execute(() -> {
            try {
                Updater.beginReplacement(getApplicationContext());
                runOnUiThread(() -> {
                    try {
                        Intent remove = new Intent(Intent.ACTION_UNINSTALL_PACKAGE,
                                Uri.parse("package:" + Updater.TARGET));
                        remove.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                        startActivityForResult(remove, REMOVE_F1);
                    } catch (Exception e) { Updater.recordError(getApplicationContext(), e); }
                    finally { busy = false; render(); }
                });
            } catch (Exception e) {
                Updater.recordError(getApplicationContext(), e);
                runOnUiThread(() -> { busy = false; render(); });
            }
        });
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REMOVE_F1) return;
        if (Updater.installedVersion(this) < 0) {
            Updater.setState(this, "Old F1 TV removed. Installing the downloaded replacement…");
            installNow();
        } else {
            Updater.prefs(this).edit().remove("replacement_pending")
                    .putString("state", "Removal cancelled. Existing F1 TV and login are unchanged.").apply();
            render();
        }
    }
    @Override public void onResume() { super.onResume(); if (status != null) render(); }
    @Override public void onDestroy() {
        Updater.prefs(this).unregisterOnSharedPreferenceChangeListener(stateChanged);
        io.shutdown();
        super.onDestroy();
    }
}
