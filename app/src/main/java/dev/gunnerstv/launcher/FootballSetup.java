package dev.gunnerstv.launcher;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** First-run and Settings football editor. Search and profile work never runs on the UI thread. */
final class FootballSetup {
    private final Activity activity;
    private final float unit;
    private final FootballSelection draft;
    private final Runnable onSaved,onClosed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Future<?> pending;
    private int generation;
    private boolean alive=true,saved;
    private final Dialog editor;
    private Dialog child;
    private final LinearLayout rows;
    private final FocusScrollView viewport;
    private final TextView cancelButton;
    FootballSetup(Activity a,float u,Runnable save,Runnable close){
        activity=a;unit=u;onSaved=save;onClosed=close;draft=FootballSettings.load(a);
        editor=dialog();LinearLayout body=body();editor.setContentView(body);
        body.addView(label("Your football",25));
        TextView hint=label("Choose your main club, followed clubs and players. Search by name; selections stay on this TV.",13);hint.setPadding(0,px(8),0,px(12));body.addView(hint);
        viewport=new FocusScrollView(a,px(10));rows=new LinearLayout(a);rows.setOrientation(LinearLayout.VERTICAL);viewport.addView(rows,new ScrollView.LayoutParams(-1,-2));body.addView(viewport,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(a);body.addView(footer);
        TextView saveButton=button("Save",()->{FootballSettings.save(a,draft);saved=true;editor.dismiss();});
        cancelButton=button("Cancel",editor::dismiss);footer.addView(saveButton,new LinearLayout.LayoutParams(0,px(48),1));footer.addView(cancelButton,new LinearLayout.LayoutParams(0,px(48),1));
        editor.setOnDismissListener(d->{alive=false;cancelWork();io.shutdownNow();if(child!=null)child.dismiss();onClosed.run();if(saved&&!activity.isFinishing())onSaved.run();});
    }
    void show(boolean first){if(first){cancelButton.setText(R.string.football_skip_setup);cancelButton.setOnClickListener(v->{FootballSettings.save(activity,new FootballSelection());saved=true;editor.dismiss();});}editor.show();size(editor,760,480);render("main");if(first)main.post(this::mainChoice);}
    void close(){editor.dismiss();}
    private int px(float x){return Math.round(unit*x);}
    private Dialog dialog(){Dialog d=new Dialog(activity);d.requestWindowFeature(Window.FEATURE_NO_TITLE);return d;}
    private LinearLayout body(){LinearLayout b=new LinearLayout(activity);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(px(24),px(20),px(24),px(20));GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF1C1E27);bg.setCornerRadius(px(22));b.setBackground(bg);return b;}
    private void size(Dialog d,int width,int height){Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(px(width),Math.min(px(height),activity.getResources().getDisplayMetrics().heightPixels-px(24)));w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}}
    private TextView label(String text,int size){TextView t=new TextView(activity);t.setText(text);t.setTextColor(Color.WHITE);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));return t;}
    private TextView button(String text,Runnable action){
        TextView b=label(text,15);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(px(14),px(5),px(14),px(5));b.setMaxLines(2);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setFocusable(true);b.setFocusableInTouchMode(true);
        Runnable paint=()->{GradientDrawable bg=new GradientDrawable();bg.setColor(b.hasFocus()?Color.WHITE:0x24FFFFFF);bg.setCornerRadius(px(12));b.setBackground(bg);b.setTextColor(b.hasFocus()?0xFF161923:Color.WHITE);};paint.run();b.setOnFocusChangeListener((v,f)->paint.run());b.setOnClickListener(v->action.run());return b;
    }
    private void row(String id,String title,Runnable click){TextView b=button(title,click);b.setTag(id);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(58));p.bottomMargin=px(8);rows.addView(b,p);}
    private void render(String focus){
        int y=viewport.getScrollY();rows.removeAllViews();row("main","Main club: "+draft.mainName()+"\nChange or choose no main club",this::mainChoice);
        rows.addView(label("Clubs · "+draft.clubs.size()+" / "+FootballSelection.MAX_CLUBS,16));
        for(FootballSelection.Team t:draft.clubs.values())row("club:"+t.id,(draft.main.equals(t.id)?"★ ":"")+t.label(),()->clubOptions(t));
        row("add-club","+ Add a club",()->search(false,r->{if(!draft.add(r.team()))limit();render("club:"+r.id);}));
        rows.addView(label("Players · "+draft.players.size()+" / "+FootballSelection.MAX_PLAYERS,16));
        for(FootballSelection.Player p:draft.players.values())row("player:"+p.id,p.name+"\n"+p.summary(),()->playerOptions(p));
        row("add-player","+ Add a player",()->search(true,this::loadPlayer));
        if(draft.empty())rows.addView(label("No preset clubs or players are added. Empty selections are allowed.",12));
        viewport.post(()->{viewport.scrollTo(0,y);View selected=rows.findViewWithTag(focus);if(selected==null)selected=rows.findViewWithTag("main");selected.requestFocus();viewport.reveal(selected);});
    }
    private void limit(){Toast.makeText(activity,"Follow up to "+FootballSelection.MAX_CLUBS+" clubs and "+FootballSelection.MAX_PLAYERS+" players.",Toast.LENGTH_LONG).show();}
    private void mainChoice(){
        if(!alive)return;
        AlertDialog d=new AlertDialog.Builder(activity).setTitle("Would you like Arsenal as your main club?")
            .setItems(new String[]{"Yes · Arsenal men's first team","Choose another club","No main club"},(dialog,which)->{
                if(which==0){if(!draft.main(new FootballSelection.Team("359","Arsenal","Premier League")))limit();render("main");}
                else if(which==1)search(false,r->{if(!draft.main(r.team()))limit();render("main");});
                else{draft.main(null);render("main");}
            }).setNegativeButton("Back",null).create();child=d;d.show();
    }
    private void clubOptions(FootballSelection.Team t){AlertDialog d=new AlertDialog.Builder(activity).setTitle(t.name).setItems(new String[]{"Make main club","Remove club"},(dialog,which)->{if(which==0)draft.main(t);else draft.removeClub(t.id);render(which==0?"club:"+t.id:"add-club");}).setNegativeButton("Back",null).create();child=d;d.show();}
    private void playerOptions(FootballSelection.Player p){FootballSelection.Player working=p.copy();AlertDialog d=new AlertDialog.Builder(activity).setTitle(p.name).setItems(new String[]{"Change match scope","Choose international side","Remove player"},(dialog,which)->{
        if(which==0)scope(working);else if(which==1)chooseNational(working);else{draft.players.remove(p.id);render("add-player");}
    }).setNegativeButton("Back",null).create();child=d;d.show();}
    private void chooseNational(FootballSelection.Player p){search(false,"Find the national side they represent",r->{p.national=r.team();scope(p);});}
    private void scope(FootballSelection.Player p){
        ArrayList<String> labels=new ArrayList<>();ArrayList<String> scopes=new ArrayList<>();
        if(p.club!=null&&p.national!=null){labels.add("Club + international · "+p.club.name+" + "+p.national.name);scopes.add("both");}
        if(p.club!=null){labels.add("Club only · "+p.club.name);scopes.add("club");}
        if(p.national!=null){labels.add("International only · "+p.national.name);scopes.add("international");}
        labels.add("Choose international side…");
        AlertDialog d=new AlertDialog.Builder(activity).setTitle("Matches for "+p.name).setItems(labels.toArray(new String[0]),(dialog,which)->{
            if(which==scopes.size()){chooseNational(p);return;}p.scope=scopes.get(which);if(!draft.add(p))limit();render("player:"+p.id);
        }).setNegativeButton("Back",null).create();child=d;d.show();
    }
    private void loadPlayer(FootballDirectory.Result result){
        FootballSelection.Player existing=draft.players.get(result.id);if(existing!=null){playerOptions(existing);return;}
        AlertDialog progress=new AlertDialog.Builder(activity).setTitle(result.name).setMessage("Loading current club and reported national sides…").setNegativeButton("Cancel",(d,w)->cancelWork()).create();child=progress;progress.setOnCancelListener(d->cancelWork());progress.show();
        work(()->FootballDirectory.profile(result),profile->{progress.dismiss();
            if(profile.nationals.size()>1){ArrayList<String> labels=new ArrayList<>();for(FootballSelection.Team t:profile.nationals)labels.add(t.label());labels.add("No international side");
                AlertDialog choose=new AlertDialog.Builder(activity).setTitle("Choose the national side for "+profile.player.name).setItems(labels.toArray(new String[0]),(d,w)->{profile.player.national=w<profile.nationals.size()?profile.nationals.get(w):null;scope(profile.player);}).setNegativeButton("Back",null).create();child=choose;choose.show();
            }else{if(!profile.internationalAvailable)Toast.makeText(activity,"International data unavailable. Choose a national side manually or follow club matches only.",Toast.LENGTH_LONG).show();scope(profile.player);}
        },error->{progress.dismiss();AlertDialog failed=new AlertDialog.Builder(activity).setTitle("Player unavailable").setMessage("Could not verify that player's current teams. Try again; existing selections are unchanged.").setPositiveButton("OK",null).create();child=failed;failed.show();});
    }
    private void cancelWork(){generation++;if(pending!=null){pending.cancel(true);pending=null;}}
    private <T> void work(Callable<T> task,Consumer<T> result,Consumer<Exception> failure){
        cancelWork();final int request=generation;
        pending=io.submit(()->{try{T value=task.call();main.post(()->{if(alive&&request==generation&&!activity.isFinishing())result.accept(value);});}catch(Exception e){main.post(()->{if(alive&&request==generation&&!activity.isFinishing())failure.accept(e);});}});
    }
    private void search(boolean player,Consumer<FootballDirectory.Result> chosen){search(player,player?"Find a player":"Find a club or national side",chosen);}
    private void search(boolean player,String title,Consumer<FootballDirectory.Result> chosen){
        Dialog d=dialog();child=d;LinearLayout body=body();d.setContentView(body);body.addView(label(title,23));
        EditText query=new EditText(activity);query.setSingleLine();query.setTextColor(Color.WHITE);query.setHintTextColor(0x99FFFFFF);query.setHint(player?"Player's full name":"Club or national team name");query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);body.addView(query,new LinearLayout.LayoutParams(-1,px(52)));
        FocusScrollView scroll=new FocusScrollView(activity,px(8));LinearLayout results=new LinearLayout(activity);results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results,new ScrollView.LayoutParams(-1,-2));
        Runnable find=()->{
            String q=query.getText().toString().trim();if(q.length()<2||q.length()>80){query.setError("Use 2–80 characters");return;}
            results.removeAllViews();results.addView(label("Searching football names…",14));
            work(()->FootballDirectory.search(q,player),found->{results.removeAllViews();
                if(found.isEmpty())results.addView(label("No football results. Try a fuller name or check the spelling.",14));
                for(FootballDirectory.Result item:found){TextView b=button(item.label(),()->{cancelWork();d.dismiss();chosen.accept(item);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(54));p.bottomMargin=px(6);results.addView(b,p);}
                if(!found.isEmpty()){InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);results.getChildAt(0).requestFocus();}
            },error->{results.removeAllViews();results.addView(label("Search unavailable. Check the connection and try again. Existing selections are safe.",14));});
        };
        body.addView(button("Search",find),new LinearLayout.LayoutParams(-1,px(44)));body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body.addView(button("Back",()->{cancelWork();d.dismiss();}),new LinearLayout.LayoutParams(-1,px(44)));
        query.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){find.run();return true;}return false;});
        d.setOnCancelListener(dialog->cancelWork());d.show();size(d,720,440);query.requestFocus();
    }
}
