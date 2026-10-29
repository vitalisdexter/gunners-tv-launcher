package dev.gunnerstv.launcher;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Future;

/** User-editable creator subscriptions, grouped by the user's followed clubs. */
final class NewsSourcesEditor extends Dialog {
    private static WeakReference<NewsSourcesEditor> current=new WeakReference<>(null);
    private final Activity owner;private final float unit;private final Runnable returned;
    private final Handler main=new Handler(Looper.getMainLooper());private Future<?> pending;private int generation;
    private Dialog child;private LinearLayout rows;
    static void open(Activity owner,float unit,Runnable returned){NewsSourcesEditor old=current.get();if(old!=null&&old.isShowing())return;NewsSourcesEditor editor=new NewsSourcesEditor(owner,unit,returned);current=new WeakReference<>(editor);editor.show();}
    static void dismissFor(Activity a){NewsSourcesEditor e=current.get();if(e!=null&&e.owner==a){e.setOnDismissListener(null);e.dismiss();}}
    private NewsSourcesEditor(Activity a,float u,Runnable done){super(a);owner=a;unit=u;returned=done;requestWindowFeature(Window.FEATURE_NO_TITLE);setOnDismissListener(d->{if(!owner.isFinishing())returned.run();});}
    private int px(float value){return Math.round(unit*value);}
    private TextView text(String value,int size){TextView t=new TextView(owner);t.setText(value);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));t.setTextColor(Color.WHITE);return t;}
    private TextView button(String title,Runnable action){TextView t=text(title,14);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(px(14),px(6),px(14),px(6));t.setMaxLines(2);t.setFocusable(true);Runnable paint=()->{GradientDrawable g=new GradientDrawable();g.setColor(t.hasFocus()?Color.WHITE:0x24FFFFFF);g.setCornerRadius(px(12));t.setBackground(g);t.setTextColor(t.hasFocus()?0xFF19212C:Color.WHITE);};paint.run();t.setOnFocusChangeListener((v,f)->paint.run());t.setOnClickListener(v->action.run());return t;}
    @Override protected void onCreate(Bundle state){super.onCreate(state);LinearLayout body=new LinearLayout(owner);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(px(24),px(20),px(24),px(20));GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF19212C);bg.setCornerRadius(px(22));body.setBackground(bg);body.addView(text("Football News creators",24));TextView hint=text("Public YouTube feeds · Latest seven days · Up to 12 club/creator sources",12);hint.setPadding(0,px(8),0,px(12));body.addView(hint);
        FocusScrollView scroll=new FocusScrollView(owner,px(9));rows=new LinearLayout(owner);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body.addView(button("Done",this::dismiss),new LinearLayout.LayoutParams(-1,px(46)));setContentView(body);render();}
    @Override public void show(){super.show();Window w=getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(px(720),Math.min(px(460),owner.getResources().getDisplayMetrics().heightPixels-px(24)));}}
    private void row(String title,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(58));p.bottomMargin=px(7);rows.addView(button(title,action),p);}
    private void render(){rows.removeAllViews();row("+ Add a YouTube creator",this::add);
        FootballSelection selected=FootballSettings.load(owner);List<FootballNewsSources.Source> list=FootballNewsSources.list(owner);
        for(FootballNewsSources.Source s:list){FootballSelection.Team club=selected.clubs.get(s.club);row((club==null?s.club:club.name)+" · "+s.name+"\nOK to remove this source",()->{AlertDialog confirm=new AlertDialog.Builder(owner).setTitle("Remove "+s.name+"?").setMessage("This stops its videos appearing here; it does not change any YouTube subscription.").setPositiveButton("Remove",(d,w)->{FootballNewsSources.remove(owner,s);render();}).setNegativeButton("Cancel",null).create();child=confirm;confirm.show();});}
        if(list.isEmpty())rows.addView(text("No sources for your chosen clubs. Add a creator by their public channel URL or UC channel ID.",14));rows.post(()->{if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();});}
    private void add(){FootballSelection selection=FootballSettings.load(owner);if(selection.clubs.isEmpty()){AlertDialog d=new AlertDialog.Builder(owner).setTitle("Choose a club first").setMessage("Add clubs in Football setup, then choose their creators here.").setPositiveButton("Football setup",(v,w)->{dismiss();MatchCentre.dismissOpen();((MainActivity)owner).footballSetup(false);}).setNegativeButton("Back",null).create();child=d;d.show();return;}
        List<FootballSelection.Team> clubs=new ArrayList<>(selection.clubs.values());String[] names=new String[clubs.size()];for(int i=0;i<clubs.size();i++)names[i]=clubs.get(i).label();AlertDialog d=new AlertDialog.Builder(owner).setTitle("Videos for which club?").setItems(names,(v,w)->input(clubs.get(w))).setNegativeButton("Back",null).create();child=d;d.show();}
    private void input(FootballSelection.Team club){EditText url=new EditText(owner);url.setSingleLine();url.setHint("https://www.youtube.com/@creator");LinearLayout box=new LinearLayout(owner);box.setPadding(px(20),0,px(20),0);box.addView(url,new LinearLayout.LayoutParams(-1,-2));
        AlertDialog d=new AlertDialog.Builder(owner).setTitle("Add creator · "+club.name).setMessage("Paste a public YouTube channel URL, @handle URL or UC channel ID. The channel's RSS identity will be checked.").setView(box).setPositiveButton("Add",null).setNegativeButton("Cancel",(v,w)->cancelLookup()).create();child=d;d.setOnCancelListener(v->cancelLookup());
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{String value=url.getText().toString().trim();try{YouTubeFeeds.directId(value);}catch(IllegalArgumentException invalid){url.setError(invalid.getMessage());return;}
            cancelLookup();int request=generation;d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);d.setMessage("Checking public channel feed…");
            pending=FootballNewsSource.IO.submit(()->{try{YouTubeFeeds.Channel channel=YouTubeFeeds.resolve(value);main.post(()->{if(request!=generation||!isShowing()||!d.isShowing())return;boolean added=FootballNewsSources.add(owner,new FootballNewsSources.Source(club.id,channel.id,channel.name));if(added){d.dismiss();render();}else{d.setMessage("Source limit reached. Remove a source first.");d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);}});}catch(Exception error){main.post(()->{if(request!=generation||!isShowing()||!d.isShowing())return;d.setMessage("Could not verify this channel. Try its /channel/UC URL or channel ID, and check the connection. No account login is needed.");d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);});}});
        }));d.show();}
    private void cancelLookup(){generation++;if(pending!=null)pending.cancel(true);}
    @Override public void dismiss(){cancelLookup();if(child!=null&&child.isShowing())child.dismiss();if(current.get()==this)current.clear();super.dismiss();}
}
