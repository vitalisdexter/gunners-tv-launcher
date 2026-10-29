package dev.gunnerstv.launcher;

import android.app.*;
import android.animation.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Personal Gunner's home. App feeds, configured weather and Arsenal; no analytics. */
public final class MainActivity extends Activity {
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable feedChanged=()->{if(this.started)discover();};
    private final android.database.ContentObserver feedObserver=new android.database.ContentObserver(handler){
        @Override public void onChange(boolean selfChange){handler.removeCallbacks(feedChanged);handler.postDelayed(feedChanged,1500);}
    };
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<View> tabs = new ArrayList<>();
    private final ArrayList<Tile> tiles = new ArrayList<>();
    private SharedPreferences prefs;
    private ForecastClock forecastClock;
    void openForecast(){ if(forecastClock==null)forecastClock=new ForecastClock(this,new TextView(this));forecastClock.resume();forecastClock.details(); }
    private SonicFeedback sonic;
    private HouseWidgets houseWidgets;
    private FrameLayout root, content, hero;
    private final ArrayList<Animator> pageAnimations=new ArrayList<>();
    private View sceneryShade;
    private Wallpaper wallpaper;
    private TextView heroTitle, heroDescription;
    private ClockPanel clock;
    private String heroKey="";
    private boolean holdGreeting;
    private ScrollView scroll;
    private LinearLayout rows;
    private String page = "home", selectedPackage = "com.google.android.youtube.tv";
    private JSONObject lastHome;
    private View sceneVeil;
    private TextView brand;
    private boolean sceneTransition;
    /** The wallpaper is pale (Silver by day): text and glyphs turn dark and glass turns white. */
    private boolean light;
    private Daylight daylight;
    private final Runnable daylightTick=this::syncDaylight;
    private FrameLayout tabBar;
    /** House Control TV is installed and enabled: only then does Home show the scenes and the 3D home. */
    private boolean house;
    /** Home pages of widgets chosen in Settings (WidgetBoard); homePage is the page shown, kept across rebuilds. */
    private WidgetBoard board;
    private WidgetBoard topBoard;
    private boolean homeTopRequested;
    private long externalNavigationUntil;
    private int homePage;
    private boolean restoringHome;
    private String restoreKey="";
    private long restoreUntil;
    private long restoreStartedAt;
    private String homeRestoreResult="pending";
    private long homeFocusGeneration;
    private final Runnable restoreHomeTask=this::restoreHomeStep;
    /** Installed apps by package, replaced as a whole on each discovery: widgets read it from their own threads. */
    private volatile Map<String,App> appIndex=Collections.emptyMap();
    private static final String HOUSE_CONTROL="dev.housecontrol.tv";
    private final BroadcastReceiver packages=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            Uri data=intent.getData();if(data==null||!HOUSE_CONTROL.equals(data.getSchemeSpecificPart()))return;
            // An update removes and re-adds the package; only the final state matters.
            if(Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())&&intent.getBooleanExtra(Intent.EXTRA_REPLACING,false))return;
            syncHouse();
        }
    };
    private boolean loaded, loading, started, homeGreeting=true,discoverAgain;
    private float unit = 1f;
    private int lastDirectionX=1, lastDirectionY=0;
    private String heroPackage="";
    private final android.view.animation.Interpolator ease = new PathInterpolator(.22f,1f,.36f,1f);
    private final android.view.animation.Interpolator drift = new PathInterpolator(.2f,.72f,.25f,1f);
    private final android.view.animation.Interpolator spring = t -> {
        if(t>=1)return 1;
        double time=t*.42,frequency=25,damping=.72;
        double damped=frequency*Math.sqrt(1-damping*damping);
        return (float)(1-Math.exp(-damping*frequency*time)*(Math.cos(damped*time)+damping*frequency/damped*Math.sin(damped*time)));
    };
    private final Runnable clockTick = new Runnable() {
        public void run() {
            if (!started) return;
            if(clock!=null)clock.refresh();
            if(page.equals("home")&&homeGreeting&&heroTitle!=null&&!welcome().contentEquals(heroTitle.getText()))heroTitle.setText(welcome());
            if(page.equals("home")&&homeGreeting&&heroDescription!=null&&!greetingLine().contentEquals(heroDescription.getText()))heroDescription.setText(greetingLine());
            handler.postDelayed(this, 1000);
        }
    };
    private String[] FIRST;
    private FootballSetup footballEditor;
    private AlertDialog contentDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("gunners", MODE_PRIVATE);
        FIRST=getResources().getStringArray(R.array.default_favourites);
        homePage=prefs.getInt("home_page",0);restoringHome=true;
        
        if(getIntent().getBooleanExtra("personal.resume_home",false))prefs.edit().putBoolean("redirect_home",true).apply();
        if(!prefs.getBoolean("personal_defaults",false))prefs.edit().putBoolean("personal_defaults",true).putString("temp_unit","c").putString("widget_order","upnext,weather,recommendations,arsenal,music,control,inputs").putString("widget_off","house,inputs").apply();
        if(!prefs.getBoolean("feeds_v3",false)){
            Set<String> off=new HashSet<>(Arrays.asList(prefs.getString("widget_off","").split(",")));
            off.addAll(Arrays.asList("house","inputs","upnext","recommendations","arsenal"));
            prefs.edit().putBoolean("feeds_v3",true).putString("widget_order",String.join(",",WidgetBoard.IDS)).putString("widget_off",String.join(",",off)).putInt("home_page",0).apply();homePage=0;
        }
        if(!prefs.getBoolean("football_v4",false)){
            Set<String> off=new HashSet<>(Arrays.asList(prefs.getString("widget_off","").split(",")));off.removeAll(Arrays.asList("weather","arsenal","music","control"));
            ArrayList<String> order=WidgetBoard.order(prefs);order.removeAll(Arrays.asList("weather","arsenal","music","control"));order.addAll(0,Arrays.asList("weather","arsenal","music","control"));
            prefs.edit().putBoolean("football_v4",true).putString("widget_order",String.join(",",order)).putString("widget_off",String.join(",",off)).apply();
        }
        unit = getResources().getDisplayMetrics().widthPixels / 960f;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        sonic=new SonicFeedback(this);
        daylight=new Daylight(this);
        // 2.2 introduces Silver, light by day and dark after sunset, as the default wallpaper.
        if(!prefs.getBoolean("silver_default",false))prefs.edit().putInt("wallpaper",SILVER).putBoolean("silver_default",true).apply();
        light=pale(variant(wallpaperStyle()));
        house=houseInstalled();
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addDataScheme("package");
        registerReceiver(packages,filter);
        createShell();
        try{getContentResolver().registerContentObserver(Uri.parse("content://android.media.tv"),true,feedObserver);}catch(RuntimeException ignored){}
        discover();
        root.post(()->{if(!FootballSettings.completed(this)&&!isFinishing())footballSetup(true);});
    }

    @Override protected void onResume() {
        super.onResume();
        started = true;
        resetSceneTransition();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
        if(loaded&&page.equals("home"))requestHomeRestore();
        if (loaded) discover();
        syncHouse();
        // A sunrise or sunset that passed while an app was open switches a moment after Home returns, once that
        // app has given back its GPU memory: switching at once could fail to allocate (EGL_BAD_ALLOC) on this stick.
        handler.removeCallbacks(daylightTick);handler.postDelayed(daylightTick,900);
        wallpaper.syncAmbient();
        if(houseWidgets!=null)houseWidgets.setActive(board==null||board.onHouse());
        if(board!=null){board.setActive(true);board.refresh();}
        if(topBoard!=null){topBoard.setActive(true);topBoard.refresh();}
    }
    @Override protected void onPause() {
        rememberHomeFocus(getCurrentFocus());homeFocusGeneration++;handler.removeCallbacks(restoreHomeTask);
        super.onPause(); if(forecastClock!=null)forecastClock.pause(); if(houseWidgets!=null)houseWidgets.setActive(false); if(board!=null)board.setActive(false);if(topBoard!=null)topBoard.setActive(false); started = false; handler.removeCallbacks(clockTick); handler.removeCallbacks(daylightTick);
        if(!sceneTransition){content.animate().cancel();content.setAlpha(1);}
        for(Tile tile:tiles) tile.movement.snap();
        wallpaper.pauseAmbient();wallpaper.rest();
    }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused);if(wallpaper!=null)wallpaper.syncAmbient(); }
    @Override protected void onDestroy() { NewsSourcesEditor.dismissFor(this);if(contentDialog!=null)contentDialog.dismiss();if(footballEditor!=null)footballEditor.close();MatchCentre.dismissFor(this); getContentResolver().unregisterContentObserver(feedObserver);try{unregisterReceiver(packages);}catch(IllegalArgumentException ignored){} if(wallpaper!=null){wallpaper.pauseAmbient();wallpaper.surfaceThread.quitSafely();}if(forecastClock!=null)forecastClock.close(); super.onDestroy(); handler.removeCallbacksAndMessages(null); loader.shutdownNow(); if(sonic!=null)sonic.release(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getBooleanExtra("personal.resume_home",false))prefs.edit().putBoolean("redirect_home",true).apply();
        String look=intent.getStringExtra("gunners.daylight");
        if(look!=null&&look.matches("day|night|auto")){daylightOverride=look;if(started)syncDaylight();}
        String demo=null; // Personal build never enables sample content.

        if(intent.hasCategory(Intent.CATEGORY_HOME)){
            boolean top=homeTopRequested||(started&&page.equals("home")&&hasWindowFocus());homeTopRequested=false;MatchCentre.dismissOpen();
            if(forecastClock!=null)forecastClock.dismissDetails();if(overlay!=null)closeOverlay(true);showPage("home",!top);if(top)root.post(this::scrollHomeTop);
        }
    }
    @Override protected void onUserLeaveHint(){super.onUserLeaveHint();homeTopRequested=page.equals("home")&&SystemClock.uptimeMillis()>externalNavigationUntil;}
    @Override public void startActivityForResult(Intent intent,int requestCode,Bundle options){homeTopRequested=false;externalNavigationUntil=SystemClock.uptimeMillis()+2000;super.startActivityForResult(intent,requestCode,options);}
    private void scrollHomeTop(){
        if(!page.equals("home")||scroll==null)return;restoringHome=false;handler.removeCallbacks(restoreHomeTask);homeFocusGeneration++;homeGreeting=true;heroPackage="@greeting";setHero("@greeting",welcome(),greetingLine());
        tabs.get(0).requestFocus();scroll.smoothScrollTo(0,0);prefs.edit().putString("home_focus_encoded",encodeHomeFocus("clock")).apply();homeRestoreResult="Home scrolled to top";
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==7){FeedCatalog.latest=null;discover();return;}
        if(board!=null)board.refresh();
        if(topBoard!=null)topBoard.refresh();
    }

    @Override public void dump(String prefix,FileDescriptor fd,PrintWriter writer,String[] args) {
        super.dump(prefix,fd,writer,args);
        if(FeedCatalog.latest!=null)for(FeedCatalog.Row row:FeedCatalog.ROWS)writer.println(prefix+"Gunner's feed: "+row.id+" count="+row.select(FeedCatalog.latest).size());
        if(board!=null)writer.println(prefix+"Gunner's rows: "+board.layout);
        if(topBoard!=null)writer.println(prefix+"Gunner's top widgets: "+topBoard.layout+" page="+topBoard.index);
        writer.println(prefix+"Gunner's scroll: "+(scroll==null?-1:scroll.getScrollY()));
        writer.println(prefix+"Gunner's football: "+FootballSource.diagnostic);
        if(wallpaper!=null)writer.println(prefix+"Gunner's ambient: running="+wallpaper.ambientRunning+" ticks="+wallpaper.ambientTicks+" seconds="+wallpaper.ambientSeconds+" style="+wallpaper.requestedStyle+" page="+page);
        writer.println(prefix+"Gunner's Home: page="+(board==null?homePage:board.index)+" focus="+savedHomeFocus().replace('\n','|')+" restoring="+restoringHome);
        writer.println(prefix+"Gunner's Home restore: requested="+restoreKey.replace('\n','|')+" result="+homeRestoreResult+" found="+(board!=null&&board.remembered(restoreKey)!=null)+" actual="+homeFocusKey(getCurrentFocus()).replace('\n','|'));
        if(daylight!=null){long now=System.currentTimeMillis();long[] sun=daylight.times(now);SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);
            writer.println(prefix+"Gunner's daylight: zone="+TimeZone.getDefault().getID()+" sunrise="+f.format(new Date(sun[0]))+" sunset="+f.format(new Date(sun[1]))+" day="+daylight.isDay(now)+" next="+f.format(new Date(daylight.nextChange(now)))+" light="+light);}
    }
    private boolean houseInstalled(){return false;}
    /** Rebuilds Home when House Control TV is installed, removed, switched on or off. */
    private void syncHouse(){
        boolean now=houseInstalled();if(now==house)return;house=now;
        if(!now)lastHome=null;
        if(page.equals("home"))showPage("home",started);
    }
    private int px(float value) { return Math.round(value * unit); }
    private boolean motion() { return prefs.getBoolean("motion", true) && ValueAnimator.areAnimatorsEnabled(); }
    private void sound() { if (prefs.getBoolean("sound", true)) sonic.open(); }
    private void navigationSound(int index) { if(prefs.getBoolean("sound",true))sonic.step((index%6)/5f,lastDirectionX+lastDirectionY); }
    private TextView text(String value, float size, int color, String family) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextColor(color);
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(size));
        view.setTypeface(Typeface.create(family, Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }
    private FrameLayout.LayoutParams pos(float w, float h, float x, float y) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w < 0 ? (int)w : px(w), h < 0 ? (int)h : px(h));
        lp.leftMargin = px(x); lp.topMargin = px(y); return lp;
    }
    private GradientDrawable shape(int color, float radius) {
        GradientDrawable gd = new GradientDrawable(); gd.setColor(color); gd.setCornerRadius(px(radius)); return gd;
    }
    /** Text and glyph colour with the given alpha: near-black on a pale wallpaper, white elsewhere. */
    private int ink(int alpha){return (alpha<<24)|(light?0x1D1D1F:0xFFFFFF);}
    /** Unfocused glass: frosted white on a pale wallpaper, a faint white veil on dark ones. */
    private int glass(){return light?0x8CFFFFFF:0x1FFFFFFF;}
    private void shadow(TextView t,float radius){if(light)t.setShadowLayer(0,0,0,0);else t.setShadowLayer(px(radius),0,px(1),0x55000000);}
    private int wallpaperStyle(){return Math.max(0,Math.min(WALLPAPERS.length-1,prefs.getInt("wallpaper",SILVER)));}
    /** Silver shows its light picture between sunrise and sunset and its dark one at night. */
    private int variant(int style){return style==SILVER&&!isDay()?SILVER_NIGHT:style;}
    /** QA: am start -n dev.gunnerstv.launcher/.MainActivity --es gunners.daylight day|night|auto previews either look (not saved). */
    private String daylightOverride="auto";
    private boolean isDay(){return daylightOverride.equals("auto")?daylight.isDay(System.currentTimeMillis()):daylightOverride.equals("day");}
    private static boolean pale(int variant){return variant==SILVER;}
    /** Follows sunrise and sunset: switches Silver's picture and, with it, the colours of the page. */
    private void syncDaylight(){
        handler.removeCallbacks(daylightTick);
        if(!started)return;
        long now=System.currentTimeMillis();
        handler.postDelayed(daylightTick,Math.max(30000,Math.min(3600000,daylight.nextChange(now)-now+2000)));
        if(overlay!=null)return; // the wallpaper picker previews freely; the theme follows once it closes
        wallpaper.update();
        boolean next=pale(variant(wallpaperStyle()));
        if(next==light)return;
        if(!motion()||sceneTransition){showPage(page,true);return;}
        content.animate().cancel();
        // Not while another app starts: a new page (and its WebView) then competes with it for GPU memory, which
        // ended in EGL_BAD_ALLOC on this stick once. Paused, the colours are left as they are; onResume switches them.
        content.animate().alpha(0).setDuration(220).setInterpolator(ease).withEndAction(()->{if(!started){content.setAlpha(1);return;}showPage(page,true);content.setAlpha(0);content.animate().alpha(1).setDuration(420).setInterpolator(ease).start();}).start();
    }
    /** Colours of the parts that stay across pages: tab bar, clock and the settings scrim. */
    private void paintChrome(){
        if(tabBar!=null)tabBar.setBackground(shape(light?0x47FFFFFF:0x1FFFFFFF,22));
        if(clock!=null)clock.refresh();
        if(sceneryShade!=null)sceneryShade.setBackgroundColor(light?0x73F2F2F5:0x80000000);
    }

    private void createShell() {
        root = new FrameLayout(this); root.setClipChildren(false); root.setClipToPadding(false);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus,newFocus)->rememberHomeFocus(newFocus));
        wallpaper = new Wallpaper(); root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        sceneryShade=new View(this);sceneryShade.setBackgroundColor(0x80000000);sceneryShade.setAlpha(0);
        root.addView(sceneryShade,new FrameLayout.LayoutParams(-1,-1));
        content = new FrameLayout(this); content.setClipChildren(false); root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        // No wordmark: the page opens with the greeting, as on tvOS.
        brand = text("gunners", 23, Color.WHITE, "sans-serif-medium"); brand.setVisibility(View.GONE);
        root.addView(brand, pos(120, 32, 48, 29));
        LinearLayout navigation = new LinearLayout(this); navigation.setGravity(Gravity.CENTER);
        String[] titles = {"Home", "Apps", "Settings"};
        String[] pages = {"home", "apps", "settings"};
        for (int i=0;i<titles.length;i++) {
            final int index=i;
            TextView tab = text(titles[i], 14, 0xB3FFFFFF, "sans-serif-medium");
            tab.setPadding(px(17), px(9), px(17), px(9)); tab.setGravity(Gravity.CENTER);
            tab.setFocusable(true); tab.setFocusableInTouchMode(true); tab.setSoundEffectsEnabled(false);
            tab.setOnClickListener(v -> { sound(); showPage(pages[index], true); });
            // One pill slides between tabs; leaving the bar it settles, translucent, on the current page.
            tab.setOnFocusChangeListener((v, focus) -> { if(focus) moveTabIndicator(true); else tab.post(()->{ if(focusedTab()<0) moveTabIndicator(true); }); });
            tab.setOnKeyListener((v,key,e) -> {
                if (e.getAction()==KeyEvent.ACTION_DOWN && key==KeyEvent.KEYCODE_DPAD_DOWN) {
                    View widget=index==0&&topBoard!=null?topBoard.first():index==0&&!tiles.isEmpty()?tiles.get(0):null;
                    if (!page.equals(pages[index])) showPage(pages[index], true); else if(widget!=null)widget.requestFocus();else focusFirst();
                    return true;
                }
                return false;
            });
            tabs.add(tab); navigation.addView(tab, new LinearLayout.LayoutParams(-2, px(36)));
        }
        FrameLayout bar = new FrameLayout(this); bar.setClipChildren(false); tabBar=bar;
        tabIndicatorShape = shape(0x24FFFFFF, 18); tabIndicator = new View(this); tabIndicator.setBackground(tabIndicatorShape);
        bar.addView(tabIndicator, new FrameLayout.LayoutParams(0, px(36), Gravity.CENTER_VERTICAL));
        bar.addView(navigation, new FrameLayout.LayoutParams(-2, -1));
        bar.setPadding(px(4),px(2),px(4),px(2)); bar.setBackground(shape(0x1FFFFFFF,22));
        FrameLayout.LayoutParams navigationPosition=pos(-2,40,0,26);navigationPosition.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        root.addView(bar,navigationPosition);
        bar.post(()->moveTabIndicator(false));
        clock = new ClockPanel(this,unit,this::openForecast);
        clock.setTag("clock");clock.setScaleX(1.08f);clock.setScaleY(1.08f);clock.setPivotX(px(252));clock.setPivotY(0);
        root.addView(clock, pos(252, 75, 660, 12));
        paintChrome();
        setContentView(root);
        showPage("home", false);
    }

    private void discover() {
        if (loading) {discoverAgain=true;return;}
        loading=true;
        loader.execute(() -> {
            ArrayList<App> found = new ArrayList<>();
            HashSet<String> seen = new HashSet<>();
            PackageManager pm = getPackageManager();
            for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
                for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                    String pkg=info.activityInfo.packageName;
                    if (pkg.equals(getPackageName()) || Arrays.asList(STOCK_HOMES).contains(pkg) || !seen.add(pkg)) continue;
                    if (pm.getLaunchIntentForPackage(pkg)==null && pm.getLeanbackLaunchIntentForPackage(pkg)==null) continue;
                    try {
                        App app = new App(); app.pkg=pkg;
                        app.label=info.loadLabel(pm).toString();
                        
                        if(pkg.equals("com.amazon.amazonvideo.livingroom"))app.label="Prime Video";
                        app.icon=bitmap(info.loadIcon(pm), 120, 120);
                        app.color=colorFor(pkg);
                        app.description=descriptionFor(pkg);
                        app.tag=tagFor(pkg);
                        Drawable banner=info.activityInfo.applicationInfo.loadBanner(pm);
                        if (banner!=null) app.banner=bitmap(banner, 320, 180);
                        found.add(app);
                    } catch (RuntimeException ignored) { /* A package can disappear during an update. */ }
                }
            }
            Collections.sort(found, (a,b)->a.label.compareToIgnoreCase(b.label));
            ContentFeedReader.Result feed=new ContentFeedReader(getApplicationContext()).read();
            Set<String> available=FeedCatalog.available(feed);
            boolean supported=false;for(App app:found)if(ContentFeedPolicy.supported(app.pkg)){supported=true;break;}
            final boolean statusNeeded=feed.status.needsSetup(supported,!available.isEmpty());
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                loading=false;
                boolean changed = !loaded || found.size()!=apps.size();
                changed|=!available.equals(prefs.getStringSet("available_feeds",Collections.emptySet()));
                changed|=statusNeeded!=prefs.getBoolean("content_status_needed",true);
                FeedCatalog.latest=feed;
                prefs.edit().putStringSet("available_feeds",available).putBoolean("content_status_needed",statusNeeded).apply();
                if(page.equals("home")){ArrayList<ArrayList<String>> desired=WidgetBoard.section(prefs,house,!InputsWidget.list(this).isEmpty(),false);changed|=board==null?!desired.isEmpty():!board.layout.equals(desired);}
                if (!changed) for (int i=0;i<found.size();i++) if (!found.get(i).pkg.equals(apps.get(i).pkg)) { changed=true; break; }
                if (changed) { apps.clear(); apps.addAll(found); HashMap<String,App> index=new HashMap<>(); for(App a:found) index.put(a.pkg,a); appIndex=index; loaded=true; showPage(page, true); }
                else {if(board!=null)board.refresh();if(topBoard!=null)topBoard.refresh();}
                if(contentDialog!=null&&contentDialog.isShowing())contentDialog.setMessage(contentReport());
                if(discoverAgain){discoverAgain=false;handler.post(this::discover);}
            });
        });
    }

    private Bitmap bitmap(Drawable drawable, int w, int h) {
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bitmap); drawable.setBounds(0,0,w,h); drawable.draw(c); return bitmap;
    }
    private ArrayList<String> order() {
        ArrayList<String> ids=new ArrayList<>();
        String saved=prefs.getString("order", "");
        if (!saved.isEmpty()) ids.addAll(Arrays.asList(saved.split(",")));
        for (String pkg:FIRST) if (!ids.contains(pkg)) ids.add(pkg);
        for (App app:apps) if (!ids.contains(app.pkg)) ids.add(app.pkg);
        return ids;
    }
    private ArrayList<App> visibleApps(boolean all) {
        ArrayList<App> result=new ArrayList<>();
        Set<String> hidden=prefs.getStringSet("hidden", Collections.emptySet());
        for (String id:order()) for (App app:apps) if (app.pkg.equals(id) && (all || !hidden.contains(id))) result.add(app);
        return result;
    }

    private void showPage(String next, boolean focus) {
        rememberHomeFocus(getCurrentFocus());
        final long focusGeneration=++homeFocusGeneration;
        handler.removeCallbacks(restoreHomeTask);
        if(next.equals("home"))restoringHome=true;
        String previous=page;
        boolean transition=loaded && !previous.equals(next) && motion();
        for(Animator animation:pageAnimations)animation.cancel();pageAnimations.clear();
        for(Tile tile:tiles) tile.movement.stop();
        content.animate().cancel();content.setAlpha(1);content.setTranslationX(0);
        if(houseWidgets!=null&&houseWidgets.current!=null)lastHome=houseWidgets.current;
        if(board!=null)homePage=board.index;
        if(topBoard!=null)prefs.edit().putInt("home_top_page",topBoard.index).apply();
        content.removeAllViews();houseWidgets=null;board=null;topBoard=null;content.setTag(null);
        page=next;homeGreeting=page.equals("home");heroPackage=homeGreeting?"@greeting":"";tiles.clear();
        light=pale(variant(wallpaperStyle()));paintChrome();
        clock.setVisibility(prefs.getBoolean("clock", true) ? View.VISIBLE : View.GONE);
        moveTabIndicator(transition);
        wallpaper.update();wallpaper.rest();wallpaper.syncAmbient();
        sceneryShade.animate().cancel();
        sceneryShade.setAlpha(page.equals("settings")?1:0);
        if (page.equals("settings")) { settings(focus); animatePage(previous,transition); return; }
        boolean home=page.equals("home");
        hero=new FrameLayout(this){@Override public boolean hasOverlappingRendering(){return false;}};
        heroTitle=text(home ? welcome() : "All your apps", 42, ink(0xFF), "sans-serif-medium");
        heroTitle.setLetterSpacing(-.035f); heroTitle.setSingleLine(true); heroTitle.setEllipsize(android.text.TextUtils.TruncateAt.END); shadow(heroTitle,10);
        hero.addView(heroTitle,pos(home?640:860,54,0,0));
        heroDescription=text(home ? greetingLine() : "Everything installed on this TV.", 16, ink(0xB3), "sans-serif");
        heroDescription.setSingleLine(true); heroDescription.setEllipsize(android.text.TextUtils.TruncateAt.END); shadow(heroDescription,8); hero.addView(heroDescription,pos(home?640:860,22,1,60));
        // Without widgets, the greeting moves down to sit just above the favourites and the wallpaper stays open above it.
        ArrayList<ArrayList<String>> layout=home?WidgetBoard.section(prefs,house,!InputsWidget.list(this).isEmpty(),false):new ArrayList<>();
        ArrayList<ArrayList<String>> topLayout=home?WidgetBoard.section(prefs,house,false,true):new ArrayList<>();
        boolean housePage=false;for(ArrayList<String> p:layout)housePage|=p.contains("house");
        content.addView(hero,pos(864,88,48,home?(!layout.isEmpty()?96:272):100));
        heroKey=home?"@greeting":"";
        if(home&&housePage){
            houseWidgets=new HouseWidgets(this,unit,new HouseWidgets.Callbacks(){
                public void up(){tabs.get(0).requestFocus();}
                public void down(float x){focusNearestTile(x);}
                public void open(){try{startActivity(new Intent().setComponent(new ComponentName("dev.housecontrol.tv","dev.housecontrol.tv.MainActivity")));overridePendingTransition(0,0);}catch(Exception e){Toast.makeText(MainActivity.this,"Install House Control to connect your home.",Toast.LENGTH_SHORT).show();}}
                public void focusHome(String title,String detail){holdGreeting=false;homeGreeting=false;setHero("@home:"+title,title,detail);navigationSound(2);}
                public void activate(){sound();}
                public boolean sceneApplied(String id){return MainActivity.this.sceneApplied(id);}
                public boolean motion(){return MainActivity.this.motion();}
                public String sceneDetail(String id,String name){return MainActivity.this.sceneDetail(id);}
                public boolean light(){return light;}
                public void edge(int direction){if(board==null)return;View from=getCurrentFocus(),to=board.flip(direction,from);if(to!=null&&to!=from)to.requestFocus();}
            });
            content.addView(houseWidgets,pos(HouseWidgets.W,HouseWidgets.PAGE_H,0,0));
        }
        if(home&&!layout.isEmpty()){
            board=new WidgetBoard(this,boardHost,layout,homePage);homePage=board.index;
            if(houseWidgets!=null&&!board.onHouse()){houseWidgets.setVisibility(View.INVISIBLE);houseWidgets.setActive(false);}
            board.setActive(started);
        }
        scroll=new ScrollView(this); scroll.setFillViewport(false); scroll.setClipChildren(true); scroll.setClipToPadding(true);
        scroll.setVerticalScrollBarEnabled(false); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); rows.setClipChildren(false); rows.setClipToPadding(false);
        rows.setPadding(px(home?0:48),px(home?16:10),px(home?0:48),px(30));
        scroll.addView(rows,new ScrollView.LayoutParams(-1,-2));
        FrameLayout scrollClip=new FrameLayout(this){@Override protected void dispatchDraw(Canvas canvas){int saved=canvas.save();canvas.clipRect(0,0,getWidth(),getHeight());super.dispatchDraw(canvas);canvas.restoreToCount(saved);}};
        scrollClip.setTag("scroll-frame");scrollClip.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        content.addView(scrollClip,pos(960, home ? 350 : 330,0,home ? 190 : 200));
        if(home)scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->{if(board!=null)board.updateVisibility();if(topBoard!=null)topBoard.updateVisibility();});
        if(home&&!topLayout.isEmpty()){
            topBoard=new WidgetBoard(this,boardHost,topLayout,prefs.getInt("home_top_page",0),true);
            rows.addView(topBoard,new LinearLayout.LayoutParams(-1,px(topBoard.totalHeight())));topBoard.post(()->{if(topBoard!=null)topBoard.setActive(started);});
        }
        ArrayList<App> list=visibleApps(!home);
        if (!loaded) {
            rows.addView(text("Finding your apps…",15,ink(0xBB),"sans-serif"));
        } else if (list.isEmpty()) {
            rows.addView(text("Your home is empty. Find your apps in the Apps tab.",15,ink(0xBB),"sans-serif"));
        } else {
            int count=home ? Math.min(6,list.size()) : list.size();
            for (int start=0;start<count;start+=6) {
                LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
                if(home)row.setPadding(px(48),0,px(48),0);
                row.setClipChildren(false); row.setClipToPadding(false); row.setGravity(Gravity.TOP);
                for (int i=start;i<Math.min(start+6,count);i++) {
                    App app=list.get(i); Tile tile=new Tile(app,i);
                    LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(px(132),px(108));
                    if (i>start) tp.leftMargin=px(14.4f);
                    row.addView(tile,tp); tiles.add(tile);
                }
                rows.addView(row,new LinearLayout.LayoutParams(-1,px(112)));
            }
        }
        if(home&&board!=null){rows.addView(board,new LinearLayout.LayoutParams(-1,px(board.totalHeight())));board.post(()->board.setActive(started));}
        if(focus)root.post(()->{if(focusGeneration!=homeFocusGeneration)return;if(page.equals("home"))requestHomeRestore();else focusFirst();});
        animatePage(previous,transition);
    }

    private void animatePage(String previous,boolean transition) {
        if(!transition)return;
        int before=previous.equals("home")?0:previous.equals("apps")?1:2;
        int after=page.equals("home")?0:page.equals("apps")?1:2;
        float direction=after>=before?1:-1;
        // Animate real element bounds rather than blending two full-screen textures.
        // This preserves the soft page entrance within a small TV chip's fill-rate budget.
        ArrayList<View> entering=new ArrayList<>();
        for(int i=0;i<content.getChildCount();i++)if(!(content.getChildAt(i) instanceof ScrollView)&&!"scroll-frame".equals(content.getChildAt(i).getTag()))entering.add(content.getChildAt(i));
        if(page.equals("settings")) {
            Object tag=content.getTag();if(tag instanceof ArrayList)for(Object row:(ArrayList<?>)tag)if(row instanceof View)entering.add((View)row);
        } else {
            for(int i=0;i<rows.getChildCount();i++)if(rows.getChildAt(i) instanceof TextView)entering.add(rows.getChildAt(i));
            entering.addAll(tiles);
        }
        for(int i=0;i<entering.size();i++) {
            View view=entering.get(i);float offset=px(16)*direction;
            view.setAlpha(0);view.setTranslationX(offset);
            ObjectAnimator fade=ObjectAnimator.ofFloat(view,View.ALPHA,0,1);fade.setDuration(270);
            ObjectAnimator slide=ObjectAnimator.ofFloat(view,View.TRANSLATION_X,offset,0);slide.setDuration(340);
            AnimatorSet set=new AnimatorSet();set.playTogether(fade,slide);set.setInterpolator(ease);set.setStartDelay(Math.min(i,5)*10);
            pageAnimations.add(set);set.start();
        }
    }

    private void focusFirst() {
        for (Tile tile:tiles) if (tile.app.pkg.equals(selectedPackage)) { tile.requestFocus(); return; }
        if (!tiles.isEmpty()) tiles.get(0).requestFocus();
    }
    /** Stable semantic keys survive view rebuilds, app launches and asynchronous programme refreshes. */
    void revealHomeRow(WidgetBoard owner,View row,int index){
        if(!page.equals("home")||scroll==null)return;
        int top=owner==topBoard?0:owner.getTop()+row.getTop()-px(12);
        if(motion()&&!restoringHome)scroll.smoothScrollTo(0,Math.max(0,top));else scroll.scrollTo(0,Math.max(0,top));
    }
    private String homeFocusKey(View view){
        if(view==null||!view.isAttachedToWindow()||!WidgetBoard.isDescendant(root,view))return "";
        if(view==clock)return "clock";
        if(view instanceof Tile)return "app:"+((Tile)view).app.pkg;
        Widget owner=null;for(View v=view;v!=null;){if(v instanceof Widget){owner=(Widget)v;break;}ViewParent p=v.getParent();v=p instanceof View?(View)p:null;}
        if(owner==null||owner.widgetId.isEmpty())return "";
        if(view==owner)return owner.widgetId;
        Object tag=view.getTag();if(!(tag instanceof String))return "";
        String key=(String)tag;
        return key.startsWith("continue:")||key.startsWith("recommendations:")?key:"widget:"+owner.widgetId+":"+key;
    }
    private void rememberHomeFocus(View view){
        if(prefs==null||!page.equals("home")||restoringHome)return;
        String key=homeFocusKey(view);if(key.isEmpty())return;
        if(board!=null)homePage=board.index;
        if(topBoard!=null)prefs.edit().putInt("home_top_page",topBoard.index).apply();
        prefs.edit().putString("home_focus_encoded",encodeHomeFocus(key)).remove("home_focus").putInt("home_page",homePage).apply();
    }
    /** Keep semantic keys byte-for-byte through SharedPreferences XML, including an empty final episode. */
    private static String encodeHomeFocus(String key){return android.util.Base64.encodeToString(key.getBytes(StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);}
    private String savedHomeFocus(){
        String encoded=prefs.getString("home_focus_encoded","");
        if(!encoded.isEmpty())try{return new String(android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP),StandardCharsets.UTF_8);}catch(IllegalArgumentException ignored){}
        String legacy=prefs.getString("home_focus","");
        // Older XML serializers can append closing-tag indentation after the key's final newline.
        int last=legacy.lastIndexOf('\n');
        if(last>=0&&legacy.substring(last+1).trim().isEmpty())legacy=legacy.substring(0,last+1);
        return legacy;
    }
    private void requestHomeRestore(){
        if(!loaded||!page.equals("home"))return;
        homeFocusGeneration++;
        restoringHome=true;restoreKey=savedHomeFocus();restoreStartedAt=SystemClock.uptimeMillis();restoreUntil=restoreStartedAt+15000;homeRestoreResult="waiting";
        handler.removeCallbacks(restoreHomeTask);handler.post(restoreHomeTask);
    }
    private View savedHomeView(){
        if(restoreKey.equals("clock"))return clock;
        if(restoreKey.startsWith("app:")){String pkg=restoreKey.substring(4);for(Tile tile:tiles)if(tile.app.pkg.equals(pkg))return tile;return null;}
        View v=topBoard==null?null:topBoard.remembered(restoreKey);return v!=null?v:board==null?null:board.remembered(restoreKey);
    }
    private void restoreHomeStep(){
        if(!restoringHome||!started||!page.equals("home")||root==null)return;
        View target=savedHomeView();
        if(target!=null){
            if(topBoard!=null&&topBoard.revealRemembered(target)){handler.postDelayed(restoreHomeTask,motion()?520:0);return;}
            if(board!=null&&board.revealRemembered(target)){handler.postDelayed(restoreHomeTask,motion()?520:0);return;}
            if(target.isShown()&&target.requestFocus()){homeRestoreResult="matched after "+(SystemClock.uptimeMillis()-restoreStartedAt)+" ms";restoringHome=false;rememberHomeFocus(target);return;}
        }
        if(!restoreKey.isEmpty()&&SystemClock.uptimeMillis()<restoreUntil){handler.postDelayed(restoreHomeTask,80);return;}
        View fallback=topBoard==null?null:topBoard.fallbackFor(restoreKey);if(fallback==null)fallback=board==null?null:board.fallbackFor(restoreKey);
        if(fallback!=null&&topBoard!=null&&topBoard.revealRemembered(fallback)){handler.postDelayed(restoreHomeTask,motion()?520:0);return;}
        if(fallback!=null&&board.revealRemembered(fallback)){handler.postDelayed(restoreHomeTask,motion()?520:0);return;}
        if(fallback!=null)fallback.requestFocus();else{holdGreeting=true;focusFirst();}
        homeRestoreResult="fallback after "+(SystemClock.uptimeMillis()-restoreStartedAt)+" ms";
        restoringHome=false;
        // A stalled provider must not erase the requested title; a real navigation or click will replace it.
        if(!(restoreKey.startsWith("continue:")||restoreKey.startsWith("recommendations:")))rememberHomeFocus(getCurrentFocus());
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getAction()==KeyEvent.ACTION_DOWN){homeFocusGeneration++;if(restoringHome){homeRestoreResult="cancelled by key "+event.getKeyCode();restoringHome=false;handler.removeCallbacks(restoreHomeTask);rememberHomeFocus(getCurrentFocus());}}
        return super.dispatchKeyEvent(event);
    }
    /** Under the greeting: today's date, quietly. */
    private String greetingLine(){return today();}
    /** "Hello, <name>" with the name saved in Settings, else the build's greeting: "Hello", or LAUNCHER_GREETING /
     *  .local/greeting.txt (see build.sh). */
    private String welcome(){String name=prefs.getString("name","").trim();return greeting()+(name.isEmpty()?"":" "+name);}
    /** Settings → Your name: typed with the TV keyboard and kept only in this app's storage on the TV. */
    private void weatherLocationEditor(){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(px(24),px(8),px(24),0);
        EditText city=new EditText(this),lat=new EditText(this),lon=new EditText(this);
        city.setHint("City (blank uses TV time zone)");lat.setHint("Latitude, -90 to 90");lon.setHint("Longitude, -180 to 180");
        city.setSingleLine(true);lat.setSingleLine(true);lon.setSingleLine(true);
        lat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        lon.setInputType(lat.getInputType());
        city.setText(prefs.getString("weather_city",""));lat.setText(prefs.getString("weather_latitude",""));lon.setText(prefs.getString("weather_longitude",""));
        fields.addView(city);fields.addView(lat);fields.addView(lon);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Weather location").setView(fields).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String name=city.getText().toString().trim(),a=lat.getText().toString().trim(),b=lon.getText().toString().trim();
            if(!(name.isEmpty()&&a.isEmpty()&&b.isEmpty())&&(name.isEmpty()||LocationConfig.coordinates(a,b)==null)){
                lat.setError("Enter a city and valid coordinates, or clear all fields");return;
            }
            prefs.edit().putString("weather_city",name).putString("weather_latitude",a).putString("weather_longitude",b).remove("weather_json").remove("weather_key").remove("weather_at").apply();
            dialog.dismiss();recreate();
        }));dialog.show();
    }
    private void nameEditor(){
        FrameLayout o=openOverlay(0xA6000000);
        FrameLayout panel=new FrameLayout(this);panel.setBackground(shape(0xF51C1C1E,24));panel.setElevation(px(24));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(440),px(232));lp.gravity=Gravity.CENTER;o.addView(panel,lp);
        panel.addView(text("Your name",22,Color.WHITE,"sans-serif-medium"),pos(392,30,24,24));
        panel.addView(text("Home greets you by time of day, followed by your name.",13,0xB3FFFFFF,"sans-serif"),pos(392,20,24,60));
        EditText field=new EditText(this);field.setSingleLine(true);field.setText(prefs.getString("name",""));field.setSelection(field.getText().length());
        field.setHint("Name");field.setHintTextColor(0x66FFFFFF);field.setTextColor(Color.WHITE);field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(17));
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});field.setPadding(px(16),0,px(16),0);
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Runnable ring=()->{GradientDrawable bg=shape(0x1FFFFFFF,12);if(field.hasFocus())bg.setStroke(px(1.5f),Color.WHITE);field.setBackground(bg);};ring.run();
        field.setOnFocusChangeListener((v,f)->{ring.run();if(f)navigationSound(0);});
        panel.addView(field,pos(392,46,24,94));
        Runnable save=()->{prefs.edit().putString("name",field.getText().toString().trim()).apply();
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(field.getWindowToken(),0);
            closeOverlay(false);showPage("settings",false);controlsFocus(1);};
        field.setOnEditorActionListener((v,action,e)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){save.run();return true;}return false;});
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);panel.addView(buttons,pos(392,42,24,166));
        LinearLayout a=new LinearLayout(this),b=new LinearLayout(this);a.setOrientation(LinearLayout.VERTICAL);b.setOrientation(LinearLayout.VERTICAL);
        buttons.addView(a,new LinearLayout.LayoutParams(px(120),-1));LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(px(120),-1);gap.leftMargin=px(10);buttons.addView(b,gap);
        TextView done=(TextView)menuRow(a,"Save",false,save);done.setGravity(Gravity.CENTER);
        TextView clear=(TextView)menuRow(b,"Clear",false,()->{field.setText("");field.requestFocus();});clear.setGravity(Gravity.CENTER);
        root.post(()->{field.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(field,0);});
    }
    private String greeting(){return ClockPanel.greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));}
    private String today(){return new SimpleDateFormat("EEEE, MMMM d",Locale.ENGLISH).format(new Date());}
    private void focusNearestTile(float x){Tile best=nearestTile(x);if(best!=null)best.requestFocus();}
    private Tile nearestTile(float x){
        Tile best=null;float d=Float.MAX_VALUE;int[] l=new int[2];
        for(Tile t:tiles){if(t.index>=6)break;t.getLocationOnScreen(l);float e=Math.abs(l[0]+t.getWidth()/2f-x);if(e<d){d=e;best=t;}}
        if(best!=null){lastDirectionY=1;lastDirectionX=0;}
        return best;
    }
    /** What the Home widgets need from the launcher: sizes, colours, sounds, the hero line and app details. */
    private final WidgetBoard.Host boardHost=new WidgetBoard.Host(){
        public float unit(){return unit;}
        public boolean light(){return light;}
        public boolean motion(){return MainActivity.this.motion();}
        public void step(int index){navigationSound(index);}
        public void open(){sound();}
        public void hero(String key,String title,String detail){holdGreeting=false;homeGreeting=false;setHero(key,title,detail);}
        public View up(){return tabs.get(0);}
        public View down(float x){return nearestTile(x);}
        public void pageChanged(boolean onHouse,int direction){if(topBoard!=null)prefs.edit().putInt("home_top_page",topBoard.index).apply();}
        public View houseEntry(int direction){if(houseWidgets==null)return null;return direction<0||houseWidgets.scenes.isEmpty()?houseWidgets.cover:houseWidgets.scenes.get(0);}
        public void nudgeHouse(int direction){
            if(houseWidgets==null||!motion())return;final HouseWidgets h=houseWidgets;h.animate().cancel();
            h.animate().translationX(direction*px(10)).setDuration(110).setInterpolator(ease).withEndAction(()->h.animate().translationX(0).setDuration(380).setInterpolator(new OvershootInterpolator(1.4f)).start()).start();
        }
        public App app(String pkg){return appIndex.get(pkg);}
        public View art(String pkg){App a=appIndex.get(pkg);return a==null?null:new Art(a);}
        public void launch(String pkg){App a=appIndex.get(pkg);if(a!=null)MainActivity.this.launch(a);else Toast.makeText(MainActivity.this,"This app is not installed.",Toast.LENGTH_SHORT).show();}
        public void start(Intent intent){try{startActivity(intent);overridePendingTransition(0,0);}catch(RuntimeException e){Toast.makeText(MainActivity.this,"This can't be opened on this TV.",Toast.LENGTH_SHORT).show();}}
        public SharedPreferences prefs(){return prefs;}
        public Activity activity(){return MainActivity.this;}
    };
    /** The Smart Home page slides in or out with the widget pages; it polls House Control only while shown. */
    private void showHouse(boolean show,int direction){
        if(houseWidgets==null)return;final HouseWidgets h=houseWidgets;h.animate().cancel();
        if(show){
            // The 3D renderer wakes once the page has landed: resuming WebGL mid-slide cost frames on this stick.
            h.setVisibility(View.VISIBLE);if(!motion())h.setActive(started);else h.postDelayed(()->{if(houseWidgets==h&&board!=null&&board.onHouse())h.setActive(started);},480);
            if(motion()){h.setTranslationX(direction*px(72));h.setAlpha(0);h.animate().translationX(0).alpha(1).setDuration(460).setStartDelay(40).setInterpolator(ease).withLayer().start();}
            else{h.setTranslationX(0);h.setAlpha(1);}
        } else {
            h.setActive(false);
            Runnable hide=()->{if(houseWidgets==h&&board!=null&&!board.onHouse()){h.setVisibility(View.INVISIBLE);h.setTranslationX(0);h.setAlpha(1);}};
            if(motion())h.animate().translationX(-direction*px(56)).alpha(0).setDuration(240).setInterpolator(ease).withLayer().withEndAction(hide).start();else hide.run();
        }
    }
    /** Apps opened from Gunner's, newest first: Up Next falls back to them without usage access. */
    private void remember(String pkg){
        ArrayList<String> list=new ArrayList<>();list.add(pkg+":"+System.currentTimeMillis());
        for(String e:prefs.getString("recent","").split(","))if(!e.isEmpty()&&!e.startsWith(pkg+":")&&list.size()<12)list.add(e);
        prefs.edit().putString("recent",String.join(",",list)).apply();
    }
    private String sceneDetail(String id){
        String app=sceneApp(id),opens=app.isEmpty()?"":" · Opens "+appLabel(app);
        switch(id){
            case "bright":return "Every light, full brightness"+opens;
            case "cozy":return "Warm, low light for the evening"+opens;
            case "movie":return "Lights down, warm glow behind the TV"+opens;
            case "night":return "Only the bedside lamps"+opens;
            case "away":return "Everything off while you are out"+opens;
            default:return "Set the mood in one press"+opens;
        }
    }
    private void showGreeting() {
        homeGreeting=true;
        if(heroTitle==null || !page.equals("home"))return;
        setHero("@greeting",welcome(),greetingLine());
    }
    /** Cross-fades the hero to a new title and line. */
    private void setHero(String key,String title,String detail){
        if(heroTitle==null||key.equals(heroKey))return;heroKey=key;
        final TextView t=heroTitle,d=heroDescription;t.animate().cancel();d.animate().cancel();
        if(!motion()){t.setText(title);d.setText(detail);t.setAlpha(1);d.setAlpha(1);t.setTranslationY(0);d.setTranslationY(0);return;}
        t.animate().alpha(0).translationY(-px(4)).setDuration(90).withEndAction(()->{
            if(heroTitle!=t||!heroKey.equals(key))return;
            t.setText(title);d.setText(detail);t.setTranslationY(px(8));d.setTranslationY(px(5));d.setAlpha(0);
            t.animate().alpha(1).translationY(0).setDuration(300).setInterpolator(ease).start();
            d.animate().alpha(1).translationY(0).setDuration(360).setStartDelay(30).setInterpolator(ease).start();
        }).start();
        d.animate().alpha(0).setDuration(90).start();
    }
    private void selectApp(App app) {
        selectedPackage=app.pkg;
        if(holdGreeting){holdGreeting=false;return;}
        if(heroTitle==null || !page.equals("home")) return;
        homeGreeting=false;setHero(app.pkg,app.label,app.description);
    }

    private void launch(App app) {
        try {
            Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(app.pkg);
            if (intent==null) intent=getPackageManager().getLaunchIntentForPackage(app.pkg);
            if (intent==null) throw new ActivityNotFoundException();
            sound(); startActivity(intent);overridePendingTransition(0,0);remember(app.pkg);
        } catch (RuntimeException error) { Toast.makeText(this,"Could not open this app. It may be updating.",Toast.LENGTH_SHORT).show(); discover(); }
    }

    private String sceneApp(String scene){try{JSONObject saved=new JSONObject(prefs.getString("scene_app_packages","{}"));if(saved.has(scene))return saved.optString(scene);}catch(Exception ignored){}return scene.equals("movie")?"com.netflix.ninja":"";}
    private String appLabel(String pkg){if(pkg.isEmpty())return "None";for(App app:apps)if(app.pkg.equals(pkg))return app.label;return pkg;}
    /* ---------- overlays: menus, notices and the wallpaper picker (no stock dialogs) ---------- */
    private View nav(){return (View)tabs.get(0).getParent().getParent();}
    private View tabIndicator;private GradientDrawable tabIndicatorShape;private ValueAnimator tabMove;private int tabColor=0x24FFFFFF;
    private int focusedTab(){for(int i=0;i<tabs.size();i++)if(tabs.get(i).hasFocus())return i;return -1;}
    private int pageTab(){return page.equals("apps")?1:page.equals("settings")?2:0;}
    /** Slides the tab pill under the focused tab (white), or the current page's tab (glass) when focus is elsewhere. */
    private void moveTabIndicator(boolean animate){
        if(tabIndicator==null||tabs.isEmpty())return;
        int focused=focusedTab(),index=focused>=0?focused:pageTab();View target=tabs.get(index),row=(View)target.getParent();
        if(target.getWidth()==0){target.post(()->moveTabIndicator(false));return;}
        final float fromX=tabIndicator.getTranslationX(),toX=row.getLeft()+target.getLeft()-((View)row.getParent()).getPaddingLeft();
        final int fromW=tabIndicator.getLayoutParams().width,toW=target.getWidth(),fromColor=tabColor,toColor=focused>=0?(light?0xFF1D1D1F:0xF5FFFFFF):(light?0xE6FFFFFF:0x24FFFFFF);
        final int[] fromText=new int[tabs.size()],toText=new int[tabs.size()];
        for(int i=0;i<tabs.size();i++){fromText[i]=((TextView)tabs.get(i)).getCurrentTextColor();toText[i]=i==focused?(light?Color.WHITE:0xFF17202B):ink(i==pageTab()?0xFF:0xA6);}
        if(tabMove!=null)tabMove.cancel();
        ArgbEvaluator argb=new ArgbEvaluator();
        ValueAnimator.AnimatorUpdateListener step=a->{float f=(float)a.getAnimatedValue();
            tabIndicator.setTranslationX(fromX+(toX-fromX)*f);ViewGroup.LayoutParams lp=tabIndicator.getLayoutParams();lp.width=Math.round(fromW+(toW-fromW)*f);tabIndicator.setLayoutParams(lp);
            tabColor=(int)argb.evaluate(f,fromColor,toColor);tabIndicatorShape.setColor(tabColor);
            float t=Math.max(0,Math.min(1,(f-.12f)/.3f)); // text flips quickly, so it never matches the pill mid-way
            for(int i=0;i<tabs.size();i++)((TextView)tabs.get(i)).setTextColor((int)argb.evaluate(t,fromText[i],toText[i]));};
        tabMove=ValueAnimator.ofFloat(0,1);tabMove.addUpdateListener(step);
        if(!animate||!motion()||fromW==0){tabMove.setDuration(0);}else{tabMove.setDuration(280);tabMove.setInterpolator(ease);}
        tabMove.start();
    }
    private FrameLayout overlay;private View overlayReturn;private Runnable overlayCancel;
    private void closeOverlay(boolean cancelled){
        if(overlay==null)return;final FrameLayout o=overlay;overlay=null;Runnable c=overlayCancel;overlayCancel=null;if(cancelled&&c!=null)c.run();
        trapFocus(false);
        // A fade's end action is skipped when the animation is cancelled, which left the picker stuck over Home:
        // the closing overlay takes no focus, and is removed on a timer, not by the animation.
        o.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);o.animate().cancel();
        if(motion()){o.animate().alpha(0).setDuration(180).start();root.postDelayed(()->root.removeView(o),200);}else root.removeView(o);
        if(overlayReturn!=null){View r=overlayReturn;overlayReturn=null;final long focusGeneration=homeFocusGeneration;root.post(()->{if(focusGeneration!=homeFocusGeneration)return;if(r.isAttachedToWindow()&&r.isShown())r.requestFocus();else if(page.equals("home"))requestHomeRestore();});}
    }
    private void trapFocus(boolean on){int mode=on?ViewGroup.FOCUS_BLOCK_DESCENDANTS:ViewGroup.FOCUS_BEFORE_DESCENDANTS;content.setDescendantFocusability(mode);((ViewGroup)nav()).setDescendantFocusability(mode);}
    private FrameLayout openOverlay(int scrim){
        // One overlay replacing another (About → Support) keeps the original place to return to.
        View back=overlay!=null?overlayReturn:getCurrentFocus();overlayReturn=null;closeOverlay(false);overlayReturn=back;
        trapFocus(true); // the page and tabs behind a dialog take no focus while it is open
        overlay=new FrameLayout(this);overlay.setBackgroundColor(scrim);overlay.setClipChildren(false);overlay.setTranslationZ(px(120));
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        if(motion()){overlay.setAlpha(0);overlay.animate().alpha(1).setDuration(220).setInterpolator(ease).start();}
        return overlay;
    }
    private TextView menuRow(LinearLayout list,String label,boolean checked,Runnable action){
        TextView row=text(label,15,Color.WHITE,"sans-serif-medium");row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(px(18),0,px(18),0);
        row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);row.setFocusable(true);row.setFocusableInTouchMode(true);row.setSoundEffectsEnabled(false);
        if(checked)row.setCompoundDrawablePadding(px(8));
        Runnable paint=()->{boolean f=row.hasFocus();row.setBackground(f?shape(0xF7FFFFFF,12):null);row.setTextColor(f?0xFF111827:Color.WHITE);row.setText((checked?"✓  ":"")+label);};
        paint.run();
        row.setOnFocusChangeListener((v,f)->{paint.run();row.animate().cancel();if(motion())row.animate().scaleX(f?1.02f:1).scaleY(f?1.02f:1).setDuration(240).setInterpolator(ease).start();if(f)navigationSound(list.indexOfChild(row));});
        row.setOnClickListener(v->{sound();action.run();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(42));lp.bottomMargin=px(4);list.addView(row,lp);return row;
    }
    /** A centred glass menu. pick receives the chosen index. */
    private void menu(String title,String[] items,int checked,java.util.function.IntConsumer pick){
        FrameLayout o=openOverlay(0xA6000000);
        FrameLayout panel=new FrameLayout(this);panel.setBackground(shape(0xF51C1C1E,24));panel.setElevation(px(24));
        int rows=Math.min(items.length,7);float h=74+rows*46+18;
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(400),px(h));lp.gravity=Gravity.CENTER;o.addView(panel,lp);
        TextView t=text(title,20,Color.WHITE,"sans-serif-medium");t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);panel.addView(t,pos(352,28,24,24));
        ScrollView sv=new ScrollView(this);sv.setVerticalScrollBarEnabled(false);sv.setClipToPadding(false);sv.setPadding(px(10),0,px(10),0);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(px(4),px(4),px(4),px(10));list.setClipToPadding(false);list.setClipChildren(false);sv.addView(list);
        panel.addView(sv,pos(400,h-66,0,62));
        View first=null;
        for(int i=0;i<items.length;i++){final int index=i;View r=menuRow(list,items[i],i==checked,()->{closeOverlay(false);pick.accept(index);});if(i==Math.max(0,checked)||first==null&&i==0)first=r;}
        if(motion()){panel.setScaleX(.96f);panel.setScaleY(.96f);panel.animate().scaleX(1).scaleY(1).setDuration(360).setInterpolator(spring).start();}
        final View focus=first;root.post(()->{if(focus!=null)focus.requestFocus();});
    }
    private void notice(String title,String message){
        FrameLayout o=openOverlay(0xA6000000);
        FrameLayout panel=new FrameLayout(this);panel.setBackground(shape(0xF51C1C1E,24));panel.setElevation(px(24));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(440),px(260));lp.gravity=Gravity.CENTER;o.addView(panel,lp);
        panel.addView(text(title,22,Color.WHITE,"sans-serif-medium"),pos(392,30,24,26));
        TextView body=text(message,14,0xC7FFFFFF,"sans-serif");body.setLineSpacing(px(5),1);panel.addView(body,pos(392,140,24,66));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);panel.addView(list,pos(140,42,286,200));
        View done=menuRow(list,"Done",false,()->closeOverlay(false));((TextView)done).setGravity(Gravity.CENTER);root.post(done::requestFocus);
    }
    /** Full-screen wallpaper picker: moving between thumbnails previews each wallpaper live. */
    private void wallpaperPicker(){
        final int saved=wallpaperStyle();
        FrameLayout o=openOverlay(Color.TRANSPARENT);
        overlayCancel=()->{wallpaper.show(saved);content.animate().alpha(1).setDuration(260).start();nav().animate().alpha(1).setDuration(260).start();sceneryShade.animate().alpha(1).setDuration(260).start();};
        content.animate().alpha(0).setDuration(240).start();nav().animate().alpha(0).setDuration(240).start();sceneryShade.animate().alpha(0).setDuration(300).start();
        View fade=new View(this);fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x99000000,0xE6000000}));o.addView(fade,pos(960,260,0,280));
        TextView name=text(WALLPAPERS[saved],34,Color.WHITE,"sans-serif-medium");name.setLetterSpacing(-.02f);name.setShadowLayer(px(10),0,px(1),0x66000000);o.addView(name,pos(600,44,48,338));
        TextView hint=text("OK to choose  ·  Back to keep the current one",13,0xB3FFFFFF,"sans-serif");o.addView(hint,pos(600,20,49,386));
        LinearLayout row=new LinearLayout(this);row.setClipChildren(false);row.setClipToPadding(false);o.addView(row,pos(864,80,48,430));
        ArrayList<View> cells=new ArrayList<>();
        for(int i=0;i<WALLPAPERS.length;i++){
            final int index=i;
            FrameLayout cell=new FrameLayout(this);cell.setFocusable(true);cell.setFocusableInTouchMode(true);cell.setSoundEffectsEnabled(false);cell.setClipToOutline(true);
            cell.setBackground(shape(0xFF101521,12));
            ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);img.setImageResource(THUMBS[i]);cell.addView(img,new FrameLayout.LayoutParams(-1,-1));
            GradientDrawable ring=shape(Color.TRANSPARENT,12);ring.setStroke(px(1.5f),i==saved?0xCCFFFFFF:0x33FFFFFF);cell.setForeground(ring);
            cell.setOnFocusChangeListener((v,f)->{
                GradientDrawable r=shape(Color.TRANSPARENT,12);r.setStroke(px(f?2:1.5f),f?Color.WHITE:index==saved?0xCCFFFFFF:0x33FFFFFF);cell.setForeground(r);cell.setElevation(px(f?18:2));
                cell.animate().cancel();float s=f?1.16f:1;if(motion())cell.animate().scaleX(s).scaleY(s).translationY(f?-px(4):0).setDuration(420).setInterpolator(spring).start();else{cell.setScaleX(s);cell.setScaleY(s);}
                if(f){name.setText(WALLPAPERS[index]);wallpaper.show(index);navigationSound(index);}
            });
            cell.setOnClickListener(v->{sound();prefs.edit().putInt("wallpaper",index).apply();overlayCancel=null;closeOverlay(false);
                content.animate().alpha(1).setDuration(260).start();nav().animate().alpha(1).setDuration(260).start();sceneryShade.animate().alpha(1).setDuration(260).start();showPage("settings",false);controlsFocus(0);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(px(100),px(56));if(i>0)lp.leftMargin=px(9);row.addView(cell,lp);cells.add(cell);
        }
        root.post(()->cells.get(saved).requestFocus());
    }

    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(PackageManager.NameNotFoundException e){return "";}}
    private void licencesScreen(){
        try(java.util.Scanner scanner=new java.util.Scanner(getResources().openRawResource(R.raw.third_party_notices),"UTF-8").useDelimiter("\\A")){
            new AlertDialog.Builder(this).setTitle("Open-source licences").setMessage(scanner.hasNext()?scanner.next():"").setPositiveButton("Done",null).show();
        }
    }
    private void aboutScreen(){
        new AlertDialog.Builder(this).setTitle("Gunner's TV Launcher · "+version())
            .setMessage("Your apps, match centre, music and weather in one home. A newly branded derivative built with open-source components. No analytics. Provider feeds and football coverage depend on their services. Not affiliated with any club or streaming provider.")
            .setNegativeButton(R.string.support_title,(d,w)->supportScreen())
            .setNeutralButton("Open-source licences",(d,w)->licencesScreen()).setPositiveButton("Done",null).show();
    }
    private void supportScreen(){
        FrameLayout o=openOverlay(0xA6000000);
        FrameLayout panel=new FrameLayout(this);panel.setBackground(shape(0xF51C1C1E,24));panel.setElevation(px(24));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(640),px(360));lp.gravity=Gravity.CENTER;o.addView(panel,lp);
        panel.addView(text(getString(R.string.support_title),24,Color.WHITE,"sans-serif-medium"),pos(584,34,28,24));
        ImageView qr=new ImageView(this);qr.setImageResource(R.drawable.support_qr);qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if(qr.getDrawable() instanceof BitmapDrawable)((BitmapDrawable)qr.getDrawable()).setFilterBitmap(false);
        qr.setBackground(shape(Color.WHITE,12));qr.setContentDescription(getString(R.string.support_qr_description));panel.addView(qr,pos(204,204,28,82));
        TextView scan=text(getString(R.string.support_scan),12,0xC7FFFFFF,"sans-serif");scan.setGravity(Gravity.CENTER);panel.addView(scan,pos(204,24,28,294));
        panel.addView(text(getString(R.string.support_headline),21,Color.WHITE,"sans-serif-medium"),pos(350,28,260,84));
        TextView body=text(getString(R.string.support_message),14,0xC7FFFFFF,"sans-serif");body.setLineSpacing(px(3),1);panel.addView(body,pos(350,72,260,122));
        panel.addView(text(getString(R.string.support_url),13,0xFFFFFFFF,"sans-serif"),pos(350,42,260,200));
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.VERTICAL);panel.addView(buttons,pos(364,100,250,250));
        View open=menuRow(buttons,getString(R.string.support_open),false,this::openSupportLink);
        menuRow(buttons,"Done",false,()->closeOverlay(false));root.post(open::requestFocus);
    }
    private void openSupportLink(){
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(getString(R.string.support_url))).addCategory(Intent.CATEGORY_BROWSABLE));}
        catch(ActivityNotFoundException|SecurityException unavailable){Toast.makeText(this,R.string.support_no_browser,Toast.LENGTH_LONG).show();}
    }
    // Stock HOME packages; this project never disables them.
    static final String[] STOCK_HOMES={"com.google.android.tvlauncher","com.google.android.apps.tv.launcherx","com.google.android.leanbacklauncher"};
    private Intent homeIntent(){return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);}
    private boolean launcherIsHome(){
        if(Build.VERSION.SDK_INT>=29){android.app.role.RoleManager roles=getSystemService(android.app.role.RoleManager.class);if(roles!=null&&roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))return true;}
        ResolveInfo r=getPackageManager().resolveActivity(homeIntent(),PackageManager.MATCH_DEFAULT_ONLY);return r!=null&&r.activityInfo!=null&&getPackageName().equals(r.activityInfo.packageName);
    }
    private void homeScreen(){
        menu("Home screen",new String[]{"Keep Gunner's","Open Google TV","Default Home app"},0,choice->{
            if(choice==0)prefs.edit().putBoolean("redirect_home",true).apply();
            if(choice==1){prefs.edit().putBoolean("redirect_home",false).apply();Intent i=getPackageManager().getLeanbackLaunchIntentForPackage("com.google.android.apps.tv.launcherx");if(i!=null)startActivity(i);}
            if(choice==2){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(ActivityNotFoundException e){Toast.makeText(this,"Choose Home app in the TV's default-app settings.",Toast.LENGTH_LONG).show();}}
        });
    }
    private void sceneApps(){
        if(!house){notice("Scene apps","Scenes come from House Control TV. Install it to control your lights from Home.");return;}
        JSONArray scenes=lastHome==null?null:lastHome.optJSONObject("plan").optJSONArray("scenes");
        if(scenes==null||scenes.length()==0){notice("Scene apps","Open Home once so Gunner's can load your scenes from House Control.");return;}
        ArrayList<String> ids=new ArrayList<>(),names=new ArrayList<>();for(int i=0;i<scenes.length();i++){JSONObject scene=scenes.optJSONObject(i);ids.add(scene.optString("id"));names.add(scene.optString("name")+"   →   "+appLabel(sceneApp(scene.optString("id"))));}
        menu("Open an app after a scene",names.toArray(new String[0]),-1,index->{
            ArrayList<String> labels=new ArrayList<>();labels.add("None — stay on Home");int current=0;String chosen=sceneApp(ids.get(index));
            for(int i=0;i<apps.size();i++){labels.add(apps.get(i).label);if(apps.get(i).pkg.equals(chosen))current=i+1;}
            menu("After "+scenes.optJSONObject(index).optString("name"),labels.toArray(new String[0]),current,choice->{
                try{JSONObject saved=new JSONObject(prefs.getString("scene_app_packages","{}"));saved.put(ids.get(index),choice==0?"":apps.get(choice-1).pkg);prefs.edit().putString("scene_app_packages",saved.toString()).apply();}catch(Exception ignored){}
                root.post(this::sceneApps);
            });
        });
    }
    /** true when the scene opens an app: the 3D card becomes the screen and flies into the modelled TV. */
    private boolean sceneApplied(String id){
        String pkg=sceneApp(id);if(pkg.isEmpty()||sceneTransition)return false;App target=null;for(App app:apps)if(app.pkg.equals(pkg))target=app;
        if(target==null){Toast.makeText(this,"The scene app is not installed.",Toast.LENGTH_SHORT).show();return false;}
        final App selected=target;if(!motion()||houseWidgets==null){launch(selected);return false;}
        sceneTransition=true;content.setTranslationZ(px(40));
        sceneVeil=new View(this);sceneVeil.setBackgroundColor(Color.BLACK);sceneVeil.setAlpha(0);sceneVeil.setFocusable(true);sceneVeil.setFocusableInTouchMode(true);sceneVeil.setOnKeyListener((v,k,e)->true);sceneVeil.setTranslationZ(px(200));root.addView(sceneVeil,new FrameLayout.LayoutParams(-1,-1));sceneVeil.requestFocus();
        final int flight=1500;
        wallpaper.pauseAmbient();
        hero.animate().alpha(0).translationY(-px(6)).setDuration(360).setInterpolator(ease).withLayer().start();
        for(Tile t:tiles)t.animate().alpha(0).translationY(px(14)).setDuration(420).setInterpolator(ease).withLayer().start();
        nav().animate().alpha(0).setDuration(320).withLayer().start();
        if(clock!=null)clock.animate().alpha(0).setDuration(320).start();
        brand.animate().alpha(0).setDuration(320).start();
        if(board!=null)board.dots.animate().alpha(0).setDuration(260).start();
        houseWidgets.fadeControls(260);houseWidgets.portalToTV(flight);
        sceneVeil.animate().alpha(1).setStartDelay(flight-420).setDuration(420).setInterpolator(new PathInterpolator(.5f,0f,1f,1f)).start();
        // Delays follow the system animation scale, like the animations themselves.
        handler.postDelayed(()->{if(sceneTransition&&started)launch(selected);},Math.round((flight+40)*Math.max(.01f,Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f))));
        return true;
    }
    private void resetSceneTransition(){
        sceneTransition=false;if(sceneVeil!=null){sceneVeil.animate().cancel();root.removeView(sceneVeil);sceneVeil=null;}if(content!=null)content.setTranslationZ(0);if(hero!=null){hero.animate().cancel();hero.setAlpha(1);hero.setTranslationY(0);}nav().animate().cancel();nav().setAlpha(1);if(clock!=null){clock.animate().cancel();clock.setAlpha(1);}if(brand!=null){brand.animate().cancel();brand.setAlpha(1);}if(sceneryShade!=null){sceneryShade.animate().cancel();sceneryShade.setAlpha(page.equals("settings")?1:0);}for(Tile t:tiles){t.animate().cancel();t.setAlpha(1);t.setTranslationY(0);}if(board!=null){board.dots.animate().cancel();board.dots.setAlpha(1);}if(houseWidgets!=null)houseWidgets.resetPortal();
    }

    private void appMenu(App app) {
        Set<String> hidden=new HashSet<>(prefs.getStringSet("hidden",Collections.emptySet()));
        String[] choices={"Open", "Move to first position", "Move left", "Move right", hidden.contains(app.pkg) ? "Show on Home" : "Hide from Home", "App info"};
        menu(app.label,choices,-1,index-> {
            if (index==0) { launch(app); return; }
            ArrayList<String> ids=order(); int current=ids.indexOf(app.pkg);
            if (index==1) { ids.remove(app.pkg); ids.add(0,app.pkg); hidden.remove(app.pkg); }
            if (index==2 && current>0) Collections.swap(ids,current,current-1);
            if (index==3 && current<ids.size()-1) Collections.swap(ids,current,current+1);
            if (index==4) { if (hidden.contains(app.pkg)) hidden.remove(app.pkg); else hidden.add(app.pkg); }
            if (index==5) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+app.pkg))); return; }
            prefs.edit().putString("order",String.join(",",ids)).putStringSet("hidden",hidden).apply();
            selectedPackage=app.pkg;showPage(page,true);
        });
    }

    /* ---------- Settings ---------- */
    private ImageView previewImage;private TextView previewTitle,previewBody;
    private void preview(int thumb,String title,String body){
        if(previewImage==null)return;
        if(thumb!=0){previewImage.setImageResource(thumb);previewImage.setAlpha(1f);}else previewImage.setAlpha(.35f);
        previewTitle.setText(title);previewBody.setText(body);
    }
    private void settings(boolean focus) {
        TextView title=text("Settings",42,ink(0xFF),"sans-serif-medium"); title.setLetterSpacing(-.035f); shadow(title,10);
        content.addView(title,pos(450,52,48,84));
        // The list scrolls itself (reveal): the stock ScrollView stops a focused row flush with its edge, where the
        // row's lift and shadow were cut off. Rows scroll away under a soft edge, not over the title.
        ScrollView sv=new ScrollView(this){@Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect){return 0;}};
        sv.setVerticalScrollBarEnabled(false); sv.setClipToPadding(false); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setVerticalFadingEdgeEnabled(true); sv.setFadingEdgeLength(px(40));
        // No padding on the ScrollView itself: Android fades only the area inside a view's padding, which left the
        // shadow outside it unfaded, as a visible rectangle. The room for the focus lift is the list's padding.
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(px(22),px(12),px(22),px(48)); list.setClipChildren(false); list.setClipToPadding(false);
        sv.addView(list);settingsScroll=sv;
        FrameLayout frame=new FrameLayout(this); frame.setTag("scroll-frame"); frame.addView(sv,new FrameLayout.LayoutParams(-1,-1)); content.addView(frame,pos(520,400,26,138));
        // live preview on the right
        FrameLayout card=new FrameLayout(this);card.setBackground(shape(glass(),22));card.setClipToOutline(true);
        previewImage=new ImageView(this);previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);card.addView(previewImage,pos(352,198,0,0));
        previewTitle=text("",19,ink(0xFF),"sans-serif-medium");card.addView(previewTitle,pos(320,26,18,214));
        previewBody=text("",13,ink(0xC2),"sans-serif");previewBody.setLineSpacing(px(4),1);card.addView(previewBody,pos(316,80,18,246));
        content.addView(card,pos(352,330,560,150));
        int wp=wallpaperStyle();
        ArrayList<View> controls=new ArrayList<>();
        caption(list,"Appearance",true);
        controls.add(option(list,"Wallpaper",()->WALLPAPERS[wallpaperStyle()],null,this::wallpaperPicker,
            ()->preview(THUMBS[wallpaperStyle()],WALLPAPERS[wallpaperStyle()],"Eight wallpapers, from silver silk to the Milky Way. Silver is light by day and dark after sunset.")));
        controls.add(option(list,"Your name",()->{String n=prefs.getString("name","").trim();return n.isEmpty()?"Add":n;},null,this::nameEditor,
            ()->preview(0,"Your name","Home greets you by name. It is saved only on this TV.")));
        controls.add(option(list,"Clock",null,"clock",()->{},()->preview(0,"Clock","Classic analogue clock, digital time and configured weather. OK on the clock opens the forecast.")));
        caption(list,"Motion and sound",false);
        controls.add(option(list,"Fluid motion",null,"motion",()->{},()->preview(0,"Fluid motion","Springy focus, depth and the 3D flight into your TV. Turn off for a calmer, simpler screen.")));
        controls.add(option(list,"Living wallpaper",null,"ambient",()->wallpaper.syncAmbient(),()->preview(THUMBS[wallpaperStyle()],"Living wallpaper","Clouds drift and water moves very slowly. Pauses whenever an app is open.")));
        controls.add(option(list,"Navigation sounds",null,"sound",()->{},()->preview(0,"Navigation sounds","Soft, quiet feedback as you move and open apps.")));
        caption(list,"Home",false);
        widgetsRow=controls.size();
        controls.add(option(list,"Home widgets",this::widgetSummary,null,this::widgetEditor,
            ()->preview(0,"Home widgets","Weather, live matches, Now Playing and Control sit above your apps. Football and content rows sit below. Choose their order and visibility.")));
        controls.add(option(list,"Recommendations",()->"Choose rows",null,this::recommendationSettings,
            ()->preview(0,"Recommendations","Turn individual video and music recommendation rows on or off. Your choices are saved.")));
        controls.add(option(list,"Weather location",()->LauncherConfig.city(this),null,this::weatherLocationEditor,
            ()->preview(0,"Weather location","Use a city and coordinates, or leave blank for the TV time zone. No location permission is needed.")));
        controls.add(option(list,"Temperature",()->new String[]{"Automatic","Celsius","Fahrenheit"}[unitIndex()],null,this::temperatureMenu,
            ()->preview(0,"Temperature","Weather in Celsius or Fahrenheit. Automatic follows the TV's region. The forecast is for "+"your configured location.")));
        footballRow=controls.size();
        controls.add(option(list,"Football setup",()->FootballSettings.load(this).summary(),null,()->footballSetup(false),
            ()->preview(0,"Your football","Choose your main club, followed clubs and players. No personal preset or source edit is required.")));
        newsSourcesRow=controls.size();controls.add(option(list,"Football News creators",()->FootballNewsSources.list(this).size()+" sources",null,()->NewsSourcesEditor.open(this,unit,()->{if(page.equals("settings")){showPage("settings",false);controlsFocus(newsSourcesRow);}}),()->preview(0,"Football News","Choose public YouTube creators for your followed clubs. Recent videos appear in the football centre's News tab.")));
        if(house)controls.add(option(list,"Scene apps",()->"Choose",null,this::sceneApps,()->preview(0,"Scene apps","Open an app after a scene — Movie can start Netflix as the lights go down.")));
        controls.add(option(list,"Home screen",()->launcherIsHome()?"Gunner's":"TV default",null,this::homeScreen,
            ()->preview(0,"Home screen",launcherIsHome()?"Gunner's opens when you press Home. Choose to return to your TV's own home screen.":"Your TV's own home screen opens when you press Home.")));
        caption(list,"System",false);
        controls.add(option(list,"Device settings",()->"",null,()->startActivity(new Intent(Settings.ACTION_SETTINGS)),()->preview(0,"Device settings","Network, picture, sound and accounts.")));
        controls.add(option(list,"About Gunner's",()->"",null,this::aboutScreen,()->preview(0,"About Gunner's","Version "+version()+" · Open-source components")));
        controls.add(option(list,getString(R.string.support_title),()->"",null,this::supportScreen,()->preview(0,getString(R.string.support_title),getString(R.string.support_preview))));
        contentSourcesRow=controls.size();controls.add(option(list,"Content sources",()->"Status",null,this::feedStatus,()->preview(0,"Content sources","See which apps are sharing recommendations and resume titles. Empty feeds stay hidden.")));
        content.setTag(controls);
        preview(THUMBS[wp],WALLPAPERS[wp],"Eight wallpapers, from silver silk to the Milky Way. Silver is light by day and dark after sunset.");
        if (focus) root.post(()->controls.get(0).requestFocus());
    }
    private void caption(LinearLayout list,String label,boolean first){
        TextView c=text(label,13,ink(0x99),"sans-serif-medium");c.setPadding(px(10),0,0,0);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(20));lp.topMargin=px(first?0:12);lp.bottomMargin=px(6);list.addView(c,lp);
    }
    void footballSetup(boolean first){
        if(footballEditor!=null||isFinishing())return;
        footballEditor=new FootballSetup(this,unit,()->{if(page.equals("settings")){showPage("settings",false);controlsFocus(footballRow);}else showPage("home",false);},()->footballEditor=null);
        footballEditor.show(first);
    }
    private void recommendationSettings(){
        ArrayList<FeedCatalog.Row> choices=new ArrayList<>();for(FeedCatalog.Row row:FeedCatalog.ROWS)if(!row.resume)choices.add(row);
        CharSequence[] labels=new CharSequence[choices.size()];boolean[] checked=new boolean[choices.size()];
        Set<String> off=new HashSet<>(Arrays.asList(prefs.getString("widget_off","").split(",")));
        for(int i=0;i<choices.size();i++){labels[i]=choices.get(i).title;checked[i]=!off.contains(choices.get(i).id);}
        new AlertDialog.Builder(this).setTitle("Recommendation rows").setMultiChoiceItems(labels,checked,(dialog,which,on)->{String id=choices.get(which).id;if(on)off.remove(id);else off.add(id);prefs.edit().putString("widget_off",String.join(",",off)).apply();})
            .setPositiveButton("Done",null).show();
    }
    private void feedStatus(){contentSources();}
    void contentSources(){
        if(contentDialog!=null&&contentDialog.isShowing()){contentDialog.setMessage(contentReport());return;}
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle("Content sources").setMessage(contentReport()).setPositiveButton("Done",null).setNeutralButton("Refresh",null);
        android.widget.Button reveal=new android.widget.Button(this);reveal.setText("Show shared rows on Home");reveal.setOnClickListener(v->{ContentFeedReader.Result current=FeedCatalog.latest;if(current==null){discover();return;}Set<String> ids=FeedCatalog.available(current);if(ids.isEmpty()){Toast.makeText(this,"No readable shared titles are available yet.",Toast.LENGTH_LONG).show();return;}Set<String> off=new HashSet<>(Arrays.asList(prefs.getString("widget_off","").split(",")));off.removeAll(ids);prefs.edit().putString("widget_off",String.join(",",off)).apply();showPage(page,false);if(contentDialog!=null)contentDialog.setMessage(contentReport());Toast.makeText(this,"Available shared rows enabled on Home.",Toast.LENGTH_SHORT).show();});builder.setView(reveal);
        if(checkSelfPermission(UpNextWidget.LISTINGS)!=PackageManager.PERMISSION_GRANTED)builder.setNegativeButton("Allow TV listings",(d,w)->requestContentAccess());
        AlertDialog dialog=builder.create();contentDialog=dialog;
        dialog.setOnDismissListener(d->{if(contentDialog==dialog)contentDialog=null;if(started&&!isFinishing()){if(page.equals("settings"))controlsFocus(contentSourcesRow);else requestHomeRestore();}});
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dialog.setMessage("Refreshing shared TV content…");FeedCatalog.latest=null;discover();}));dialog.show();
    }
    void requestContentAccess(){
        String permission=UpNextWidget.LISTINGS;
        if(checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED){FeedCatalog.latest=null;discover();return;}
        try{PermissionInfo info=getPackageManager().getPermissionInfo(permission,0);if((info.protectionLevel&PermissionInfo.PROTECTION_MASK_BASE)!=PermissionInfo.PROTECTION_DANGEROUS){Toast.makeText(this,"This firmware does not offer a user-grantable listings permission. Check Content sources for accessible data.",Toast.LENGTH_LONG).show();return;}}
        catch(PackageManager.NameNotFoundException missing){Toast.makeText(this,"TV listings access is not exposed by this firmware.",Toast.LENGTH_LONG).show();return;}
        if(shouldShowRequestPermissionRationale(permission)||!prefs.getBoolean("asked_listings",false)){prefs.edit().putBoolean("asked_listings",true).apply();requestPermissions(new String[]{permission},7);}
        else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));
    }
    private String contentReport(){
        StringBuilder body=new StringBuilder();ContentFeedReader.Result feed=FeedCatalog.latest;
        body.append(feed==null?"Shared content has not been refreshed yet.":feed.status.message()+"\n\n"+feed.status.counts()).append("\n\n");
        for(FeedCatalog.Row row:FeedCatalog.ROWS){int count=feed==null?0:row.select(feed).size();body.append(row.title).append(": ").append(count==0?"not currently shared":count+" titles shared").append(WidgetBoard.on(prefs,row.id)?"":" · Hidden in Home widgets").append("\n");}
        body.append("\nMusic uses recommendations shared by the TV apps. No Spotify Premium or account connection is needed for shared cards. YouTube watch history is not exposed. Now Playing requires an active media session; some TV apps stop playback on Home.\n\nFootball: ESPN public feed. Choose clubs, players and a main club in Football setup. All supplied competitions are included; player participation stays pending until lineups are confirmed. The live widget falls back only to your selected main club.");
        if(feed!=null&&!feed.error.isEmpty())body.append("\n\n").append(feed.error);
        body.append("\n\nOnly searchable app-shared records can be shown. Google TV's private recommendations and provider account history are not accessible through these tables.");return body.toString();
    }
    private ScrollView settingsScroll;private ValueAnimator settingsGlide;private int widgetsRow,footballRow,newsSourcesRow,contentSourcesRow;
    /** Scrolls the settings list so the focused row, with room for its lift and shadow (and its group's caption
     *  above the first row of a group), clears the faded edges. */
    private void reveal(View row){
        ScrollView sv=settingsScroll;if(sv==null||row.getParent()==null||row.getParent().getParent()!=sv)return;
        ViewGroup list=(ViewGroup)row.getParent();int i=list.indexOfChild(row),top=row.getTop(),bottom=row.getBottom();
        if(i>0&&!list.getChildAt(i-1).isFocusable())top=list.getChildAt(i-1).getTop();
        if(i<=1)top=0;
        int margin=px(54),h=sv.getHeight(),y=sv.getScrollY(),max=Math.max(0,list.getHeight()-h),target=y;
        if(top-margin<y)target=top-margin;else if(bottom+margin>y+h)target=bottom+margin-h;
        target=Math.max(0,Math.min(max,target));
        if(settingsGlide!=null)settingsGlide.cancel();
        if(target==y)return;
        if(!motion()){sv.scrollTo(0,target);return;}
        settingsGlide=ValueAnimator.ofInt(y,target);settingsGlide.setDuration(420);settingsGlide.setInterpolator(ease);
        settingsGlide.addUpdateListener(a->sv.scrollTo(0,(int)a.getAnimatedValue()));settingsGlide.start();
    }
    private String widgetSummary(){boolean inputs=!InputsWidget.list(this).isEmpty();int n=0;for(String id:WidgetBoard.IDS)if(WidgetBoard.on(prefs,id)&&(house||!id.equals("house"))&&(inputs||!id.equals("inputs")))n++;return n==0?"Off":n+" on";}
    private int unitIndex(){String u=prefs.getString("temp_unit","auto");return u.equals("c")?1:u.equals("f")?2:0;}
    private void temperatureMenu(){
        menu("Temperature",new String[]{"Automatic","Celsius","Fahrenheit"},unitIndex(),choice->{
            prefs.edit().putString("temp_unit",new String[]{"auto","c","f"}[choice]).apply();showPage("settings",false);controlsFocus(widgetsRow+3);});
    }
    private void widgetEditor(){new WidgetEditor();}
    /** Settings → Home widgets. OK turns a widget on or off; Hold OK picks it up, Up and Down move it, OK or Back
     *  puts it down. On the right, miniature pages show how the widgets fill Home, updated with every change. */
    private final class WidgetEditor {
        final ArrayList<String> order=WidgetBoard.order(prefs);
        final HashSet<String> off=new HashSet<>();
        final HashMap<String,Runnable> paints=new HashMap<>();
        final LinearLayout list=new LinearLayout(MainActivity.this);
        final FocusScrollView viewport=new FocusScrollView(MainActivity.this,px(10));
        final FrameLayout pages=new FrameLayout(MainActivity.this);
        final TextView hint;
        String moving;boolean ignoreUp;
        WidgetEditor(){
            for(String id:prefs.getString("widget_off","").split(","))if(!id.isEmpty())off.add(id);
            FrameLayout o=openOverlay(0xB3000000);
            overlayCancel=()->root.post(()->{showPage("settings",false);controlsFocus(widgetsRow);});
            FrameLayout panel=new FrameLayout(MainActivity.this);panel.setBackground(shape(0xF51C1C1E,26));panel.setElevation(px(24));panel.setClipChildren(false);
            float h=Math.min(480,getResources().getDisplayMetrics().heightPixels/unit-32);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(760),px(h));lp.gravity=Gravity.CENTER;o.addView(panel,lp);
            panel.addView(text("Home widgets",24,Color.WHITE,"sans-serif-medium"),pos(420,32,28,26));
            hint=text("",13,0xB3FFFFFF,"sans-serif");hint.setSingleLine(true);panel.addView(hint,pos(420,18,29,64));setHint();
            list.setOrientation(LinearLayout.VERTICAL);list.setClipChildren(false);list.setClipToPadding(false);list.setPadding(px(12),0,px(12),0);
            viewport.addView(list,new ScrollView.LayoutParams(-1,-2));panel.addView(viewport,pos(436,h-112,20,90));
            pages.setClipChildren(true);panel.addView(pages,pos(262,Math.min(370,h-60),472,34));
            for(String id:order)list.addView(row(id),rowParams());
            drawPages();
            if(motion()){panel.setScaleX(.96f);panel.setScaleY(.96f);panel.animate().scaleX(1).scaleY(1).setDuration(360).setInterpolator(spring).start();}
            root.post(()->list.getChildAt(0).requestFocus());
        }
        LinearLayout.LayoutParams rowParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(54));p.bottomMargin=px(7);return p;}
        final boolean inputs=!InputsWidget.list(MainActivity.this).isEmpty();
        boolean available(String id){return id.equals("house")?house:id.equals("inputs")?inputs:true;}
        void setHint(){hint.setText(moving!=null?"Up and Down move it. OK puts it down.":"OK turns a widget on or off. Hold OK to move it.");}
        GradientDrawable oval(int color){GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);d.setColor(color);return d;}
        View row(String id){
            FrameLayout row=new FrameLayout(MainActivity.this);row.setFocusable(true);row.setFocusableInTouchMode(true);row.setSoundEffectsEnabled(false);row.setClipChildren(false);
            row.setContentDescription(WidgetBoard.title(id));
            View disc=new View(MainActivity.this);row.addView(disc,pos(32,32,12,11));
            Glyph glyph=new Glyph(MainActivity.this).kind(WidgetBoard.glyph(id));row.addView(glyph,pos(18,18,19,18));
            TextView name=text(WidgetBoard.title(id),15,Color.WHITE,"sans-serif-medium");row.addView(name,pos(280,19,56,9));
            TextView about=text(available(id)?WidgetBoard.about(id):id.equals("inputs")?"Only on TVs: a TV stick has no HDMI inputs.":"Install House Control TV to add your home.",11.5f,0x99FFFFFF,"sans-serif");
            about.setSingleLine(true);about.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(about,pos(288,15,56,31));
            View track=new View(MainActivity.this),knob=new View(MainActivity.this);row.addView(track,pos(40,24,368,15));row.addView(knob,pos(20,20,370,17));
            TextView grip=text("≡",22,0xFF111827,"sans-serif-medium");grip.setGravity(Gravity.CENTER);grip.setAlpha(0);row.addView(grip,pos(40,24,368,13));
            Runnable paint=()->{
                boolean f=row.hasFocus(),m=id.equals(moving),lit=f||m,on=!off.contains(id)&&available(id);
                row.setBackground(shape(lit?0xFAFFFFFF:0x14FFFFFF,14));
                name.setTextColor(lit?0xFF111827:Color.WHITE);about.setTextColor(lit?0xFF5B6472:0x99FFFFFF);
                disc.setBackground(oval(on?0xFF0A84FF:lit?0x1A000000:0x26FFFFFF));glyph.color(on?Color.WHITE:lit?0xFF111827:0xCCFFFFFF);
                track.setBackground(shape(on?0xFF34C759:lit?0x2E111827:0x33FFFFFF,12));knob.setBackground(shape(Color.WHITE,10));knob.setElevation(px(1.5f));
                float x=on?px(16):0;knob.animate().cancel();if(motion())knob.animate().translationX(x).setDuration(260).setInterpolator(spring).start();else knob.setTranslationX(x);
                track.setAlpha(m?0:1);knob.setAlpha(m?0:1);grip.setAlpha(m?1:0);
                row.setAlpha(available(id)?1:.5f);
            };
            paints.put(id,paint);paint.run();
            row.setOnFocusChangeListener((v,f)->{paint.run();lift(row,f);if(f){navigationSound(list.indexOfChild(row));viewport.post(()->viewport.reveal(row));}});
            row.setOnClickListener(v->{
                if(moving!=null)return;sound();
                if(!available(id)){row.animate().cancel();row.setTranslationX(0);if(motion())row.animate().translationX(px(6)).setDuration(70).withEndAction(()->row.animate().translationX(0).setDuration(320).setInterpolator(new OvershootInterpolator(3f)).start()).start();return;}
                if(!off.remove(id))off.add(id);save();paint.run();drawPages();
            });
            row.setOnLongClickListener(v->{sound();moving=id;ignoreUp=true;setHint();paint.run();lift(row,true);return true;});
            row.setOnKeyListener((v,key,e)->{
                if(!id.equals(moving))return false;
                boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
                if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_BACK){if(!down){if(ignoreUp&&key!=KeyEvent.KEYCODE_BACK)ignoreUp=false;else drop(row);}return true;}
                if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){if(down)move(row,id,key==KeyEvent.KEYCODE_DPAD_UP?-1:1);return true;}
                return key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;
            });
            return row;
        }
        void lift(View row,boolean f){
            boolean m=moving!=null&&row.hasFocus();float s=m?1.045f:f?1.02f:1;row.animate().cancel();
            if(motion())row.animate().scaleX(s).scaleY(s).setDuration(300).setInterpolator(spring).start();else{row.setScaleX(s);row.setScaleY(s);}
            row.setElevation(px(m?22:f?10:0));row.setTranslationZ(px(m?10:0));
        }
        /** Swaps the picked-up row with its neighbour. The neighbour moves in the list (the focused row stays
         *  attached, so it keeps focus) and both glide from their old places. */
        void move(View row,String id,int d){
            int i=order.indexOf(id),j=i+d;if(j<0||j>=order.size())return;
            View other=list.getChildAt(j);int rowTop=row.getTop(),otherTop=other.getTop();
            Collections.swap(order,i,j);list.removeView(other);list.addView(other,i,rowParams());
            for(View v:new View[]{row,other}){float from=v==row?rowTop-otherTop:otherTop-rowTop;v.animate().cancel();v.setTranslationY(from);
                if(motion())v.animate().translationY(0).setDuration(300).setInterpolator(ease).start();else v.setTranslationY(0);}
            if(motion())row.animate().scaleX(1.045f).scaleY(1.045f).setDuration(300).start();
            else{row.setScaleX(1.045f);row.setScaleY(1.045f);}
            viewport.post(()->viewport.reveal(row));
            if(motion())viewport.postDelayed(()->viewport.reveal(row),320);
            navigationSound(j);save();drawPages();
        }
        void drop(View row){moving=null;sound();setHint();for(Runnable r:paints.values())r.run();lift(row,true);save();}
        void save(){WidgetBoard.save(prefs,order,off);}
        /** Miniature Home pages: the greeting, the widgets of each page, and the apps that stay underneath. */
        void drawPages(){
            pages.removeAllViews();
            pages.addView(text("Widgets above · Content below apps",13,0x99FFFFFF,"sans-serif-medium"),pos(260,16,2,0));
            ArrayList<ArrayList<String>> layout=WidgetBoard.pages(prefs,house,inputs);
            if(layout.isEmpty()){TextView t=text("Only the greeting and your apps.",12.5f,0x80FFFFFF,"sans-serif");pages.addView(t,pos(250,18,2,30));return;}
            for(int p=0;p<Math.min(12,layout.size());p++){
                ArrayList<String> names=new ArrayList<>();for(String id:layout.get(p))names.add(WidgetBoard.title(id));
                TextView label=text((p+1)+"   "+String.join(" · ",names),11,0xCCFFFFFF,"sans-serif");
                label.setBackground(shape(0x14FFFFFF,6));label.setPadding(px(8),px(5),px(8),0);
                pages.addView(label,pos(260,25,0,28+p*28));
            }
        }
    }
    @SuppressWarnings("unchecked") private void controlsFocus(int i) { root.post(()-> { Object tag=content.getTag(); if(tag instanceof ArrayList) ((ArrayList<View>)tag).get(i).requestFocus(); }); }
    /** A settings row: either a value with a chevron, or an Apple-style switch bound to a preference. */
    private View option(LinearLayout parent,String title,java.util.function.Supplier<String> value,String toggle,Runnable action,Runnable onFocus) {
        FrameLayout row=new FrameLayout(this);row.setFocusable(true);row.setFocusableInTouchMode(true);row.setSoundEffectsEnabled(false);
        TextView name=text(title,15,Color.WHITE,"sans-serif-medium");row.addView(name,pos(300,20,18,12));
        TextView detail=value==null?null:text(value.get()+(value.get().equals("")?"›":"   ›"),14,0x99FFFFFF,"sans-serif");
        if(detail!=null){detail.setGravity(Gravity.RIGHT);row.addView(detail,pos(200,20,258,12));}
        final View track=new View(this),knob=new View(this);
        if(toggle!=null){row.addView(track,pos(40,24,418,9));row.addView(knob,pos(20,20,420,11));}
        Runnable paint=()->{boolean f=row.hasFocus();row.setBackground(shape(f?0xFAFFFFFF:light?0x8CFFFFFF:0x14FFFFFF,14));name.setTextColor(f?0xFF111827:ink(0xFF));
            if(detail!=null)detail.setTextColor(f?0xFF5B6472:ink(0x99));
            if(toggle!=null){boolean on=toggle.equals("motion")?motion():prefs.getBoolean(toggle,true);track.setBackground(shape(on?0xFF34C759:f||light?0x2E111827:0x33FFFFFF,12));knob.setBackground(shape(Color.WHITE,10));knob.setElevation(px(1.5f));
                float x=on?px(16):0;knob.animate().cancel();if(motion())knob.animate().translationX(x).setDuration(260).setInterpolator(spring).start();else knob.setTranslationX(x);}};
        paint.run();
        row.setOnFocusChangeListener((v,f)->{paint.run();row.animate().cancel();if(motion())row.animate().scaleX(f?1.02f:1).scaleY(f?1.02f:1).setDuration(300).setInterpolator(spring).start();else{row.setScaleX(1);row.setScaleY(1);}row.setElevation(px(f?10:0));if(f){onFocus.run();navigationSound(parent.indexOfChild(row));reveal(row);}});
        row.setOnClickListener(v->{sound();
            if(toggle!=null){boolean on=toggle.equals("motion")?motion():prefs.getBoolean(toggle,true);prefs.edit().putBoolean(toggle,!on).apply();
                if(toggle.equals("clock"))clock.setVisibility(!on?View.VISIBLE:View.GONE);paint.run();}
            action.run();});
        row.setContentDescription(title);
        row.setOnKeyListener((v,key,e)-> {if(e.getAction()==KeyEvent.ACTION_DOWN && key==KeyEvent.KEYCODE_DPAD_UP && parent.getChildAt(1)==row) {tabs.get(2).requestFocus();return true;}return false;});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(44));lp.bottomMargin=px(6);parent.addView(row,lp);return row;
    }
    @Override public void onBackPressed() {
        if (overlay!=null) { closeOverlay(true); return; }
        if (!page.equals("home")) showPage("home",true);
        else if (scroll!=null && scroll.getScrollY()>0) {scroll.smoothScrollTo(0,0);if(!tiles.isEmpty())tiles.get(0).requestFocus();}
        else tabs.get(0).requestFocus();
    }

    private final class Tile extends FrameLayout {
        final App app;
        final Art art;
        final FrameLayout face;
        final Shine light;
        final GradientDrawable rim;
        final TextView label;
        final int index;
        final TileMotion movement;
        Tile(App item,int number) {
            super(MainActivity.this);app=item;index=number;setTag("app:"+app.pkg);
            setClipChildren(false);setClipToPadding(false);setFocusable(true);setFocusableInTouchMode(true);setSoundEffectsEnabled(false);
            setContentDescription(app.label+". OK: open. Hold OK: organize app.");
            face=new FrameLayout(MainActivity.this);GradientDrawable faceBg=shape(app.color,16);faceBg.setStroke(Math.max(1,px(.6f)),0x1FFFFFFF);face.setBackground(faceBg);face.setClipToOutline(true);face.setElevation(px(2));face.setCameraDistance(px(900));
            art=new Art(app);face.addView(art,new FrameLayout.LayoutParams(-1,-1));
            light=new Shine();light.setAlpha(0);face.addView(light,new FrameLayout.LayoutParams(-1,-1));
            rim=shape(Color.TRANSPARENT,16);rim.setStroke(px(1.2f),0xF0FFFFFF);rim.setAlpha(0);face.setForeground(rim);
            addView(face,pos(132,76,0,0));
            label=text(app.label,13.5f,ink(0xFF),"sans-serif-medium");label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setAlpha(0);shadow(label,6);
            addView(label,pos(160,22,-14,88));
            movement=new TileMotion(this);
            setOnFocusChangeListener((v,f)-> {
                movement.focus(f);
                face.setElevation(px(f?14:2));rim.setAlpha(f?210:0);
                label.animate().cancel();if(motion())label.animate().alpha(f?1:0).translationY(f?0:-px(3)).setDuration(f?260:140).setInterpolator(ease).start();else label.setAlpha(f?1:0);
                setTranslationZ(px(f?20:0));
                if(f) {
                    selectApp(app);wallpaper.follow(index);
                    if(index<6) scroll.post(()->scroll.smoothScrollTo(0,0));
                    else requestRectangleOnScreen(new Rect(-px(12),-px(14),getWidth()+px(12),getHeight()+px(5)),false);
                }
            });
            setOnClickListener(v->launch(app));
            setOnLongClickListener(v->{sound();appMenu(app);return true;});
            setOnKeyListener((v,key,e)-> {
                if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) {
                    movement.press(e.getAction()==KeyEvent.ACTION_DOWN);return false;
                }
                if(e.getAction()!=KeyEvent.ACTION_DOWN) return false;
                if(page.equals("home")&&key==KeyEvent.KEYCODE_DPAD_DOWN&&board!=null){View below=board.entry(WidgetBoard.centerX(this));if(below!=null)below.requestFocus();return true;}
                if(key==KeyEvent.KEYCODE_DPAD_UP && index<6){
                    int[] l=new int[2];getLocationOnScreen(l);float x=l[0]+getWidth()/2f;
                    View above=page.equals("home")&&topBoard!=null?topBoard.nearest(x):null;
                    if(page.equals("home")&&board!=null&&board.onHouse()&&houseWidgets!=null)houseWidgets.focusNearest(x);
                    else if(above!=null)above.requestFocus();else tabs.get(pageTab()).requestFocus();
                    return true;}
                int next=-1;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT && index%6>0) next=index-1;
                if(key==KeyEvent.KEYCODE_DPAD_RIGHT && index%6<5 && index+1<tiles.size()) next=index+1;
                if(key==KeyEvent.KEYCODE_DPAD_DOWN && index+6<tiles.size()) next=index+6;
                if(key==KeyEvent.KEYCODE_DPAD_UP && index>=6) next=index-6;
                if(next>=0){if(e.getRepeatCount()==0 || e.getRepeatCount()%2==0){homeGreeting=false;lastDirectionX=Integer.compare(next%6,index%6);lastDirectionY=Integer.compare(next/6,index/6);navigationSound(next);tiles.get(next).requestFocus();}return true;}
                return key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT || key==KeyEvent.KEYCODE_DPAD_DOWN;
            });
        }
        @Override protected void onDetachedFromWindow(){movement.stop();super.onDetachedFromWindow();}
    }

    /** Native property animations keep the static card artwork cached on the GPU. */
    private final class TileMotion {
        final Tile tile;
        boolean focused,pressed;
        TileMotion(Tile tile){this.tile=tile;}
        void focus(boolean value) {
            focused=value;pressed=false;
            if(value && motion()) {
                tile.face.animate().cancel();
                tile.face.setRotationY(-lastDirectionX*4.5f);tile.face.setRotationX(lastDirectionY*3f);
            }
            start();
            if(motion()) {
                tile.art.animate().cancel();
                float depthX=focused?px((tile.index%6-2.5f)*.7f):0;
                float depthY=focused?-px(1):0;
                if(focused){tile.art.setTranslationX(depthX-px(lastDirectionX*4.5f));tile.art.setTranslationY(depthY+px(lastDirectionY*3));tile.light.setTranslationX(px(lastDirectionX*12));}
                tile.art.animate().translationX(depthX).translationY(depthY).setDuration(520).setInterpolator(drift).start();
                tile.light.animate().translationX(focused?-depthX*2:0).alpha(focused?.6f:0).setDuration(580).setInterpolator(drift).start();
            }
        }
        void press(boolean down){pressed=down;start();}
        void start(){
            if(!motion()){snap();return;}
            tile.face.animate().cancel();tile.label.animate().cancel();tile.light.animate().cancel();
            float scale=(focused?1.11f:1)-(pressed?.03f:0);
            tile.face.animate().scaleX(scale).scaleY(scale).translationY(focused?-px(3):0).rotationX(0).rotationY(0).setDuration(pressed?65:420).setInterpolator(pressed?ease:spring).withLayer().start();
            tile.label.animate().scaleX(focused?1.025f:1).scaleY(focused?1.025f:1).setDuration(260).setInterpolator(ease).start();
            tile.light.animate().alpha(focused?.6f:0).setDuration(200).setInterpolator(ease).start();
        }
        void stop(){tile.face.animate().cancel();tile.art.animate().cancel();tile.label.animate().cancel();tile.light.animate().cancel();}
        void snap(){
            stop();pressed=false;
            float scale=focused?1.11f:1;
            tile.face.setScaleX(scale);tile.face.setScaleY(scale);tile.face.setTranslationY(focused?-px(3):0);tile.face.setRotationX(0);tile.face.setRotationY(0);
            tile.art.setTranslationX(0);tile.art.setTranslationY(0);tile.light.setTranslationX(0);tile.light.setAlpha(focused?.6f:0);
            tile.label.setScaleX(focused?1.025f:1);tile.label.setScaleY(focused?1.025f:1);
        }
    }

    private final class Art extends View {
        final App app; final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final RectF bounds=new RectF();Shader finish;
        Art(App app) {super(MainActivity.this);this.app=app;}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            finish=new LinearGradient(0,0,0,h,new int[]{0x20FFFFFF,0x00FFFFFF,0x14000000},new float[]{0,.46f,1},Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight();
            bounds.set(0,0,w,h);
            p.setColor(app.color);p.setShader(null);c.drawRoundRect(bounds,px(16),px(16),p);
            String pkg=app.pkg;
            if(pkg.equals("com.google.android.youtube.tv")) {
                float cx=w/2,cy=h/2;
                p.setColor(0xFFFF0033);bounds.set(cx-px(25),cy-px(17),cx+px(25),cy+px(17));c.drawRoundRect(bounds,px(10),px(10),p);
                p.setColor(Color.WHITE);Path path=new Path();path.moveTo(cx-px(5),cy-px(9));path.lineTo(cx+px(10),cy);path.lineTo(cx-px(5),cy+px(9));path.close();c.drawPath(path,p);
            } else if(pkg.equals("com.netflix.ninja")) {
                p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(px(54));p.setColor(0xFFE50914);p.setTextAlign(Paint.Align.CENTER);c.drawText("N",w/2,h/2+px(19),p);
            } else if(pkg.equals("com.soundcloud.android")) {
                drawIcon(c,app.icon,48);
            } else if(pkg.equals("com.ted.android.tv")) {
                p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(px(33));p.setTextAlign(Paint.Align.CENTER);p.setColor(0xFFFF302B);c.drawText("TED",w/2,h/2+px(12),p);
            } else if(pkg.equals("stream.tv.online")) {
                float cx=w/2,cy=h/2-px(2);
                p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(px(2.5f));
                bounds.set(cx-px(27),cy-px(18),cx+px(27),cy+px(18));c.drawRoundRect(bounds,px(6),px(6),p);
                c.drawLine(cx,cy+px(18),cx,cy+px(25),p);c.drawLine(cx-px(10),cy+px(25),cx+px(10),cy+px(25),p);
                p.setStyle(Paint.Style.FILL);Path play=new Path();play.moveTo(cx-px(5),cy-px(9));play.lineTo(cx+px(10),cy);play.lineTo(cx-px(5),cy+px(9));play.close();c.drawPath(play,p);
            } else if(pkg.equals("com.phlox.tvwebbrowser")) {
                float cx=w/2,cy=h/2,r=px(26);
                p.setColor(0xF4FFFFFF);c.drawCircle(cx,cy,r,p);
                p.setColor(0xFFB4C6D6);p.setStrokeWidth(px(1));
                for(int tick=0;tick<24;tick++) {
                    double a=tick*Math.PI/12;float inner=r-px(tick%3==0?6:3);
                    c.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*(r-px(2)),cy+(float)Math.sin(a)*(r-px(2)),p);
                }
                c.save();c.rotate(38,cx,cy);
                Path needle=new Path();needle.moveTo(cx,cy-px(21));needle.lineTo(cx+px(5),cy);needle.lineTo(cx-px(5),cy);needle.close();p.setColor(0xFFE65E62);c.drawPath(needle,p);
                needle.reset();needle.moveTo(cx,cy+px(21));needle.lineTo(cx+px(5),cy);needle.lineTo(cx-px(5),cy);needle.close();p.setColor(0xFF42677F);c.drawPath(needle,p);c.restore();
            } else if(pkg.equals("com.google.android.vending") || pkg.equals("com.android.vending")) {
                drawIcon(c,app.icon,47);
            } else if(app.banner!=null) {
                float ratio=Math.min(w/app.banner.getWidth(),h/app.banner.getHeight());
                float bw=app.banner.getWidth()*ratio,bh=app.banner.getHeight()*ratio;
                bounds.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);p.setAlpha(255);c.drawBitmap(app.banner,null,bounds,p);
            } else drawIcon(c,app.icon,48);
            p.setShader(finish);
            bounds.set(0,0,w,h);c.drawRoundRect(bounds,px(16),px(16),p);p.setShader(null);
        }
        private void drawIcon(Canvas c,Bitmap icon,float size) {float s=px(size);bounds.set((getWidth()-s)/2,(getHeight()-s)/2,(getWidth()+s)/2,(getHeight()+s)/2);p.setAlpha(255);c.drawBitmap(icon,null,bounds,p);}
    }

    /** A separate compositor layer keeps the icon display list static during focus. */
    private final class Shine extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Shine(){super(MainActivity.this);}
        @Override public boolean hasOverlappingRendering(){return false;}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            paint.setShader(new LinearGradient(-w*.4f,0,w*.95f,h,new int[]{0x00FFFFFF,0x40FFFFFF,0x00FFFFFF},new float[]{0,.5f,1},Shader.TileMode.CLAMP));
        }
        @Override protected void onDraw(Canvas canvas){canvas.drawRect(0,0,getWidth(),getHeight(),paint);}
    }

    /** Independent compositor surfaces: distant clouds drift behind the cut-out landscape. */
    private final class Wallpaper extends FrameLayout {
        final HandlerThread surfaceThread=new HandlerThread("Gunner's scenery",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        final Handler surfaceHandler;
        final SceneLayer sky, terrain;
        final FrameLayout skyTrack;
        volatile boolean ambientRunning;
        volatile float ambientSeconds,cloudX,cloudY;
        long lastAmbientTick,ambientTicks;
        private final Runnable ambientTick=new Runnable(){public void run(){
            if(!ambientEligible()){syncAmbient();return;}
            long now=SystemClock.uptimeMillis();
            ambientSeconds+=Math.min(.25f,(now-lastAmbientTick)/1000f);lastAmbientTick=now;
            cloudX=px(14)*(float)Math.sin(ambientSeconds*Math.PI*2/112);
            cloudY=px(.65f)*(float)Math.sin(ambientSeconds*Math.PI*2/83);
            if(blend>=1){sky.drawSurface();terrain.drawSurface();ambientTicks++;}
            handler.postDelayed(this,100);
        }};
        Bitmap skySource, terrainSource;
        int requestedStyle=-1;
        volatile float blend=1;
        ValueAnimator sceneTransition;
        Wallpaper() {
            super(MainActivity.this);
            surfaceThread.start();surfaceHandler=new Handler(surfaceThread.getLooper());
            setClipChildren(false);setClipToPadding(false);
            sky=new SceneLayer(false);terrain=new SceneLayer(true);
            skyTrack=new FrameLayout(MainActivity.this);skyTrack.setClipChildren(false);skyTrack.setClipToPadding(false);
            skyTrack.addView(sky,new FrameLayout.LayoutParams(-1,-1));
            int margin=px(30);
            int width=getResources().getDisplayMetrics().widthPixels+margin*2;
            int height=getResources().getDisplayMetrics().heightPixels+margin*2;
            FrameLayout.LayoutParams back=new FrameLayout.LayoutParams(width,px(220)+margin);
            back.leftMargin=-margin;back.topMargin=-margin;addView(skyTrack,back);
            FrameLayout.LayoutParams front=new FrameLayout.LayoutParams(width,height);
            front.leftMargin=-margin;front.topMargin=-margin;addView(terrain,front);
            update();
        }
        void follow(int index) {
            skyTrack.animate().cancel();terrain.animate().cancel();
            float position=(index%6-2.5f)/2.5f;
            float arc=1-position*position;
            if(motion()) {
                skyTrack.animate().translationX(-position*px(requestedStyle>=3?11:5)).translationY(px(arc*(requestedStyle>=3?2.2f:.7f))).setDuration(1000).setInterpolator(drift).start();
                terrain.animate().translationX(-position*px(22)).translationY(px(arc*3-Math.min(index/6,2)*2)).setDuration(680).setInterpolator(drift).start();
            } else rest();
        }
        void rest(){
            skyTrack.animate().cancel();terrain.animate().cancel();
            skyTrack.setTranslationX(0);skyTrack.setTranslationY(0);terrain.setTranslationX(0);terrain.setTranslationY(0);
        }
        private boolean ambientEligible() {
            return started && hasWindowFocus() && !page.equals("settings") && requestedStyle!=2
                && motion() && prefs.getBoolean("ambient",true) && sky.scene!=null && terrain.scene!=null;
        }
        void syncAmbient() {
            if(ambientEligible()) {
                if(!ambientRunning){ambientRunning=true;lastAmbientTick=SystemClock.uptimeMillis();handler.postDelayed(ambientTick,100);}
            } else {
                pauseAmbient();
                if(!motion() || !prefs.getBoolean("ambient",true) || requestedStyle==2) {
                    cloudX=0;cloudY=0;sky.drawSurface();terrain.drawSurface();
                }
            }
        }
        void pauseAmbient(){ambientRunning=false;handler.removeCallbacks(ambientTick);}
        private Bitmap compose(Bitmap source,int style,boolean foreground,int width,int height) {
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(result);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            if(!foreground || style==2)c.drawColor(base(style));
            if(source!=null && style!=2) {
                // Shared destination bounds keep the horizon registered; only the terrain has alpha.
                // Normalize almost-opaque generated terrain pixels, preserving antialiased skyline edges.
                // Otherwise a moving sky faintly shines through the mountain interiors.
                if(foreground)paint.setColorFilter(new ColorMatrixColorFilter(new float[]{1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,1.04f,0}));
                c.drawBitmap(source,null,new RectF(0,0,width,height),paint);paint.setColorFilter(null);
                if(foreground)paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                if(style==1){paint.setColor(0x504B3159);c.drawRect(0,0,width,height,paint);paint.setAlpha(255);}
            }
            if(foreground)paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            if(pale(style)) {
                // A white veil behind the greeting and the favourites keeps dark text crisp on the pale silk.
                paint.setShader(new LinearGradient(0,0,width*.75f,0,new int[]{0x8CF6F6F8,0x33F6F6F8,0x00F6F6F8},new float[]{0,.5f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,width,height,paint);
                paint.setShader(new LinearGradient(0,0,0,height,new int[]{0x00F2F2F5,0x00F2F2F5,0x59F2F2F5},new float[]{0,.55f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,width,height,paint);
            } else {
                // Silver's night picture is neutral graphite: its shading is black, not the blue of the older scenes.
                int tint=style>=SILVER?0x000000:0x080D18;
                paint.setShader(new LinearGradient(0,0,width*.88f,0,new int[]{0xA0000000|tint,0x30000000|tint,tint},new float[]{0,.5f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,width,height,paint);
                paint.setShader(new LinearGradient(0,0,0,height,new int[]{0x40000000|tint,0x08000000|tint,0x88000000|tint,(style>=SILVER?0xC8000000:0xE8000000)|tint},new float[]{0,.3f,.68f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,width,height,paint);
            }
            return result;
        }
        void update(){show(wallpaperStyle());}
        /** Fill under the picture: pale silver, graphite, or the night blue of the original scenes. */
        int base(int style){return pale(style)?0xFFE4E5E8:style>=SILVER?0xFF0B0B0D:0xFF080C14;}
        /** Shows a wallpaper without saving it (the picker previews live). Silver resolves to its day or night picture. */
        void show(final int choice) {
            final int style=variant(choice);
            if(requestedStyle==style)return;
            requestedStyle=style;
            final int width=getResources().getDisplayMetrics().widthPixels;
            final int height=getResources().getDisplayMetrics().heightPixels;
            loader.execute(()-> {
                Bitmap back,front;
                if(style>=3) {
                    // Photographic and rendered wallpapers: one full-height layer, no cut-out terrain.
                    BitmapFactory.Options full=new BitmapFactory.Options();full.inPreferredConfig=Bitmap.Config.ARGB_8888;
                    Bitmap photo=BitmapFactory.decodeResource(getResources(),PHOTOS[Math.min(PHOTOS.length-1,style-3)],full);
                    back=compose(photo,style,false,width,height);photo.recycle();
                    front=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);
                } else {
                    if(skySource==null) {
                        BitmapFactory.Options rgb=new BitmapFactory.Options();rgb.inPreferredConfig=Bitmap.Config.RGB_565;
                        skySource=BitmapFactory.decodeResource(getResources(),R.drawable.fjord_sky,rgb);
                        terrainSource=BitmapFactory.decodeResource(getResources(),R.drawable.fjord_terrain);
                    }
                    back=compose(skySource,style,false,width,height);
                    front=compose(terrainSource,style,true,width,height);
                }
                runOnUiThread(()->{
                    if(isDestroyed() || requestedStyle!=style){back.recycle();front.recycle();return;}
                    // Photos and renders have no cut-out terrain: drop its full-screen surface (about 28 MB of buffers).
                    terrain.setVisibility(style>=3?View.GONE:View.VISIBLE);
                    FrameLayout.LayoutParams track=(FrameLayout.LayoutParams)skyTrack.getLayoutParams();int depth=style>=3?getResources().getDisplayMetrics().heightPixels+px(60):px(220)+px(30);
                    if(track.height!=depth){track.height=depth;skyTrack.setLayoutParams(track);}
                    if(sceneTransition!=null)sceneTransition.cancel();
                    sky.previous=sky.scene;terrain.previous=terrain.scene;sky.scene=back;terrain.scene=front;
                    if(sky.previous!=null && motion()) {
                        sceneTransition=ValueAnimator.ofFloat(0,1);sceneTransition.setDuration(420);sceneTransition.setInterpolator(ease);
                        sceneTransition.addUpdateListener(a->{blend=(float)a.getAnimatedValue();sky.drawSurface();terrain.drawSurface();});
                        sceneTransition.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){sky.previous=null;terrain.previous=null;blend=1;sky.drawSurface();terrain.drawSurface();}});
                        sceneTransition.start();
                    } else {sky.previous=null;terrain.previous=null;blend=1;sky.drawSurface();terrain.drawSurface();}
                    syncAmbient();
                });
            });
        }
        private final class SceneLayer extends SurfaceView implements SurfaceHolder.Callback {
            final boolean foreground;
            volatile Bitmap scene,previous;
            final java.util.concurrent.atomic.AtomicBoolean drawPending=new java.util.concurrent.atomic.AtomicBoolean();
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            final RectF bounds=new RectF();
            WaterMesh water;
            int waterWidth,waterHeight;
            SceneLayer(boolean front) {
                super(MainActivity.this);foreground=front;
                if(front){setZOrderMediaOverlay(true);getHolder().setFormat(PixelFormat.TRANSLUCENT);}
                getHolder().addCallback(this);
            }
            @Override public void surfaceCreated(SurfaceHolder holder){drawSurface();}
            @Override public void surfaceChanged(SurfaceHolder holder,int format,int w,int h){drawSurface();}
            @Override public void surfaceDestroyed(SurfaceHolder holder){animate().cancel();}
            void drawSurface() {
                if(!drawPending.compareAndSet(false,true))return;
                surfaceHandler.post(()->{try{drawSurfaceNow();}finally{drawPending.set(false);}});
            }
            void drawSurfaceNow() {
                if(!getHolder().getSurface().isValid())return;
                Canvas c=null;
                try {
                    c=getHolder().lockHardwareCanvas();if(c==null)return;
                    c.drawColor(foreground && scene!=null?Color.TRANSPARENT:base(requestedStyle),PorterDuff.Mode.SRC);
                    if(!foreground)c.translate(cloudX,cloudY);
                    // Sky uses a shallow surface, but the same virtual full-scene crop as terrain.
                    bounds.set(0,0,c.getWidth(),getResources().getDisplayMetrics().heightPixels+px(60));
                    if(previous!=null && blend<1) {
                        paint.setAlpha(Math.round(255*(1-blend)));c.drawBitmap(previous,null,bounds,paint);
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));paint.setAlpha(Math.round(255*blend));
                    }
                    if(scene!=null) {
                        if(foreground && ambientRunning && previous==null && blend>=1 && requestedStyle<2) {
                            if(water==null || waterWidth!=c.getWidth() || waterHeight!=(int)bounds.height()) {
                                waterWidth=c.getWidth();waterHeight=(int)bounds.height();water=new WaterMesh(waterWidth,waterHeight,unit);
                            }
                            water.draw(c,scene,ambientSeconds,paint);
                        } else c.drawBitmap(scene,null,bounds,paint);
                    }
                    paint.setXfermode(null);paint.setAlpha(255);
                } catch(IllegalStateException|IllegalArgumentException ignored){/* Surface may disappear while the TV sleeps. */}
                finally{if(c!=null)try{getHolder().unlockCanvasAndPost(c);}catch(RuntimeException ignored){}}
            }
        }
    }
    static final String[] WALLPAPERS={"Fjord","Dusk","Graphite","Milky Way","Calm Sea","Dawn","Silk","Silver"};
    /** Silver is one choice with two pictures: SILVER by day, SILVER_NIGHT after sunset (see variant()). */
    static final int SILVER=7,SILVER_NIGHT=8;
    static final int[] PHOTOS={R.drawable.wp_milkyway,R.drawable.wp_sea,R.drawable.wp_dawn,R.drawable.wp_silk,R.drawable.wp_silver_day,R.drawable.wp_silver_night};
    static final int[] THUMBS={R.drawable.thumb_0,R.drawable.thumb_1,R.drawable.thumb_2,R.drawable.thumb_3,R.drawable.thumb_4,R.drawable.thumb_5,R.drawable.thumb_6,R.drawable.thumb_7};
    static final class App {String pkg,label,description,tag;Bitmap icon,banner;int color;}
    private int colorFor(String pkg) {
        if(pkg.contains("youtube.tv"))return 0xFFF6F5F5;
        if(pkg.contains("netflix"))return 0xFF141414;
        if(pkg.contains("soundcloud"))return 0xFFFF6900;
        if(pkg.contains("ted.android"))return 0xFFF5F5F5;
        if(pkg.contains("phlox"))return 0xFF304B6C;
        if(pkg.contains("stream.tv"))return 0xFF265EAF;
        if(pkg.contains("tvmusic"))return 0xFF26131B;
        if(pkg.contains("amazon"))return 0xFF142C3E;
        return 0xFF293341;
    }
    private String tagFor(String pkg) {
        if(pkg.contains("youtube.tv"))return "WATCH YOUR WAY";
        if(pkg.contains("netflix"))return "TIME FOR A GREAT STORY";
        if(pkg.contains("soundcloud")||pkg.contains("tvmusic"))return "THE SOUND OF YOUR EVENING";
        if(pkg.contains("ted.android"))return "IDEAS WORTH EXPLORING";
        if(pkg.contains("phlox"))return "A WINDOW TO YOUR WORLD";
        return "YOUR SPACE";
    }
    private String descriptionFor(String pkg) {
        if(pkg.contains("youtube.tv"))return "Favorite creators. New discoveries. A bigger screen.";
        if(pkg.contains("netflix"))return "Great stories. Even better evenings.";
        if(pkg.contains("soundcloud"))return "Find your sound. Set the mood.";
        if(pkg.contains("tvmusic"))return "The artists you love. The mood you are in.";
        if(pkg.contains("ted.android"))return "A new perspective can change everything.";
        if(pkg.contains("phlox"))return "The web, from the comfort of your sofa.";
        if(pkg.contains("disney"))return "Stories for every age, all in one place.";
        if(pkg.contains("spotify"))return "Your music and podcasts, on the big speakers.";
        if(pkg.contains("amazon"))return "Movies and series for your next great evening.";
        return "Your apps. Always within reach.";
    }
}

