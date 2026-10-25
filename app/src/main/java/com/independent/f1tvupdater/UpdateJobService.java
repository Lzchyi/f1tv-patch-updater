package com.independent.f1tvupdater;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class UpdateJobService extends JobService {
    private Thread thread;
    @Override public boolean onStartJob(JobParameters params) {
        thread = new Thread(() -> {
            try { SelfUpdater.check(getApplicationContext(), false); }
            catch (Exception e) { SelfUpdater.recordError(getApplicationContext(), e); }
            try { Updater.checkAndDownload(getApplicationContext(), message -> {}); }
            catch (Exception e) { Updater.recordError(getApplicationContext(), e); }
            finally { jobFinished(params, false); }
        }, "F1TV-update-check");
        thread.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (thread != null) thread.interrupt();
        return true;
    }
}
