package dev.gunnerstv.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/** Provider club crests. Invalid or unavailable artwork keeps a readable club monogram. */
final class ArsenalBadge extends FrameLayout {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap image){return image.getByteCount();}
    };
    private final ImageView image;
    private final TextView fallback;
    private String logo="";
    private boolean loading;

    ArsenalBadge(Context context,float unit,float size){
        super(context);setFocusable(false);
        GradientDrawable plate=new GradientDrawable();plate.setColor(0xFFF1F2F0);plate.setCornerRadius(size*unit*.25f);plate.setStroke(Math.max(1,Math.round(unit*.6f)),0x22000000);setBackground(plate);
        fallback=new TextView(context);fallback.setTextColor(0xFF26343D);fallback.setGravity(Gravity.CENTER);fallback.setIncludeFontPadding(false);
        fallback.setTextSize(TypedValue.COMPLEX_UNIT_PX,size*unit*.28f);fallback.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));addView(fallback,new LayoutParams(-1,-1));
        image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);int padding=Math.round(size*unit*.13f);image.setPadding(padding,padding,padding,padding);addView(image,new LayoutParams(-1,-1));
    }
    void bind(String name,String abbreviation,String url){
        setContentDescription(name);String letters=abbreviation==null?"":abbreviation.trim();
        if(letters.isEmpty()){StringBuilder initials=new StringBuilder();for(String part:name.split("\\s+"))if(!part.isEmpty()&&initials.length()<3)initials.append(part.charAt(0));letters=initials.toString();}
        fallback.setText(letters.substring(0,Math.min(3,letters.length())).toUpperCase(Locale.UK));
        String valid=safeUrl(url);if(!valid.equals(logo)){logo=valid;image.setImageDrawable(null);fallback.setVisibility(VISIBLE);loading=false;}
        load();
    }
    private static String safeUrl(String value){
        if(value==null||value.length()>1024)return "";
        try{URI u=new URI(value);return "https".equalsIgnoreCase(u.getScheme())&&"a.espncdn.com".equalsIgnoreCase(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1?value:"";}catch(Exception ignored){return "";}
    }
    private void show(Bitmap bitmap){image.setImageBitmap(bitmap);fallback.setVisibility(INVISIBLE);}
    private void load(){
        if(logo.isEmpty())return;Bitmap hit=CACHE.get(logo);if(hit!=null){show(hit);return;}
        if(loading||!isAttachedToWindow())return;loading=true;String requested=logo;
        WidgetBoard.ART.execute(()->{
            Bitmap bitmap=CACHE.get(requested);HttpURLConnection connection=null;
            try{
                if(bitmap!=null){final Bitmap queuedHit=bitmap;MAIN.post(()->{if(requested.equals(logo)){loading=false;if(isAttachedToWindow())show(queuedHit);}});return;}
                connection=(HttpURLConnection)new URL(requested).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(3500);connection.setReadTimeout(4000);
                if(connection.getResponseCode()!=200)throw new java.io.IOException("Crest unavailable");
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int count,total=0;while((count=in.read(buffer))!=-1){total+=count;if(total>512000)throw new java.io.IOException("Crest too large");out.write(buffer,0,count);}
                    byte[] bytes=out.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                    if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>4096||bounds.outHeight>4096)throw new java.io.IOException("Invalid crest dimensions");
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>192)options.inSampleSize*=2;
                    bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap!=null)CACHE.put(requested,bitmap);
                }
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            Bitmap result=bitmap;MAIN.post(()->{if(!requested.equals(logo))return;loading=false;if(result!=null&&isAttachedToWindow())show(result);});
        });
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();load();}
}
