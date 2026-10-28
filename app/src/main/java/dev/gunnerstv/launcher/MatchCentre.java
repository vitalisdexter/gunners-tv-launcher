package dev.gunnerstv.launcher;

import android.app.Dialog;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** A TV matchday dashboard: real data, four browsable sections and stable remote focus. */
final class MatchCentre extends Dialog {
    private static final int WHITE=0xFFF4F5F7,MUTED=0xFF9DAAB7,MINT=0xFFA9E6CA;
    private static final String[] TITLES={"Overview","Head to head","Lineups","Commentary"};
    private static MatchCentre current;
    private final WidgetBoard.Host host;
    private final ArsenalMatch match;
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final ArrayList<TextView> tabs=new ArrayList<>();
    private final int[] positions=new int[4];
    private final int homeAccent,awayAccent;
    private FrameLayout hero;
    private LinearLayout body;
    private ScrollView scroll;
    private TextView stamp,refreshButton;
    private int tab;
    private boolean loading,foreground;
    private String error="";
    private final Runnable refreshTick=this::refresh;
    static void dismissOpen(){if(current!=null)current.dismiss();}
    static void open(WidgetBoard.Host host,ArsenalMatch match,Runnable returned){
        dismissOpen();MatchCentre centre=new MatchCentre(host,match);current=centre;
        centre.setOnDismissListener(d->{centre.timer.removeCallbacksAndMessages(null);if(current==centre)current=null;returned.run();});centre.show();
    }
    private MatchCentre(WidgetBoard.Host host,ArsenalMatch match){
        super(host.activity(),android.R.style.Theme_Material_NoActionBar);this.host=host;this.match=match;
        homeAccent=accent(match.homeId);awayAccent=accent(match.awayId);
    }
    private static int accent(String id){switch(id){
        case "359":case "364":case "360":return 0xFFB94452;
        case "363":return 0xFF3269C4;case "382":return 0xFF67A6CA;
        case "367":case "86":return 0xFF8D93B0;case "817":return 0xFFCBAD42;
        default:return 0xFF528B84;
    }}
    private int px(float v){return Math.round(v*host.unit());}
    private GradientDrawable round(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(px(radius));return d;}
    private GradientDrawable panel(int start,int end,float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(px(radius));d.setStroke(px(.7f),0x24FFFFFF);return d;}
    private TextView text(String value,float size,int color){
        TextView t=new TextView(getContext());t.setText(value);t.setTextColor(color);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));
        t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);return t;
    }
    private TextView label(String value,float size,int color){TextView t=text(value,size,color);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;}
    private FrameLayout.LayoutParams pos(float w,float h,float x,float y){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w<0?(int)w:px(w),h<0?(int)h:px(h));p.leftMargin=px(x);p.topMargin=px(y);return p;}
    private TextView button(String title,Runnable action){
        TextView t=label(title,12.5f,WHITE);t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setId(View.generateViewId());t.setSoundEffectsEnabled(false);
        t.setBackground(round(0x16FFFFFF,11));
        t.setOnFocusChangeListener((v,f)->{t.setTextColor(f?0xFF14221E:WHITE);t.setBackground(round(f?MINT:0x16FFFFFF,11));lift(t,f);});
        t.setOnClickListener(v->{host.open();action.run();});return t;
    }
    private void lift(View v,boolean focus){
        v.animate().cancel();if(host.motion())v.animate().scaleX(focus?1.025f:1).scaleY(focus?1.025f:1).setDuration(180).start();
        else {v.setScaleX(1);v.setScaleY(1);}
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout column=new LinearLayout(getContext());column.setOrientation(LinearLayout.VERTICAL);column.setPadding(px(48),px(22),px(48),px(14));
        column.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF152530,0xFF0E151E,0xFF1B1927}));
        FrameLayout top=new FrameLayout(getContext());
        top.addView(button("‹  Fixtures",this::dismiss),pos(100,34,0,0));
        TextView kicker=label("M A T C H D A Y",10,MINT);top.addView(kicker,pos(230,15,120,0));
        top.addView(label("Football centre",20,WHITE),pos(400,26,119,14));
        refreshButton=button("↻  Refresh",this::refresh);top.addView(refreshButton,pos(108,34,756,0));
        column.addView(top,new LinearLayout.LayoutParams(-1,px(40)));
        hero=new FrameLayout(getContext());hero.setClipToOutline(true);hero.setBackground(panel(0xFF24323D,0xFF171F2B,22));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,px(148));hp.topMargin=px(10);column.addView(hero,hp);
        LinearLayout nav=new LinearLayout(getContext());nav.setPadding(px(4),px(4),px(4),px(4));nav.setBackground(round(0x66101821,15));
        for(int n=0;n<TITLES.length;n++){
            final int target=n;TextView b=label(TITLES[n],13,WHITE);b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setId(View.generateViewId());b.setSoundEffectsEnabled(false);b.setTag("match-tab:"+n);
            b.setOnFocusChangeListener((v,f)->{lift(b,f);if(f){host.step(target);select(target,true);}paintTabs();});
            b.setOnClickListener(v->{host.open();select(target,true);});
            b.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){
                    int next=Math.max(0,Math.min(3,target+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));tabs.get(next).requestFocus();return true;
                }
                return false;
            });
            tabs.add(b);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(n>0)p.leftMargin=px(4);nav.addView(b,p);
        }
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,px(44));np.topMargin=px(12);np.bottomMargin=px(6);column.addView(nav,np);
        scroll=new ScrollView(getContext());scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setFillViewport(false);
        body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);body.setPadding(px(5),px(4),px(5),px(8));body.setClipChildren(false);scroll.addView(body);
        FrameLayout clip=new FrameLayout(getContext()){@Override protected void dispatchDraw(Canvas c){int s=c.save();c.clipRect(0,0,getWidth(),getHeight());super.dispatchDraw(c);c.restoreToCount(s);}};
        clip.addView(scroll,new FrameLayout.LayoutParams(-1,-1));column.addView(clip,new LinearLayout.LayoutParams(-1,0,1));
        stamp=label("",9.5f,MUTED);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,px(18));sp.topMargin=px(4);column.addView(stamp,sp);
        setContentView(column);render();
    }
    @Override public void show(){super.show();Window w=getWindow();if(w!=null){w.setLayout(-1,-1);w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);w.getDecorView().setSystemUiVisibility(5894);}tabs.get(0).requestFocus();refresh();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);foreground=focused;timer.removeCallbacks(refreshTick);if(focused&&isShowing())timer.postDelayed(refreshTick,60000);}
    private void refresh(){
        timer.removeCallbacks(refreshTick);if(loading||!isShowing())return;loading=true;refreshButton.setText("Updating…");stamp.setText("Updating match details…");
        WidgetBoard.IO.execute(()->{String failure="";try{FootballSource.shared(getContext()).details(match);}catch(Exception e){failure="Connection unavailable · Showing saved match details";}final String problem=failure;
            timer.post(()->{loading=false;if(!isShowing())return;error=problem;refreshButton.setText("↻  Refresh");render();if(foreground)timer.postDelayed(refreshTick,match.live()?60000:5*60000);});
        });
    }
    private String time(long at,String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.UK);f.setTimeZone(TimeZone.getTimeZone("Europe/London"));return f.format(new Date(at));}
    private String state(){return match.live()?"● LIVE  ·  "+MatchDetail.join(match.minute,match.status):"post".equals(match.state)?match.status:"KICKOFF  ·  "+time(match.kickoff,"EEE d MMM · HH:mm");}
    private TextView centered(String value,float size,int color){TextView t=label(value,size,color);t.setGravity(Gravity.CENTER);return t;}
    private void render(){
        hero.removeAllViews();hero.addView(new Pitch(),new FrameLayout.LayoutParams(-1,-1));
        TextView league=centered(match.league.toUpperCase(Locale.UK),10.5f,0xCCFFFFFF);league.setLetterSpacing(.10f);hero.addView(league,pos(440,18,212,12));
        ArsenalBadge h=new ArsenalBadge(getContext(),host.unit(),72),a=new ArsenalBadge(getContext(),host.unit(),72);h.bind(match.home,match.homeShort,match.homeLogo);a.bind(match.away,match.awayShort,match.awayLogo);
        hero.addView(h,pos(72,72,116,30));hero.addView(a,pos(72,72,676,30));
        hero.addView(centered(match.home,19,WHITE),pos(274,27,15,108));hero.addView(centered(match.away,19,WHITE),pos(274,27,575,108));
        TextView result=centered("pre".equals(match.state)?time(match.kickoff,"HH:mm"):match.score(),51,WHITE);result.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));hero.addView(result,pos(310,65,277,35));
        TextView status=centered(state(),10.5f,match.live()?MINT:0xDDE4ECF0);status.setBackground(round(match.live()?0x285EDBA7:0x16FFFFFF,12));hero.addView(status,pos(310,25,277,107));
        renderBody(true);paintTabs();stamp.setText(error.isEmpty()?"ESPN  ·  "+(match.updated>0?"Updated "+time(match.updated,"HH:mm"):"Awaiting details")+"   /   UK time     ·     ← → Switch tabs     ·     ↓ Explore     ·     Back to fixtures":error);
    }
    private void select(int target,boolean animate){
        if(body==null||target==tab){paintTabs();return;}positions[tab]=scroll.getScrollY();tab=target;renderBody(false);paintTabs();
        if(animate&&host.motion()){body.animate().cancel();body.setAlpha(.45f);body.setTranslationY(px(5));body.animate().alpha(1).translationY(0).setDuration(200).start();}
    }
    private void openTab(int target){tabs.get(target).requestFocus();select(target,true);}
    private void paintTabs(){
        if(tabs.isEmpty())return;
        for(int n=0;n<tabs.size();n++){
            TextView b=tabs.get(n);boolean focus=b.hasFocus(),selected=n==tab;
            b.setText(TITLES[n]);b.setTextColor(focus?0xFF14221E:selected?MINT:MUTED);
            GradientDrawable bg=round(focus?MINT:selected?0x243EA781:Color.TRANSPARENT,11);if(selected&&!focus)bg.setStroke(px(.7f),0x5589D8BC);b.setBackground(bg);
        }
    }
    private FrameLayout card(String tag,float height,Runnable click){
        FrameLayout frame=new FrameLayout(getContext());frame.setTag(tag);frame.setId(View.generateViewId());frame.setFocusable(true);frame.setSoundEffectsEnabled(false);
        frame.setMinimumHeight(px(height));frame.setBackground(panel(0xD823313E,0xD119222E,16));frame.setPadding(0,0,0,0);
        frame.setOnFocusChangeListener((v,f)->{GradientDrawable d=panel(f?0xFF30483F:0xD823313E,f?0xFF243A35:0xD119222E,16);d.setStroke(px(f?1.2f:.7f),f?MINT:0x24FFFFFF);frame.setBackground(d);if(f)host.step(0);});
        if(click!=null)frame.setOnClickListener(v->{host.open();click.run();});
        return frame;
    }
    private void title(FrameLayout card,String value){TextView t=label(value,10,MINT);t.setLetterSpacing(.08f);card.addView(t,pos(350,16,16,13));}
    private void section(String title,String detail){
        FrameLayout line=new FrameLayout(getContext());line.addView(label(title,13,WHITE),pos(480,22,3,4));TextView d=label(detail,10,MUTED);d.setGravity(Gravity.RIGHT);line.addView(d,pos(320,18,530,7));body.addView(line,new LinearLayout.LayoutParams(-1,px(32)));
    }
    private void addFull(View v,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?-2:px(height));p.bottomMargin=px(9);body.addView(v,p);}
    private void addPair(View first,View second,int height){
        LinearLayout row=new LinearLayout(getContext());row.setClipChildren(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,height<0?-2:px(height),1);row.addView(first,p);
        LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,height<0?-2:px(height),1);q.leftMargin=px(14);row.addView(second,q);addFull(row,-1);
    }
    private void empty(String tag,String headline,String explanation,Runnable action){
        FrameLayout c=card(tag,110,action);title(c,headline.toUpperCase(Locale.UK));TextView t=text(explanation,14,MUTED);t.setLineSpacing(px(3),1);c.addView(t,pos(760,54,18,42));addFull(c,110);
    }
    private void renderBody(boolean preserve){
        if(body==null)return;int y=scroll.getScrollY();View focused=body.findFocus();Object key=focused==null?null:focused.getTag();body.removeAllViews();MatchDetail d=match.detail;
        if(tab==0)overview(d);else if(tab==1)history(d);else if(tab==2)lineups(d);else commentary(d);
        if(preserve&&key!=null){View target=body.findViewWithTag(key);if(target!=null)target.requestFocus();}
        int target=preserve?y:positions[tab];scroll.post(()->scroll.scrollTo(0,target));
    }
    private void overview(MatchDetail d){
        section(match.live()?"Match live":"post".equals(match.state)?"Match report":"Match preview",time(match.kickoff,"EEEE d MMMM"));
        FrameLayout info=card("overview:info",118,null);title(info,"MATCH INFORMATION");
        TextView venue=text(d!=null&&!d.venue.isEmpty()?d.venue:"Venue to be confirmed",18,WHITE);venue.setMaxLines(1);venue.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(venue,pos(380,27,16,38));
        info.addView(label(d!=null&&!d.location.isEmpty()?d.location:match.league,12,MUTED),pos(380,18,16,69));
        info.addView(label(d!=null&&!d.referee.isEmpty()?"Referee · "+d.referee:"UK kickoff · "+time(match.kickoff,"EEE d MMM, HH:mm"),11,0xCCD9E3E8),pos(380,18,16,93));
        FrameLayout form=card("overview:form",118,()->openTab(1));title(form,"RECENT FORM");
        formRow(form,match.home,d==null?"":d.homeForm,39);formRow(form,match.away,d==null?"":d.awayForm,76);addPair(info,form,118);
        FrameLayout meeting=card("overview:meeting",97,()->openTab(1));title(meeting,"HEAD TO HEAD   ›");
        if(d!=null&&!d.h2h.isEmpty()){
            ArsenalMatch last=d.h2h.get(0);meeting.addView(label(last.home+"  "+last.score()+"  "+last.away,16,WHITE),pos(380,25,16,37));
            meeting.addView(label(time(last.kickoff,"d MMM yyyy")+"  ·  "+d.h2h.size()+" previous meetings",11,MUTED),pos(380,18,16,68));
        }else{meeting.addView(label("Previous meetings",17,WHITE),pos(380,24,16,37));meeting.addView(label(loading?"Loading match history…":"No meetings supplied yet",11,MUTED),pos(380,18,16,68));}
        FrameLayout teams=card("overview:lineups",97,()->openTab(2));title(teams,"TEAM SHEETS   ›");
        int announced=0;if(d!=null)for(MatchDetail.Lineup l:d.lineups)if(l.confirmed)announced++;
        teams.addView(label(announced==2?"Both starting XIs confirmed":announced==1?"One starting XI confirmed":"Awaiting team news",17,WHITE),pos(380,25,16,37));
        teams.addView(label(announced>0?"Explore starters, formations and substitutes":"Published near kickoff when available",11,MUTED),pos(380,18,16,68));addPair(meeting,teams,97);
        if(!match.following().isEmpty()){FrameLayout followed=card("overview:following",58,null);title(followed,"PLAYERS YOU FOLLOW");followed.addView(label(match.following(),12,0xFFD6C2F2),pos(795,20,16,33));addFull(followed,60);}
        if(!match.goals.isEmpty()){section("Goal events",match.goals.size()+" reported");for(int i=0;i<match.goals.size();i++)event("goal:"+i,match.goals.get(i),true,i==0);}
        if(match.live()){FrameLayout feed=card("overview:feed",60,()->openTab(3));title(feed,"LIVE COMMENTARY   ›");feed.addView(label(d!=null&&!d.commentary.isEmpty()?d.commentary.size()+" latest updates available":"Open the match feed",13,WHITE),pos(790,22,16,32));addFull(feed,62);}
    }
    private void formRow(FrameLayout card,String team,String raw,float y){
        card.addView(label(team,12.5f,WHITE),pos(160,21,16,y+4));String value=raw==null?"":raw.replaceAll("[^WDL]","");
        if(value.isEmpty()){card.addView(label("Not available",11,MUTED),pos(200,20,186,y+4));return;}
        if(value.length()>5)value=value.substring(value.length()-5);
        for(int i=0;i<value.length();i++){char c=value.charAt(i);int fill=c=='W'?0xFF306752:c=='D'?0xFF78633B:0xFF713F4E;TextView chip=centered(String.valueOf(c),11,WHITE);chip.setBackground(round(fill,7));card.addView(chip,pos(27,27,186+i*35,y));}
    }
    private void history(MatchDetail d){
        section("Recent meetings","Latest first");
        if(d==null||d.h2h.isEmpty()){empty("history:empty","No history available","Previous meetings will appear here when the match feed supplies them.",null);return;}
        int[] balance=MatchDetail.balance(match,d.h2h);FrameLayout summary=card("history:balance",86,null);
        summary.addView(centered(match.home,12,MUTED),pos(250,19,10,12));summary.addView(centered("DRAW",10,MUTED),pos(250,19,292,12));summary.addView(centered(match.away,12,MUTED),pos(250,19,574,12));
        summary.addView(centered(""+balance[0],29,WHITE),pos(250,39,10,34));summary.addView(centered(""+balance[1],29,0xFFD5C399),pos(250,39,292,34));summary.addView(centered(""+balance[2],29,WHITE),pos(250,39,574,34));
        View bar=new View(getContext());bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{homeAccent,0xFF7A715D,awayAccent}));summary.addView(bar,pos(814,3,14,81));addFull(summary,87);
        for(ArsenalMatch m:d.h2h){
            FrameLayout row=card("history:"+m.id,73,null);row.addView(label(time(m.kickoff,"d MMM"),13,WHITE),pos(100,20,16,15));row.addView(label(time(m.kickoff,"yyyy"),10,MUTED),pos(80,18,16,40));
            ArsenalBadge h=new ArsenalBadge(getContext(),host.unit(),31),a=new ArsenalBadge(getContext(),host.unit(),31);h.bind(m.home,m.homeShort,m.homeLogo);a.bind(m.away,m.awayShort,m.awayLogo);row.addView(h,pos(31,31,121,16));row.addView(a,pos(31,31,674,16));
            TextView home=label(m.home,14,WHITE);home.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(home,pos(205,27,163,18));
            TextView result=centered(m.score(),22,MINT);result.setBackground(round(0xFF182C28,10));row.addView(result,pos(100,39,378,12));row.addView(label(m.away,14,WHITE),pos(180,27,490,22));
            row.addView(label(m.league,9,MUTED),pos(650,16,121,52));addFull(row,74);
        }
    }
    private void lineups(MatchDetail d){
        section("Team sheets","Starting XI · Formations · Substitutes");
        MatchDetail.Lineup home=null,away=null;if(d!=null)for(MatchDetail.Lineup l:d.lineups){if(match.homeId.equals(l.teamId))home=l;else if(match.awayId.equals(l.teamId))away=l;}
        addPair(teamSheet(home,true),teamSheet(away,false),-1);
    }
    private LinearLayout teamSheet(MatchDetail.Lineup lineup,boolean home){
        String team=home?match.home:match.away;LinearLayout sheet=new LinearLayout(getContext());sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(px(14),px(12),px(14),px(12));sheet.setBackground(panel(0xEA22313C,0xEA18232E,16));
        FrameLayout heading=new FrameLayout(getContext());ArsenalBadge crest=new ArsenalBadge(getContext(),host.unit(),38);crest.bind(team,home?match.homeShort:match.awayShort,home?match.homeLogo:match.awayLogo);heading.addView(crest,pos(38,38,0,0));
        heading.addView(label(team,16,WHITE),pos(245,23,50,0));heading.addView(label(lineup!=null&&!lineup.formation.isEmpty()?"Formation "+lineup.formation:"Formation pending",11,MUTED),pos(300,20,50,25));sheet.addView(heading,new LinearLayout.LayoutParams(-1,px(55)));
        boolean present=lineup!=null&&!lineup.starters.isEmpty();
        TextView status=label(present?(lineup.confirmed?"STARTING XI":"PARTIAL LINEUP"):"NOT ANNOUNCED",10,present?MINT:0xFFE5C88D);status.setPadding(px(4),px(2),0,px(8));sheet.addView(status,new LinearLayout.LayoutParams(-1,px(28)));
        if(!present){sheet.setFocusable(true);sheet.setTag("lineup:pending:"+team);sheet.setOnFocusChangeListener((v,f)->sheet.setBackground(panel(f?0xFF30483F:0xEA22313C,0xEA18232E,16)));TextView message=text("Team sheets appear close to kickoff, when supplied for this match.",14,MUTED);message.setPadding(px(4),px(12),px(4),px(20));sheet.addView(message);return sheet;}
        int player=0;for(String value:lineup.starters)playerRow(sheet,value,team+":starter:"+player++);
        if(!lineup.bench.isEmpty()){TextView subs=label("SUBSTITUTES",10,MUTED);subs.setPadding(px(4),px(14),0,px(7));sheet.addView(subs,new LinearLayout.LayoutParams(-1,px(37)));for(String value:lineup.bench)playerRow(sheet,value,team+":sub:"+player++);}
        return sheet;
    }
    private void playerRow(LinearLayout parent,String value,String key){
        FrameLayout row=card("lineup:"+key,33,null);String[] parts=value.split("  ",2);String number=parts.length==2?parts[0]:"•",name=parts.length==2?parts[1]:value;
        TextView jersey=centered(number,10,MINT);jersey.setBackground(round(0x222B9871,6));row.addView(jersey,pos(26,24,5,4));row.addView(label(name,12,WHITE),pos(335,25,40,7));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(34));p.bottomMargin=px(5);parent.addView(row,p);
    }
    private void commentary(MatchDetail d){
        section(match.live()?"Live match feed":"Match feed",d==null?"":d.commentary.size()+" updates · Latest first");
        if(d==null||d.commentary.isEmpty()){empty("commentary:empty","Waiting for commentary","pre".equals(match.state)?"Live text appears during the game when covered. Explore the preview and previous meetings while you wait.":"Text commentary is not supplied for this match. Reported goals remain in Overview.",null);return;}
        for(int i=0;i<d.commentary.size();i++)event("commentary:"+i,d.commentary.get(i),false,i==0);
    }
    private void event(String key,String value,boolean goal,boolean latest){
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(px(14),px(12),px(14),px(12));row.setBackground(panel(latest?0xFF293E38:0xD8222E3B,0xD918222D,13));row.setTag(key);row.setId(View.generateViewId());row.setFocusable(true);
        row.setOnFocusChangeListener((v,f)->row.setBackground(panel(f?0xFF355444:latest?0xFF293E38:0xD8222E3B,0xD918222D,13)));
        int split=value.indexOf("  ·  ");if(split<0)split=value.indexOf("   ");String minute=split>0?value.substring(0,split).trim():goal?"GOAL":latest?"LAST":"•";String content=split>0?value.substring(split).trim():value;if(content.startsWith("·"))content=content.substring(1).trim();
        TextView badge=centered(minute,11,MINT);badge.setBackground(round(0x222B9871,8));row.addView(badge,new LinearLayout.LayoutParams(px(62),px(30)));
        TextView text=text(content,14,WHITE);text.setLineSpacing(px(4),1);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=px(18);row.addView(text,p);addFull(row,-1);
    }
    /** Static pitch lines and two club-coloured glows; no continuous background animation. */
    private final class Pitch extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Pitch(){super(MatchCentre.this.getContext());setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w*.16f,h*.45f,w*.48f,new int[]{(homeAccent&0xFFFFFF)|0x65000000,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.84f,h*.45f,w*.48f,new int[]{(awayAccent&0xFFFFFF)|0x65000000,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setColor(0x0EFFFFFF);p.setStrokeWidth(px(.7f));c.drawRoundRect(px(16),px(8),w-px(16),h-px(8),px(9),px(9),p);c.drawLine(w/2,px(8),w/2,h-px(8),p);c.drawCircle(w/2,h/2,px(39),p);p.setStyle(Paint.Style.FILL);
        }
    }
}
