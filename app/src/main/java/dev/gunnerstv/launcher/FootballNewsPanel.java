package dev.gunnerstv.launcher;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;

/** Recent public creator videos. No watch history or recommendation records are fabricated. */
final class FootballNewsPanel extends LinearLayout {
    private final WidgetBoard.Host host;private final Handler main=new Handler(Looper.getMainLooper());
    private final TextView status;private final LinearLayout filters,grid;
    private FootballNewsSource.Result data;private Future<?> pending;private int generation;private boolean loading;private String club="",selected="";
    FootballNewsPanel(WidgetBoard.Host host){super(host.activity());this.host=host;setOrientation(VERTICAL);setClipChildren(false);
        LinearLayout actions=new LinearLayout(getContext());actions.addView(button("Creators",()->NewsSourcesEditor.open(host.activity(),host.unit(),this::sourcesChanged)));actions.addView(button("Refresh videos",()->refresh(true)));addView(actions,new LayoutParams(-1,px(38)));
        status=text("Public YouTube creator videos · Latest seven days",12);status.setPadding(px(6),px(6),0,px(6));addView(status);
        HorizontalScrollView bar=new HorizontalScrollView(getContext());bar.setHorizontalScrollBarEnabled(false);filters=new LinearLayout(getContext());bar.addView(filters);addView(bar,new LayoutParams(-1,px(40)));
        grid=new LinearLayout(getContext());grid.setOrientation(VERTICAL);grid.setClipChildren(false);addView(grid);filters();}
    private int px(float v){return Math.round(v*host.unit());}
    private TextView text(String value,int size){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));t.setTextColor(0xFFF4F5F7);return t;}
    private GradientDrawable bg(boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0xFF35574E:0xFF20313E);d.setCornerRadius(px(12));d.setStroke(px(focus?1.5f:.6f),focus?0xFFA9E6CA:0x30FFFFFF);return d;}
    private TextView button(String title,Runnable action){TextView t=text(title,13);t.setPadding(px(14),px(5),px(14),px(5));t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setId(View.generateViewId());t.setTag("news-action:"+title);t.setBackground(bg(false));t.setOnFocusChangeListener((v,f)->{t.setBackground(bg(f));if(f)host.step(0);});t.setOnClickListener(v->{host.open();action.run();});return t;}
    private void filters(){boolean focused=filters.hasFocus();filters.removeAllViews();TextView all=button(club.isEmpty()?"✓ All clubs":"All clubs",()->{club="";filters();render();});all.setTag("news-filter:all");filters.addView(all);
        for(FootballSelection.Team t:FootballSettings.load(getContext()).clubs.values()){TextView b=button((club.equals(t.id)?"✓ ":"")+t.name,()->{club=t.id;filters();render();});b.setTag("news-filter:"+t.id);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,px(36));p.leftMargin=px(5);filters.addView(b,p);}if(focused)filters.post(()->{View v=filters.findViewWithTag("news-filter:"+(club.isEmpty()?"all":club));if(v!=null)v.requestFocus();});}
    private void sourcesChanged(){generation++;loading=false;if(pending!=null)pending.cancel(true);if(isAttachedToWindow())refresh(false);}
    void refresh(boolean force){if(loading)return;loading=true;final int request=++generation;status.setText("Loading recent creator videos…");Context app=getContext().getApplicationContext();pending=FootballNewsSource.IO.submit(()->{FootballNewsSource.Result result=FootballNewsSource.load(app,force);main.post(()->{if(request!=generation||!isAttachedToWindow())return;loading=false;data=result;filters();render();});});}
    private void render(){View focus=grid.findFocus();if(focus!=null&&focus.getTag()!=null)selected=focus.getTag().toString();grid.removeAllViews();
        if(data==null)return;List<YouTubeFeeds.Video> videos=new ArrayList<>();for(YouTubeFeeds.Video v:data.videos)if(club.isEmpty()||v.clubs.contains(club))videos.add(v);
        status.setText((data.failed>0?"Some feeds unavailable · Cached videos · ":"")+data.sources+" creator feeds · "+videos.size()+" videos in seven days"+(videos.size()>24?" · Showing newest 24":""));
        if(videos.isEmpty()){TextView empty=text(data.sources==0?"Add creators for your followed clubs. The Creators button lets you choose public YouTube channels.":data.failed>0?"No recent cached videos. Check the connection and try Refresh.":"These feeds have no recent uploads in the last seven days.",14);empty.setPadding(px(8),px(18),px(8),px(20));grid.addView(empty);return;}
        SimpleDateFormat date=new SimpleDateFormat("d MMM · HH:mm",Locale.getDefault());
        for(int first=0;first<Math.min(videos.size(),24);first+=3){LinearLayout row=new LinearLayout(getContext());row.setClipChildren(false);
            for(int n=first;n<Math.min(first+3,Math.min(videos.size(),24));n++){YouTubeFeeds.Video v=videos.get(n);FrameLayout card=new FrameLayout(getContext());card.setFocusable(true);card.setId(View.generateViewId());card.setTag("news:"+v.id);card.setBackground(bg(false));card.setClipToOutline(true);card.setContentDescription(v.title+" · "+v.author+" · "+date.format(new Date(v.published)));
                ImageView image=new ImageView(getContext());image.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(image,new FrameLayout.LayoutParams(-1,px(140)));NewsArtwork.load(v.id,image);
                TextView title=text(v.title,13);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,px(40));tp.topMargin=px(146);tp.leftMargin=tp.rightMargin=px(10);card.addView(title,tp);
                TextView by=text(v.author+" · "+date.format(new Date(v.published)),10);by.setSingleLine(true);by.setEllipsize(android.text.TextUtils.TruncateAt.END);by.setTextColor(0xFFB8C5CC);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,px(18));bp.topMargin=px(191);bp.leftMargin=bp.rightMargin=px(10);card.addView(by,bp);
                card.setOnFocusChangeListener((view,f)->{card.setBackground(bg(f));if(f){selected="news:"+v.id;host.step(0);}});card.setOnClickListener(view->{selected="news:"+v.id;host.open();open(v.id);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,px(216),1);if(n>first)p.leftMargin=px(12);row.addView(card,p);}
            while(row.getChildCount()<3){View space=new View(getContext());LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(0,px(216),1);gap.leftMargin=px(12);row.addView(space,gap);}
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=px(10);p.bottomMargin=px(4);grid.addView(row,p);}
        if(focus!=null){View restore=grid.findViewWithTag(selected);if(restore!=null)restore.requestFocus();}
    }
    private void open(String id){Uri uri=Uri.parse(YouTubeFeeds.watch(id));for(String pkg:new String[]{FeedCatalog.YOUTUBE,"com.google.android.youtube"})try{host.activity().startActivity(new Intent(Intent.ACTION_VIEW,uri).setPackage(pkg));return;}catch(ActivityNotFoundException|SecurityException ignored){}
        try{host.activity().startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException|SecurityException unavailable){Toast.makeText(getContext(),"No YouTube or browser handler is available.",Toast.LENGTH_LONG).show();}}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refresh(false);}
    @Override protected void onDetachedFromWindow(){generation++;loading=false;if(pending!=null)pending.cancel(true);super.onDetachedFromWindow();}
}
