package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Classic analogue dial, readable digital time and cached configured location conditions. No extra network requests. */
final class ClockPanel extends FrameLayout {
    private final float unit;
    private final SharedPreferences prefs;
    private final TextView greeting,time,temperature,detail;
    private final Glyph weather;
    private final Dial dial;
    private String weatherRaw="",condition="Weather",next="";
    ClockPanel(Context c,float unit,Runnable open){
        super(c);this.unit=unit;prefs=c.getSharedPreferences("gunners",0);
        setBackground(shape(0xDE101114,19));setClipChildren(false);
        View glass=new View(c);glass.setBackground(shape(0xB0202226,14));addView(glass,at(141,45,52,5));
        View weatherGlass=new View(c);weatherGlass.setBackground(shape(0xC05A6875,14));addView(weatherGlass,at(49,45,198,5));
        dial=new Dial(c);addView(dial,at(45,45,5,5));
        greeting=label(10.5f,0xBBFFFFFF,"sans-serif-medium");addView(greeting,at(130,14,59,7));
        time=label(23,Color.WHITE,"sans-serif-medium");addView(time,at(135,28,58,20));
        weather=new Glyph(c).kind("partly").color(0xF2FFFFFF);addView(weather,at(15,15,214,8));
        temperature=label(15.5f,Color.WHITE,"sans-serif-medium");temperature.setText("—°");temperature.setGravity(Gravity.CENTER);addView(temperature,at(47,23,199,25));
        detail=label(10.5f,0xDDFFFFFF,"sans-serif-medium");addView(detail,at(230,17,12,54));
        setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Clock and configured weather. OK opens the forecast.");
        setOnFocusChangeListener((v,f)->{GradientDrawable bg=shape(0xDE101114,19);if(f)bg.setStroke(px(1),Color.WHITE);setBackground(bg);});
        setOnClickListener(v->open.run());refresh();
    }
    private int px(float n){return Math.round(n*unit);}
    private LayoutParams at(float w,float h,float x,float y){LayoutParams l=new LayoutParams(px(w),px(h));l.leftMargin=px(x);l.topMargin=px(y);return l;}
    private GradientDrawable shape(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(px(radius));return d;}
    private TextView label(float size,int color,String face){TextView t=new TextView(getContext());t.setTextSize(TypedValue.COMPLEX_UNIT_PX,px(size));t.setTextColor(color);t.setTypeface(Typeface.create(face,Typeface.NORMAL));t.setIncludeFontPadding(false);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;}
    static String greeting(int hour){return hour<5||hour>=22?"Good Night":hour<12?"Good Morning":hour<18?"Good Afternoon":"Good Evening";}
    void refresh(){
        Date now=new Date();int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello=greeting(hour);if(!hello.contentEquals(greeting.getText()))greeting.setText(hello);
        boolean twentyFour=android.text.format.DateFormat.is24HourFormat(getContext());
        String value=new SimpleDateFormat(twentyFour?"HH:mm":"h:mm",Locale.UK).format(now);
        if(!twentyFour)value+=" "+new SimpleDateFormat("a",Locale.UK).format(now).toLowerCase(Locale.UK);
        if(!value.contentEquals(time.getText())){SpannableString s=new SpannableString(value);if(!twentyFour)s.setSpan(new RelativeSizeSpan(.47f),value.length()-2,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);time.setText(s);}
        String raw=prefs.getString("weather_json","");
        if(!raw.equals(weatherRaw)){
            weatherRaw=raw;
            try{
                JSONObject data=new JSONObject(raw),current=data.getJSONObject("current");
                int code=current.optInt("weather_code");weather.kind(Glyph.weather(code,current.optInt("is_day",1)==1));
                temperature.setText(Math.round(current.getDouble("temperature_2m"))+"°");condition=Glyph.weatherName(code);next="";
                JSONObject hourly=data.optJSONObject("hourly");if(hourly!=null&&code<51){JSONArray codes=hourly.optJSONArray("weather_code"),times=hourly.optJSONArray("time");
                    for(int i=1;codes!=null&&times!=null&&i<Math.min(codes.length(),times.length());i++)if(codes.optInt(i)>=51){String stamp=times.optString(i);if(stamp.length()>=16)next=Glyph.weatherName(codes.optInt(i))+" from "+stamp.substring(11,16);break;}}
            }catch(JSONException ignored){temperature.setText("—°");condition="Weather";next="";}
        }
        String date=new SimpleDateFormat("EEE d",Locale.UK).format(now).toUpperCase(Locale.UK);
        boolean stale=System.currentTimeMillis()-prefs.getLong("weather_at",0)>2*3600000L;
        String line=date+"  ·  "+(stale?"Saved weather":next.isEmpty()?condition+" · configured location":next);
        if(!line.contentEquals(detail.getText())){SpannableString s=new SpannableString(line);s.setSpan(new ForegroundColorSpan(0xFFEF766D),0,date.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);detail.setText(s);}
        dial.invalidate();
    }
    private static final class Dial extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Dial(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            c.save();c.scale(getWidth()/100f,getHeight()/100f);c.translate(50,50);
            p.setStyle(Paint.Style.FILL);p.setColor(0xFF323A3D);c.drawCircle(0,0,49,p);p.setColor(0xFFF4EEDB);c.drawCircle(0,0,46,p);
            p.setColor(0xFF4B514F);p.setStrokeCap(Paint.Cap.ROUND);
            for(int n=0;n<60;n++){c.save();c.rotate(n*6);p.setStrokeWidth(n%5==0?2.5f:1);c.drawLine(0,-41,0,n%5==0?-36:-39,p);c.restore();}
            p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(9.5f);p.setTextAlign(Paint.Align.CENTER);
            for(int n=1;n<=12;n++){double angle=Math.toRadians(n*30-90);float x=(float)Math.cos(angle)*30,y=(float)Math.sin(angle)*30; c.drawText(String.valueOf(n),x,y-(p.ascent()+p.descent())/2,p);}
            Calendar now=Calendar.getInstance();float sec=now.get(Calendar.SECOND),min=now.get(Calendar.MINUTE)+sec/60f,hour=now.get(Calendar.HOUR)+min/60f;
            hand(c,hour*30,23,4.5f,0xFF222A2C);hand(c,min*6,33,3.1f,0xFF222A2C);hand(c,sec*6,35,1.2f,0xFFD17A43);
            p.setColor(0xFF222A2C);c.drawCircle(0,0,3,p);c.restore();
        }
        private void hand(Canvas c,float angle,float length,float width,int color){c.save();c.rotate(angle);p.setColor(color);p.setStrokeWidth(width);c.drawLine(0,5,0,-length,p);c.restore();}
    }
}
