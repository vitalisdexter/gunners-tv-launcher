package dev.gunnerstv.launcher;

/** Keeps a focused row and its focus halo inside a bounded scrolling viewport. */
final class FocusViewport {
    static int target(int current,int viewport,int content,int top,int bottom,int margin){
        int max=Math.max(0,content-viewport),next=Math.max(0,Math.min(max,current));
        if(bottom-top+2*margin>viewport)next=top-margin;
        else if(top-margin<next)next=top-margin;
        else if(bottom+margin>next+viewport)next=bottom+margin-viewport;
        return Math.max(0,Math.min(max,next));
    }
}
