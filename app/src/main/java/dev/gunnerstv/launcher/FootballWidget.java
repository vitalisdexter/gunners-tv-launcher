package dev.gunnerstv.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Three fixture cards, a remote-driven carousel and a match centre with confirmed player selection. */
final class FootballWidget extends Widget {
    private final FootballSource source;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final FrameLayout strip;
    private final ArrayList<ArsenalMatch> matches=new ArrayList<>();
    private final ArrayList<View> cards=new ArrayList<>();
    private final TextView status;
    private ArsenalSource.Snapshot data;
    private boolean loading;
    private int start;
    private Dialog dialog;
    private LinearLayout details;
    private String selected="";
    private final Runnable refreshTick=this::refresh;
    private final Runnable rotate=()->{if(canRotate()){move(CarouselWindow.advance(start,cards.size(),3));schedule();}};
    FootballWidget(Context c,WidgetBoard.Host host){
        super(c,host,864);source=FootballSource.shared(c);start=host.prefs().getInt("football_start",0);
        addView(text("Football · Your clubs & players",13,ink(0xE6),"sans-serif-medium"),pos(360,18,20,13));
        status=text("Loading fixtures…",10,ink(0x99),"sans-serif");status.setGravity(Gravity.RIGHT);addView(status,pos(440,16,404,15));
        strip=new FrameLayout(c){
            @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){super.onSizeChanged(w,h,oldW,oldH);setClipBounds(new android.graphics.Rect(0,0,w,h));}
            @Override public void addFocusables(ArrayList<View> out,int direction,int mode){for(int i=start;i<Math.min(start+3,cards.size());i++)cards.get(i).addFocusables(out,direction,mode);}
        };
        strip.setClipChildren(true);strip.setClipToPadding(true);
        // A stationary outer clip is needed: Android translates a scrolled view's own clip bounds.
        FrameLayout viewport=new FrameLayout(c){@Override protected void dispatchDraw(android.graphics.Canvas canvas){int saved=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());super.dispatchDraw(canvas);canvas.restoreToCount(saved);}};
        viewport.addView(strip,new LayoutParams(-1,-1));addView(viewport,pos(864,150,0,34));
    }
    @Override void setActive(boolean value){super.setActive(value);timer.removeCallbacks(refreshTick);timer.removeCallbacks(rotate);if(value){refresh();schedule();}else if(dialog!=null)dialog.dismiss();}
    @Override void refresh(){
        if(!active||loading)return;loading=true;
        WidgetBoard.IO.execute(()->{ArsenalSource.Snapshot fresh=source.load(true);post(()->{
            loading=false;if(!isAttachedToWindow())return;data=fresh;render();
            if(dialog!=null&&details!=null)renderDetails();
            if(active){timer.removeCallbacks(refreshTick);timer.postDelayed(refreshTick,60000);}
        });});
    }
    private void render(){
        String focus="";for(int i=0;i<cards.size();i++)if(cards.get(i).hasFocus())focus=matches.get(i).id;
        matches.clear();matches.addAll(data.matches.subList(0,Math.min(6,data.matches.size())));cards.clear();strip.removeAllViews();
        status.setText(data.matches.isEmpty()?data.error:(data.offline?"Cached · ":"ESPN · ")+"By kickoff · Hold OK for all fixtures");
        for(int i=0;i<matches.size();i++){
            final int index=i;ArsenalMatch m=matches.get(i);View card=matchCard(m,264,136,false);card.setTag("match:"+m.id);
            card.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN||(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT))return false;
                int next=index+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);if(next<0||next>=cards.size())return false;
                move(CarouselWindow.reveal(start,next,cards.size(),3));cards.get(next).requestFocus();return true;
            });
            cards.add(card);strip.addView(card,pos(264,136,20+i*280,5));
        }
        move(CarouselWindow.clamp(start,cards.size(),3));
        if(!focus.isEmpty())for(int i=0;i<matches.size();i++)if(focus.equals(matches.get(i).id)){move(CarouselWindow.reveal(start,i,cards.size(),3));cards.get(i).requestFocus();break;}
        schedule();
    }
    private View matchCard(ArsenalMatch m,float width,float height,boolean inDialog){
        FrameLayout card=new FrameLayout(getContext());card.setClipChildren(false);
        int tint=m.playerNames.isEmpty()?0xFF203642:0xFF3B2853;
        Runnable skin=()->{GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{tint,0xF5131923});bg.setCornerRadius(px(15));bg.setStroke(px(card.hasFocus()?1.5f:.6f),card.hasFocus()?0xFFF7F0DE:0x30FFFFFF);card.setBackground(bg);};
        skin.run();TextView competition=text(m.league,10,0xB3FFFFFF,"sans-serif-medium");card.addView(competition,pos(width-20,15,10,9));
        if(m.live()){View dot=new View(getContext());dot.setBackground(oval(0xFFA2F5BA));card.addView(dot,pos(7,7,width-18,13));}
        ArsenalBadge home=new ArsenalBadge(getContext(),host.unit(),34),away=new ArsenalBadge(getContext(),host.unit(),34);
        home.bind(m.home,m.homeShort,m.homeLogo);away.bind(m.away,m.awayShort,m.awayLogo);card.addView(home,pos(34,34,25,31));card.addView(away,pos(34,34,width-59,31));
        TextView score=text("pre".equals(m.state)?time(m.kickoff,"HH:mm"):m.score(),23,Color.WHITE,"sans-serif-light");score.setGravity(Gravity.CENTER);card.addView(score,pos(width-128,33,64,32));
        TextView names=text(m.home+"  ·  "+m.away,12,0xF2FFFFFF,"sans-serif-medium");names.setGravity(Gravity.CENTER);card.addView(names,pos(width-20,19,10,71));
        TextView state=text(matchStatus(m),10.5f,m.live()?0xFF96F5B7:0xBBFFFFFF,"sans-serif-medium");state.setGravity(Gravity.CENTER);card.addView(state,pos(width-20,17,10,92));
        TextView followed=text(m.following().isEmpty()?"OK · Match centre":m.following(),9,0xBBDBD3FF,"sans-serif");followed.setGravity(Gravity.CENTER);card.addView(followed,pos(width-20,15,10,114));
        card.setFocusable(true);card.setFocusableInTouchMode(true);card.setSoundEffectsEnabled(false);
        card.setContentDescription(m.title()+" · "+matchStatus(m)+" · "+m.following());
        card.setOnFocusChangeListener((v,focused)->{skin.run();if(focused){timer.removeCallbacks(rotate);host.step(0);if(!inDialog)host.hero("football:"+m.id,m.title(),m.league+" · "+matchStatus(m)+(m.following().isEmpty()?"":" · "+m.following()));}else schedule();});
        card.setOnClickListener(v->{host.open();selected=m.id;MatchCentre.open(host,m,()->{if(v.isAttachedToWindow())v.requestFocus();});});
        card.setOnLongClickListener(v->{selected=m.id;openDetails();return true;});return card;
    }
    private void move(int next){start=next;strip.scrollTo(px(start*280),0);host.prefs().edit().putInt("football_start",start).apply();}
    private boolean canRotate(){return active&&isAttachedToWindow()&&isShown()&&hasWindowFocus()&&motion()&&!hasFocus()&&dialog==null&&cards.size()>3;}
    private void schedule(){timer.removeCallbacks(rotate);if(canRotate())timer.postDelayed(rotate,7000);}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)schedule();else timer.removeCallbacks(rotate);}
    private String time(long at,String format){SimpleDateFormat f=new SimpleDateFormat(format,Locale.UK);f.setTimeZone(TimeZone.getTimeZone("Europe/London"));return f.format(new Date(at));}
    private String matchStatus(ArsenalMatch m){return m.live()?"LIVE · "+m.minute+" · "+m.status:"post".equals(m.state)?m.status:time(m.kickoff,"EEE d MMM · HH:mm")+(m.status.toLowerCase(Locale.ROOT).contains("postpon")?" · Postponed":"");}
    private void openDetails(){
        timer.removeCallbacks(rotate);if(dialog!=null){renderDetails();return;}
        dialog=new Dialog(host.activity(),android.R.style.Theme_Material_NoActionBar);
        ScrollView scroll=new ScrollView(getContext());scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF192635,0xFF15121E}));
        details=new LinearLayout(getContext());details.setOrientation(LinearLayout.VERTICAL);details.setPadding(px(48),px(25),px(48),px(32));scroll.addView(details);dialog.setContentView(scroll);
        dialog.setOnDismissListener(d->{dialog=null;details=null;for(View card:cards)if(("match:"+selected).equals(card.getTag())){int n=cards.indexOf(card);move(CarouselWindow.reveal(start,n,cards.size(),3));card.requestFocus();break;}schedule();});
        dialog.show();Window win=dialog.getWindow();if(win!=null){win.setLayout(-1,-1);win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);win.getDecorView().setSystemUiVisibility(5894);}
        renderDetails();
    }
    private void line(String value,float size,int color){TextView t=text(value,size,color,"sans-serif");t.setSingleLine(false);t.setPadding(0,px(6),0,px(6));details.addView(t,new LinearLayout.LayoutParams(-1,-2));}
    private void renderDetails(){
        if(details==null||data==null)return;View previous=details.findFocus();String tag=previous==null?"":String.valueOf(previous.getTag());details.removeAllViews();
        line("Football · Match centre",28,Color.WHITE);line("Arsenal · Chelsea · Man City · Man United · Liverpool · Tottenham",11,0x99FFFFFF);
        line("Following Cristiano Ronaldo & Kylian Mbappé across club and international competitions",11,0xBBBDA8F6);
        ArsenalMatch chosen=null;for(ArsenalMatch m:data.matches)if(m.id.equals(selected)){chosen=m;break;}if(chosen==null&&!data.matches.isEmpty())chosen=data.matches.get(0);
        if(chosen!=null){
            line(chosen.title()+"   "+("pre".equals(chosen.state)?time(chosen.kickoff,"HH:mm"):chosen.score()),26,Color.WHITE);
            line(chosen.league+" · "+matchStatus(chosen),13,0xCCFFFFFF);
            if(!chosen.following().isEmpty())line(chosen.following(),14,0xFFD7C4FF);
            if(chosen.goals.isEmpty())line("pre".equals(chosen.state)?"Lineups and goal events appear when the provider publishes them.":"No goal events reported in this feed.",12,0x99FFFFFF);
            else for(String goal:chosen.goals)line("⚽  "+goal,14,0xFFEADFC4);
            line("ESPN · "+(data.offline?"Cached · ":"")+"Updated "+time(chosen.updated,"d MMM HH:mm")+" · UK time",10,0x88FFFFFF);
        }
        line("Fixtures & recent results",18,Color.WHITE);
        for(int start=0;start<data.matches.size();start+=3){
            LinearLayout row=new LinearLayout(getContext());row.setPadding(px(2),px(6),px(2),px(10));
            for(int n=start;n<Math.min(start+3,data.matches.size());n++){
                ArsenalMatch m=data.matches.get(n);View card=matchCard(m,272,136,true);card.setTag("detail:"+m.id);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(px(272),px(136));p.rightMargin=px(16);row.addView(card,p);
                if(tag.equals(card.getTag()))card.requestFocus();
            }
            details.addView(row);
        }
        if(details.findFocus()==null){View v=details.findViewWithTag("detail:"+selected);if(v!=null)v.requestFocus();}
    }
    private void refreshDetail(ArsenalMatch m){
        WidgetBoard.IO.execute(()->{boolean ok=true;try{source.details(m);}catch(Exception e){ok=false;}final boolean success=ok;post(()->{if(!isAttachedToWindow())return;if(!success)Toast.makeText(getContext(),"Could not refresh. Showing saved match information.",Toast.LENGTH_SHORT).show();if(dialog!=null)renderDetails();});});
    }
    @Override protected void onDetachedFromWindow(){active=false;timer.removeCallbacksAndMessages(null);if(dialog!=null)dialog.dismiss();super.onDetachedFromWindow();}
}
