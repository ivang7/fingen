package com.yoshione.fingen.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.yoshione.fingen.FgConst;

public class Settings {

    public class Dropbox {
        final SharedPreferences prefs;

        private final String PREF_KEY = "com.yoshione.fingen.dropbox";

        private final String ACCOUNT_KEY = FgConst.PREF_DROPBOX_ACCOUNT;
        private final String TOKEN_KEY = "dropbox-token";

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

        public void removeAll() {
            removeAccount();
            removeToken();
        }
    }

    public class GoogleDrive {
        final SharedPreferences prefs;

        private final String PREF_KEY = "com.yoshione.fingen.googledrive";

        private final String ACCOUNT_KEY = "googledrive_account";
        private final String TOKEN_KEY = "googledrive-token";

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

        public void removeAll() {
            removeAccount();
            removeToken();
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
