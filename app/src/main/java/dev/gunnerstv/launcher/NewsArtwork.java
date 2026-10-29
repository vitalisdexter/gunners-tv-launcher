package dev.gunnerstv.launcher;

import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.lang.ref.WeakReference;

final class NewsArtwork {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(4_000_000){@Override protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    static void load(String id,ImageView target){if(!YouTubeFeeds.video(id))return;Bitmap cached=CACHE.get(id);if(cached!=null){target.setImageBitmap(cached);return;}WeakReference<ImageView> weak=new WeakReference<>(target);
        WidgetBoard.ART.execute(()->{Bitmap result=null;try{byte[] bytes=RemoteData.get("https://i.ytimg.com/vi/"+id+"/hqdefault.jpg",2_000_000,false);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);o.inSampleSize=RemoteData.imageSample(o.outWidth,o.outHeight,480);o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;result=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}catch(Exception|OutOfMemoryError ignored){}Bitmap image=result;if(image!=null)MAIN.post(()->{CACHE.put(id,image);ImageView view=weak.get();if(view!=null&&view.isAttachedToWindow())view.setImageBitmap(image);});});
    }
}
