package com.fatcow.cowreader;

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

class MyToastClass {

    public static MyToastClass currentToast;

    private Toast toast;


    public MyToastClass(View b, String s){
        View parent = (View)b.getParent();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View toastDesign = inflater.inflate(R.layout.toast_design, (ViewGroup) parent.findViewById(R.id.toast_design_root)); //toast_design.xml 파일의 toast_design_root 속성을 로드

        TextView text = toastDesign.findViewById(R.id.TextView_toast_design);
        text.setText(s); // toast_design.xml 파일에서 직접 텍스트를 지정 가능

        //OffsetCalc
        int yOffset = 0;
        Rect gvr = new Rect();
        if (b.getGlobalVisibleRect(gvr))
        {
            View root = b.getRootView();
            yOffset = root.getBottom() - gvr.bottom;
        }
        //Add 2 dip
        float dip = 2f;
        int px = Math.round(dip * ((float) parent.getContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));

        toast = new Toast(parent.getContext().getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, yOffset + px); // CENTER를 기준으로 0, 0 위치에 메시지 출력
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastDesign);

        MyToastClass.currentToast = this;
    }

    public void show(){
        toast.show();
    }

    public void cancel(){
        toast.cancel();
    }

}
