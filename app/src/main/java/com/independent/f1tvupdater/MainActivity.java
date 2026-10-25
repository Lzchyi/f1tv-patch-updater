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
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private TextView status;
    private TextView installed;
    private TextView latest;
    private TextView heading;
    private TextView badge;
    private TextView notice;
    private Button install;
    private Button check;
    private Button open;
    private boolean busy;
    private static final int REMOVE_F1 = 42;
    private final SharedPreferences.OnSharedPreferenceChangeListener stateChanged =
            (prefs, key) -> runOnUiThread(() -> { if (!isDestroyed()) render(); });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setBackgroundColor(0xff080a0d);
        boolean compact = getResources().getConfiguration().screenHeightDp < 500;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(compact ? 20 : 28), dp(48), dp(compact ? 20 : 26));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tv_banner);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(logo, new LinearLayout.LayoutParams(dp(128), -1));
        TextView title = text(getString(R.string.app_title), 26, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(dp(18), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView daily = text("Daily checks on", 13, 0xffc3c9d1);
        daily.setGravity(Gravity.CENTER);
        daily.setPadding(dp(16), dp(9), dp(16), dp(9));
        daily.setBackground(panel(0xff14181d, 24, 0xff41474f));
        header.addView(daily);
        LinearLayout.LayoutParams headerSize = new LinearLayout.LayoutParams(-1, dp(compact ? 56 : 64));
        headerSize.bottomMargin = dp(compact ? 14 : 20);
        root.addView(header, headerSize);

        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(24), dp(20), dp(24), dp(20));
        card.setBackground(panel(0xff14181d, 16, 0xff252a31));
        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        badge = text("", 12, 0xffff5959);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setLetterSpacing(.09f);
        summary.addView(badge);
        heading = text("", compact ? 28 : 32, Color.WHITE);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(0, dp(6), 0, 0);
        summary.addView(heading);
        status = text("", 17, 0xffc3c9d1);
        status.setMaxLines(2);
        status.setPadding(0, dp(8), 0, 0);
        summary.addView(status);
        notice = text("", 14, 0xffe6bf76);
        notice.setMaxLines(2);
        notice.setPadding(0, dp(12), 0, 0);
        summary.addView(notice);
        card.addView(summary, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout versions = new LinearLayout(this);
        versions.setGravity(Gravity.CENTER_VERTICAL);
        installed = versionColumn(versions, "On your TV");
        View divider = new View(this);
        divider.setBackgroundColor(0xff41474f);
        LinearLayout.LayoutParams dividerSize = new LinearLayout.LayoutParams(dp(1), dp(60));
        dividerSize.leftMargin = dp(12);
        dividerSize.rightMargin = dp(12);
        versions.addView(divider, dividerSize);
        latest = versionColumn(versions, "Latest version");
        LinearLayout.LayoutParams versionSize = new LinearLayout.LayoutParams(dp(260), -2);
        versionSize.leftMargin = dp(24);
        card.addView(versions, versionSize);
        root.addView(card, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = new LinearLayout(this);
        check = button("Check for updates", v -> checkNow());
        install = button("Install update", v -> installNow());
        open = button("Open F1 TV", v -> {
            Intent launch = getPackageManager().getLeanbackLaunchIntentForPackage(Updater.TARGET);
            if (launch == null) launch = getPackageManager().getLaunchIntentForPackage(Updater.TARGET);
            if (launch == null) show("F1 TV is not installed or has no launcher.");
            else startActivity(launch);
        });
        actions.addView(check, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams middle = new LinearLayout.LayoutParams(0, -1, 1);
        middle.leftMargin = dp(12);
        middle.rightMargin = dp(12);
        actions.addView(install, middle);
        actions.addView(open, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams actionSize = new LinearLayout.LayoutParams(-1, dp(compact ? 72 : 82));
        actionSize.topMargin = dp(compact ? 14 : 20);
        root.addView(actions, actionSize);
        install.setNextFocusLeftId(check.getId());
        install.setNextFocusRightId(open.getId());
        check.setNextFocusLeftId(check.getId());
        open.setNextFocusRightId(open.getId());
        for (Button action : new Button[] {check, install, open}) {
            action.setNextFocusUpId(action.getId());
            action.setNextFocusDownId(action.getId());
        }

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.addView(text(getString(R.string.disclaimer), 12, 0xff9ba4b0),
                new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams footerSize = new LinearLayout.LayoutParams(-1, dp(24));
        footerSize.topMargin = dp(compact ? 12 : 18);
        root.addView(footer, footerSize);
        check.requestFocus();
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
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }
    private GradientDrawable panel(int color, int radius, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), border);
        return shape;
    }
    private TextView versionColumn(LinearLayout parent, String label) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(text(label, 13, 0xffadb5c0));
        TextView value = text("", 26, Color.WHITE);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setPadding(0, dp(8), 0, 0);
        column.addView(value);
        parent.addView(column, new LinearLayout.LayoutParams(0, -2, 1));
        return value;
    }
    private Button button(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setId(View.generateViewId());
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(getColorStateList(R.color.tv_button_text));
        b.setBackgroundResource(R.drawable.tv_button);
        b.setBackgroundTintList(null);
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setPadding(dp(16), dp(8), dp(16), dp(8));
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setMaxLines(2);
        b.setCompoundDrawablePadding(dp(8));
        b.setOnFocusChangeListener((view, focused) ->
                b.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        focused ? R.drawable.focus_chevron : 0, 0, 0, 0));
        b.setOnClickListener(click);
        return b;
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
    private String shortVersion(String value) {
        java.util.regex.Matcher match = java.util.regex.Pattern
                .compile("^v?([0-9]+(?:\\.[0-9]+)+)").matcher(value);
        return match.find() ? match.group(1) : "—";
    }
    private void show(String raw) {
        String state = raw.toLowerCase(java.util.Locale.ROOT);
        boolean ready = Updater.ready(this);
        boolean replacement = ready && Updater.prefs(this).getBoolean("candidate_replacement", false);
        boolean missing = Updater.installedVersion(this) < 0;
        String error = Updater.prefs(this).getString("last_error", "");
        badge.setTextColor(0xffff5959);
        notice.setText(replacement && !missing
                ? "You’ll need to sign in again after this update."
                : "Updates are installed only when you choose.");
        if (state.contains("confirm") && (state.contains("waiting") || state.contains("awaiting"))) {
            badge.setText("ONE MORE STEP");
            heading.setText("Approve on your TV");
            status.setText("Follow the Android prompt to finish.");
        } else if (state.contains("downloading")) {
            badge.setText("DOWNLOADING");
            heading.setText("Downloading update");
            status.setText("Keep your TV online. You can install when it’s ready.");
        } else if (state.contains("verifying") || state.contains("validating") ||
                state.contains("architecture")) {
            badge.setText("GETTING READY");
            heading.setText("Getting your update ready");
            status.setText("Making sure the update works with your TV.");
        } else if (state.startsWith("checking")) {
            badge.setText("CHECKING");
            heading.setText("Checking for updates");
            status.setText("Looking for the latest version of F1 TV.");
        } else if (state.contains("permit") || state.contains("permission")) {
            badge.setText("ONE-TIME SETUP");
            heading.setText("Allow updates on your TV");
            status.setText("Turn on permission for this updater, then come back and choose Install update.");
        } else if (state.contains("failed") || state.contains("did not finish") || !error.isEmpty()) {
            badge.setText("TRY AGAIN");
            heading.setText("Couldn’t finish the update");
            String detail = error.toLowerCase(java.util.Locale.ROOT);
            status.setText(detail.contains("storage") ? "Your TV needs more free space."
                    : detail.contains("newer android") ? "This update needs a newer version of Android."
                    : detail.contains("http error 403") || detail.contains("http error 429")
                            ? "The update service is busy. Please try again later."
                    : detail.contains("digest") || detail.contains("sha-256") || detail.contains("signing") ||
                      detail.contains("bundle") || detail.contains("split") || detail.contains("abi") ||
                      detail.contains("package")
                            ? "This update couldn’t be verified for your TV. Check for updates again."
                    : ready ? "Your download is saved. Choose Install update to try again."
                    : "Check your connection and try checking for updates again.");
        } else if (ready) {
            badge.setText("UPDATE AVAILABLE");
            heading.setText(missing ? "Finish installing F1 TV" : "Ready to update");
            status.setText(missing ? "Your update is saved. Choose Install update to finish."
                    : "The latest version is ready to install.");
        } else if (missing || state.contains("no launcher")) {
            badge.setText("GET STARTED");
            heading.setText("F1 TV isn’t installed");
            status.setText("Install F1 TV first, then check for updates here.");
        } else if (state.contains("up to date") || state.contains("no newer") ||
                state.contains("installed successfully")) {
            badge.setText("ALL SET");
            badge.setTextColor(0xff9dd5b4);
            heading.setText("You’re up to date");
            status.setText("Your TV has the latest compatible version.");
        } else {
            badge.setText("READY TO CHECK");
            heading.setText("Keep F1 TV up to date");
            status.setText("Check for an update, or open F1 TV to start watching.");
        }
    }

    private void render() {
        installed.setText(shortVersion(Updater.installedVersionName(this)));
        latest.setText(shortVersion(Updater.prefs(this).getString("candidate_tag", "")));
        String state = Updater.prefs(this).getString("state", "No checks yet");
        show(state);
        check.setEnabled(!busy);
        install.setEnabled(!busy && Updater.ready(this) &&
                !state.equals("Waiting for Android installation confirmation") &&
                !state.equals("Awaiting installation confirmation"));
        check.setNextFocusRightId(install.isEnabled() ? install.getId() : open.getId());
        open.setNextFocusLeftId(install.isEnabled() ? install.getId() : check.getId());
    }
    private void checkNow() {
        if (busy) return;
        busy = true;
        render();
        show("Checking for updates…");
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
            show("Allow permission for this updater in Android settings, then return and try again.");
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        busy = true;
        render();
        show("Validating the update before installation…");
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
                .setTitle("Install F1 TV again?")
                .setMessage("This update needs a fresh install. Removing F1 TV clears your sign-in " +
                        "and local settings. You’ll need to sign in again.\n\n" +
                        "The update is already saved on your TV. Android will ask you to confirm " +
                        "removal, then installation.\n\nIf you leave partway through, reopen this updater " +
                        "and choose Install update.")
                .setNegativeButton("Keep current version", null)
                .setPositiveButton("Continue", (dialog, which) -> removeForReplacement())
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
