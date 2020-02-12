package com.fatcow.cowreader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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

    private View currentActiveLayout;
    private Book book;

    private String selectedFile;
    private int viewDirection;
    private int cropSize;
    private int controlType;
    private int currentSubPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_comic);
        hideSystemUI();

        if (getIntent().hasExtra("com.example.folderexplorer.SELECTEDFILE")) {
            selectedFile = getIntent().getStringExtra("com.example.folderexplorer.SELECTEDFILE");
        }else{
            finish();
        }

        loadBook();
        loadPref();
        initUi();
    }
    @Override
    public void onResume(){
        super.onResume();
        hideSystemUI();
        updateUi();
        reloadPage();
        savePref();
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
        String[] fileNameDir = selectedFile.split("/");
        return fileNameDir[fileNameDir.length-1];
    }

    private void loadPref(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String fileName = getFileName();
        cropSize = sharedPref.getInt("ComicActivity." + fileName + ".cropSize", 0);
        currentSubPage = sharedPref.getInt("ComicActivity." + fileName + ".currentSubPage", SUBPAGE_LEFT);
        controlType = sharedPref.getInt("ComicActivity." + fileName + ".controlType", CONTROLTYPE_BOTH);
        viewDirection = sharedPref.getInt("ComicActivity." + fileName + ".viewDirection", VIEWDIR_NORMAL);
    }

    private void savePref() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String fileName = getFileName();
        editor.putString("LastViewFile", selectedFile);
        editor.putInt("ComicActivity." + fileName + ".currentPage", book.currentPageNumber);
        editor.putInt("ComicActivity." + fileName + ".totalPage", book.getTotalPageCount());
        editor.putInt("ComicActivity." + fileName + ".cropSize", cropSize);
        editor.putInt("ComicActivity." + fileName + ".controlType", controlType);
        editor.putInt("ComicActivity." + fileName + ".viewDirection", viewDirection);
        editor.putInt("ComicActivity." + fileName + ".currentSubPage", currentSubPage);
        editor.apply();
    }

    private void loadBook(){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        book = new Book(selectedFile, sharedPref.getInt("ComicActivity." + getFileName() + ".currentPage", 0));
        if(book.totalPage == 0){
            showToast(getResources().getString(R.string.comic_no_image_file));
            finish();
        }
    }

    private void initUi() {

        //INITIALIZES ALL UI
        final GestureDetector mDetector = new GestureDetector(this, new MyGestureDetector());

        findViewById(R.id.prevBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(controlType != CONTROLTYPE_SLIDE){
                    goPrevPage();
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
                    goNextPage();
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
                updateUi();
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
                showToast(msg);
                savePref();
                reloadPage();
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
                showToast(s);
                savePref();
            }
        });
        findViewById(R.id.option5Btn).setOnClickListener(new DebouncedOnClickListener(200) {
            @Override
            public void onDebouncedClick(View view) {
                disableUi(currentActiveLayout);
                startChapterListActivity();
                savePref();
            }
        });

        ((SeekBar)findViewById(R.id.pageSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    loadNewPage(book.getPageByPageNumber(i));
                }
                String s = getResources().getString(R.string.number_page) + " " + (i + 1) + "/" + book.getTotalPageCount();
                ((TextView)findViewById(R.id.currentPageTextView)).setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePref();
            }
        });

        ((SeekBar)findViewById(R.id.cropSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    cropSize = i;
                }
                String s = getResources().getString(R.string.number_crop) + " " + (cropSize);
                ((TextView)findViewById(R.id.cropTextView)).setText(s);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                reloadPage();
                savePref();
            }
        });

        ((SeekBar)findViewById(R.id.cropSeekBar)).setMax((50));
        ((SeekBar)findViewById(R.id.cropSeekBar)).setProgress(cropSize);


        disableUi(findViewById(R.id.pageSeekLayout));
        disableUi(findViewById(R.id.cropSizeLayout));
        disableUi(findViewById(R.id.optionUiContainerLayout));

        findViewById(R.id.comicImageView).post(new Runnable() {
            @Override
            public void run() {
                reloadPage();
            }
        });


    }

    private void updateUi(){
        String[] temp = selectedFile.split("/");
        ((TextView)findViewById(R.id.imageFileTitleTextView)).setText(temp[temp.length - 1]);
        ((SeekBar)findViewById(R.id.pageSeekBar)).setProgress(book.currentPageNumber);
        ((SeekBar)findViewById(R.id.pageSeekBar)).setMax(book.getTotalPageCount() - 1);
    }


    private void startChapterListActivity() {
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
                loadNewPage(book.getFirstPageOfChapter(data.getIntExtra("com.example.folderexplorer.CHAPTERVIEWCHAPTER", 0)));
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


    private void goPrevPage(){
        disableUi(findViewById(R.id.optionUiContainerLayout));
        if(book.getCurrentPage().requireSubPage()){
            if(currentSubPage == SUBPAGE_RIGHT){
                loadNewPage(book.getCurrentPage());
            }else{
                loadNewPage(book.getPrevPage(), SUBPAGE_RIGHT);
            }
        }else{
            loadNewPage(book.getPrevPage(), SUBPAGE_RIGHT);
        }
        updateUi();
        savePref();
    }
    private void goNextPage(){
        disableUi(findViewById(R.id.optionUiContainerLayout));
        if(book.getCurrentPage().requireSubPage()){
            if(currentSubPage == SUBPAGE_RIGHT){
                loadNewPage(book.getNextPage());
            }else{
                loadNewPage(book.getCurrentPage(), SUBPAGE_RIGHT);
            }
        }else{
            loadNewPage(book.getNextPage());
        }
        updateUi();
        savePref();
    }

    private void loadNewPage(BasePage p){
        loadNewPage(p, SUBPAGE_LEFT);
    }
    private void loadNewPage(BasePage p, int subPage){
        try {
                View civ = findViewById(R.id.comicImageView);
            int screenWidth = civ.getWidth();
            int screenHeight = civ.getHeight();
            if(screenWidth == 0 || screenHeight == 0){
                return;
            }
            Bitmap originalImage = null;
            if(p instanceof ZipPage){
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
                options.inSampleSize = Math.max(1, Math.max(sampleWidth/screenWidth, sampleHeight/screenHeight)/2);

                options.inJustDecodeBounds = false;


                //Decodes image using options created above

                is = zf.getInputStream(ze);
                originalImage = BitmapFactory.decodeStream(is, null, options);
                is.close();
            }else if(p instanceof PdfPage){
                File f = new File(p.pageImageDirectory);
                PdfRenderer renderer = null;
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
                if (fileDescriptor != null) {
                    renderer = new PdfRenderer(fileDescriptor);
                }
                PdfRenderer.Page pg = renderer.openPage(((PdfPage) p).pageIndex);
                int[] colors = new int[pg.getWidth()*pg.getHeight()*4];
                Arrays.fill(colors, Color.WHITE);
                originalImage = Bitmap.createBitmap(colors,pg.getWidth()*2, pg.getHeight()*2, Bitmap.Config.ARGB_8888);
                pg.render(originalImage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                pg.close();

                // close the renderer
                renderer.close();
            }

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
            if(imageRatio > screenRatio){
                int trueCropX = (int)(cropSize/200.0*imageWidth);
                imageWidth = imageWidth - 2*trueCropX;

                float newImageRatio = (float)imageWidth/(float)imageHeight;
                int trueCropY = 0;
                if(newImageRatio < screenRatio){
                    int newImageHeight = (int)(imageWidth/screenRatio);
                    trueCropY = (imageHeight - newImageHeight)/2;
                    imageHeight = newImageHeight;
                }

                newImageRatio = (float)imageWidth/(float)imageHeight;
                finalImage = Bitmap.createBitmap(subpageImage, trueCropX, trueCropY, imageWidth, imageHeight);
                finalImage = Bitmap.createScaledBitmap(finalImage, screenWidth, (int)(screenWidth/newImageRatio), true);
            }else{
                int trueCropY = (int)(cropSize/200.0*imageHeight);
                imageWidth = imageWidth - 2*trueCropY;

                float newImageRatio = (float)imageWidth/(float)imageHeight;
                int trueCropX = 0;
                if(newImageRatio > screenRatio){
                    int newImageWidth = (int)(imageHeight*screenRatio);
                    trueCropX = (imageWidth - newImageWidth)/2;
                    imageHeight = newImageWidth;
                }

                newImageRatio = (float)imageWidth/(float)imageHeight;
                finalImage = Bitmap.createBitmap(subpageImage, trueCropX, trueCropY, imageWidth, imageHeight);
                finalImage = Bitmap.createScaledBitmap(finalImage, (int)(screenHeight*newImageRatio), screenHeight, true);
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

    private void reloadPage(){
        loadNewPage(book.getCurrentPage(), currentSubPage);
    }

    private void showToast(String s){
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

    abstract class BasePage implements Comparable<BasePage>{
        protected Chapter parentChapter;
        protected String pageImageDirectory;

        private BasePage(String imgDir, Chapter parent){
            parentChapter = parent;
            pageImageDirectory = imgDir;
        }
        abstract boolean requireSubPage();


    }

    class ZipPage extends BasePage{
        private ZipPage(String imgDir, Chapter parent) {
            super(imgDir, parent);
        }

        @Override
        protected boolean requireSubPage(){
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
        public int compareTo(BasePage p){
            String[] sp1 = pageImageDirectory.split("/");
            String imageName1 = sp1[sp1.length-1];

            String[] sp2 = p.pageImageDirectory.split("/");
            String imageName2 = sp2[sp2.length-1];
            return new AlphanumComparator().compare(imageName1, imageName2);
        }
    }

    class PdfPage extends BasePage{

        private int pageIndex;
        private PdfPage(String imgDir, Chapter parent, int pg) {
            super(imgDir, parent);
            pageIndex = pg;
        }
        @Override
        protected boolean requireSubPage(){
            boolean result = false;
            try{
                File f = new File(pageImageDirectory);
                PdfRenderer renderer = null;
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
                if (fileDescriptor != null) {
                    renderer = new PdfRenderer(fileDescriptor);
                }

                PdfRenderer.Page pdfPage = renderer.openPage(pageIndex);
                result = ((float)pdfPage.getWidth()/(float)pdfPage.getHeight()) > 1;

                // close the renderer
                renderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        public int compareTo(BasePage p){
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
        private ArrayList<BasePage> pages = new ArrayList<>();


        public Chapter(String cName, Book parent){
            chapterName = cName;
            parentBook = parent;
        }

        public void addPage(BasePage p){
            pages.add(p);
        }

        public void removePage(BasePage p){
            pages.remove(p);
        }

        public int getChapterPageCount(){
            return pages.size();
        }

        public BasePage getPage(int pageNumber){
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
        private int totalPage = 0;
        public Book(String fileDir, int currentPgNo){
            loadFile(fileDir);
            currentPageNumber = currentPgNo;
        }

        private void loadFile(String fileDir){
            if(fileDir.toLowerCase().endsWith("zip")){
                loadZipFile(fileDir);
            }else if(fileDir.toLowerCase().endsWith("pdf")){
                loadPdfFile(fileDir);
            }
        }
        private void loadZipFile(String zipFileDir) {
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
                    if (!ze.isDirectory() && testForImageFileName(fileDir)) {
                        String[] sp = fileDir.split("/");
                        if (sp.length == 1){
                            // This is when file is directly in zipfile
                            //If current directory does not exist, make a new one!
                            if (!hasChapter("root")) {
                                addChapter("root");
                            }
                            getChapterByName("root").addPage(new ZipPage(fileDir, getChapterByName("root")));
                        }else {
                            //If current directory does not exist, make a new one!
                            String folderName = sp[sp.length - 2];
                            if (!hasChapter(folderName)) {
                                addChapter(folderName);
                            }
                            getChapterByName(folderName).addPage(new ZipPage(fileDir, getChapterByName(folderName)));
                        }
                    }
                }

                //Sort but leave the root chapter on top
                if(hasChapter("root")){
                    Chapter rootChapter = getChapterByName("root");
                    chapters.remove(rootChapter);
                    Collections.sort(chapters);
                    chapters.add(0, rootChapter);
                }else{
                    Collections.sort(chapters);
                }

                //Sort pages
                for(Chapter c:chapters){
                    Collections.sort(c.pages);
                }


                //Count total pages
                totalPage = 0;
                for(Chapter c:chapters){
                    totalPage = totalPage + c.getChapterPageCount();
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e){
                charsetIndex = charsetIndex + 1;
                if(charsetIndex == charsetList.length){
                    e.printStackTrace();
                }else{
                    loadFile(zipFileDir);
                }
            }
        }

        private void loadPdfFile(String pdfFileDir){
            try{
                File f = new File(pdfFileDir);
                PdfRenderer renderer = null;
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
                if (fileDescriptor != null) {
                    renderer = new PdfRenderer(fileDescriptor);
                }
                addChapter("PDF");
                totalPage = renderer.getPageCount();
                for (int i = 0; i < totalPage; i++) {
                    getChapterByName("PDF").addPage(new PdfPage(pdfFileDir, getChapterByName("PDF"), i));
                    /*
                    PdfRenderer.Page page = renderer.openPage(i);

                    // say we render for showing on the screen
                    page.render(mBitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY);

                    // do stuff with the bitmap

                    // close the page
                    page.close();
                    */
                }

                // close the renderer
                renderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addChapter(String chapterName){
            if(!hasChapter(chapterName)){
                chapters.add(new Chapter(chapterName, this));
            }
        }
        private boolean hasChapter(String chapterName){
            for(Chapter c:chapters){
                if(c.chapterName.equals(chapterName)){
                    return true;
                }
            }
            return false;
        }

        private Chapter getChapterByName(String chapterName){
            for(Chapter c:chapters){
                if(c.chapterName.equals(chapterName)){
                    return c;
                }
            }
            return null;
        }

        private int getPageNumber(BasePage p){
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


        private boolean testForImageFileName(String s){
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

        public BasePage getPageByPageNumber(int pageNumber){
            int remainingPageNumber = pageNumber;
            if(pageNumber > getTotalPageCount() - 1){
                remainingPageNumber = getTotalPageCount() - 1;
            }
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

        private BasePage getPrevPage(){
            if(currentPageNumber == 0){
                showToast(getResources().getString(R.string.toast_first_page));
                return null;
            }
            else{
                return getPageByPageNumber(currentPageNumber - 1);
            }
        }

        private BasePage getNextPage(){
            if(currentPageNumber == getTotalPageCount() - 1){
                showToast(getResources().getString(R.string.toast_last_page));
                return null;
            }
            else{
                return getPageByPageNumber(currentPageNumber + 1);
            }
        }

        private BasePage getFirstPageOfChapter(int chapterNumber){
            BasePage p = chapters.get(chapterNumber).getPage(0);
            currentPageNumber = getPageNumber(p);
            return chapters.get(chapterNumber).getPage(0);
        }

        private int getTotalPageCount(){
            return totalPage;
        }

        private BasePage getCurrentPage(){
            return getPageByPageNumber(currentPageNumber);
        }

        private Chapter getCurrentChapter(){
            return getChapterByPageNumber(currentPageNumber);
        }

        private Charset getCurrentCharset(){
            return charsetList[charsetIndex];
        }

    }
}



