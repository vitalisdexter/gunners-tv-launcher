package dev.gunnerstv.launcher;

import android.animation.ValueAnimator;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/** Six genuine shared titles, with three readable cards in a quiet, self-advancing carousel. */
final class UpNextWidget extends Widget {
    static final Uri WATCH_NEXT=Uri.parse("content://android.media.tv/watch_next_program");
    static final String LISTINGS="android.permission.READ_TV_LISTINGS";
    static final int SLOTS=3,MAX_ITEMS=6;
    static final long ADVANCE_MS=7000;
    static final float PAD=18,GAP_X=12,CARD_Y=38;
    /** Posters already decoded, by link: Home rebuilds often (every return from an app), posters rarely change. */
    static final LinkedHashMap<String,Bitmap> posters=new LinkedHashMap<String,Bitmap>(18,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> e){return size()>18;}};
    static final HashMap<String,ArrayList<PosterTarget>> posterLoads=new HashMap<>();
    static final Handler ART_MAIN=new Handler(Looper.getMainLooper());
    static final class Item{String key,title,line,detail,pkg,art;Intent intent;float progress=-1;boolean app,allow;}

    final float cardW=(w-PAD*2-GAP_X*(SLOTS-1))/SLOTS,cardH=cardW*9/16;
    final boolean recommendations;
    final FeedCatalog.Row feedRow;
    final String heading;
    private ContentFeedReader.Result latest;
    private android.app.Dialog browser;
    final Handler handler=new Handler(Looper.getMainLooper());
    final FrameLayout viewport;
    final FrameLayout cards;
    final TextView empty;
    final TextView counter;
    final ArrayList<Item> homeItems=new ArrayList<>();
    final ArrayList<View> homeCards=new ArrayList<>();
    final String prefix,viewportPref;
    int viewportStart,generation;
    boolean loading;
    ValueAnimator slide;
    final Runnable advance=()->{
        if(!canAdvance())return;
        moveTo(CarouselWindow.advance(viewportStart,homeCards.size(),SLOTS),true);
        scheduleAdvance();
    };
    final Runnable settleFocus=this::scheduleAdvance;
    String shown=null;   // null: nothing shown yet, so an empty list still shows its message

    UpNextWidget(Context context,WidgetBoard.Host host){
        this(context,host,false);
    }
    UpNextWidget(Context context,WidgetBoard.Host host,boolean recommendations){
        this(context,host,recommendations,null);
    }
    UpNextWidget(Context context,WidgetBoard.Host host,FeedCatalog.Row row){
        this(context,host,!row.resume,row);
    }
    private UpNextWidget(Context context,WidgetBoard.Host host,boolean recommendations,FeedCatalog.Row row){
        super(context,host,WidgetBoard.width("upnext"));
        this.recommendations=recommendations;
        this.feedRow=row;
        heading=row==null?(recommendations?"Recommended for you":"Continue Watching"):row.title;
        prefix=row==null?(recommendations?"recommendations:":"continue:"):"feed:"+row.id+":";
        viewportPref=prefix+"carousel_start";
        viewportStart=host.prefs().getInt(viewportPref,0);
        TextView caption=text(heading,12.5f,ink(0xCC),"sans-serif-medium");addView(caption,pos(w-140,16,PAD+2,14));
        counter=text("",10.5f,ink(0x80),"sans-serif-medium");counter.setGravity(Gravity.RIGHT);addView(counter,pos(100,16,w-PAD-100,15));
        viewport=new FrameLayout(context){
            @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
                super.onSizeChanged(width,height,oldWidth,oldHeight);
                // Clip this viewport itself; the deliberately oversized strip must not paint over its neighbour.
                setClipBounds(new Rect(0,0,width,height));
            }
            @Override public void addFocusables(ArrayList<View> views,int direction,int mode){
                // Offscreen cards remain addressable by stable tag, but cannot capture focus from another widget.
                for(int i=viewportStart;i<Math.min(viewportStart+SLOTS,homeCards.size());i++)homeCards.get(i).addFocusables(views,direction,mode);
            }
        };
        viewport.setClipChildren(true);viewport.setClipToPadding(true);
        FrameLayout clip=new FrameLayout(context){@Override protected void dispatchDraw(Canvas canvas){int saved=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());super.dispatchDraw(canvas);canvas.restoreToCount(saved);}};
        clip.addView(viewport,new LayoutParams(-1,-1));addView(clip,pos(w,cardH+66,0,CARD_Y-10));
        cards=new FrameLayout(context);cards.setClipChildren(false);cards.setClipToPadding(false);viewport.addView(cards,new LayoutParams(px(w),-1));
        empty=text("Waiting for titles shared by your streaming apps.",12,ink(0x99),"sans-serif");empty.setGravity(Gravity.CENTER);empty.setVisibility(GONE);addView(empty,pos(w-PAD*2,20,PAD,88));
    }
    @Override void refresh(){
        if(loading||!isAttachedToWindow())return;
        loading=true;final int request=++generation;
        final Context app=getContext().getApplicationContext();
        WidgetBoard.IO.execute(()->{
            ContentFeedReader.Result result=FeedCatalog.latest;
            if(result==null)result=new ContentFeedReader(app).read();
            ArrayList<Item> items=load(app,result);
            final ContentFeedReader.Result loaded=result;
            handler.post(()->{if(request!=generation)return;loading=false;if(!isAttachedToWindow())return;latest=loaded;show(items);});
        });
    }

    private ArrayList<Item> load(Context context,ContentFeedReader.Result result){
        java.util.List<ContentFeedPolicy.Item> shared=feedRow==null?(recommendations?result.recommendations:result.continuing):feedRow.select(result);
        if(shared.isEmpty()&&!result.status.listingsGranted){ArrayList<Item> allow=new ArrayList<>();Item i=new Item();i.allow=true;i.key="allow";i.title="Allow shared TV listings";i.line="Continue Watching & recommendations";i.detail="Read titles your streaming apps share with Android TV.";allow.add(i);return allow;}
        return feedItems(shared);
    }
    private ArrayList<Item> feedItems(java.util.List<ContentFeedPolicy.Item> source){
        ArrayList<Item> out=new ArrayList<>();
        for(ContentFeedPolicy.Item r:source){
            Item i=new Item();i.key=r.key();i.pkg=r.pkg;i.title=r.title;i.art=r.art;
            i.line=join(r.app,feedRow!=null&&feedRow.music?r.author:recommendations?"":r.episode);
            int progress=ContentFeedPolicy.progress(r.position,r.duration);
            i.progress=recommendations||progress<0?-1:progress/100f;
            i.detail=join(r.app,join(r.episode,r.description));
            try{i.intent=Intent.parseUri(r.intent,Intent.URI_INTENT_SCHEME);}catch(Exception ignored){}
            out.add(i);
        }
        return out;
    }
    /** Hold OK opens all the published titles, retaining their original app links. */
    private void showAll(){
        if(latest==null||browser!=null)return;
        stopAdvance();
        android.app.Dialog d=new android.app.Dialog(host.activity(),android.R.style.Theme_Material_NoActionBar);browser=d;
        d.setOnDismissListener(dialog->{if(browser==d)browser=null;scheduleAdvance();});
        LinearLayout column=new LinearLayout(getContext());column.setOrientation(LinearLayout.VERTICAL);column.setPadding(px(44),px(30),px(44),px(30));column.setBackgroundColor(0xFF101114);
        TextView heading=text(this.heading,28,Color.WHITE,"sans-serif-medium");column.addView(heading);
        TextView info=text("Shared by your apps · Back to return",12,0xAAFFFFFF,"sans-serif");LinearLayout.LayoutParams il=new LinearLayout.LayoutParams(-1,px(35));il.topMargin=px(10);column.addView(info,il);
        ScrollView scroll=new ScrollView(getContext());LinearLayout grid=new LinearLayout(getContext());grid.setOrientation(LinearLayout.VERTICAL);scroll.addView(grid);column.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        java.util.List<Item> items=feedItems(feedRow==null?(recommendations?latest.recommendations:latest.continuing):feedRow.select(latest));
        for(int start=0;start<items.size();start+=3){
            LinearLayout row=new LinearLayout(getContext());row.setClipChildren(false);row.setPadding(px(10),px(12),px(10),px(18));
            for(int n=start;n<Math.min(start+3,items.size());n++){
                View v=card(items.get(n),n,false);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(px(cardW),px(cardH+48));cp.rightMargin=px(20);row.addView(v,cp);
            }
            grid.addView(row);
        }
        d.setContentView(column);android.view.Window win=d.getWindow();if(win!=null){win.setLayout(-1,-1);win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
        d.show();if(win!=null)win.setLayout(-1,-1);
    }
    static String join(String a,String b){return a==null||a.isEmpty()?(b==null?"":b):b==null||b.isEmpty()?a:a+" · "+b;}
    static String left(long ms){long m=Math.max(1,ms/60000);return m<60?m+" min left":m/60+" h "+(m%60>0?m%60+" min ":"")+"left";}
    static String ago(long at){long m=Math.max(0,(System.currentTimeMillis()-at)/60000);if(m<2)return "Just now";if(m<60)return m+" min ago";if(m<1440)return m/60+" h ago";long d=m/1440;return d==1?"Yesterday":d+" days ago";}

    /** Refreshes retain the actual selected title and viewport; idle movement never requests focus. */
    private void show(ArrayList<Item> items){
        ArrayList<Item> limited=new ArrayList<>(items.subList(0,Math.min(MAX_ITEMS,items.size())));
        StringBuilder key=new StringBuilder();for(Item i:limited)key.append(i.key).append('|').append(i.title).append('|').append(i.line).append('|').append(i.art).append('|').append(i.progress).append(';');
        if(key.toString().equals(shown)){scheduleAdvance();return;}shown=key.toString();
        String focusedKey=null;int focusedIndex=-1;for(int s=0;s<homeCards.size();s++)if(homeCards.get(s).hasFocus()){focusedKey=homeItems.get(s).key;focusedIndex=s;}
        stopAdvance();cards.removeAllViews();homeCards.clear();homeItems.clear();homeItems.addAll(limited);
        empty.setVisibility(limited.isEmpty()?VISIBLE:GONE);
        cards.setLayoutParams(new LayoutParams(px(Math.max(w,PAD*2+limited.size()*cardW+Math.max(0,limited.size()-1)*GAP_X)),-1));
        for(int s=0;s<limited.size();s++){View v=card(limited.get(s),s,true);homeCards.add(v);cards.addView(v,pos(cardW,cardH+46,PAD+s*(cardW+GAP_X),10));}
        moveTo(CarouselWindow.clamp(viewportStart,homeCards.size(),SLOTS),false);
        if(focusedKey!=null&&active&&!homeCards.isEmpty()){int index=indexOf(focusedKey);if(index<0)index=Math.min(focusedIndex,homeCards.size()-1);reveal(index,false);homeCards.get(index).requestFocus();}
        scheduleAdvance();
    }
    private int indexOf(String key){for(int i=0;i<homeItems.size();i++)if(homeItems.get(i).key.equals(key))return i;return -1;}
    private void reveal(int index,boolean animate){moveTo(CarouselWindow.reveal(viewportStart,index,homeCards.size(),SLOTS),animate);}
    private void moveTo(int start,boolean animate){
        viewportStart=CarouselWindow.clamp(start,homeCards.size(),SLOTS);
        if(host.prefs().getInt(viewportPref,-1)!=viewportStart)host.prefs().edit().putInt(viewportPref,viewportStart).apply();
        counter.setText(homeCards.isEmpty()?"":(viewportStart+1)+"–"+Math.min(viewportStart+SLOTS,homeCards.size())+" / "+homeCards.size());
        if(slide!=null){slide.cancel();slide=null;}
        int to=px(viewportStart*(cardW+GAP_X)),from=viewport.getScrollX();
        if(!animate||!motion()||from==to){viewport.scrollTo(to,0);return;}
        slide=ValueAnimator.ofInt(from,to);slide.setDuration(650);slide.setInterpolator(ease);
        slide.addUpdateListener(a->viewport.scrollTo((int)a.getAnimatedValue(),0));slide.start();
    }
    private boolean canAdvance(){return active&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&hasWindowFocus()&&motion()&&browser==null&&!hasFocus()&&homeCards.size()>SLOTS;}
    private void scheduleAdvance(){handler.removeCallbacks(advance);if(canAdvance())handler.postDelayed(advance,ADVANCE_MS);}
    private void stopAdvance(){handler.removeCallbacks(advance);if(slide!=null){slide.cancel();slide=null;}viewport.scrollTo(px(viewportStart*(cardW+GAP_X)),0);}
    @Override void setActive(boolean value){super.setActive(value);if(value)scheduleAdvance();else stopAdvance();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(viewport==null)return;if(focused)scheduleAdvance();else stopAdvance();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(viewport==null)return;if(visibility==VISIBLE)scheduleAdvance();else stopAdvance();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refresh();scheduleAdvance();}

    private View card(Item item,int slot,boolean home){
        Context c=getContext();
        FrameLayout cell=new FrameLayout(c);cell.setClipChildren(false);
        FrameLayout face=new FrameLayout(c);face.setClipToOutline(true);face.setElevation(px(2));
        GradientDrawable bg=round(item.allow?(light()?0x14000000:0x1AFFFFFF):0xFF1C1C1E,12);face.setBackground(bg);
        cell.addView(face,pos(cardW,cardH,0,0));
        if(item.allow){
            Glyph g=new Glyph(c).kind("tv").color(ink(0xD9));face.addView(g,pos(30,30,(cardW-30)/2,cardH/2-24));
            TextView t=text("Allow access",12,ink(0xD9),"sans-serif-medium");t.setGravity(Gravity.CENTER);face.addView(t,pos(cardW,16,0,cardH/2+12));
        } else if(item.app){
            View art=host.art(item.pkg);if(art!=null)face.addView(art,new LayoutParams(-1,-1));
        } else {
            ImageView poster=new ImageView(c);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);face.addView(poster,new LayoutParams(-1,-1));
            View art=host.art(item.pkg);if(art!=null){art.setAlpha(1);face.addView(art,0,new LayoutParams(-1,-1));}
            if(item.art!=null)loadPoster(item.art,poster,art);
            View shade=new View(c);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x00000000,0x8C000000}));face.addView(shade,new LayoutParams(-1,-1));
            if(item.progress>0){
                View track=new View(c);track.setBackground(round(0x59FFFFFF,1.5f));face.addView(track,pos(cardW-20,3,10,cardH-10));
                View fill=new View(c);fill.setBackground(round(Color.WHITE,1.5f));face.addView(fill,pos((cardW-20)*item.progress,3,10,cardH-10));
            }
        }
        GradientDrawable rim=round(Color.TRANSPARENT,12);rim.setStroke(px(1.2f),0xF0FFFFFF);rim.setAlpha(0);face.setForeground(rim);
        TextView title=text(item.title,13,ink(0xF2),"sans-serif-medium");cell.addView(title,pos(cardW,17,1,cardH+9));
        TextView line=text(item.line==null?"":item.line,11.5f,ink(0x99),"sans-serif");cell.addView(line,pos(cardW,15,1,cardH+28));
        face.setPivotX(px(cardW/2));face.setPivotY(px(cardH/2));
        cell.setFocusable(true);cell.setFocusableInTouchMode(true);cell.setSoundEffectsEnabled(false);cell.setContentDescription(item.title);
        if(home){
            cell.setTag(prefix+item.key);
            cell.setOnKeyListener((v,keyCode,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN||(keyCode!=KeyEvent.KEYCODE_DPAD_LEFT&&keyCode!=KeyEvent.KEYCODE_DPAD_RIGHT))return false;
                int next=slot+(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);
                if(next<0||next>=homeCards.size())return false;
                reveal(next,true);homeCards.get(next).requestFocus();return true;
            });
        }
        cell.setOnFocusChangeListener((v,f)->{
            face.animate().cancel();title.animate().cancel();line.animate().cancel();float s=f?1.08f:1;
            if(motion()){face.animate().scaleX(s).scaleY(s).translationY(f?-px(2):0).setDuration(f?460:300).setInterpolator(f?spring:ease).start();
                title.animate().translationY(f?px(5):0).setDuration(300).setInterpolator(ease).start();line.animate().translationY(f?px(5):0).setDuration(300).setInterpolator(ease).start();}
            else{face.setScaleX(s);face.setScaleY(s);}
            face.setElevation(px(f?14:2));rim.setAlpha(f?220:0);cell.setTranslationZ(px(f?20:0));
            if(f){
                if(home){handler.removeCallbacks(advance);reveal(slot,true);}
                host.hero(prefix+item.key,item.title,item.detail);host.step(slot);
            }else if(home){handler.removeCallbacks(settleFocus);handler.post(settleFocus);}
        });
        cell.setOnClickListener(v->{host.open();press(face,1.08f);open(item);});
        cell.setOnLongClickListener(v->{showAll();return true;});
        return cell;
    }
    private void open(Item item){
        if(item.allow){
            ((MainActivity)host.activity()).requestContentAccess();
            return;
        }
        if(item.intent!=null){
            try{Intent safe=new Intent(item.intent);safe.setSelector(null);safe.setClipData(null);
                if(safe.getComponent()!=null&&!item.pkg.equals(safe.getComponent().getPackageName())){host.launch(item.pkg);return;}
                safe.setPackage(item.pkg);safe.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);host.activity().startActivity(safe);return;}catch(RuntimeException ignored){}
        }
        host.launch(item.pkg);
    }
    /** Loads a poster off the main thread (content:, android.resource: or https: links), scaled to the card. */
    private void loadPoster(String link,ImageView view,View fallback){
        Bitmap cached=posters.get(link);if(cached!=null){view.setImageBitmap(cached);if(fallback!=null)fallback.setVisibility(INVISIBLE);return;}
        PosterTarget target=new PosterTarget(view,fallback,this);
        ArrayList<PosterTarget> waiting=posterLoads.get(link);if(waiting!=null){waiting.add(target);return;}
        waiting=new ArrayList<>();waiting.add(target);posterLoads.put(link,waiting);
        final Context context=getContext().getApplicationContext();final int width=px(cardW)*2;
        WidgetBoard.ART.execute(()->{
            Bitmap b=null;
            try{
                byte[] data=read(context,link);
                BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);
                int sample=RemoteData.imageSample(o.outWidth,o.outHeight,width);
                o=new BitmapFactory.Options();o.inSampleSize=sample;b=BitmapFactory.decodeByteArray(data,0,data.length,o);
            }catch(Exception|OutOfMemoryError ignored){}
            final Bitmap result=b;
            ART_MAIN.post(()->{
                ArrayList<PosterTarget> targets=posterLoads.remove(link);if(result==null)return;
                posters.put(link,result);if(targets==null)return;
                for(PosterTarget t:targets){ImageView image=t.view.get();View backup=t.fallback.get();UpNextWidget owner=t.owner.get();
                    if(image==null||owner==null||!image.isAttachedToWindow())continue;
                    image.setImageBitmap(result);image.setAlpha(0f);image.animate().alpha(1).setDuration(owner.motion()?320:0).withEndAction(()->{if(backup!=null)backup.setVisibility(INVISIBLE);}).start();
                }
            });
        });
    }
    private static final class PosterTarget{
        final WeakReference<ImageView> view;final WeakReference<View> fallback;final WeakReference<UpNextWidget> owner;
        PosterTarget(ImageView image,View backup,UpNextWidget widget){view=new WeakReference<>(image);fallback=new WeakReference<>(backup);owner=new WeakReference<>(widget);}
    }
    @Override protected void onDetachedFromWindow(){active=false;generation++;loading=false;stopAdvance();handler.removeCallbacks(settleFocus);if(browser!=null)browser.dismiss();super.onDetachedFromWindow();}
    private static byte[] read(Context context,String link)throws Exception{
        if(link.startsWith("res:"))return RemoteData.read(context.getResources().openRawResource(Integer.parseInt(link.substring(4))),6_000_000);
        Uri uri=Uri.parse(link);String scheme=uri.getScheme();
        if("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme))return RemoteData.get(link,6_000_000,true);
        if(!"content".equals(scheme)&&!"android.resource".equals(scheme))throw new java.io.IOException("Unsupported artwork URI");
        return RemoteData.read(context.getContentResolver().openInputStream(uri),6_000_000);
    }
}
