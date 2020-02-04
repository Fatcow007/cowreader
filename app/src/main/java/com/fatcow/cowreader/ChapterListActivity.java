package com.fatcow.cowreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

public class ChapterListActivity extends AppCompatActivity {

    private int currentPage;
    private int currentChap;
    private String selectedFile;
    private ArrayList<String> fileChapters;
    private ChapterAdapter chapterAdapter;
    private ListView chapterListview;
    private TextView chapterFileTitleTextView;
    private Button chapterOrderChangeBtn;
    private int sortType;
    private Hashtable<String, ArrayList> zipFileImageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_chapter_list);

        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.CHAPTERVIEWFILE");
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE")) {
            currentPage = getIntent().getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE", 0);
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER")) {
            currentChap = getIntent().getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", 0);
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERS")) {
            fileChapters = getIntent().getStringArrayListExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERS");
        }
        if (getIntent().hasExtra("com.example.folderexplorer.CHAPTERVIEWHASHTABLE")) {
            Serializable data = getIntent().getSerializableExtra("com.example.folderexplorer.CHAPTERVIEWHASHTABLE");
            zipFileImageContainer = new Hashtable<String, ArrayList>((HashMap) data);
        }
        if (getIntent().hasExtra("com.example.folderexplorer.SORTINGOPTIONS")) {
            sortType = getIntent().getIntExtra("com.example.folderexplorer.SORTINGOPTIONS", ComicActivity.SORTTYPE_BASIC);
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
        chapterListview = findViewById(R.id.chapterListview);
        chapterFileTitleTextView = findViewById(R.id.chapterFileTitleTextView);

        chapterFileTitleTextView.setText(selectedFile);
        chapterListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                sendSelectedChapter(i);
            }
        });

        chapterOrderChangeBtn = findViewById(R.id.chapterOrderChangeBtn);
        chapterOrderChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Change ordering option
                if(sortType == ComicActivity.SORTTYPE_BASIC){
                    sortType = ComicActivity.SORTTYPE_CUSTOM;
                }else if(sortType ==ComicActivity.SORTTYPE_CUSTOM){
                    sortType = ComicActivity.SORTTYPE_NONE;
                }else if(sortType ==ComicActivity.SORTTYPE_NONE) {
                    sortType = ComicActivity.SORTTYPE_BASIC;
                }
                String currentChapterString = fileChapters.get(currentChap);
                fileChapters = StaticMethodClass.sort(((ArrayList<String>)fileChapters.clone()), sortType);
                currentChap = Arrays.asList(fileChapters).indexOf(currentChapterString);

                changeViewItems();
            }
        });
    }

    private void sendSelectedChapter(int index){
        Intent intent = new Intent();
        intent.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", index);
        intent.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERS", fileChapters);
        intent.putExtra("com.example.folderexplorer.SORTINGOPTIONS", sortType);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("com.example.folderexplorer.SORTINGOPTIONS", sortType);
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }


    private void changeViewItems(){
        chapterAdapter = new ChapterAdapter(this, fileChapters, currentChap, currentPage, zipFileImageContainer);
        chapterListview.setAdapter(chapterAdapter);
    }
}
