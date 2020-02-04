package com.fatcow.cowreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class StaticMethodClass {

    public static ArrayList<String> sort(ArrayList<String> arrayToSort, int sortType){
        if(sortType == ComicActivity.SORTTYPE_BASIC){
            Collections.sort(arrayToSort);
        }
        if(sortType == ComicActivity.SORTTYPE_CUSTOM){
            Collections.sort(arrayToSort, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    ArrayList<String> n1 = new ArrayList<>();
                    ArrayList<String> n2 = new ArrayList<>();
                    n1.addAll(Arrays.asList(s1.split("[^0-9]+")));
                    n2.addAll(Arrays.asList(s2.split("[^0-9]+")));
                    int m = Math.min(n1.size(), n2.size());
                    if(m == 0){
                        return n2.size() - n1.size();
                    }else{
                        int dif = n1.size() - n2.size();
                        if(n1.get(0).equals("")){
                            n1.remove(0);
                        }
                        if(n2.get(0).equals("")){
                            n2.remove(0);
                        }
                        String[] zeros = new String[Math.abs(dif)];
                        Arrays.fill(zeros, "0");
                        if(dif > 0){
                            n2.addAll(0, Arrays.asList(zeros));
                        }else if(dif < 0){
                            n1.addAll(0, Arrays.asList(zeros));
                        }
                        for(int i = 0; i < n1.size(); i++){
                            if(!n1.get(i).equals(n2.get(i))){
                                return Integer.parseInt(n1.get(i)) - Integer.parseInt(n2.get(i));
                            }
                        }
                    }
                    return 0;
                }
            });
        }
        return arrayToSort;
    }

}
