package com.yoshione.fingen.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.evernote.android.job.DailyJob;
import com.evernote.android.job.JobRequest;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.yoshione.fingen.ActivityBackup;
import com.yoshione.fingen.BuildConfig;
import com.yoshione.fingen.DBHelper;
import com.yoshione.fingen.FGApplication;
import com.yoshione.fingen.FgConst;
import com.yoshione.fingen.dropbox.DropboxClient;
import com.yoshione.fingen.googledrive.GoogleDriveClient;
import com.yoshione.fingen.model.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by slv on 27.10.2017.
 * /
 */

public class BackupJob extends DailyJob {

    public static final String TAG = "job_backup_tag";

    public static int schedule() {
        // schedule between 1 and 6 AM
        return DailyJob.schedule(new JobRequest.Builder(TAG),
                TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(00),
                TimeUnit.HOURS.toMillis(6) + TimeUnit.MINUTES.toMillis(00));
    }

    @NonNull
    @Override
    protected DailyJobResult onRunDailyJob(Params params) {
        if (!BuildConfig.FLAVOR.equals("nd")) {
            Context context = FGApplication.getContext();
            File zip = null;
            try {
                zip = DBHelper.getInstance(context).backupDB(true);
                createDropboxBackup(context, zip);
                createGoogleDriveBackup(context, zip);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return DailyJobResult.SUCCESS;
    }

    private void createDropboxBackup(Context context, File zip) {
        Settings.Dropbox settings = new Settings(context).getDropbox();
        String token = settings.getToken();
        // Do the task here
        if (token != null && zip != null) {
            DbxClientV2 dbxClient = DropboxClient.getClient(token);
            // Upload to Dropbox
            try {
                InputStream inputStream = new FileInputStream(zip);
                dbxClient.files().uploadBuilder("/" + zip.getName()) //Path in the user's Dropbox to save the file.
                        .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                        .uploadAndFinish(inputStream);
                settings.setLastSuccessfulBackupDate(new Date());
            } catch (DbxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//                Log.d("Upload Status", "Success");
        }
    }

    private void createGoogleDriveBackup(final Context context, final File zip) {
        final Settings.GoogleDrive settings = new Settings(context).getGoogleDrive();
        String token = settings.getToken();
        // Do the task here
        if (token != null && zip != null) {
            // Upload to Google Drive
            final Task<GoogleSignInAccount> task = GoogleDriveClient.silentSignIn(context);
            if (task.isSuccessful()) {
                // There's immediate result available.
                GoogleSignInAccount account = task.getResult();
                executeGoogleDriveBackup(context, account, zip, settings);
            } else {
                // There's no immediate result ready, waits for the async callback.
                //showProgressIndicator();
                task.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(Task task) {
                        try {
                            GoogleSignInAccount account = (GoogleSignInAccount)task.getResult(ApiException.class);
                            executeGoogleDriveBackup(context, account, zip, settings);
                        } catch (ApiException apiException) {
                            // apiException.getStatusCode() the detailed error code
                        } catch(Throwable exception) {
                        }
                    }
                });
            }

        }
    }

    private void executeGoogleDriveBackup(Context context, GoogleSignInAccount account, File zip, final Settings.GoogleDrive settings) {
        GoogleDriveClient googleDriveClient = new GoogleDriveClient(context, account);

        googleDriveClient.uploadFileAsync(zip, true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        settings.setLastSuccessfulBackupDate(new Date());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

}
