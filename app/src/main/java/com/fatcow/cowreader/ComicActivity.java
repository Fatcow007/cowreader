package com.fatcow.cowreader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ComicActivity extends AppCompatActivity{


    private final int CONTROLTYPE_BOTH = 253;
    private final int CONTROLTYPE_TOUCH = 254;
    private final int CONTROLTYPE_SLIDE = 255;

    private final int VIEWDIR_NORMAL = 0;
    private final int VIEWDIR_REVERSE = 1;

    public static final int SUBPAGE_LEFT = 0;
    public static final int SUBPAGE_RIGHT = 1;

    private final int extraView = 15; //shows some extra image in pixels

    private String selectedFile;
    private int viewDirection;
    private View currentActiveLayout;
    private Book book;

    private int cropSize;
    private int controlType;
    private int currentSubPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_comic);
        _hideSystemUI();

        if (getIntent().hasExtra("com.example.folderexplorer.SELECTEDFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.SELECTEDFILE");
        }else{
            finish();
        }


        _loadBook();
        _loadPref();
        _initUi();
        _updateUi();
        _reloadPage();
        _savePref();
    }
    @Override
    public void onResume(){
        super.onResume();
        _hideSystemUI();
        _updateUi();
    }


    // This snippet hides the system bars.
    private void _hideSystemUI() {
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

    private Point _getScreenSize(){
        final int version = android.os.Build.VERSION.SDK_INT;
        int width, height;
        Display screen = getWindowManager().getDefaultDisplay();
        if (version >= 13)
        {
            Point size = new Point();
            screen.getSize(size);
            return size;
        }
        else
        {
            width = screen.getWidth();
            height = screen.getHeight();
            return new Point(width, height);
        }
    }

    private String _getFileName(){
        String[] fileNameDir = selectedFile.split("/");
        return fileNameDir[fileNameDir.length-1];
    }

    private void _loadPref(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String fileName = _getFileName();
        cropSize = sharedPref.getInt("ComicActivity." + fileName + ".cropSize", 0);
        currentSubPage = sharedPref.getInt("ComicActivity." + fileName + ".currentSubPage", SUBPAGE_LEFT);
        controlType = sharedPref.getInt("ComicActivity." + fileName + ".controlType", CONTROLTYPE_BOTH);
        viewDirection = sharedPref.getInt("ComicActivity." + fileName + ".viewDirection", VIEWDIR_NORMAL);

    }

    private void _savePref() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String fileName = _getFileName();
        editor.putString("LastViewFile", selectedFile);
        editor.putInt("ComicActivity." + fileName + ".currentPage", book.currentPageNumber);
        editor.putInt("ComicActivity." + fileName + ".totalPage", book.getTotalPageCount());
        editor.putInt("ComicActivity." + fileName + ".cropSize", cropSize);
        editor.putInt("ComicActivity." + fileName + ".controlType", controlType);
        editor.putInt("ComicActivity." + fileName + ".viewDirection", viewDirection);
        editor.putInt("ComicActivity." + fileName + ".currentSubPage", currentSubPage);
        editor.apply();
    }

    private void _loadBook(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        book = new Book(selectedFile, sharedPref.getInt("ComicActivity." + _getFileName() + ".currentPage", 0));
    }

    private void _initUi() {

        //INITIALIZES ALL UI
        final GestureDetector mDetector = new GestureDetector(this, new MyGestureDetector());

        findViewById(R.id.prevBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlType != CONTROLTYPE_SLIDE){
                    _goPrevPage();
                }
            }
        });
        findViewById(R.id.prevBtn).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        findViewById(R.id.nextBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlType != CONTROLTYPE_SLIDE){
                    _goNextPage();
                }
            }
        });
        findViewById(R.id.nextBtn).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        findViewById(R.id.menuBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _updateUi();
                toggleUi(findViewById(R.id.optionUiContainerLayout));
                disableUi(currentActiveLayout);
            }
        });
        findViewById(R.id.menuBtn).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDetector.onTouchEvent(motionEvent);
            }
        });
        findViewById(R.id.option1Btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(currentActiveLayout);
                currentActiveLayout = null;
                // Changes viewing order
                String msg;
                if(viewDirection == VIEWDIR_NORMAL){
                    viewDirection = VIEWDIR_REVERSE;
                    msg = getResources().getString(R.string.toast_left_to_right);
                }else{
                    viewDirection = VIEWDIR_NORMAL;
                    msg = getResources().getString(R.string.toast_right_to_left);
                }
                _showToast(msg);
                _savePref();
                _reloadPage();
            }
        });
        findViewById(R.id.option2Btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(currentActiveLayout);
                currentActiveLayout = findViewById((R.id.pageSeekLayout));
                toggleUi(findViewById(R.id.pageSeekLayout));
            }
        });
        findViewById(R.id.option3Btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(currentActiveLayout);
                currentActiveLayout = findViewById(R.id.cropSizeLayout);
                toggleUi(findViewById(R.id.cropSizeLayout));
            }
        });
        findViewById(R.id.option4Btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableUi(currentActiveLayout);
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
                _showToast(s);
                _savePref();
            }
        });
        findViewById(R.id.option5Btn).setOnClickListener(new DebouncedOnClickListener(200) {
            @Override
            public void onDebouncedClick(View view) {
                disableUi(currentActiveLayout);
                _startChapterListActivity();
                _savePref();
            }
        });

        ((SeekBar)findViewById(R.id.pageSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    _loadNewPage(book.getPageByPageNumber(i));
                }
                String s = getResources().getString(R.string.number_page) + " " + (i + 1) + "/" + book.getTotalPageCount();
                ((TextView)findViewById(R.id.currentPageTextView)).setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                _savePref();
            }
        });

        ((SeekBar)findViewById(R.id.cropSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cropSize = i;
                String s = getResources().getString(R.string.number_crop) + " " + (cropSize);
                ((TextView)findViewById(R.id.cropTextView)).setText(s);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                _reloadPage();
                _savePref();
            }
        });

        ((SeekBar)findViewById(R.id.cropSeekBar)).setMax((100));
        ((SeekBar)findViewById(R.id.cropSeekBar)).setProgress(cropSize);


        disableUi(findViewById(R.id.pageSeekLayout));
        disableUi(findViewById(R.id.cropSizeLayout));
        disableUi(findViewById(R.id.optionUiContainerLayout));


    }

    private void _updateUi(){
        String[] temp = selectedFile.split("/");
        ((TextView)findViewById(R.id.imageFileTitleTextView)).setText(temp[temp.length - 1]);
        ((SeekBar)findViewById(R.id.pageSeekBar)).setProgress(book.currentPageNumber);
        ((SeekBar)findViewById(R.id.pageSeekBar)).setMax(book.getTotalPageCount() - 1);
    }


    private void _startChapterListActivity() {
        ArrayList<String> chapterNames = new ArrayList<>();
        ArrayList<Integer> chapterPages = new ArrayList<>();
        for(Chapter c:book.chapters){
            chapterNames.add(c.chapterName);
            chapterPages.add(c.getChapterPageCount());
        }
        Intent i = new Intent(this, ChapterListActivity.class);
        i.putExtra("com.example.folderexplorer.CHAPTERVIEWFILE", selectedFile);
        i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", book.chapters.indexOf(book.getCurrentChapter()));
        i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGE", book.getCurrentChapter().pages.indexOf(book.getCurrentPage()));
        i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERNAMES", chapterNames);
        i.putExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTERPAGECOUNTS", chapterPages);
        startActivityForResult(i, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                _loadNewPage(book.getFirstPageOfChapter(data.getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", 0)));
            }
            _updateUi();
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


    private void _goPrevPage(){
        disableUi(findViewById(R.id.optionUiContainerLayout));
        if(book.getCurrentPage().requireSubPage()){
            if(currentSubPage == SUBPAGE_RIGHT){
                _loadNewPage(book.getCurrentPage());
            }else{
                _loadNewPage(book.getPrevPage(), SUBPAGE_RIGHT);
            }
        }else{
            _loadNewPage(book.getPrevPage(), SUBPAGE_RIGHT);
        }
        _updateUi();
        _savePref();
    }
    private void _goNextPage(){
        disableUi(findViewById(R.id.optionUiContainerLayout));
        if(book.getCurrentPage().requireSubPage()){
            if(currentSubPage == SUBPAGE_RIGHT){
                _loadNewPage(book.getNextPage());
            }else{
                _loadNewPage(book.getCurrentPage(), SUBPAGE_RIGHT);
            }
        }else{
            _loadNewPage(book.getNextPage());
        }
        _updateUi();
        _savePref();
    }

    private void _loadNewPage(Page p){
        _loadNewPage(p, SUBPAGE_LEFT);
    }
    private void _loadNewPage(Page p, int subPage){
        try {

            Point screenSize = _getScreenSize();
            int screenWidth = screenSize.x;
            int screenHeight = screenSize.y;
            ZipFile zf;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                zf = new ZipFile(selectedFile, book.getCurrentCharset());
            } else {
                zf = new ZipFile(selectedFile);
            }

            ZipEntry ze = new ZipEntry(p.pageImageDirectory);
            InputStream is = zf.getInputStream(ze);

            BitmapFactory.Options options = new BitmapFactory.Options();


            //Uses inJustDecodeBound option to prevent out-of-memory by super large image files

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            int sampleWidth = options.outWidth;
            int sampleHeight = options.outHeight;
            is.close();
            //Sets SampleSize to save memory. SampleSize halves if width > height (thus bigger image)
            options.inSampleSize = Math.max(1, Math.max(sampleWidth/screenSize.x, sampleHeight/screenSize.y)/2);

            options.inJustDecodeBounds = false;


            //Decodes image using options created above

            is = zf.getInputStream(ze);
            Bitmap originalImage = BitmapFactory.decodeStream(is, null, options);
            is.close();

            //Resize image to fit current screen

            int imageWidth = originalImage.getWidth();
            int imageHeight = originalImage.getHeight();

            Bitmap subpageImage = originalImage;
            if(p.requireSubPage()){
                int subPageModifier = 0;
                if(subPage == SUBPAGE_RIGHT ^ viewDirection == VIEWDIR_REVERSE){
                    subPageModifier = imageWidth/2 - extraView;
                }
                imageWidth = imageWidth/2 + extraView;
                subpageImage = Bitmap.createBitmap(originalImage, subPageModifier, 0, imageWidth, imageHeight);
                currentSubPage = subPage;
            }

            Bitmap finalImage;
            float screenRatio = (float)screenWidth/(float)screenHeight;
            float imageRatio = (float)imageWidth/(float)imageHeight;
            float scaleRatio;
            if(imageRatio > screenRatio){
                scaleRatio = (float)(imageWidth)/(float)(screenWidth);
                int trueCropX = (int)(cropSize*scaleRatio);
                int trueCropY = (int)(trueCropX/imageRatio);

                imageWidth = imageWidth - 2*trueCropX;
                imageHeight = imageHeight - 2*trueCropY;
                finalImage = Bitmap.createBitmap(subpageImage, trueCropX, trueCropY, imageWidth, imageHeight);
                finalImage = Bitmap.createScaledBitmap(finalImage, screenWidth, (int)(screenWidth/imageRatio), true);
            }else{
                scaleRatio = imageHeight/screenHeight;

                int trueCropY = (int)(cropSize*scaleRatio);
                int trueCropX = (int)(trueCropY*imageRatio);

                imageWidth = imageWidth - 2*trueCropX;
                imageHeight = imageHeight - 2*trueCropY;
                finalImage = Bitmap.createBitmap(subpageImage, trueCropX, trueCropY, imageWidth, imageHeight);
                finalImage = Bitmap.createScaledBitmap(finalImage, (int)(screenHeight*imageRatio), screenHeight, true);
            }
            //Handle null image
            if(originalImage == null){
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                finalImage = Bitmap.createBitmap(screenWidth, screenHeight, conf);
            }

            //Allocate Bitmap to View
            ((ImageView)findViewById(R.id.comicImageView)).setImageBitmap(finalImage);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void _reloadPage(){
        _loadNewPage(book.getCurrentPage());
    }

    private void _showToast(String s){
        new MyToastClass(findViewById(R.id.option1Btn), s).show();
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
                        _goPrevPage();
                    } else {
                        _goNextPage();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    class Page implements Comparable<Page>{
        public String pageImageDirectory;
        public Chapter parentChapter;
        public Page(String imgDir, Chapter parent){
            pageImageDirectory = imgDir;
            parentChapter = parent;
        }
        public boolean requireSubPage(){
            try{
                ZipFile zf;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    zf = new ZipFile(selectedFile, book.getCurrentCharset());
                } else {
                    zf = new ZipFile(selectedFile);
                }

                ZipEntry ze = new ZipEntry(pageImageDirectory);
                InputStream is = zf.getInputStream(ze);

                BitmapFactory.Options options = new BitmapFactory.Options();


                //Uses inJustDecodeBound option to prevent out-of-memory by super large image files

                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                is.close();
                float ratio = (float)(options.outWidth) / (float)(options.outHeight);
                return ratio > 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        public int compareTo(Page p){
            String[] sp1 = pageImageDirectory.split("/");
            String imageName1 = sp1[sp1.length-1];

            String[] sp2 = p.pageImageDirectory.split("/");
            String imageName2 = sp2[sp2.length-1];
            return new AlphanumComparator().compare(imageName1, imageName2);
        }
    }

    class Chapter implements Comparable<Chapter>{

        public String chapterName;
        public Book parentBook;
        private ArrayList<Page> pages = new ArrayList<>();


        public Chapter(String cName, Book parent){
            chapterName = cName;
            parentBook = parent;
        }

        public void addPage(Page p){
            pages.add(p);
        }

        public void removePage(Page p){
            pages.remove(p);
        }

        public void sortPage(){ Collections.sort(pages); }

        public int getChapterPageCount(){
            return pages.size();
        }

        public Page getPage(int pageNumber){
            return pages.get(pageNumber);
        }


        @Override
        public int compareTo(Chapter c){
            return new AlphanumComparator().compare(chapterName, c.chapterName);
        }
    }

    class Book{
        private int currentPageNumber;
        public ArrayList<Chapter> chapters = new ArrayList<>();
        private final Charset[] charsetList = {Charset.forName("UTF-8"), Charset.forName("cp949")};
        private int charsetIndex = 0;
        public Book(String fileDir, int currentPgNo){
            _loadFile(fileDir);
            currentPageNumber = currentPgNo;
        }


        private void _loadFile(String zipFileDir) {
            try {
                ZipFile zf;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    zf = new ZipFile(zipFileDir, charsetList[charsetIndex]);
                }else{
                    zf = new ZipFile(zipFileDir);
                }
                Enumeration e = zf.entries();

                while (e.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry) e.nextElement();
                    String fileDir= ze.getName();
                    if (!ze.isDirectory() && _testForImageFileName(fileDir)) {
                        String[] sp = fileDir.split("/");
                        if (sp.length == 1){
                            // This is when file is directly in zipfile
                            //If current directory does not exist, make a new one!
                            if (!_hasChapter("root")) {
                                _addChapter("root");
                            }
                            _getChapterByName("root").addPage(new Page(fileDir, _getChapterByName("root")));
                        }else {
                            //If current directory does not exist, make a new one!
                            String folderName = sp[sp.length - 2];
                            if (!_hasChapter(folderName)) {
                                _addChapter(folderName);
                            }
                            _getChapterByName(folderName).addPage(new Page(fileDir, _getChapterByName(folderName)));
                        }
                    }
                }

                //Sort but leave the root chapter on top
                if(_hasChapter("root")){
                    Chapter rootChapter = _getChapterByName("root");
                    chapters.remove(rootChapter);
                    Collections.sort(chapters);
                    chapters.add(0, rootChapter);
                }

                //Sort pages
                for(Chapter c:chapters){
                    c.sortPage();
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e){
                charsetIndex = charsetIndex + 1;
                if(charsetIndex == charsetList.length){
                    e.printStackTrace();
                }else{
                    _loadFile(zipFileDir);
                }
            }
        }


        private void _addChapter(String chapterName){
            if(!_hasChapter(chapterName)){
                chapters.add(new Chapter(chapterName, this));
            }
        }
        private boolean _hasChapter(String chapterName){
            for(Chapter c:chapters){
                if(c.chapterName.equals(chapterName)){
                    return true;
                }
            }
            return false;
        }

        private Chapter _getChapterByName(String chapterName){
            for(Chapter c:chapters){
                if(c.chapterName.equals(chapterName)){
                    return c;
                }
            }
            return null;
        }

        private int _getPageNumber(Page p){
            int pageNo = 0;
            for(Chapter c: chapters){
                if(c.chapterName.equals(p.parentChapter.chapterName)){
                    pageNo = pageNo + p.parentChapter.pages.indexOf(p);
                    break;
                }else{
                    pageNo = pageNo + c.getChapterPageCount();
                }
            }
            return pageNo;
        }


        private boolean _testForImageFileName(String s){
            String[] fileFormatList = {".bmp",".gif",".jpg",".png", "jpeg"};
            for(String ff : fileFormatList){
                if(s.toLowerCase().endsWith(ff)){
                    return true;
                }
            }
            return false;
        }

        public Chapter getChapterByPageNumber(int pageNumber){
            int remainingPageNumber = pageNumber;
            currentPageNumber = pageNumber;
            for(Chapter c:chapters){
                if(c.getChapterPageCount() <= remainingPageNumber){
                    remainingPageNumber = remainingPageNumber - c.getChapterPageCount();
                }else{
                    return c;
                }
            }
            return null;
        }

        public Page getPageByPageNumber(int pageNumber){
            int remainingPageNumber = pageNumber;
            currentPageNumber = pageNumber;
            for(Chapter c:chapters){
                if(c.getChapterPageCount() <= remainingPageNumber){
                    remainingPageNumber = remainingPageNumber - c.getChapterPageCount();
                }else{
                    return c.getPage(remainingPageNumber);
                }
            }
            return null;
        }

        public Page getPrevPage(){
            if(currentPageNumber == 0){
                return null;
            }
            else{
                return getPageByPageNumber(currentPageNumber - 1);
            }
        }

        public Page getNextPage(){
            if(currentPageNumber == getTotalPageCount()){
                return null;
            }
            else{
                return getPageByPageNumber(currentPageNumber + 1);
            }
        }

        public Page getFirstPageOfChapter(int chapterNumber){
            Page p = chapters.get(chapterNumber).getPage(0);
            currentPageNumber = _getPageNumber(p);
            return chapters.get(chapterNumber).getPage(0);
        }

        public int getTotalPageCount(){
            int pc = 0;
            for(Chapter c:chapters){
                pc = pc + c.getChapterPageCount();
            }
            return pc;
        }

        public Page getCurrentPage(){
            return getPageByPageNumber(currentPageNumber);
        }

        public Chapter getCurrentChapter(){
            return getChapterByPageNumber(currentPageNumber);
        }

        public Charset getCurrentCharset(){
            return charsetList[charsetIndex];
        }

    }
}



