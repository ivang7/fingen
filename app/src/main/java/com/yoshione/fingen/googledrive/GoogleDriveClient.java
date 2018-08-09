package com.yoshione.fingen.googledrive;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GoogleDriveClient {

    GoogleSignInAccount account;
    DriveClient mDriveClient;
    DriveResourceClient driveResourceClient;
    Context context;

    public GoogleDriveClient(Context context, GoogleSignInAccount account) {
        if(account == null) return;
        this.context = context;
        this.account = account;
        mDriveClient = Drive.getDriveClient(context, account);
        driveResourceClient = Drive.getDriveResourceClient(context, account);
    }

    public String getAccountDisplayName() {
        return account.getDisplayName();
    }

    public Task<MetadataBuffer> listFiles() {
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/zip"))
                .build();
        return driveResourceClient.query(query);
    }

    public Task<DriveContents> getFileContent(Metadata metadata) {
        DriveId driveId = metadata.getDriveId();
        DriveFile driveFile = driveId.asDriveFile();
        return driveResourceClient.openFile(driveFile, DriveFile.MODE_READ_ONLY);
    }

    public Task<Void> discardContents(DriveContents content) {
        return driveResourceClient.discardContents(content);
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[64*1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer,0,length);
        }
    }

    public Task<DriveFile> uploadFileAsync(final File file) {
        final Task<DriveFolder> appFolderTask = driveResourceClient.getAppFolder();
        final Task<DriveContents> createContentsTask = driveResourceClient.createContents();
        return Tasks.whenAll(appFolderTask, createContentsTask)
            .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                @Override
                public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                    DriveFolder parent = appFolderTask.getResult();
                    DriveContents contents = createContentsTask.getResult();

                    try(InputStream inputStream = new FileInputStream(file);
                        OutputStream outputStream = contents.getOutputStream()) {
                        copyStream(inputStream, outputStream);
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(file.getName())
                            .setMimeType("application/zip")
                            .setStarred(true)
                            .build();

                    return driveResourceClient.createFile(parent, changeSet, contents);
                }
            });
    }

    public static Task<GoogleSignInAccount> silentSignIn(Context context) {
        GoogleSignInClient googleSignInClient = buildGoogleSignInClient(context);
        return googleSignInClient.silentSignIn();
    }

    public static void signIn(Activity activity, int requestCode) {
        GoogleSignInClient googleSignInClient = buildGoogleSignInClient(activity);
        activity.startActivityForResult(googleSignInClient.getSignInIntent(), requestCode);
    }

    public static Task<Void> signOut(Context context) {
        GoogleSignInClient googleSignInClient = buildGoogleSignInClient(context);
        return googleSignInClient.signOut();
    }

    private static GoogleSignInClient buildGoogleSignInClient(Context context) {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(context, signInOptions);
    }

}
