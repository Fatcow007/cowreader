package com.fatcow.cowreader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Hashtable;

public class ChapterAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private ArrayList<String> chapters;
    private int currentChap;
    private Hashtable<String, ArrayList> zipFileHash;
    private int currentPage;


    public ChapterAdapter(Context c, ArrayList<String> ch, int currentC, int pg, Hashtable<String, ArrayList> h){
        mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(ch != null){
            chapters = ch;
        }else{
            chapters = new ArrayList<String>();
        }
        currentChap = currentC;
        currentPage = pg;
        zipFileHash = h;
    }

    @Override
    public int getCount() {
        return chapters.size();
    }

    @Override
    public String getItem(int i) {
        return chapters.get(i);
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

        chapterTextView.setText(chapters.get(i));
        int totalPage = zipFileHash.get(chapters.get(i)).size();
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
