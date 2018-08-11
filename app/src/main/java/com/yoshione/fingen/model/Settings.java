package com.yoshione.fingen.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.yoshione.fingen.FgConst;

import java.util.Date;

public class Settings {

    public class Dropbox {
        final SharedPreferences prefs;

        private final String PREF_KEY = "com.yoshione.fingen.dropbox";

        private final String ACCOUNT_KEY = FgConst.PREF_DROPBOX_ACCOUNT;
        private final String TOKEN_KEY = "dropbox-token";
        private final String LAST_SUCCESSFUL_BACKUP_DATE_KEY = FgConst.PREF_SHOW_LAST_SUCCESFUL_BACKUP_TO_DROPBOX;

        public Dropbox(Context context) {
            prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        }

        public String getAccount() {
            return prefs.getString(ACCOUNT_KEY, "");
        }

        public void setAccount(String value) {
            prefs.edit().putString(ACCOUNT_KEY, value).apply();
        }

        public void removeAccount() {
            prefs.edit().remove(ACCOUNT_KEY).apply();
        }

        public String getToken() {
            return prefs.getString(TOKEN_KEY, null);
        }

        public void setToken(String value) {
            prefs.edit().putString(TOKEN_KEY, value).apply();
        }

        public void removeToken() {
            prefs.edit().remove(TOKEN_KEY).apply();
        }

        public Date getLastSuccessfulBackupDate() {
            Long dateLong = prefs.getLong(LAST_SUCCESSFUL_BACKUP_DATE_KEY, 0);
            if(dateLong == 0) return null;
            return new Date(dateLong);
        }

        public void setLastSuccessfulBackupDate(Date value) {
            Long dateLong = value == null ? 0 : value.getTime();
            prefs.edit().putLong(LAST_SUCCESSFUL_BACKUP_DATE_KEY, dateLong).apply();
        }

        public void removeLastSuccessfulBackupDate() {
            prefs.edit().remove(LAST_SUCCESSFUL_BACKUP_DATE_KEY).apply();
        }



        public void removeAll() {
            removeAccount();
            removeToken();
            removeLastSuccessfulBackupDate();
        }
    }

    public class GoogleDrive {
        final SharedPreferences prefs;

        private final String PREF_KEY = "com.yoshione.fingen.googledrive";

        private final String ACCOUNT_KEY = "google-drive-account";
        private final String TOKEN_KEY = "google-drive-token";
        private final String LAST_SUCCESSFUL_BACKUP_DATE_KEY = FgConst.PREF_SHOW_LAST_SUCCESSFUL_BACKUP_TO_GOOGLE_DRIVE;

        public GoogleDrive(Context context) {
            prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        }

        public String getAccount() {
            return prefs.getString(ACCOUNT_KEY, "");
        }

        public void setAccount(String value) {
            prefs.edit().putString(ACCOUNT_KEY, value).apply();
        }

        public void removeAccount() {
            prefs.edit().remove(ACCOUNT_KEY).apply();
        }

        public String getToken() {
            return prefs.getString(TOKEN_KEY, null);
        }

        public void setToken(String value) {
            prefs.edit().putString(TOKEN_KEY, value).apply();
        }

        public void removeToken() {
            prefs.edit().remove(TOKEN_KEY).apply();
        }

        public Date getLastSuccessfulBackupDate() {
            Long dateLong = prefs.getLong(LAST_SUCCESSFUL_BACKUP_DATE_KEY, 0);
            if(dateLong == 0) return null;
            return new Date(dateLong);
        }

        public void setLastSuccessfulBackupDate(Date value) {
            Long dateLong = value == null ? 0 : value.getTime();
            prefs.edit().putLong(LAST_SUCCESSFUL_BACKUP_DATE_KEY, dateLong).apply();
        }

        public void removeLastSuccessfulBackupDate() {
            prefs.edit().remove(LAST_SUCCESSFUL_BACKUP_DATE_KEY).apply();
        }


        public void removeAll() {
            removeAccount();
            removeToken();
            removeLastSuccessfulBackupDate();
        }
    }

    private final Context context;
    private Dropbox dropboxPrefs;
    private GoogleDrive googleDrivePrefs;

    public Settings(Context context) {
        this.context = context;
    }

    public Dropbox getDropbox() {
        if(dropboxPrefs == null) dropboxPrefs = new Dropbox(context);
        return dropboxPrefs;
    }

    public void removeDropbox() {
        getDropbox();
        dropboxPrefs.removeAll();
    }

    public GoogleDrive getGoogleDrive() {
        if(googleDrivePrefs == null) googleDrivePrefs = new GoogleDrive(context);
        return googleDrivePrefs;
    }

    public void removeGoogleDrive() {
        getGoogleDrive();
        googleDrivePrefs.removeAll();
    }
}
