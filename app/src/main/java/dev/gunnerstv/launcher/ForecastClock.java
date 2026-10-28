package dev.gunnerstv.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.*;

/** Configured forecast, cached locally; all refreshes run only while Home is in front. */
final class ForecastClock {
    private String endpoint(){double[] p=LauncherConfig.place(activity);return p==null?null:"https://api.open-meteo.com/v1/forecast?latitude="+p[0]+"&longitude="+p[1]
            +"&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m"
            +"&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max"
            +"&temperature_unit=celsius&wind_speed_unit=mph&timezone=auto&forecast_days=5";}
    private final Activity activity;
    private final TextView widget;
    private final SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private JSONObject data;
    private long fetched;
    private boolean active,loading,closed;
    private Dialog dialog;
    private final Runnable refresh=this::fetch;
    ForecastClock(Activity activity,TextView widget) {
        this.activity=activity; this.widget=widget; prefs=activity.getSharedPreferences("personal_weather",0);
        if(!String.valueOf(endpoint()).equals(prefs.getString("location_key","")))prefs.edit().clear().putString("location_key",String.valueOf(endpoint())).apply();
        try { data=new JSONObject(prefs.getString("forecast","")); fetched=prefs.getLong("fetched",0); } catch(JSONException ignored) { }
        widget.setOnClickListener(v -> details()); update();
    }
    void resume() { active=true; update(); fetch(); }
    void pause() { active=false; main.removeCallbacks(refresh); if(dialog!=null) dialog.dismiss(); }
    void close() { closed=true; pause(); worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
    void dismissDetails() { if(dialog!=null)dialog.dismiss(); }
    private void fetch() {
        if(!active||closed||loading||endpoint()==null) return;
        if(data!=null&&System.currentTimeMillis()-fetched<30*60_000L) {
            main.removeCallbacks(refresh); main.postDelayed(refresh,30*60_000L-(System.currentTimeMillis()-fetched)); return;
        }
        loading=true;
        worker.execute(() -> {
            JSONObject result=null;
            HttpURLConnection connection=null;
            try {
                byte[] bytes=RemoteData.get(endpoint(),256*1024,false);
                JSONObject parsed=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
                if(parsed.has("current")&&parsed.has("daily")&&parsed.getJSONObject("daily").getJSONArray("time").length()>=5) result=parsed;
            } catch(Exception ignored) { }
            finally { if(connection!=null)connection.disconnect(); }
            JSONObject received=result;
            main.post(() -> {
                if(closed) return; loading=false;
                if(received!=null) {data=received;fetched=System.currentTimeMillis();prefs.edit().putString("forecast",data.toString()).putLong("fetched",fetched).apply();}
                update(); main.removeCallbacks(refresh); if(active)main.postDelayed(refresh,received!=null?30*60_000L:5*60_000L);
            });
        });
    }
    private void update() {
        if(data==null) {widget.setText("Weather\n"+LauncherConfig.city(activity)+"\n— °C");widget.setContentDescription("configured weather, forecast loading. Press OK for forecast.");return;}
        JSONObject current=data.optJSONObject("current"); if(current==null)return;
        int code=current.optInt("weather_code",0);
        widget.setText(currentSymbol(current)+"\n"+degree(current.optDouble("temperature_2m",Double.NaN))+"\n"+LauncherConfig.city(activity)+"");
        widget.setTextSize(11);
        widget.setContentDescription(""+LauncherConfig.city(activity)+", "+degree(current.optDouble("temperature_2m",Double.NaN))+" Celsius, "+condition(code)+". Press OK for five-day forecast.");
        if(dialog!=null&&dialog.isShowing()) renderDialog();
    }
    void details() {
        if(dialog!=null&&dialog.isShowing())return;
        dialog=new Dialog(activity,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setOnDismissListener(d -> dialog=null);
        renderDialog(); dialog.show();
        if(dialog.getWindow()!=null) dialog.getWindow().setLayout(-1,-1);
        fetch();
    }
    private void renderDialog() {
        if(dialog==null)return;
        FrameLayout root=new FrameLayout(activity);root.addView(new ForecastBackdrop(activity),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout center=new LinearLayout(activity);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(dp(40),dp(38),dp(40),dp(14));root.addView(center,new FrameLayout.LayoutParams(-1,-1));
        center.addView(new View(activity),new LinearLayout.LayoutParams(1,0,1));
        TextClock clock=new TextClock(activity);clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");clock.setTextColor(0xfff3f2ed);clock.setTextSize(100);clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL));clock.setIncludeFontPadding(false);clock.setGravity(Gravity.CENTER);center.addView(clock);
        TextClock date=new TextClock(activity);date.setFormat12Hour("EEEE, MMMM d");date.setFormat24Hour("EEEE, MMMM d");date.setTextColor(0xffd0cbd8);date.setTextSize(21);date.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(-1,dp(35));dateParams.topMargin=dp(2);center.addView(date,dateParams);
        TextView city=label(""+LauncherConfig.city(activity)+" · Forecast loading…",15,0xffd2ccd8);city.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cityParams=new LinearLayout.LayoutParams(-1,dp(40));cityParams.topMargin=dp(12);center.addView(city,cityParams);
        LinearLayout forecast=new LinearLayout(activity);forecast.setGravity(Gravity.CENTER);forecast.setPadding(0,dp(8),0,dp(8));
        LinearLayout.LayoutParams forecastParams=new LinearLayout.LayoutParams(dp(740),dp(124));forecastParams.topMargin=dp(3);center.addView(forecast,forecastParams);
        String status="Weather by Open-Meteo · Celsius · Back returns Home";
        if(data!=null) {
            JSONObject current=data.optJSONObject("current"),daily=data.optJSONObject("daily");
            if(current!=null&&daily!=null) {
                int code=current.optInt("weather_code",0);
                city.setText(currentSymbol(current)+"  "+condition(code)+"   H "+degree(at(daily,"temperature_2m_max",0))+"  L "+degree(at(daily,"temperature_2m_min",0))+"   ·   "+LauncherConfig.city(activity)+"");
                TextView temperature=label(currentSymbol(current)+"  "+degree(current.optDouble("temperature_2m",Double.NaN))+"C",17,0xffe3dee9);
                FrameLayout.LayoutParams tempParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);tempParams.setMargins(0,dp(23),dp(30),0);root.addView(temperature,tempParams);
                JSONArray dates=daily.optJSONArray("time");
                for(int i=0;dates!=null&&i<Math.min(5,dates.length());i++) {
                    LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);
                    GradientDrawable bg=new GradientDrawable();bg.setColor(0x861a192c);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),0x304d526d);card.setBackground(bg);
                    String day=i==0?"Today":LocalDate.parse(dates.optString(i)).format(DateTimeFormatter.ofPattern("EEE",Locale.UK));
                    card.addView(label(day,12,0xffdce0ed));int weatherCode=(int)at(daily,"weather_code",i);
                    card.addView(new WeatherAnimation(activity,weatherCode),new LinearLayout.LayoutParams(dp(65),dp(41)));
                    card.addView(label(degree(at(daily,"temperature_2m_max",i))+" / "+degree(at(daily,"temperature_2m_min",i)),14,Color.WHITE));
                    card.addView(label("Rain "+Math.round(at(daily,"precipitation_probability_max",i))+"%",10,0xffafb7cb));
                    LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(0,-1,1);cardParams.setMargins(dp(5),0,dp(5),0);forecast.addView(card,cardParams);
                }
                String timestamp=android.text.format.DateFormat.format("HH:mm",fetched).toString();
                status="Weather by Open-Meteo · Updated "+timestamp+(System.currentTimeMillis()-fetched>2*60*60_000L?" · Saved forecast (offline)":"")+" · Back returns Home";
            }
        } else {
            forecast.addView(label("Forecast unavailable. It will retry when the network returns.",14,0xffc0b9cc));
        }
        TextView footer=label(status,9,0xff87879f);footer.setGravity(Gravity.CENTER);center.addView(footer,new LinearLayout.LayoutParams(-1,dp(24)));
        center.addView(new View(activity),new LinearLayout.LayoutParams(1,0,1));
        TextView back=label("‹ Back",12,Color.WHITE);back.setPadding(dp(12),dp(8),dp(12),dp(8));back.setFocusable(true);back.setClickable(true);
        GradientDrawable backBg=new GradientDrawable();backBg.setCornerRadius(dp(14));backBg.setColor(0x701e2438);back.setBackground(backBg);
        back.setOnFocusChangeListener((v,focused) -> {GradientDrawable b=new GradientDrawable();b.setCornerRadius(dp(14));b.setColor(0x701e2438);if(focused)b.setStroke(dp(1),Color.WHITE);v.setBackground(b);});
        back.setOnClickListener(v -> {if(dialog!=null)dialog.dismiss();});
        FrameLayout.LayoutParams backParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);backParams.setMargins(dp(25),dp(20),0,0);root.addView(back,backParams);
        dialog.setContentView(root);back.requestFocus();
    }
    private static double at(JSONObject object,String name,int index) {JSONArray a=object.optJSONArray(name);return a==null?Double.NaN:a.optDouble(index,Double.NaN);}
    private static String degree(double value) {return Double.isNaN(value)?"—°":Math.round(value)+"°";}
    static String condition(int code) {
        if(code==0)return "Clear";if(code<=2)return "Partly cloudy";if(code==3)return "Cloudy";if(code<=48)return "Foggy";if(code<=67)return "Rain";if(code<=77)return "Snow";if(code<=82)return "Showers";if(code<=86)return "Snow showers";return "Thunderstorms";
    }
    private static String currentSymbol(JSONObject current) {return current.optInt("weather_code",0)==0&&current.optInt("is_day",1)==0?"☾":symbol(current.optInt("weather_code",0));}
    private static String symbol(int code) {if(code==0)return "☀";if(code<=3)return "☁";if(code<=48)return "≋";if(code<=67||code>=80&&code<=82)return "☂";if(code<=86)return "❄";return "ϟ";}
    private TextView label(String value,float size,int color) {TextView t=new TextView(activity);t.setText(value);t.setTextColor(color);t.setTextSize(size);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER);return t;}
    private int dp(float value) {return Math.round(value*activity.getResources().getDisplayMetrics().density);}

    /** Small vector weather scenes; redraws are limited to 15 fps and stop on detach. */
    private static final class WeatherAnimation extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int code;
        private boolean moving;
        WeatherAnimation(android.content.Context context,int code) {super(context);this.code=code;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onAttachedToWindow() {super.onAttachedToWindow();moving=Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)>0;}
        @Override protected void onDetachedFromWindow() {moving=false;super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas canvas) {
            float w=getWidth(),h=getHeight(),phase=moving?(SystemClock.uptimeMillis()%6000)/6000f:0;
            canvas.save();canvas.scale(w/100f,h/64f);
            if(code<=2) {
                canvas.save();canvas.translate(43,27);canvas.rotate(phase*360);paint.setColor(0xffffd17d);paint.setStrokeWidth(2);
                for(int i=0;i<8;i++){canvas.drawLine(0,-17,0,-21,paint);canvas.rotate(45);}canvas.drawCircle(0,0,11,paint);canvas.restore();
            }
            if(code>0) {
                float drift=(float)Math.sin(phase*Math.PI*2)*3;canvas.save();canvas.translate(drift,0);paint.setColor(0xffd9e5ee);
                canvas.drawRoundRect(23,26,79,43,9,9,paint);canvas.drawCircle(41,27,12,paint);canvas.drawCircle(57,26,15,paint);canvas.drawCircle(71,32,10,paint);canvas.restore();
            }
            if(code>=51) {
                paint.setColor(code>=71&&code<=77||code>=85&&code<=86?0xffecf6ff:0xff81caee);paint.setStrokeWidth(2);
                for(int i=0;i<4;i++) {float y=44+((phase*34+i*9)%17);float x=32+i*11;canvas.drawLine(x,y,x-2,y+4,paint);}
            }
            if(code>=95){paint.setColor(0xffffdb7e);Path bolt=new Path();bolt.moveTo(54,38);bolt.lineTo(46,50);bolt.lineTo(53,49);bolt.lineTo(48,60);bolt.lineTo(63,44);bolt.lineTo(55,45);bolt.close();canvas.drawPath(bolt,paint);}
            canvas.restore();if(moving&&getContext().getSharedPreferences("gunners",0).getBoolean("motion",true)&&isShown())postInvalidateDelayed(67);
        }
    }
    private static final class ForecastBackdrop extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ForecastBackdrop(android.content.Context context){super(context);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();c.drawColor(0xff101114);paint.setShader(new RadialGradient(w*.8f,h*.15f,w,new int[]{0xff30343c,0xff14161b,0xff0b0c0e},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);}
    }

}
