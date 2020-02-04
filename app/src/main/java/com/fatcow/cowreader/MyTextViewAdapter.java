package com.fatcow.cowreader;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyTextViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private ArrayList<String> mStringArrayList;
    private int textSize;
    private int lineHeight;

    public MyTextViewAdapter(Activity a, ArrayList<String> s, int t, int l){
        this.mContext = a;
        this.mStringArrayList = s;
        textSize = t;
        lineHeight = l;
    }

    @Override
    public MyTextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_line_layout,
                parent, false);
        return new MyTextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final String item = mStringArrayList.get(position);
        MyTextViewAdapter.MyTextViewHolder myTextViewHolder = (MyTextViewAdapter.MyTextViewHolder)holder;
        if (item != null) {
            myTextViewHolder.singleLineTextView.setTextSize((float)(textSize * 0.4 + 5));
            myTextViewHolder.singleLineTextView.setLineSpacing(lineHeight,  1);
            myTextViewHolder.singleLineTextView.setPadding(0, 0, 0, lineHeight);
            myTextViewHolder.singleLineTextView.setText(item);
        }

    }

    @Override
    public int getItemCount() {
        return mStringArrayList.size();
    }

    public class MyTextViewHolder extends RecyclerView.ViewHolder{

        TextView singleLineTextView;

        public MyTextViewHolder(View itemView) {
            super(itemView);
            singleLineTextView = itemView.findViewById(R.id.singleLineTextView);

        }

    }
}
