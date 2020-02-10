package com.fatcow.cowreader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ChapterListActivity extends AppCompatActivity {

    private int currentPage;
    private int currentChap;
    private String selectedFile;
    private ArrayList<String> chapterNames;
    private ArrayList<Integer> chapterPageCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_chapter_list);

        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.CHAPTERVIEWFILE");
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER")) {
            currentChap = getIntent().getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", 0);
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE")) {
            currentPage = getIntent().getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE", 0);
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERNAMES")) {
            chapterNames = getIntent().getStringArrayListExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERNAMES");
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERNAMES")) {
            chapterPageCounts = getIntent().getIntegerArrayListExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGECOUNTS");
        }

        hideSystemUI();
        initViews();
        changeViewItems();
    }
    @Override
    public void onResume(){
        super.onResume();
        hideSystemUI();
    }


    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void initViews(){
        ((TextView)(findViewById(R.id.chapterFileTitleTextView))).setText(selectedFile);
        ((ListView)(findViewById(R.id.chapterListview))).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                sendSelectedChapter(i);
            }
        });

    }

    private void sendSelectedChapter(int index){
        Intent intent = new Intent();
        intent.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", index);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }


    private void changeViewItems(){
        ChapterAdapter chapterAdapter = new ChapterAdapter(this, currentChap, currentPage, chapterNames, chapterPageCounts);
        ((ListView)(findViewById(R.id.chapterListview))).setAdapter(chapterAdapter);
    }



    private class ChapterAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private int currentChap;
        private int currentPage;
        private ArrayList<String> chapterNames;
        private ArrayList<Integer> chapterPageCounts;


        public ChapterAdapter(Context c, int currentCh, int currentPg, ArrayList<String> chNames, ArrayList<Integer> chPageCounts){
            mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            currentChap = currentCh;
            currentPage = currentPg;
            chapterNames = chNames;
            chapterPageCounts = chPageCounts;
        }

        @Override
        public int getCount() {
            return chapterNames.size();
        }

        @Override
        public String getItem(int i) {
            return chapterNames.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = mInflater.inflate(R.layout.chapter_item_layout, null);
            TextView chapterTextView = v.findViewById(R.id.chapterTextView);
            TextView chapterSizeTextView = v.findViewById(R.id.chapterSizeTextView);

            chapterTextView.setText(chapterNames.get(i));
            int totalPage = chapterPageCounts.get(i);
            int readPage = totalPage;
            if(i == currentChap){
                chapterTextView.setTextColor(0xFFFFEB3B);
                readPage = currentPage + 1;
            }
            if(i > currentChap){
                readPage = 0;
            }
            chapterSizeTextView.setText("[" + readPage + "/" +totalPage + "]");

            return v;
        }
    }

}
