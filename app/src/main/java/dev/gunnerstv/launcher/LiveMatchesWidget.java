package dev.gunnerstv.launcher;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Selected live fixtures take priority; otherwise show Arsenal's next scheduled game. */
final class LiveMatchesWidget extends Widget {
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final ArrayList<ArsenalMatch> live=new ArrayList<>();
    private final TextView title,count,home,away,score,status,stamp;
    private final ArsenalBadge homeCrest,awayCrest;
    private int index;private boolean loading,offline;
    private final Runnable tick=this::refresh;
    LiveMatchesWidget(Context c,WidgetBoard.Host host){
        super(c,host,WidgetBoard.width("arsenal"));setTag("arsenal");
        View pitch=new View(c){final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas canvas){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(px(.8f));p.setColor(0x13FFF3E3);canvas.drawRoundRect(px(15),px(36),getWidth()-px(15),getHeight()-px(20),px(5),px(5),p);canvas.drawLine(getWidth()/2,px(36),getWidth()/2,getHeight()-px(20),p);canvas.drawCircle(getWidth()/2,getHeight()/2,px(25),p);}};addView(pitch,new LayoutParams(-1,-1));
        title=text("MATCH CENTRE",13.5f,0xFFFFF3E3,"sans-serif-medium");addView(title,pos(w-130,20,20,14));
        count=text("",10,0xFFA2F5BA,"sans-serif-medium");count.setGravity(Gravity.RIGHT);addView(count,pos(90,18,w-110,15));
        homeCrest=new ArsenalBadge(c,host.unit(),56);awayCrest=new ArsenalBadge(c,host.unit(),56);addView(homeCrest,pos(56,56,64,42));addView(awayCrest,pos(56,56,w-120,42));
        home=text("",14,0xFFFFF3E3,"sans-serif-medium");away=text("",14,0xFFFFF3E3,"sans-serif-medium");home.setGravity(Gravity.CENTER);away.setGravity(Gravity.CENTER);addView(home,pos(144,22,20,106));addView(away,pos(144,22,w-164,106));
        score=text("—",34,0xFFFFF3E3,"sans-serif-light");score.setGravity(Gravity.CENTER);addView(score,pos(128,48,(w-128)/2,46));
        status=text("Checking your fixtures…",12,0xFFFFF3E3,"sans-serif-medium");status.setGravity(Gravity.CENTER);addView(status,pos(w-40,20,20,137));
        stamp=text("",10,0xBBFFF3E3,"sans-serif");stamp.setGravity(Gravity.CENTER);addView(stamp,pos(w-40,18,20,163));
        control(this,0,1.025f,this::skin,()->new String[]{"live-matches",live.isEmpty()?"Match centre":live.get(index).live()?"Live matches":"Arsenal's next game",live.isEmpty()?"Waiting for fixtures":live.get(index).title()+" · "+(live.get(index).live()?live.get(index).score():time(live.get(index).kickoff,"EEE d MMM · HH:mm"))},()->{if(!live.isEmpty())MatchCentre.open(host,live.get(index),this::requestFocus);});
        setOnKeyListener((v,key,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN||live.size()<2||(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT))return false;
            index=Math.floorMod(index+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1),live.size());paint();host.step(index);return true;});
    }
    private void skin(boolean focus){GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{focus?0xFFB52640:0xF5911D33,0xF443101D});bg.setCornerRadius(px(22));bg.setStroke(px(focus?1.5f:.7f),focus?0xFFFFF3E3:0x40FFF3E3);setBackground(bg);}
    @Override void setActive(boolean value){super.setActive(value);timer.removeCallbacks(tick);if(value)refresh();}
    @Override void refresh(){if(!active||loading)return;loading=true;WidgetBoard.IO.execute(()->{
        ArsenalSource.Snapshot data=FootballSource.shared(getContext()).load(true);post(()->{loading=false;if(!isAttachedToWindow())return;String selected=live.isEmpty()?"":live.get(index).id;live.clear();live.addAll(FootballPolicy.widgetMatches(data.matches,System.currentTimeMillis()));index=0;for(int i=0;i<live.size();i++)if(live.get(i).id.equals(selected))index=i;offline=data.offline;paint();timer.removeCallbacks(tick);if(active)timer.postDelayed(tick,60000);});
    });}
    private String time(long at,String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.UK);f.setTimeZone(TimeZone.getTimeZone("Europe/London"));return f.format(new Date(at));}
    private void paint(){
        boolean empty=live.isEmpty();homeCrest.setVisibility(empty?INVISIBLE:VISIBLE);awayCrest.setVisibility(empty?INVISIBLE:VISIBLE);
        if(empty){title.setText("MATCH CENTRE");home.setText("");away.setText("");score.setText("—");count.setText("");status.setText("Arsenal's next fixture unavailable");stamp.setText(offline?"Cached data · Waiting for connection":"Waiting for the fixture schedule");setContentDescription(status.getText());return;}
        ArsenalMatch m=live.get(index);boolean playing=m.live();title.setText(playing?"LIVE MATCHES":"ARSENAL · NEXT MATCH");home.setText(m.home);away.setText(m.away);homeCrest.bind(m.home,m.homeShort,m.homeLogo);awayCrest.bind(m.away,m.awayShort,m.awayLogo);score.setText(playing?m.score():time(m.kickoff,"HH:mm"));count.setText(playing?"● LIVE  "+(index+1)+"/"+live.size():"UP NEXT");count.setTextColor(playing?0xFFA2F5BA:0xDDFFF3E3);
        status.setText(playing?MatchDetail.join(m.minute,m.status):time(m.kickoff,"EEE d MMM")+" · "+m.league);stamp.setText((offline?"Cached · ":"")+(playing?(live.size()>1?"‹  Left / Right for all live games  ›":"OK · Lineups & commentary"):"OK · Preview & head to head"));setContentDescription(m.title()+" · "+(playing?"LIVE · "+m.score()+" · "+m.minute+" · "+(index+1)+" of "+live.size():"Arsenal's next game · "+time(m.kickoff,"EEE d MMM HH:mm")));
    }
    @Override protected void onDetachedFromWindow(){timer.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
