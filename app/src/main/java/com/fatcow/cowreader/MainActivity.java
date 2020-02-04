package com.fatcow.cowreader;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    /*This is the first activity that appears when app launches
    * and does following actions.
    * 1. Checks for permission.
    * 2. Loads preferences.
    * 3. Creates an gui for users*/



    private HashMap<String, View> uiContainer = new HashMap<String, View>();
    private String recentFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Runs only once after startup
        PermManager pm = new PermManager(this);
        pm.getPerms();
        _initUi();
        _loadPref();
        _reloadUI();
    }
    @Override
    public void onResume(){
        super.onResume();

        //Runs every time when this activity becomes active
        _loadPref();
        _reloadUI();
    }

    private void _initUi(){
        //Initializes needed UI elements
        uiContainer.put("openFileBtn", findViewById(R.id.openFileBtn));
        uiContainer.put("openRecentFileBtn", findViewById(R.id.openRecentFileBtn));
        uiContainer.put("selectedFileTextView", findViewById(R.id.selectedFileTextView));


        //Open File Button
        uiContainer.get("openFileBtn").setOnClickListener(new DebouncedOnClickListener(2000) {
            @Override
            public void onDebouncedClick(View view) {
                Intent openFileListActivity = new Intent(getApplicationContext(), FileListActivity.class);
                startActivity(openFileListActivity);
            }
        });
        //Open Recent File Button
        uiContainer.get("openRecentFileBtn").setOnClickListener(new DebouncedOnClickListener(2000) {
            @Override
            public void onDebouncedClick(View view) {
                Intent openFileListActivity = new Intent(getApplicationContext(), FileListActivity.class);
                if(recentFile != null){
                    openFileListActivity.putExtra("com.example.folderexplorer.DIRECTOPENFILE", recentFile);
                }
                startActivity(openFileListActivity);
            }
        });
        uiContainer.get("openRecentFileBtn").setEnabled(false);
    }

    private void _reloadUI(){
        //Sets up text
        String[] sp = recentFile.split("/");
        String s;
        if(sp.length > 0){
            s = getResources().getString(R.string.file_recent_file_found_text) + sp[sp.length - 1];
        }else{
            s = getResources().getString(R.string.file_recent_file_not_found_text);
        }
        ((TextView)uiContainer.get("selectedFileTextView")).setText(s);

    }

    private void _loadPref(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        recentFile = sharedPref.getString("LastViewFile", "");
    }



    private class PermManager{

        private final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        private final int EXTERNAL_REQUEST = 138;
        private AppCompatActivity c;

        public PermManager(AppCompatActivity activity){
            c = activity;
        }
        private boolean _checkPerm(){
            boolean allGranted = true;
            for(String s : EXTERNAL_PERMS){
                if( PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(c, s) ){
                    allGranted = false;
                }
            }
            return allGranted;
        }
        private void _requestPerm() {

            final int version = Build.VERSION.SDK_INT;
            if (version >= 23) {
                ActivityCompat.requestPermissions(c,
                        EXTERNAL_PERMS,
                        EXTERNAL_REQUEST);
            }
        }
        private boolean getPerms(){
            if(!_checkPerm()){
                _requestPerm();
                return _checkPerm();
            }else{
                return true;
            }
        }
    }


}
