package com.fatcow.cowreader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileListActivity extends AppCompatActivity {

    private String currentDirectory;
    private String currentRootDir;
    private int lastSelectedItem;
    private final String[] compatibleFileExtensions = {"zip", "txt", "pdf"};
    private static final String DEFAULT_STORAGE_PATH = "/storage/sdcard0";
    private static final String SD_CARD = "sdcard";
    private static final String EXT_SD_CARD = "extsdcard";

    private String[] availableStorageDirectories;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        availableStorageDirectories = getAvailableStorageDirectories();
        currentDirectory = availableStorageDirectories[0];
        _initUi();

        //Open File immediately if given
        if (_hasDirectFileOpenIntent()) {
            _directFileOpen();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        availableStorageDirectories = getAvailableStorageDirectories();
        _updateUi();
    }

    private void _initUi(){
        //Initializes needed UI elements

        lastSelectedItem = 0;
        ((ListView)findViewById(R.id.fileListView)).setOnItemClickListener(new DebouncedItemOnClickListener(2000) {
            @Override
            public void onDebouncedClick(AdapterView<?> adapterView, View view, int i, long l) {
                lastSelectedItem = i;
                File selectedFile = _getDirectoryList().get(i);
                if(selectedFile.isDirectory()){
                    currentDirectory = selectedFile.getAbsolutePath();
                    _updateUi();
                }else{
                    _openFile(selectedFile);
                }
            }
        });

        findViewById(R.id.changeRootStorageBtn).setOnClickListener(new DebouncedOnClickListener(200) {
            @Override
            public void onDebouncedClick(View v) {
                _changeRootDirectory();
            }
        });

        ((EditText)findViewById(R.id.searchFileText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                _updateUi();
            }
        });

    }

    private void _updateUi(){
        if(availableStorageDirectories.length > 1) {
            findViewById(R.id.changeRootStorageBtn).setAlpha(1);
            findViewById(R.id.changeRootStorageBtn).setEnabled(true);
        }else{
            findViewById(R.id.changeRootStorageBtn).setAlpha((float)0.5);
            findViewById(R.id.changeRootStorageBtn).setEnabled(false);
        }
        ((TextView)findViewById(R.id.fileDirectoryTextView)).setText(currentDirectory);
        _updateListView();
    }

    private String[] getAvailableStorageDirectories(){

        ArrayList<String> storageDirs = new ArrayList<>();

        String extStorage = System.getenv("EXTERNAL_STORAGE");
        String secondaryStorage = System.getenv("SECONDARY_STORAGE");
        //String emulatedStorage = System.getenv("EMULATED_STORAGE_TARGET");

        if (TextUtils.isEmpty(extStorage)) {
            if (new File(DEFAULT_STORAGE_PATH).exists()) {
                storageDirs.add(DEFAULT_STORAGE_PATH);
            } else {
                storageDirs.add(Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        } else {
            storageDirs.add(extStorage);
        }

        if (!TextUtils.isEmpty(secondaryStorage)) {
            final String[] rawSecondaryStorages = secondaryStorage.split(File.pathSeparator);
            Collections.addAll(storageDirs, rawSecondaryStorages);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            List<String> paths = new ArrayList<>();

            for (File file : this.getExternalFilesDirs("external")) {
                if (file != null && !file.equals(this.getExternalFilesDir("external"))) {
                    int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                    String path = file.getAbsolutePath().substring(0, index);
                    paths.add(path);
                }
            }
            //Try default external sdcard path
            if (paths.isEmpty()) paths.add("/storage/sdcard1");

            String strings[] = paths.toArray(new String[0]);
            for (String s : strings) {
                File f = new File(s);
                if (!storageDirs.contains(s) && f.canRead())
                    storageDirs.add(s);
            }
        }

        currentRootDir = SD_CARD;
        return storageDirs.toArray(new String[0]);
    }

    private boolean _hasDirectFileOpenIntent(){
        return getIntent().hasExtra("com.example.folderexplorer.DIRECTOPENFILE");
    }

    private void _directFileOpen(){
        String selectedFile = getIntent().getStringExtra("com.example.folderexplorer.DIRECTOPENFILE");
        File f = new File(selectedFile);
        currentDirectory = f.getParentFile().getAbsolutePath();
        _openFile(f);
    }

    private void _openFile(File f){
        if(f == null){
            String s = getResources().getString(R.string.toast_file_not_found);
            new MyToastClass(findViewById(R.id.placeholderBtn), s).show();
        }else{
            _openActivityByType(f.getPath());
        }
    }

    private void _openActivityByType(String filePath){
        if(filePath.endsWith(".txt")) {
            Intent openTextViewActivity = new Intent(getApplicationContext(), TextViewActivity.class);
            openTextViewActivity.putExtra("com.example.folderexplorer.SELECTEDFILE", filePath);
            startActivity(openTextViewActivity);
        }else if(filePath.endsWith(".zip") || filePath.endsWith(".pdf")){
            Intent openComicActivity = new Intent(getApplicationContext(), ComicActivity.class);
            openComicActivity.putExtra("com.example.folderexplorer.SELECTEDFILE", filePath);
            startActivity(openComicActivity);
        }else{
            String s = getResources().getString(R.string.toast_file_type_not_supported);
            new MyToastClass(findViewById(R.id.placeholderBtn), s).show();
        }
    }


    private void _changeRootDirectory(){
        if(currentRootDir.equals(SD_CARD) && availableStorageDirectories.length > 1){
            currentRootDir = EXT_SD_CARD;
            currentDirectory = availableStorageDirectories[1];
            _updateUi();
        }else{
            currentRootDir = SD_CARD;
            currentDirectory = availableStorageDirectories[0];
            _updateUi();
        }
    }

    private ArrayList<File> _getDirectoryList(){
        ArrayList<File> directoryList = new ArrayList<>();
        File currentDirFile = new File(currentDirectory);
        File[] fileArray = currentDirFile.listFiles();
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

        //Search filter
        //Empty string is automatically in all strings
        ArrayList<File> filteredDirectoryList = new ArrayList<>();
        for(File f : directoryList){
            String fn = f.getName().toLowerCase();
            if(fn.contains(getSearchString().toLowerCase())){
                filteredDirectoryList.add(f);
            }
        }
        return filteredDirectoryList;
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
        ArrayList<File> dirList = _getDirectoryList();
        FileAdapter fileAdapter = new FileAdapter(this, dirList);
        fileAdapter.setParentDirExist(!pathEqualsRootDir(currentDirectory));
        ((ListView)findViewById(R.id.fileListView)).setAdapter(fileAdapter);
        ((ListView)findViewById(R.id.fileListView)).setSelection(lastSelectedItem);
    }

    private int getLastReadDirectory(ArrayList<File> dirList){

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String lastFileDir = sharedPref.getString("LastViewFile", "");
        if(!lastFileDir.equals("")){
            for(File f:dirList){
                if(f.getAbsolutePath().equals(lastFileDir)){
                    return dirList.indexOf(f);
                }
            }
        }
        return 0;
    }

    private boolean pathEqualsRootDir(String path){
        for(String s:availableStorageDirectories){
            if(s.equals(path)){
                return true;
            }
        }
        return false;
    }

    private String getSearchString(){
        return ((EditText)findViewById((R.id.searchFileText))).getText().toString();
    }



    @Override
    public void onBackPressed() {
        if(pathEqualsRootDir(currentDirectory)){
            super.onBackPressed();
        }else{
            currentDirectory = new File(currentDirectory).getParent();
            _updateUi();
        }
    }

    private class FileAdapter extends BaseAdapter {

        LayoutInflater mInflater;
        List<File> files;
        boolean parentDirExists;

        public FileAdapter(Context c, List<File> f){
            mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            files = f;
            parentDirExists = false;
        }

        public void setParentDirExist(boolean b){
            parentDirExists = b;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public File getItem(int i) {
            return files.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = mInflater.inflate(R.layout.file_item_layout, null);
            TextView fileNameTextView =  v.findViewById(R.id.fileNameTextView);
            TextView fileViewPageTextView =  v.findViewById(R.id.fileViewPageTextView);
            File f = files.get(i);
            String fileName = f.getName();
            if(f.isDirectory()){
                fileName = fileName.concat("/");
            }
            fileNameTextView.setText(fileName);
            SharedPreferences sharedPref = v.getContext().getSharedPreferences(
                    v.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            int currentLinePage;
            int totalLinePage;
            String lastViewFile;
            if(fileName.endsWith(".zip")||fileName.endsWith(".pdf")){
                currentLinePage = sharedPref.getInt("ComicActivity." + fileName + ".currentPage", 0);
                totalLinePage = sharedPref.getInt("ComicActivity." + fileName + ".totalPage", 0);
            }else{
                currentLinePage = sharedPref.getInt("TextViewActivity." + fileName + ".currentLine", 0);
                totalLinePage = sharedPref.getInt("TextViewActivity." + fileName + ".totalLine", 0);
            }
            lastViewFile = sharedPref.getString("LastViewFile", "");
            if(lastViewFile != "" && lastViewFile.equals(f.getPath())){
                fileNameTextView.setTextColor(0xFFFFEB3B);
            }
            if(totalLinePage != 0){
                fileViewPageTextView.setText("[" + (currentLinePage+1) + "/" + totalLinePage + "]");
                ImageView fileNameBackground = v.findViewById(R.id.fileNameBackground);
                fileNameBackground.setBackgroundColor(0xff252525);
            }

            return v;
        }
    }

}
