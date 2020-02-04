package com.fatcow.cowreader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ComicActivity extends AppCompatActivity{

    private String selectedFile;
    private Hashtable<String, ArrayList> zipFileImageContainer;
    private ArrayList<String> zipFileOrderedDirectoryList;
    private ArrayList<String> totalPageList;

    private HashMap<String, View> uiContainer = new HashMap<String, View>();

    private final int CONTROLTYPE_BOTH = 253;
    private final int CONTROLTYPE_TOUCH = 254;
    private final int CONTROLTYPE_SLIDE = 255;

    public static final int SORTTYPE_BASIC = 125;
    public static final int SORTTYPE_CUSTOM = 126;
    public static final int SORTTYPE_NONE = 127;

    private final int VIEWDIR_NORMAL = 0;
    private final int VIEWDIR_REVERSE = 1;

    private final int SUBPAGE_LEFT = 0;
    private final int SUBPAGE_RIGHT = 1;



    private int screenWidth;
    private int screenHeight;

    private int maskWidth;
    private int maskHeight;
    private int currentPage;
    private int totalPage;
    private int viewDirection;
    private int currentSubPage;
    private boolean currentPageUseDoublePage;
    private Bitmap currentPageBitmap;

    private int cropSize;
    private int controlType;
    private int sortType;
    private final int extraView = 15; //shows some extra image in pixels

    public GestureDetector mDetector;

    Charset defCharset = Charset.forName("UTF-8");
    Charset korCharset = Charset.forName("cp949");
    Charset currentCharset = defCharset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_comic);
        hideSystemUI();

        if (getIntent().hasExtra("com.example.folderexplorer.SELECTEDFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.SELECTEDFILE");
        }else{
            finish();
        }


        Display screen = getWindowManager().getDefaultDisplay();
        screenWidth = screen.getWidth();
        screenHeight = screen.getHeight();

        mDetector = new GestureDetector(this, new MyGestureDetector());

        loadPref();
        loadFile();
        initUi();
        updateUi();
        loadNewPage(currentPage);
        if(currentPageUseDoublePage){
            loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
        }else{
            loadBitmap(currentPageBitmap);
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        hideSystemUI();
        updateUi();
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

    private String getFileName(){
        String fileName;
        String[] fileNameDir = selectedFile.toString().split("/");
        if(fileNameDir.length != 0){
            fileName = fileNameDir[fileNameDir.length - 1];
        }else{
            fileName = selectedFile.toString();
        }
        return fileName;
    }

    private void loadPref(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String fileName = getFileName();
        currentPage = sharedPref.getInt("ComicActivity." + fileName + ".currentPage", 0);
        totalPage = sharedPref.getInt("ComicActivity." + fileName + ".totalPage", 0);
        cropSize = sharedPref.getInt("ComicActivity." + fileName + ".cropSize", 0);
        controlType = sharedPref.getInt("ComicActivity." + fileName + ".controlType", CONTROLTYPE_BOTH);
        sortType = sharedPref.getInt("ComicActivity." + fileName + ".sortType", ComicActivity.SORTTYPE_BASIC);
        viewDirection = sharedPref.getInt("ComicActivity." + fileName + ".viewDirection", VIEWDIR_NORMAL);

    }

    private void savePref() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String fileName = getFileName();
        editor.putString("LastViewFile", selectedFile);
        editor.putInt("ComicActivity." + fileName + ".currentPage", currentPage);
        editor.putInt("ComicActivity." + fileName + ".totalPage", totalPage);
        editor.putInt("ComicActivity." + fileName + ".cropSize", cropSize);
        editor.putInt("ComicActivity." + fileName + ".controlType", controlType);
        editor.putInt("ComicActivity." + fileName + ".basicSort", sortType);
        editor.putInt("ComicActivity." + fileName + ".viewDirection", viewDirection);
        editor.apply();
    }

    private void loadFile(){
        if(selectedFile.toLowerCase().endsWith(".zip")){
            loadZipFileContent();
        }else if(selectedFile.toLowerCase().endsWith(".pdf")){
            loadPdfFileContent();
        }
    }

    private void initUi() {

        //INITIALIZES ALL UI

        uiContainer.put("comicImageView", findViewById(R.id.comicImageView));
        uiContainer.put("imageFileTitleTextView", findViewById(R.id.imageFileTitleTextView));
        uiContainer.put("optionUiContainerLayout", findViewById(R.id.optionUiContainerLayout));
        uiContainer.put("cropSizeLayout", findViewById(R.id.cropSizeLayout));
        uiContainer.put("pageSeekLayout", findViewById(R.id.pageSeekLayout));
        uiContainer.put("menuBtn", findViewById(R.id.menuBtn));
        uiContainer.put("prevBtn", findViewById(R.id.prevBtn));
        uiContainer.put("nextBtn", findViewById(R.id.nextBtn));
        uiContainer.put("option1Btn", findViewById(R.id.option1Btn));
        uiContainer.put("option2Btn", findViewById(R.id.option2Btn));
        uiContainer.put("option3Btn", findViewById(R.id.option3Btn));
        uiContainer.put("option4Btn", findViewById(R.id.option4Btn));
        uiContainer.put("option5Btn", findViewById(R.id.option5Btn));
        uiContainer.put("currentPageTextView", findViewById(R.id.currentPageTextView));
        uiContainer.put("cropTextView", findViewById(R.id.cropTextView));
        uiContainer.put("pageSeekBar", findViewById(R.id.pageSeekBar));
        uiContainer.put("cropSeekBar", findViewById(R.id.cropSeekBar));
        uiContainer.put("currentActiveOption", null);


        uiContainer.get("prevBtn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlType != CONTROLTYPE_SLIDE){
                    goPrevPage();
                }
            }
        });
        uiContainer.get("prevBtn").setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        uiContainer.get("nextBtn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlType != CONTROLTYPE_SLIDE){
                    goNextPage();
                }
            }
        });
        uiContainer.get("nextBtn").setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        uiContainer.get("menuBtn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateUi();
                toggleUi(uiContainer.get("optionUiContainerLayout"));
                disableUi(uiContainer.get("currentActiveOption"));
            }
        });
        uiContainer.get("menuBtn").setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        uiContainer.get("option1Btn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(uiContainer.get("currentActiveOption"));
                uiContainer.put("currentActiveOption", null);
                // Changes viewing order
                String msg;
                if(viewDirection == VIEWDIR_NORMAL){
                    viewDirection = VIEWDIR_REVERSE;
                    msg = getResources().getString(R.string.toast_left_to_right);
                }else{
                    viewDirection = VIEWDIR_NORMAL;
                    msg = getResources().getString(R.string.toast_right_to_left);
                }
                new MyToastClass(uiContainer.get("option1Btn"), msg).show();
                savePref();
            }
        });
        uiContainer.get("option2Btn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(uiContainer.get("currentActiveOption"));
                uiContainer.put("currentActiveOption", uiContainer.get("pageSeekLayout"));
                toggleUi(uiContainer.get("pageSeekLayout"));
            }
        });
        uiContainer.get("option3Btn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(uiContainer.get("currentActiveOption"));
                uiContainer.put("currentActiveOption", uiContainer.get("cropSizeLayout"));
                toggleUi(uiContainer.get("cropSizeLayout"));
            }
        });
        uiContainer.get("option4Btn").setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(uiContainer.get("currentActiveOption"));
                String[] toastText = new String[] {getResources().getString(R.string.toast_touch_and_gesture),
                        getResources().getString(R.string.toast_touch_only),
                        getResources().getString(R.string.toast_gesture_only)};
                if(controlType == CONTROLTYPE_BOTH){
                    controlType = CONTROLTYPE_SLIDE;
                }else if(controlType == CONTROLTYPE_SLIDE){
                    controlType = CONTROLTYPE_TOUCH;
                }else if(controlType == CONTROLTYPE_TOUCH){
                    controlType = CONTROLTYPE_BOTH;
                }
                String s = toastText[controlType - CONTROLTYPE_BOTH];
                new MyToastClass(uiContainer.get("option1Btn"), s).show();
                savePref();
            }
        });
        uiContainer.get("option5Btn").setOnClickListener(new DebouncedOnClickListener(2000) {
            @Override
            public void onDebouncedClick(View view) {
                disableUi(uiContainer.get("currentActiveOption"));
                startChapterListActivity();
                savePref();
            }
        });

        ((SeekBar)uiContainer.get("pageSeekBar")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    currentPage = i;
                    loadNewPage(currentPage);
                    if(currentPageUseDoublePage){
                        loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                    }else{
                        loadBitmap(currentPageBitmap);
                    }
                }
                String s = getResources().getString(R.string.number_page) + " " + (currentPage + 1) + "/" + totalPage;
                ((TextView)uiContainer.get("currentPageTextView")).setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePref();
            }
        });

        ((SeekBar)uiContainer.get("cropSeekBar")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cropSize = i;
                String s = getResources().getString(R.string.number_crop) + " " + (cropSize);
                ((TextView)uiContainer.get("cropTextView")).setText(s);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                loadNewPage(currentPage);
                savePref();
            }
        });

        ((SeekBar)uiContainer.get("cropSeekBar")).setMax((100));
        ((SeekBar)uiContainer.get("cropSeekBar")).setProgress(cropSize);


        disableUi(uiContainer.get("pageSeekLayout"));
        disableUi(uiContainer.get("cropSizeLayout"));
        disableUi(uiContainer.get("optionUiContainerLayout"));


    }

    private void updateUi(){
        String[] temp = selectedFile.split("/");
        ((TextView)uiContainer.get("imageFileTitleTextView")).setText(temp[temp.length - 1]);
        ((SeekBar)uiContainer.get("pageSeekBar")).setProgress(currentPage);
        ((SeekBar)uiContainer.get("pageSeekBar")).setMax(totalPage - 1);
    }


    private void startChapterListActivity() {
        if(selectedFile.toLowerCase().endsWith(".pdf")){
            String s = getResources().getString(R.string.toast_pdf_not_supported);
            new MyToastClass(uiContainer.get("option1Btn"), s).show();
        }else{
            Intent i = new Intent(this, ChapterListActivity.class);
            i.putExtra("com.example.folderexplorer.CHAPTERVIEWFILE", selectedFile);
            i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", getChapterIndexFromPage(currentPage)[0]);
            i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE", getChapterIndexFromPage(currentPage)[1]);
            i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERS", zipFileOrderedDirectoryList);
            i.putExtra("com.example.folderexplorer.CHAPTERVIEWHASHTABLE", zipFileImageContainer);
            i.putExtra("com.example.folderexplorer.SORTINGOPTIONS", sortType);
            startActivityForResult(i, 1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int[] currentChapIndex = getChapterIndexFromPage(currentPage);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                int newCurrentChapter = data.getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", 0);
                zipFileOrderedDirectoryList = data.getStringArrayListExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERS");
                sortType = data.getIntExtra("com.example.folderexplorer.SORTINGOPTIONS", sortType);
                updateTotalPageListForZip();
                loadNewPage(getFirstPageOfChapter(newCurrentChapter));
                if(currentPageUseDoublePage){
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                }else{
                    loadBitmap(currentPageBitmap);
                }

            }else if(resultCode == RESULT_CANCELED){
                sortType = data.getIntExtra("com.example.folderexplorer.SORTINGOPTIONS", sortType);
                updateTotalPageListForZip();
                currentPage = getFirstPageOfChapter(currentChapIndex[0]) + currentChapIndex[1];
            }
            updateUi();
        }
    }


    private void toggleUi(View v) {
        if(v.isEnabled()) {
            disableUi(v);
        }else{
            enableUi(v);
        }
    }

    private void disableUi(View v) {
        if(v != null){
            v.setEnabled(false);
            v.setVisibility(View.GONE);
        }
    }
    private void enableUi(View v) {
        if(v != null){
            v.setEnabled(true);
            v.setVisibility(View.VISIBLE);
        }
        v.bringToFront();
        ((View)v.getParent()).requestLayout();
        ((View)v.getParent()).invalidate();
    }



    private int getFirstPageOfChapter(int chapter){
        int page = 0;
        if(chapter == 0){
            return page;
        }
        for(int i = 0; i < chapter; i++){
            page += zipFileImageContainer.get(zipFileOrderedDirectoryList.get(i)).size();
        }
        return page;
    }

    private int[] getChapterIndexFromPage(int page){
        int chapter = 0;
        int index = page;
        for(int i = 0; i < zipFileOrderedDirectoryList.size(); i++){
            int chapterPageCount = zipFileImageContainer.get(zipFileOrderedDirectoryList.get(i)).size();
            if(zipFileImageContainer.get(zipFileOrderedDirectoryList.get(i)).size() < index){
                index -= chapterPageCount + 1;
            }else{
                chapter = i;
                break;
            }
        }
        int[] result = {chapter, index};
        return result;
    }

    private void goPrevPage(){
        disableUi(uiContainer.get("optionUiContainerLayout"));
        if(viewDirection == VIEWDIR_NORMAL){
            _goPrevPage();
        }else{
            _goNextPage();
        }
        updateUi();
        savePref();
    }
    private void goNextPage(){
        disableUi(uiContainer.get("optionUiContainerLayout"));
        if (viewDirection == VIEWDIR_NORMAL) {
            _goNextPage();
        }else{
            _goPrevPage();
        }
        updateUi();
        savePref();
    }
    private void _goPrevPage() {
        if(currentPageUseDoublePage){
            if(viewDirection == VIEWDIR_NORMAL){
                if(currentSubPage == SUBPAGE_RIGHT) {
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                }else{
                    if(currentPage > 0) {
                        loadNewPage(currentPage - 1);
                        if (currentPageUseDoublePage) {
                            loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_RIGHT));
                        } else {
                            loadBitmap(currentPageBitmap);
                        }
                    }else {
                        String s = getResources().getString(R.string.toast_first_page);
                        new MyToastClass(uiContainer.get("option1Btn"), s).show();
                    }
                }
            }else if(viewDirection == VIEWDIR_REVERSE){
                if(currentSubPage == SUBPAGE_LEFT){
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_RIGHT));
                }else{
                    if(currentPage > 0) {
                        loadNewPage(currentPage - 1);
                        if (currentPageUseDoublePage) {
                            loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                        } else {
                            loadBitmap(currentPageBitmap);
                        }
                    }else {
                        String s = getResources().getString(R.string.toast_first_page);
                        new MyToastClass(uiContainer.get("option1Btn"), s).show();
                    }
                }
            }
        }else{
            if(currentPage > 0){
                loadNewPage(currentPage - 1);
                if(currentPageUseDoublePage){
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_RIGHT));
                }else {
                    loadBitmap(currentPageBitmap);
                }
            }else {
                String s = getResources().getString(R.string.toast_first_page);
                new MyToastClass(uiContainer.get("option1Btn"), s).show();
            }
        }
    }

    private void _goNextPage() {
        if(currentPageUseDoublePage){
            if(viewDirection == VIEWDIR_NORMAL){
                if(currentSubPage == SUBPAGE_LEFT) {
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_RIGHT));
                }else{
                    if(currentPage < totalPage) {
                        loadNewPage(currentPage + 1);
                        if (currentPageUseDoublePage) {
                            loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                        } else {
                            loadBitmap(currentPageBitmap);
                        }
                    }else{
                        String s = getResources().getString(R.string.toast_last_page);
                        new MyToastClass(uiContainer.get("option1Btn"), s).show();
                    }
                }
            }else if(viewDirection == VIEWDIR_REVERSE){
                if(currentSubPage == SUBPAGE_RIGHT){
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                }else{
                    if(currentPage < totalPage) {
                        loadNewPage(currentPage + 1);
                        if (currentPageUseDoublePage) {
                            loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_RIGHT));
                        } else {
                            loadBitmap(currentPageBitmap);
                        }
                    }else{
                        String s = getResources().getString(R.string.toast_last_page);
                        new MyToastClass(uiContainer.get("option1Btn"), s).show();
                    }
                }
            }
        }else{
            if(currentPage < totalPage){
                loadNewPage(currentPage + 1);
                if(currentPageUseDoublePage){
                    loadBitmap(getHalfBitmap(currentPageBitmap, SUBPAGE_LEFT));
                }else{
                    loadBitmap(currentPageBitmap);
                }
            }else {
                String s = getResources().getString(R.string.toast_last_page);
                new MyToastClass(uiContainer.get("option1Btn"), s).show();
            }
        }
    }


    private void loadBitmap(Bitmap b) {
        Bitmap finalImage = Bitmap.createBitmap(b);

        if(selectedFile.toLowerCase().endsWith("zip")){
            float cropSizeTrueValue =  1 + (cropSize/(float)100);
            getMaskSize(b.getWidth(), b.getHeight());
            finalImage = getMaskScaledBitmap(finalImage, cropSizeTrueValue);
            finalImage = maskBitmap(finalImage);
        }else{
            if(cropSize > 0){
                //Crop Image
                int imgWidth = finalImage.getWidth();
                int imgHeight = finalImage.getHeight();
                finalImage = finalImage.createBitmap(finalImage, (imgWidth - maskWidth)/2, (imgHeight - maskHeight)/2, maskWidth, maskHeight);
            }
        }
        ((ImageView)uiContainer.get("comicImageView")).setImageBitmap(finalImage);

    }

    private Bitmap getMaskScaledBitmap(Bitmap b, float zoomVal){
        return b.createScaledBitmap(b, (int)(maskWidth * zoomVal), (int)(maskHeight * zoomVal), false);
    }

    private Bitmap maskBitmap(Bitmap b){
        int xStart = 0;
        int yStart = 0;
        if(b.getWidth() > maskWidth){
            xStart = (b.getWidth() - maskWidth)/2;
        }
        if(b.getHeight() > maskHeight){
            yStart = (b.getHeight() - maskHeight)/2;
        }
        return b.createBitmap(b, xStart, yStart, maskWidth, maskHeight);
    }

    private Bitmap getHalfBitmap(Bitmap b, int side){
        if (side == SUBPAGE_RIGHT){
            currentSubPage = SUBPAGE_RIGHT;
            return Bitmap.createBitmap(b, b.getWidth()/2 - extraView,0,b.getWidth()/2 + extraView, b.getHeight());
        }
        if (side == SUBPAGE_LEFT){
            currentSubPage = SUBPAGE_LEFT;
            return Bitmap.createBitmap(b, 0,0,b.getWidth()/2 + extraView, b.getHeight());
        }
        return null;
    }

    private void loadNewPage(int page){
        if(selectedFile.toLowerCase().endsWith(".zip")){
            loadNewZipPage(page, currentCharset);
        } else if (selectedFile.toLowerCase().endsWith(("pdf"))) {
            loadNewPdfPage(page);
        }
        currentPage = page;
    }

    private void loadNewZipPage(int page, Charset ch) {
        try {
            ZipFile zf;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                zf = new ZipFile(selectedFile, ch);
            } else {
                zf = new ZipFile(selectedFile);
            }
            String pageToOpen = (String)totalPageList.get(page);

            ZipEntry ze = new ZipEntry(pageToOpen);
            InputStream is = zf.getInputStream(ze);

            BitmapFactory.Options options = new BitmapFactory.Options();


            //Uses inJustDecodeBound option to prevent out-of-memory by super large image files
            //note ratio is halved if image has width > height
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            int imgWidth = options.outWidth;
            int imgHeight = options.outHeight;

            //Sets SampleSize to save memory. SampleSize halves if width > height (thus bigger image)

            int doubleSize = 1;
            if (imgWidth > imgHeight) {
                doubleSize = 2;
                currentPageUseDoublePage = true;
            }else{
                currentPageUseDoublePage = false;
            }
            if (imgWidth > screenWidth) {
                int ratio = (int) Math.floor((float) imgWidth / ((float) screenWidth * doubleSize));
                options.inSampleSize = ratio;
            }
            options.inJustDecodeBounds = false;


            //Decodes image using options created above
            is = zf.getInputStream(ze);
            currentPageBitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();


            //Handle null image
            if(currentPageBitmap == null){
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                currentPageBitmap = Bitmap.createBitmap(screenWidth, screenHeight, conf);
            }



        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            if (ch != korCharset) {
                loadNewZipPage(page, korCharset);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadNewPdfPage(int index) {
        try {
            // create a new renderer
            File file = new File(selectedFile);
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
            PdfRenderer.Page page = renderer.openPage(index);
            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            getMaskSize(page.getWidth(), page.getHeight());
            int[] optimizedSize = getOptimizedSize();
            currentPageBitmap = Bitmap.createBitmap(optimizedSize[0], optimizedSize[1], conf); // this creates a MUTABLE bitmap
            //Canvas canvas = new Canvas(currentPageBitmap);
            //canvas.drawColor(Color.WHITE);
            //canvas.drawBitmap(currentPageBitmap, 0, 0, null);
            page.render(currentPageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();

            currentPage = index;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int[] getOptimizedSize() {

        float cropSizeTrueValue =  1 + (cropSize/(float)100);

        float optimizedWidth = maskWidth*cropSizeTrueValue;
        float optimizedHeight = maskHeight*cropSizeTrueValue;

        return new int[]{(int) optimizedWidth, (int) optimizedHeight};
    }

    private void getMaskSize(int width, int height){
        float ratio = (float) width / (float) height;
        float screenRatio = (float) screenWidth / (float) screenHeight;
        if(ratio < screenRatio){
            maskHeight = screenHeight;
            maskWidth = (int)(screenHeight*ratio);
        }else{
            maskHeight = (int)(screenWidth/ratio);
            maskWidth = screenWidth;
        }
    }

    private void loadZipFileContent() {
        try {
            ZipFile zf = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                zf = new ZipFile(selectedFile, currentCharset);
            }else{
                zf = new ZipFile(selectedFile);
            }
            Enumeration e = zf.entries();
            zipFileImageContainer = new Hashtable();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                String fileDir = ze.getName();
                if (!ze.isDirectory() && testForImageFileName(fileDir)) {
                    String[] sp = fileDir.split("/");
                    if (sp.length == 1){
                        // This is when file is directly in zipfile
                        //If current directory does not exist, make a new one!
                        if (!zipFileImageContainer.containsKey("root")) {
                            zipFileImageContainer.put("root", new ArrayList<String>());
                        }
                        zipFileImageContainer.get("root").add(fileDir);
                    }else {
                        //If current directory does not exist, make a new one!
                        if (zipFileImageContainer.get(sp[sp.length - 2]) == null) {
                            zipFileImageContainer.put(sp[sp.length - 2], new ArrayList<String>());
                        }

                        //put files in current directory
                        zipFileImageContainer.get(sp[sp.length - 2]).add(fileDir);
                    }
                }
            }

            //Sorting!
            Set set = zipFileImageContainer.keySet();
            set.remove("root");
            ArrayList<String> zipFileOrderedDirectoryListOriginal = new ArrayList<String>();
            zipFileOrderedDirectoryListOriginal.addAll(set);
            List<String> both = new ArrayList<String>();
            zipFileOrderedDirectoryList = new ArrayList<String>();
            if(zipFileImageContainer.containsKey("root")){
                zipFileOrderedDirectoryList.add("root");
            }
            zipFileOrderedDirectoryList.addAll(StaticMethodClass.sort(zipFileOrderedDirectoryListOriginal, sortType));
            /* FILE SORT DISABLED FOR NOW

            for(ArrayList imgList : zipFileImageContainer.values()) {
                Collections.sort(imgList);
            }

            */
            updateTotalPageListForZip();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            currentCharset = changeCharset();
            if(currentCharset == null){
                e.printStackTrace();
            }else{
                loadZipFileContent();
            }
        }
    }

    private void updateTotalPageListForZip(){
        totalPageList = new ArrayList<String>();
        for(int i = 0; i < zipFileOrderedDirectoryList.size(); i++) {
            totalPageList.addAll(zipFileImageContainer.get(zipFileOrderedDirectoryList.get(i)));
        }
        totalPage = totalPageList.size();
    }


    private Charset changeCharset(){
        if(currentCharset != korCharset){
            return korCharset;
        }
        else{
            return null;
        }
    }


    private void loadPdfFileContent() {
        try{
            // create a new renderer
            File file = new File(selectedFile);
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
            // get pagecount
            totalPage = renderer.getPageCount();
            renderer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean testForImageFileName(String s){
        String[] fileFormatList = {".bmp",".gif",".jpg",".png", "jpeg"};
        for(String ff : fileFormatList){
            if(s.toLowerCase().endsWith(ff)){
                return true;
            }
        }
        return false;
    }

    // Following is used for gesture detector function overrides
    class MyGestureDetector implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            if(controlType != CONTROLTYPE_TOUCH) {
                if (Math.abs(v)> Math.abs(v1)) {
                    if (v > 0) {
                        goPrevPage();
                    } else {
                        goNextPage();
                    }
                    return true;
                }
            }
            return false;
        }
    }
}



