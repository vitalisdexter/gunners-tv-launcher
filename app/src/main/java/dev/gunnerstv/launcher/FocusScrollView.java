package dev.gunnerstv.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.widget.ScrollView;

/** A remote-driven list that keeps the selected row visible after focus or reorder. */
final class FocusScrollView extends ScrollView {
    private final int halo;
    FocusScrollView(Context c,int margin){
        super(c);halo=margin;setVerticalScrollBarEnabled(false);setClipToPadding(false);
        setSmoothScrollingEnabled(false);setFocusable(false);setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setPadding(0,margin,0,margin);
    }
    @Override public void requestChildFocus(View child,View focused){super.requestChildFocus(child,focused);post(()->reveal(focused));}
    void reveal(View focused){
        if(focused==null||!focused.isAttachedToWindow()||getChildCount()==0||getHeight()==0)return;
        Rect r=new Rect(0,0,focused.getWidth(),focused.getHeight());offsetDescendantRectToMyCoords(focused,r);
        int height=getChildAt(0).getBottom()+getPaddingBottom();
        scrollTo(0,FocusViewport.target(getScrollY(),getHeight(),height,r.top,r.bottom,halo));
    }
    @Override public View focusSearch(View focused,int direction){
        if(direction==FOCUS_UP||direction==FOCUS_DOWN){
            View next=FocusFinder.getInstance().findNextFocus(this,focused,direction);return next==null?focused:next;
        }
        return super.focusSearch(focused,direction);
    }
}
