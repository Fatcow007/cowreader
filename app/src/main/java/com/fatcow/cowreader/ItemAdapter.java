package com.fatcow.cowreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class ItemAdapter extends BaseAdapter {

    LayoutInflater mInflater;
    List<File> files;
    boolean parentDirExists;

    public ItemAdapter(Context c, List<File> f){
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
        if(i == 0 && parentDirExists){
            fileName = fileName.concat("..");
        }else if(f.isDirectory()){
            fileName = fileName.concat("/");
        }
        fileNameTextView.setText(fileName);
        SharedPreferences sharedPref = v.getContext().getSharedPreferences(
                v.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int currentLinePage;
        int totalLinePage;
        String lastViewFile;
        if(fileName.endsWith(".zip")||fileName.endsWith(".pdf")){
            currentLinePage = sharedPref.getInt("ComicActivity." + fileName + ".currentPageAsTotal", 0);
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
