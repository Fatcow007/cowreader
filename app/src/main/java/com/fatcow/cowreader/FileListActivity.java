package com.fatcow.cowreader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileListActivity extends AppCompatActivity {

    private String rootDirectory;
    private String currentDirectory;
    private ArrayList<File> directoryList;
    private final String[] compatibleFileExtensions = {"zip", "txt", "pdf"};
    private HashMap<String, View> uiContainer = new HashMap<String, View>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        rootDirectory = Environment.getExternalStorageDirectory().toString();
        _initUi();
        _changeDirectory(rootDirectory);

        //Open File immediately if given
        if (getIntent().hasExtra("com.example.folderexplorer.DIRECTOPENFILE")) {
            String selectedFile = getIntent().getStringExtra("com.example.folderexplorer.DIRECTOPENFILE");
            File f = new File(selectedFile);
            currentDirectory = f.getParentFile().getPath();
            _changeDirectory(currentDirectory);
            _openFile(f);

        }

    }

    private void _initUi(){
        //Initializes needed UI elements
        uiContainer.put("fileListView", findViewById(R.id.fileListView));

        ((ListView)uiContainer.get("fileListView")).setOnItemClickListener(new DebouncedItemOnClickListener(2000) {
            @Override
            public void onDebouncedClick(AdapterView<?> adapterView, View view, int i, long l) {
                File selectedFile = directoryList.get(i);
                if(selectedFile.isDirectory()){
                    _changeDirectory(selectedFile.getPath());
                }else{
                    _openFile(selectedFile);
                }
            }
        });
    }

    private void _reloadUi(){
        ItemAdapter itemAdapter = new ItemAdapter(this, directoryList);
        itemAdapter.setParentDirExist(!rootDirectory.equals(currentDirectory));
        ((ListView)uiContainer.get("fileListView")).setAdapter(itemAdapter);
    }

    private void _openFile(File f){
        if(f == null){
            String s = getResources().getString(R.string.toast_file_not_found);
            new MyToastClass(findViewById(R.id.placeholderBtn), s).show();
        }else {
            if (f.getPath().endsWith(".txt")) {
                Intent openTextViewActivity = new Intent(getApplicationContext(), TextViewActivity.class);
                openTextViewActivity.putExtra("com.example.folderexplorer.SELECTEDFILE", f.getPath());
                startActivity(openTextViewActivity);
            } else if(f.getPath().endsWith(".zip") || f.getPath().endsWith(".pdf")){
                Intent openComicActivity = new Intent(getApplicationContext(), ComicActivity.class);
                openComicActivity.putExtra("com.example.folderexplorer.SELECTEDFILE", f.getPath());
                startActivity(openComicActivity);
            }
        }
    }

    private void _changeDirectory(String dir){
        if(new File(dir).isDirectory()){
            currentDirectory = dir;
            _updateDirectoryList();
            _updateListView();
            _reloadUi();
        }
    }

    private void _updateDirectoryList(){
        File f = new File(currentDirectory);
        directoryList = new ArrayList<File>();
        if (f.getParent() != null && !rootDirectory.equals(currentDirectory)){
            directoryList.add(f.getParentFile());
        }
        File[] fileArray = f.listFiles();
        if(fileArray != null){
            Arrays.sort(fileArray);
        }
        if(fileArray != null){
            for(File f2 : fileArray){
                if(f2.isDirectory()){
                    if(!f2.getName().startsWith(".")){
                        directoryList.add(f2);
                    }
                }
            }
            for(File f2 : fileArray){
                if(!f2.isDirectory()){
                    if(_isCompatible(f2)) {
                        directoryList.add(f2);
                    }
                }
            }
        }
    }

    private boolean _isCompatible(File f){
        for(String s : compatibleFileExtensions) {
            if (f.getName().endsWith(s)){
                return true;
            }
        }
        return false;
    }

    private void _updateListView(){
    }

    @Override
    public void onBackPressed() {
        if(rootDirectory.equals(currentDirectory)){
            super.onBackPressed();
        }else{
            _changeDirectory(new File(currentDirectory).getParent());
        }
    }

}
