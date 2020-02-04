package com.fatcow.cowreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class TextViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private String selectedFile;

    private MyTextViewAdapter myTextViewAdapter;
    private ArrayList<String> myTextArrayList = new ArrayList<>();

    private ArrayList<Tuple> charsetTuples;
    private int selectedCharset;

    //UI Elements
    private Button currentOptionBtn;
    private Button fontSizeBtn;
    private Button charsetChangeBtn;
    private Button searchBtn;
    private Button moveLineBtn;
    private Button textMenuBtn;
    private Button nextTextPageBtn;
    private Button prevTextPageBtn;
    private Button searchGoPrevBtn;
    private Button searchGoNextBtn;

    private SeekBar textSizeSeekBar;
    private SeekBar lineSizeSeekBar;
    private SeekBar lineSeekBar;

    private TextView textSizeTextView;
    private TextView lineSizeTextView;
    private TextView lineTextView;
    private TextView textFileTitleTextView;
    private EditText searchEditText;

    private int currentTextSize = 30;
    private int currentLineHeight = 30;

    private int currentLine = 0;
    private int totalLine = 0;
    private int lineStep = 1;

    private int decodeMode = 0;
    private int currentSearchIndex = 0;
    private Integer[] searchResults;



    public GestureDetectorCompat menuMDetector;
    public GestureDetectorCompat nextMDetector;
    public GestureDetectorCompat prevMDetector;



    public class Tuple<String, Charset> {
        public final String x;
        public final Charset y;

        public Tuple(String x, Charset y) {
            this.x = x;
            this.y = y;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_text_view);
        hideSystemUI();

        if (getIntent().hasExtra("com.example.folderexplorer.SELECTEDFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.SELECTEDFILE");
        }
        loadPref();
        initCharsetTuple();
        initTextView();
        initBtn();

        savePref();

        //findViewById(R.id.loadingPanel).setVisibility(View.GONE);
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


    private void initCharsetTuple() {
        charsetTuples = new ArrayList<Tuple>();
        charsetTuples.add(new Tuple("UTF-8", Charset.forName("UTF-8")));
        charsetTuples.add(new Tuple("EUC-KR", Charset.forName("EUC-KR")));
        charsetTuples.add(new Tuple("UTF-16", Charset.forName("UTF-16")));
        charsetTuples.add(new Tuple("UTF-32", Charset.forName("UTF-32")));
        charsetTuples.add(new Tuple("US-ASCII", Charset.forName("US-ASCII")));
        charsetTuples.add(new Tuple("CP949", Charset.forName("CP949")));
        charsetTuples.add(new Tuple("EUC-KR", Charset.forName("EUC-KR")));
    }
    private String getFileName(){
        String fileName;
        String[] fileNameDir = selectedFile.split("/");
        if(fileNameDir.length != 0){
            fileName = fileNameDir[fileNameDir.length - 1];
        }else{
            fileName = selectedFile;
        }
        return fileName;
    }

    private void loadPref(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String fileName = getFileName();
        currentLine = sharedPref.getInt("TextViewActivity." + fileName + ".currentLine", 0);
        totalLine = sharedPref.getInt("TextViewActivity." + fileName + ".totalLine", 0);
        currentTextSize = sharedPref.getInt("TextViewActivity." + fileName + ".currentTextSize", 30);
        currentLineHeight = sharedPref.getInt("TextViewActivity." + fileName + ".currentLineHeight", 30);
        selectedCharset = sharedPref.getInt("TextViewActivity." + fileName + ".selectedCharset", 0);
    }

    private void savePref() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String fileName = getFileName();
        editor.putString("LastViewFile", selectedFile);
        editor.putInt("TextViewActivity." + fileName + ".currentLine", currentLine);;
        editor.putInt("TextViewActivity." + fileName + ".totalLine", totalLine);
        editor.putInt("TextViewActivity." + fileName + ".currentTextSize", currentTextSize);
        editor.putInt("TextViewActivity." + fileName + ".currentLineHeight", currentLineHeight);
        editor.putInt("TextViewActivity." + fileName + ".selectedCharset", selectedCharset);
        editor.apply();
    }


    private void changeCharset(int index) {
        selectedCharset = index;
        String s = getResources().getString(R.string.toast_encoding) + " " + charsetTuples.get(selectedCharset).x;
        new MyToastClass(charsetChangeBtn, s).show();
        readTextFile();
    }

    private void initTextView() {
        recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                    closeOptionView(currentOptionBtn);
                    closeOptionBtns(new Button[]{fontSizeBtn, charsetChangeBtn, searchBtn, moveLineBtn});
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                currentLine = layoutManager.findFirstVisibleItemPosition();
                lineSeekBar.setProgress(currentLine / lineStep);
                updateLineOption();
                savePref();
            }
        });

        //textLoadTask = (TextLoadTask) new TextLoadTask().execute(charsetTuples.get(selectedCharset));
        readTextFile();


    }

    private void reloadAdapter(){
        myTextViewAdapter = new MyTextViewAdapter(this, myTextArrayList, currentTextSize, currentLineHeight);
        recyclerView.setAdapter(myTextViewAdapter);
        goToLine(currentLine);
    }

    private void initBtn() {

        textMenuBtn = findViewById((R.id.textMenuBtn));
        fontSizeBtn = findViewById(R.id.fontSizeBtn);
        charsetChangeBtn = findViewById(R.id.charsetChangeBtn);
        searchBtn = findViewById(R.id.searchBtn);
        moveLineBtn = findViewById(R.id.moveLineBtn);

        textSizeSeekBar = findViewById(R.id.textSizeSeekBar);
        lineSizeSeekBar = findViewById(R.id.lineSizeSeekBar);
        lineSeekBar = findViewById(R.id.pageSeekBar);

        textFileTitleTextView = findViewById(R.id.textFileTitleTextView);
        textSizeTextView = findViewById(R.id.textSizeTextView);
        lineSizeTextView = findViewById(R.id.lineSizeTextView);
        lineTextView = findViewById(R.id.lineTextView);

        nextTextPageBtn = findViewById(R.id.nextTextPageBtn);
        prevTextPageBtn = findViewById(R.id.prevTextPageBtn);


        toggleOptionBtns(new Button[]{fontSizeBtn, charsetChangeBtn, searchBtn, moveLineBtn});

        menuMDetector = new GestureDetectorCompat(this, new MyGestureListener());
        textMenuBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return menuMDetector.onTouchEvent(motionEvent)
                        || recyclerView.dispatchTouchEvent(motionEvent);
            }
        });

        nextMDetector = new GestureDetectorCompat(this, new MyNextGestureListener());
        nextTextPageBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return nextMDetector.onTouchEvent(motionEvent)
                        || recyclerView.dispatchTouchEvent(motionEvent);
            }
        });

        prevMDetector = new GestureDetectorCompat(this, new MyPrevGestureListener());
        prevTextPageBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return prevMDetector.onTouchEvent(motionEvent)
                        || recyclerView.dispatchTouchEvent(motionEvent);
            }
        });

        fontSizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentOptionBtn != fontSizeBtn){
                    closeOptionView(currentOptionBtn);
                }
                currentOptionBtn = fontSizeBtn;
                toggleFontSizeOption();
            }
        });
        //Make Defaults for fontSize/lineHeight options
        initFontSizeOption();
        updateFontSizeOptions();

        charsetChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentOptionBtn != charsetChangeBtn){
                    closeOptionView(currentOptionBtn);
                }
                currentOptionBtn = charsetChangeBtn;
                if (selectedCharset == charsetTuples.size() - 1) {
                    changeCharset(0);
                } else {
                    changeCharset(selectedCharset + 1);
                }

                savePref();
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentOptionBtn != searchBtn){
                    closeOptionView(currentOptionBtn);
                }
                currentOptionBtn = searchBtn;
                toggleSearchOption();
            }
        });

        initSearchOption();

        moveLineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentOptionBtn != moveLineBtn){
                    closeOptionView(currentOptionBtn);
                }
                currentOptionBtn = moveLineBtn;
                toggleLineOption();
            }
        });
        String[] temp = selectedFile.split("/");
        textFileTitleTextView.setText(temp[temp.length - 1]);
        initLineOption();
        updateLineOption();

        //Dunno if this is useful, but just in case
        textMenuBtn.bringToFront();

    }

    private void initSearchOption() {
        searchGoPrevBtn = findViewById(R.id.searchGoPrevBtn);
        searchGoNextBtn = findViewById(R.id.searchGoNextBtn);
        searchEditText = findViewById(R.id.searchEditText);

        toggleSearchOption(); //Turns them off

        searchGoPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(searchResults.length > 0){
                    if(currentSearchIndex > 0) {
                        currentSearchIndex--;
                        goToLine(searchResults[currentSearchIndex]);
                    }else{
                        currentSearchIndex = searchResults.length - 1;
                        goToLine(searchResults[currentSearchIndex]);
                    }
                }else{
                    String s = getResources().getString(R.string.toast_search_not_found);
                    new MyToastClass(charsetChangeBtn, s).show();
                }
            }
        });

        searchGoNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(searchResults.length > 0){
                    if(currentSearchIndex < searchResults.length - 1) {
                        currentSearchIndex++;
                        goToLine(searchResults[currentSearchIndex]);
                    }else{
                        currentSearchIndex = 0;
                        goToLine(searchResults[currentSearchIndex]);
                    }
                }else{
                    String s = getResources().getString(R.string.toast_search_not_found);
                    new MyToastClass(charsetChangeBtn, s).show();
                }
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                currentSearchIndex = 0;
                searchResults = searchForText(searchEditText.getText().toString());
                if(searchResults.length > 0){
                    goToLine(searchResults[currentSearchIndex]);
                }
            }
        });

    }

    private void toggleSearchOption() {
        boolean toggle = true;
        int visibility = View.VISIBLE;
        if(searchGoPrevBtn.isEnabled()){
            toggle = false;
            visibility = View.GONE;
        }
        searchGoPrevBtn.setEnabled(toggle);
        searchGoNextBtn.setEnabled(toggle);
        searchEditText.setEnabled(toggle);

        searchGoPrevBtn.setVisibility(visibility);
        searchGoNextBtn.setVisibility(visibility);
        searchEditText.setVisibility(visibility);
    }


    private void initFontSizeOption() {
        toggleFontSizeOption(); //Turn them off for now
        textSizeSeekBar.setMax(100);
        textSizeSeekBar.setProgress(currentTextSize);
        lineSizeSeekBar.setMax(100);
        lineSizeSeekBar.setProgress(currentLineHeight);

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setTextSize(i);
                updateFontSizeOptions();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                savePref();
            }
        });
        lineSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setLineHeight(i);
                updateFontSizeOptions();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                savePref();
            }
        });
    }
    private void initLineOption() {
        toggleLineOption(); //Turn them off for now
        if(totalLine > 100){
            lineStep = totalLine / 100;
            lineSeekBar.setMax(100);
        }else{
            lineSeekBar.setMax(totalLine);
        }
        lineSeekBar.setProgress(currentLine);

        lineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) {
                    int lineToGo = i * lineStep;
                    if (lineToGo < totalLine - 1) {
                        goToLine(lineToGo);
                        currentLine = lineToGo;
                    } else {
                        goToLine(totalLine);
                        currentLine = totalLine;
                    }
                    updateLineOption();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                savePref();
            }
        });
    }
    private void setTextSize(int i){
        currentTextSize = i;
        reloadAdapter();
    }
    private void setLineHeight(int i){
        currentLineHeight = i;
        reloadAdapter();
    }

    private void closeOptionView(Button b){
        if(b != null){
            if(MyToastClass.currentToast != null){
                MyToastClass.currentToast.cancel();
            }
            if(b.equals(fontSizeBtn)){
                if(textSizeSeekBar.isEnabled()){
                    toggleFontSizeOption();
                }
            }
            if(b.equals(charsetChangeBtn)){

            }
            if(b.equals(searchBtn)){
                if(searchEditText.isEnabled()){
                    toggleSearchOption();
                }
            }
            if(b.equals(moveLineBtn)){
                if(lineSeekBar.isEnabled()){
                    toggleLineOption();
                }
            }
        }
    }

    private void toggleOptionBtns(Button[] optionBtns){
        if(optionBtns[0].isEnabled()){
            closeOptionBtns(optionBtns);
        }else{
            openOptionBtns(optionBtns);
        }
    }

    private void openOptionBtns(Button[] optionBtns){
        for(Button b: optionBtns){
            b.setEnabled(true);
            b.setVisibility(View.VISIBLE);
        }
        textFileTitleTextView.setVisibility(View.VISIBLE);

    }
    private void closeOptionBtns(Button[] optionBtns){
        for(Button b: optionBtns){
            b.setEnabled(false);
            b.setVisibility(View.GONE);
            textFileTitleTextView.setVisibility(View.GONE);
        }
    }

    private Integer[] searchForText(String s){
        ArrayList<Integer> foundIndex = new ArrayList<>();
        int index = 0;
        if(!s.equals("")) {
            for (String text : myTextArrayList) {
                if (text.contains(s)) {
                    foundIndex.add(index);
                }
                index++;
            }
        }
        return foundIndex.toArray(new Integer[foundIndex.size()]);
    }

    private void toggleFontSizeOption(){
        boolean toggle = true;
        int visibility = View.VISIBLE;
        if(textSizeSeekBar.isEnabled()){
            toggle = false;
            visibility = View.GONE;
        }
        textSizeTextView.setVisibility(visibility);
        textSizeTextView.setEnabled(toggle);
        lineSizeTextView.setVisibility(visibility);
        lineSizeTextView.setEnabled(toggle);
        textSizeSeekBar.setVisibility(visibility);
        textSizeSeekBar.setEnabled(toggle);
        lineSizeSeekBar.setVisibility(visibility);
        lineSizeSeekBar.setEnabled(toggle);
    }

    private void toggleLineOption(){
        boolean toggle = true;
        int visibility = View.VISIBLE;
        if(lineSeekBar.isEnabled()){
            toggle = false;
            visibility = View.GONE;
        }
        lineTextView.setVisibility(visibility);
        lineTextView.setEnabled(toggle);
        lineSeekBar.setVisibility(visibility);
        lineSeekBar.setEnabled(toggle);
    }

    private void updateFontSizeOptions(){
        String s = getResources().getString(R.string.number_text_size) + " " + currentTextSize + "sp";
        textSizeTextView.setText(s);
        String st = getResources().getString(R.string.number_line_height) + " " + currentLineHeight + "sp";
        lineSizeTextView.setText(st);
    }

    private void updateLineOption(){
        String s = (currentLine + 1) + getResources().getString(R.string.number_line);
        lineTextView.setText(s);
    }

    private void goToLine(int i){
        layoutManager.scrollToPositionWithOffset(i, 100);
        //recyclerView.scrollToPosition(i);
    }

    private void readTextFile(){
        try {
            //Clear my text array list where all text are stored
            myTextArrayList.clear();
            //Add two lines at start just because :D
            myTextArrayList.add("\n");
            myTextArrayList.add("\n");

            //Lets read text file!
            FileInputStream fileInputStream = new FileInputStream(selectedFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, (String)charsetTuples.get(selectedCharset).x);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder sb = new StringBuilder();
            while((line = bufferedReader.readLine()) != null) {
                switch (decodeMode){
                    case 0:
                        if(!line.startsWith(" ")){
                            sb.append("  ");  //Auto Indent
                        }
                        sb.append(line);
                        myTextArrayList.add(sb.toString());
                        if(sb.length() != 0){
                            sb.delete(0, sb.length());
                        }
                        break;
                    case 1:
                        if(line.isEmpty() || line.equals((" ")) || line.length() < 2){
                            //End of paragraph, or single character, write 'em
                            String sbs = sb.toString();
                            if(sbs != ""){
                                myTextArrayList.add(sbs);
                            }
                            myTextArrayList.add(line);
                            if(sb.length() != 0){
                                sb.delete(0, sb.length());
                            }
                        }else if(line.startsWith("   ")){
                            String sbs = sb.toString();
                            if(!sbs.isEmpty()){
                                myTextArrayList.add(sbs);
                            }
                            if(sb.length() != 0){
                                sb.delete(0, sb.length());
                            }

                            if(sb.length() == 0){
                                sb.append("  ");
                            }
                            sb.append(line);
                        }else if(line.matches("[^.!?]+[.!?]+[^.!?]+[.!?'\"]+$")){
                            String sbs = sb.toString();
                            if(!sbs.isEmpty()){
                                myTextArrayList.add(sbs);
                            }
                            if(sb.length() != 0){
                                sb.delete(0, sb.length());
                            }
                            myTextArrayList.add(line);
                        }else{
                            if(sb.length() == 0){
                                sb.append("  ");
                            }
                            sb.append(line);
                        }
                        break;
                }
            }
            totalLine = myTextArrayList.size();
            reloadAdapter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goPrevPage(){
        Display screen = getWindowManager().getDefaultDisplay();
        int screenHeight = screen.getHeight();
        recyclerView.scrollBy(0, -screenHeight);
    }

    public void goNextPage(){
        Display screen = getWindowManager().getDefaultDisplay();
        int screenHeight = screen.getHeight();
        recyclerView.scrollBy(0, screenHeight);
    }
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            closeOptionView(currentOptionBtn);
            toggleOptionBtns(new Button[]{fontSizeBtn, charsetChangeBtn, searchBtn, moveLineBtn});
            return true;
        }
    }
    class MyNextGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            goNextPage();
            return true;
        }
    }
    class MyPrevGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            goPrevPage();
            return true;
        }
    }

}
