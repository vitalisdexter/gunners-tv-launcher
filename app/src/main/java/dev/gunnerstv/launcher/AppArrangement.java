package dev.gunnerstv.launcher;

import java.util.*;

/** A private reorder draft. Only explicit confirmation should persist order(). */
final class AppArrangement {
    private final ArrayList<String> order,visible;
    final String selected;
    final int columns;
    AppArrangement(List<String> order,List<String> visible,String selected,int columns){
        if(columns<1||!visible.contains(selected)||new HashSet<>(order).size()!=order.size()
            ||new HashSet<>(visible).size()!=visible.size()||!order.containsAll(visible))throw new IllegalArgumentException("Invalid app arrangement");
        this.order=new ArrayList<>(order);this.visible=new ArrayList<>(visible);this.selected=selected;this.columns=columns;
    }
    List<String> items(){return Collections.unmodifiableList(visible);}
    ArrayList<String> order(){return new ArrayList<>(order);}
    int index(){return visible.indexOf(selected);}
    boolean move(int dx,int dy){
        if(Math.abs(dx)+Math.abs(dy)!=1)return false;
        int from=index(),column=from%columns;
        if(dx<0&&column==0||dx>0&&column==columns-1)return false;
        int to=from+dx+dy*columns;
        if(to<0||to>=visible.size())return false;
        String target=visible.get(to);Collections.swap(order,order.indexOf(selected),order.indexOf(target));Collections.swap(visible,from,to);return true;
    }
}
