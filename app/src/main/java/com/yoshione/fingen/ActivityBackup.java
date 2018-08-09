package com.yoshione.fingen;

import android.Manifest;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.yoshione.fingen.dropbox.DropboxClient;
import com.yoshione.fingen.dropbox.UploadTask;
import com.yoshione.fingen.dropbox.UserAccountTask;
import com.yoshione.fingen.googledrive.GoogleDriveClient;
import com.yoshione.fingen.interfaces.IOnComplete;
import com.yoshione.fingen.model.Settings;
import com.yoshione.fingen.utils.DateTimeFormatter;
import com.yoshione.fingen.utils.FabMenuController;
import com.yoshione.fingen.utils.FileUtils;
import com.yoshione.fingen.widgets.ToolbarActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class ActivityBackup extends ToolbarActivity {

    private static final String TAG = "ActivityBackup";

    private static final int MSG_SHOW_DIALOG = 0;
    private static final int MSG_DOWNLOAD_FILE = 1;

    private static final int GOOGLE_DRIVE_ACCEESS_REQUEST_CODE = 1000;

    @BindView(R.id.fabBackup)
    FloatingActionButton fabBackup;
    @BindView(R.id.fabRestore)
    FloatingActionButton fabRestore;
    @BindView(R.id.fabRestoreFromDropbox)
    FloatingActionButton fabRestoreFromDropbox;
    @BindView(R.id.fabRestoreFromGoogleDrive)
    FloatingActionButton fabRestoreFromGoogleDrive;
    @BindView(R.id.fabMenuButtonRoot)
    FloatingActionButton fabMenuButtonRoot;
    @BindView(R.id.switchCompatEnablePasswordProtection)
    SwitchCompat mSwitchCompatEnablePasswordProtection;
    @BindView(R.id.editTextPassword)
    EditText mEditTextPassword;

    @BindView(R.id.editTextDropboxAccount)
    EditText mEditTextDropboxAccount;
    @BindView(R.id.textInputLayoutDropboxAccount)
    TextInputLayout mTextInputLayoutDropboxAccount;
    @BindView(R.id.textViewLastBackupToDropbox)
    TextView mTextViewLastBackupToDropbox;
    @BindView(R.id.buttonLogoutFromDropbox)
    Button mButtonLogoutFromDropbox;

    @BindView(R.id.editTextGoogleDriveAccount)
    EditText mEditTextGoogleDriveAccount;
    @BindView(R.id.textInputLayoutGoogleDriveAccount)
    TextInputLayout mTextInputLayoutGoogleDriveAccount;
    @BindView(R.id.textViewLastBackupToGoogleDrive)
    TextView mTextViewLastBackupToGoogleDrive;
    @BindView(R.id.buttonLogoutFromGoogleDrive)
    Button mButtonLogoutFromGoogleDrive;

    @BindView(R.id.fabBGLayout)
    View fabBGLayout;
    @BindView(R.id.fabBackupLayout)
    LinearLayout mFabBackupLayout;
    @BindView(R.id.fabRestoreLayout)
    LinearLayout mFabRestoreLayout;
    @BindView(R.id.fabRestoreFromDropboxLayout)
    LinearLayout mFabRestoreFromDropboxLayout;
    @BindView(R.id.fabRestoreFromGoogleDriveLayout)
    LinearLayout mFabRestoreFromGoogleDriveLayout;

    private SharedPreferences prefs;
    private Settings settings;
    private UpdateRwHandler mHandler;
    private FabMenuController mFabMenuController;
    private GoogleDriveClient googleDriveClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settings = new Settings(getApplicationContext());
        initFabMenu();
        initPasswordProtection();
        initDropbox();
        initGoogleDrive();
        mHandler = new UpdateRwHandler(this);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_backup;
    }

    @Override
    protected String getLayoutTitle() {
        return getString(R.string.ent_backup_data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.action_go_home).setVisible(true);
        return true;

    }

    private void updateViewWithGoogleDriveAccount() {
        if(googleDriveClient == null) {
            this.mEditTextGoogleDriveAccount.setText("");
        } else {
            this.mEditTextGoogleDriveAccount.setText(googleDriveClient.getAccountDisplayName());
        }
        mButtonLogoutFromGoogleDrive.setVisibility(googleDriveClient == null ? View.GONE : View.VISIBLE);
    }

    private void setGoogleSignInAccount(GoogleSignInAccount account) {
        if(account == null) {
            googleDriveClient = null;
        } else {
            googleDriveClient = new GoogleDriveClient(ActivityBackup.this, account);
        }
        updateViewWithGoogleDriveAccount();
    }

    private void connectGoogleDrive() {
        final Task<GoogleSignInAccount> task = GoogleDriveClient.silentSignIn(this.getBaseContext());
        if (task.isSuccessful()) {
            // There's immediate result available.
            GoogleSignInAccount account = task.getResult();
            setGoogleSignInAccount(account);
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            //showProgressIndicator();
            task.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(Task task) {
                    try {
                        //hideProgressIndicator();
                        GoogleSignInAccount account = (GoogleSignInAccount)task.getResult(ApiException.class);
                        setGoogleSignInAccount(account);
                    } catch (ApiException apiException) {
                        // You can get from apiException.getStatusCode() the detailed error code
                        // e.g. GoogleSignInStatusCodes.SIGN_IN_REQUIRED (code: 4) means user needs to take
                        // explicit action to finish sign-in;
                        // Please refer to GoogleSignInStatusCodes Javadoc for details
                        //updateButtonsAndStatusFromErrorCode(apiException.getStatusCode());
                        setGoogleSignInAccount(null);
                    } catch(Throwable exception) {
                        setGoogleSignInAccount(null);
                    }
                }
            });
        }
    }

    private void initGoogleDrive() {
        this.mEditTextGoogleDriveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleDriveClient.signIn(ActivityBackup.this, GOOGLE_DRIVE_ACCEESS_REQUEST_CODE);
            }
        });

        mButtonLogoutFromGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Task<Void> signOutTask = GoogleDriveClient.signOut(ActivityBackup.this);
                if(signOutTask.isSuccessful()) {
                    setGoogleSignInAccount(null);
                } else {
                    signOutTask.addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            setGoogleSignInAccount(null);
                        }
                    });
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GOOGLE_DRIVE_ACCEESS_REQUEST_CODE:
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                    setGoogleSignInAccount(account);
                } else if(resultCode == RESULT_CANCELED) {

                }
                break;
        }
    }

    private void initPasswordProtection() {
        mSwitchCompatEnablePasswordProtection.setChecked(prefs.getBoolean("enable_backup_password", false));
        mSwitchCompatEnablePasswordProtection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                String password = prefs.getString("backup_password", "");
                prefs.edit().putBoolean("enable_backup_password", isChecked & !password.isEmpty()).apply();
            }
        });
        mEditTextPassword.setText(prefs.getString("backup_password", ""));
        mEditTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String password = String.valueOf(charSequence);
                prefs.edit().putString("backup_password", password).apply();
                prefs.edit().putBoolean("enable_backup_password", mSwitchCompatEnablePasswordProtection.isChecked()
                        & !password.isEmpty()).apply();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void initDropbox() {
        getDropboxUserAccount();
        final String token = settings.getDropbox().getToken();
        mEditTextDropboxAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (token == null) {
                    Auth.startOAuth2Authentication(ActivityBackup.this, getString(R.string.DROPBOX_APP_KEY));
                }
            }
        });
        mButtonLogoutFromDropbox.setVisibility(token == null ? View.GONE : View.VISIBLE);
        mButtonLogoutFromDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DropboxClient.getClient(token).auth().tokenRevoke();
                        } catch (DbxException e) {
                            e.printStackTrace();
                        }
                    }
                });
                settings.getDropbox().removeAll();
                initDropbox();
            }
        });
        initLastDropboxBackupField();
    }

    private void initLastDropboxBackupField() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Long dateLong = preferences.getLong(FgConst.PREF_SHOW_LAST_SUCCESFUL_BACKUP_TO_DROPBOX, 0);
        String title = getString(R.string.ttl_last_backup_to_dropbox);
        if (dateLong == 0) {
            title = String.format("%s -", title);
        } else {
            Date date = new Date(dateLong);
            DateTimeFormatter dtf = DateTimeFormatter.getInstance(this);
            title = String.format("%s %s %s", title, dtf.getDateMediumString(date), dtf.getTimeShortString(date));
        }
        mTextViewLastBackupToDropbox.setText(title);
    }

    public void saveAccessToken() {
        String accessToken = Auth.getOAuth2Token(); //generate Access Token
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            settings.getDropbox().setToken(accessToken);
        }
    }

    @Override
    public void onResume() {
        saveAccessToken();
        initDropbox();
        //initGoogleDrive();
        connectGoogleDrive();
        super.onResume();
    }

    protected void getDropboxUserAccount() {
        mEditTextDropboxAccount.setText(settings.getDropbox().getAccount());
        String token = settings.getDropbox().getToken();
        if (token == null) return;
        new UserAccountTask(DropboxClient.getClient(token), new UserAccountTask.TaskDelegate() {
            @Override
            public void onAccountReceived(FullAccount account) {
                //Print account's info
                Log.d("User", account.getEmail());
                Log.d("User", account.getName().getDisplayName());
                Log.d("User", account.getAccountType().name());
                mEditTextDropboxAccount.setText(account.getEmail());
                settings.getDropbox().setAccount(account.getEmail());
            }

            @Override
            public void onError(Exception error) {
                Log.d("User", "Error receiving account details.");
            }
        }).execute();
    }

    private void initFabMenu() {
        mFabMenuController = new FabMenuController(fabMenuButtonRoot, fabBGLayout, this, mFabBackupLayout, mFabRestoreLayout, mFabRestoreFromDropboxLayout, mFabRestoreFromGoogleDriveLayout);

        mFabMenuController.setOnShowListener(new FabMenuController.OnShowListener() {
            @Override
            public boolean onShowRequest(View view) {
                if(view == mFabRestoreFromDropboxLayout) return settings.getDropbox().getToken() != null;
                if(view == mFabRestoreFromGoogleDriveLayout) return googleDriveClient != null;
                return true;
            }
        });

        fabBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityBackupPermissionsDispatcher.backupDBWithPermissionCheck((ActivityBackup) v.getContext());
                mFabMenuController.closeFABMenu();
            }
        });
        fabRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityBackupPermissionsDispatcher.restoreDBWithPermissionCheck((ActivityBackup) v.getContext());
                mFabMenuController.closeFABMenu();
            }
        });
        fabRestoreFromDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityBackupPermissionsDispatcher.restoreDBFromDropboxWithPermissionCheck((ActivityBackup) v.getContext());
                mFabMenuController.closeFABMenu();
            }
        });
        fabRestoreFromGoogleDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityBackupPermissionsDispatcher.restoreDBFromGoogleDriveWithPermissionCheck((ActivityBackup) v.getContext());
                mFabMenuController.closeFABMenu();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mFabMenuController.isFABOpen()) {
            mFabMenuController.closeFABMenu();
        } else {
            super.onBackPressed();
        }
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void backupDB() {
        String token = settings.getDropbox().getToken();
        try {
            File zip = DBHelper.getInstance(getApplicationContext()).backupDB(true);
            if (token != null && zip != null) {
                new UploadTask(DropboxClient.getClient(token), zip, new IOnComplete() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(ActivityBackup.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                        SharedPreferences preferences = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(ActivityBackup.this);
                        preferences.edit().putLong(FgConst.PREF_SHOW_LAST_SUCCESFUL_BACKUP_TO_DROPBOX, new Date().getTime()).apply();
                        initLastDropboxBackupField();
                    }
                }).execute();
            }
            if(googleDriveClient != null) {
                googleDriveClient.uploadFileAsync(zip)
                        .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                Toast.makeText(ActivityBackup.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ActivityBackup.this, "File uploaded failure", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void restoreDB() {
        new FileSelectDialog(this).showSelectBackupDialog();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void restoreDBFromDropbox() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String token = settings.getDropbox().getToken();
                List<Metadata> metadataList;
                List<MetadataItem> items = new ArrayList<>();
                try {
                    metadataList = DropboxClient.getListFiles(DropboxClient.getClient(token));
                    for (int i = metadataList.size() - 1; i >= 0; i--) {
                        if (metadataList.get(i).getName().toLowerCase().contains(".zip")) {
                            items.add(new MetadataItem((FileMetadata) metadataList.get(i)));
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error read list of files from Dropbox");
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_DIALOG, items));
            }
        });
        t.start();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void restoreDBFromGoogleDrive() {
        if(googleDriveClient != null) {
            googleDriveClient.listFiles()
                    .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                        @Override
                        public void onSuccess(MetadataBuffer metadataBuffer) {
                            List<GoogleDriveMetadataItem> items = new ArrayList<>();
                            for(int i = 0; i < metadataBuffer.getCount(); i++) {
                                items.add(new GoogleDriveMetadataItem(metadataBuffer.get(i)));
                            }
                            mHandler.sendMessage(mHandler.obtainMessage(MSG_SHOW_DIALOG, items));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error read list of files from Dropbox");
                        }
                    });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        ActivityBackupPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationaleForContact(PermissionRequest request) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        showRationaleDialog(R.string.msg_permission_rw_external_storage_rationale, request);
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onCameraDenied() {
        // NOTE: Deal with a denied permission, e.g. by showing specific UI
        // or disabling certain functionality
        Toast.makeText(this, R.string.msg_permission_rw_external_storage_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onCameraNeverAskAgain() {
        Toast.makeText(this, R.string.msg_permission_rw_external_storage_never_askagain, Toast.LENGTH_SHORT).show();
    }

    private void showRationaleDialog(@StringRes int messageResId, final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.act_next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(R.string.msg_permission_rw_external_storage_rationale)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityBackupPermissionsDispatcher.checkPermissionsWithPermissionCheck(this);
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void checkPermissions() {
        Log.d(TAG, "Check permissions");
    }

    private static class UpdateRwHandler extends Handler {
        WeakReference<ActivityBackup> mActivity;

        UpdateRwHandler(ActivityBackup activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final ActivityBackup activity = mActivity.get();
            if (activity.isFinishing()) return;
//            if (activity == null) return;
            switch (msg.what) {
                case MSG_SHOW_DIALOG:
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
                    builderSingle.setTitle(activity.getResources().getString(R.string.ttl_select_db_file));

                    final ArrayAdapter<IMetadataItem> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_singlechoice);
                    arrayAdapter.addAll((List<IMetadataItem>) msg.obj);

                    builderSingle.setNegativeButton(
                            activity.getResources().getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ListView lw = ((AlertDialog) dialog).getListView();
                            final IMetadataItem item = (IMetadataItem) lw.getAdapter().getItem(which);
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    final File file = new File(activity.getCacheDir(), item.toString());

                                    if(item instanceof MetadataItem) {
                                        MetadataItem casted = (MetadataItem)item;
                                        try {
                                            OutputStream outputStream = new FileOutputStream(file);
                                            Settings settings = new Settings(activity);
                                            String token = settings.getDropbox().getToken();
                                            DbxClientV2 dbxClient = DropboxClient.getClient(token);
                                            dbxClient.files().download(casted.mMetadata.getPathLower(), casted.mMetadata.getRev())
                                                    .download(outputStream);
                                            activity.mHandler.sendMessage(activity.mHandler.obtainMessage(MSG_DOWNLOAD_FILE, file));
                                        } catch (DbxException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else if(item instanceof GoogleDriveMetadataItem) {
                                        final GoogleDriveMetadataItem casted = (GoogleDriveMetadataItem)item;
                                        try {
                                            final OutputStream outputStream = new FileOutputStream(file);

                                            final GoogleDriveClient[] client = new GoogleDriveClient[1];

                                            GoogleDriveClient.silentSignIn(FGApplication.getContext())
                                                    .continueWithTask(new Continuation<GoogleSignInAccount, Task<DriveContents>>() {
                                                        @Override
                                                        public Task<DriveContents> then(@NonNull Task<GoogleSignInAccount> task) throws Exception {
                                                            GoogleSignInAccount account = task.getResult();
                                                            client[0] = new GoogleDriveClient(FGApplication.getContext(), account);
                                                            return client[0].getFileContent(casted.mMetadata);
                                                        }
                                                    }).continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                                                        @Override
                                                        public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                                            DriveContents driveContents = task.getResult();
                                                            InputStream inputStream = driveContents.getInputStream();
                                                            GoogleDriveClient.copyStream(inputStream, outputStream);
                                                            return client[0].discardContents(driveContents);
                                                        }
                                                    }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            activity.mHandler.sendMessage(activity.mHandler.obtainMessage(MSG_DOWNLOAD_FILE, file));
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }
                            });
                            thread.start();
                        }
                    });
                    builderSingle.show();
                    break;
                case MSG_DOWNLOAD_FILE:
                    try {
                        DBHelper.getInstance(activity).showRestoreDialog(((File) msg.obj).getCanonicalPath(), activity);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

        }
    }

    private interface IMetadataItem {
        String toString();
    }

    private class MetadataItem implements IMetadataItem {
        private FileMetadata mMetadata;

        public MetadataItem(FileMetadata metadata) {
            mMetadata = metadata;
        }

        public String toString() {
            return mMetadata.getName();
        }
    }

    private class GoogleDriveMetadataItem implements IMetadataItem {
        private com.google.android.gms.drive.Metadata mMetadata;

        public GoogleDriveMetadataItem(com.google.android.gms.drive.Metadata metadata) {
            mMetadata = metadata;
        }

        public String toString() {
            return mMetadata.getTitle();
        }
    }

    private class FileSelectDialog {
        final AppCompatActivity activity;

        FileSelectDialog(AppCompatActivity activity) {
            this.activity = activity;
        }

        void showSelectBackupDialog() {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
            builderSingle.setTitle(activity.getResources().getString(R.string.ttl_select_db_file));

            List<File> files = FileUtils.getListFiles(getApplicationContext(), new File(FileUtils.getExtFingenBackupFolder()), ".zip");
            List<String> names = new ArrayList<>();
            String path;
            for (int i = files.size() - 1; i >= 0; i--) {
                path = files.get(i).toString();
                names.add(path.substring(path.lastIndexOf("/") + 1));
            }

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_singlechoice);
            arrayAdapter.addAll(names);

            builderSingle.setNegativeButton(
                    activity.getResources().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ListView lw = ((AlertDialog) dialog).getListView();
                    String fileName = (String) lw.getAdapter().getItem(which);
                    DBHelper.getInstance(activity).showRestoreDialog(FileUtils.getExtFingenBackupFolder() + fileName, activity);
                }
            });
            builderSingle.show();
        }
    }
}
