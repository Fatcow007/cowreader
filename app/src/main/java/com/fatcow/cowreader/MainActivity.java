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

public class MainActivity extends AppCompatActivity {


    private final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int EXTERNAL_REQUEST = 138;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Runs only once after startup
        _requestPerm();
        _initUi();
    }
    @Override
    public void onResume(){
        super.onResume();

        _reloadUI();
    }

    private void _initUi(){
        //Initializes needed UI elements

        //Open File Button
        findViewById(R.id.openFileBtn).setOnClickListener(new DebouncedOnClickListener(200) {
            @Override
            public void onDebouncedClick(View view) {
                Intent openFileListActivity = new Intent(getApplicationContext(), FileListActivity.class);
                startActivity(openFileListActivity);
            }
        });
        //Open Recent File Button
        findViewById(R.id.openRecentFileBtn).setOnClickListener(new DebouncedOnClickListener(200) {
            @Override
            public void onDebouncedClick(View view) {
                Intent openFileListActivity = new Intent(getApplicationContext(), FileListActivity.class);
                if(_hasLastOpenFile()){
                    openFileListActivity.putExtra("com.example.folderexplorer.DIRECTOPENFILE", _getLastOpenFile());
                }
                startActivity(openFileListActivity);
            }
        });
    }


    private void _reloadUI() {
        if(_hasLastOpenFile()){
            findViewById(R.id.openRecentFileBtn).setEnabled(true);
            findViewById(R.id.openRecentFileBtn).setAlpha(1);
        }else{
            findViewById(R.id.openRecentFileBtn).setEnabled(false);
            findViewById(R.id.openRecentFileBtn).setAlpha((float)(0.5));
        }
        String s;
        if(!_hasLastOpenFile()){
            s = getResources().getString(R.string.file_recent_file_not_found_text);
        }else{
            String[] sp = _getLastOpenFile().split("/");
            s = getResources().getString(R.string.file_recent_file_found_text) + sp[sp.length - 1];
        }
        ((TextView)findViewById(R.id.selectedFileTextView)).setText(s);
    }


    private String _getLastOpenFile(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString("LastViewFile", "");
    }

    private boolean _hasLastOpenFile(){
        return !_getLastOpenFile().equals("");
    }

    private boolean _hasPerm(){
        boolean granted = true;
        for(String s : EXTERNAL_PERMS){
            if( PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, s) ){
                granted = false;
            }
        }
        return granted;
    }

    private void _requestPerm(){
        if(!_hasPerm()){
            final int version = Build.VERSION.SDK_INT;
            if (version >= 23) {
                ActivityCompat.requestPermissions(this,
                        EXTERNAL_PERMS,
                        EXTERNAL_REQUEST);
            }
        }
    }
}
