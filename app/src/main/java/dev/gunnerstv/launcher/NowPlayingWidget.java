package dev.gunnerstv.launcher;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.*;

/** Now Playing: the media session of the app playing on this TV (Spotify, YouTube Music, SoundCloud…) with its
 *  artwork, progress and previous / play-pause / next. Android hands media sessions only to a notification
 *  listener, so Gunner's declares one (MediaListener) that reads no notifications; the user allows it once. */
final class NowPlayingWidget extends Widget {
    static final String[] MUSIC={"com.spotify.tv.android","com.google.android.youtube.tvmusic","com.soundcloud.android","com.apple.android.music","deezer.android.tv","com.amazon.music.tv","com.aspiro.tidal","ru.yandex.music","com.google.android.youtube.tv"};
    static final float PAD=18,ART=104,TX=136,CTRL_Y=130;
    final Handler handler=new Handler(Looper.getMainLooper());
    final MediaSessionManager sessions;
    final ComponentName listener;
    final FrameLayout art;
    final ImageView artImage;
    final Glyph artGlyph,playGlyph;
    final TextView source,title,artist,elapsed,remaining;
    final View track,fill;
    final FrameLayout prev,play,next;
    final TextView openPill,helpPill;
    final LinearLayout shortcuts;
    MediaController controller;
    boolean access=true,listening;
    String artKey="";
    final MediaSessionManager.OnActiveSessionsChangedListener changed=list->pick(list);
    final MediaController.Callback callback=new MediaController.Callback(){
        @Override public void onPlaybackStateChanged(PlaybackState state){render();}
        @Override public void onMetadataChanged(MediaMetadata metadata){render();}
        @Override public void onSessionDestroyed(){if(controller!=null)controller.unregisterCallback(this);controller=null;repick();}
    };
    final Runnable tick=new Runnable(){public void run(){progress();if(active&&playing())handler.postDelayed(this,1000);}};
    final Runnable connectionChanged=()->handler.post(()->{if(active)repick();});
    final Runnable poll=new Runnable(){public void run(){if(active){repick();handler.postDelayed(this,10000);}}};

    NowPlayingWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("music"));
        sessions=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        listener=new ComponentName(context,MediaListener.class);
        art=new FrameLayout(context);art.setClipToOutline(true);art.setBackground(round(light()?0x14000000:0x1AFFFFFF,14));art.setElevation(px(3));addView(art,pos(ART,ART,PAD,PAD));
        artGlyph=new Glyph(context).kind("note").color(ink(0x80));art.addView(artGlyph,pos(40,40,(ART-40)/2,(ART-40)/2));
        artImage=new ImageView(context);artImage.setScaleType(ImageView.ScaleType.CENTER_CROP);art.addView(artImage,new LayoutParams(-1,-1));
        float tw=w-TX-PAD;
        source=text("",10.5f,ink(0x8C),"sans-serif-medium");source.setLetterSpacing(.08f);addView(source,pos(tw,14,TX,22));
        title=text("",17,ink(0xFF),"sans-serif-medium");addView(title,pos(tw,22,TX,40));
        artist=text("",13,ink(0xB3),"sans-serif");addView(artist,pos(tw,17,TX,66));
        track=new View(context);track.setBackground(round(ink(0x26),1.5f));addView(track,pos(tw,3,TX,96));
        fill=new View(context);fill.setBackground(round(ink(0xD9),1.5f));fill.setPivotX(0);addView(fill,pos(tw,3,TX,96));
        elapsed=text("",10.5f,ink(0x80),"sans-serif");addView(elapsed,pos(60,13,TX,104));
        remaining=text("",10.5f,ink(0x80),"sans-serif");remaining.setGravity(Gravity.RIGHT);addView(remaining,pos(60,13,w-PAD-60,104));
        prev=button("previous",34,TX-4,CTRL_Y+4,0,()->skip(false));
        play=button("play",42,TX+38,CTRL_Y,1,this::togglePlayback);
        next=button("next",34,TX+88,CTRL_Y+4,2,()->skip(true));
        playGlyph=(Glyph)play.getChildAt(1);
        openPill=pill("Open",3,()->{if(controller!=null)host.launch(controller.getPackageName());});
        // Right-aligned by layout, not by translation: focus search goes by layout position.
        LayoutParams op=new LayoutParams(-2,px(32));op.gravity=Gravity.TOP|Gravity.RIGHT;op.rightMargin=px(PAD);op.topMargin=px(CTRL_Y+5);addView(openPill,op);
        helpPill=pill("Music help",4,()->new android.app.AlertDialog.Builder(host.activity()).setTitle("Now Playing and YouTube Music")
            .setMessage("Allow Gunner's notification access, start a song in the music app, then return Home. On TVs, YouTube Music may play through the YouTube app.\n\nThe provider must keep playback or a controllable media session active on Home. If it stops playback or removes its session, Gunner's cannot supply background play or invent the track. Use Open to return to that app. Controls are enabled only when the session advertises them.\n\nNo notifications or account history are read.")
            .setPositiveButton("Done",null).show());addView(helpPill,pos(110,28,TX,92));
        shortcuts=new LinearLayout(context);shortcuts.setClipChildren(false);shortcuts.setClipToPadding(false);addView(shortcuts,pos(tw+10,34,TX-4,CTRL_Y+4));
        render();
    }
    private FrameLayout button(String kind,float size,float x,float y,int index,Runnable action){
        FrameLayout b=new FrameLayout(getContext());View disc=new View(getContext());b.addView(disc,new LayoutParams(-1,-1));
        Glyph g=new Glyph(getContext()).kind(kind);float gs=size*.46f;b.addView(g,pos(gs,gs,(size-gs)/2,(size-gs)/2));
        b.setPivotX(px(size/2));b.setPivotY(px(size/2));
        control(b,index,1.14f,f->{disc.setBackground(oval(f?focusFill():restFill()));g.color(f?focusInk():ink(0xF2));},()->hero(),action);
        addView(b,pos(size,size,x,y));return b;
    }
    private TextView pill(String label,int index,Runnable action){return pill(label,index,null,action);}
    /** A rounded text button; hero overrides the Home title while it is focused (app shortcuts). */
    private TextView pill(String label,int index,String[] hero,Runnable action){
        TextView p=text(label,12.5f,ink(0xF2),"sans-serif-medium");p.setGravity(Gravity.CENTER);p.setPadding(px(16),0,px(16),0);
        control(p,index,1.08f,f->{p.setBackground(round(f?focusFill():restFill(),16));p.setTextColor(f?focusInk():ink(0xF2));},()->hero!=null?hero:hero(),action);
        return p;
    }
    @Override void setActive(boolean value){
        if(value==active)return;super.setActive(value);
        if(value){
            MediaListener.observe(connectionChanged);repick();handler.postDelayed(poll,10000);
        } else {
            try{sessions.removeOnActiveSessionsChangedListener(changed);}catch(RuntimeException ignored){}
            listening=false;MediaListener.remove(connectionChanged);use(null);handler.removeCallbacks(tick);handler.removeCallbacks(poll);
        }
    }
    @Override void refresh(){if(active)repick();}
    private void repick(){try{if(!listening){sessions.addOnActiveSessionsChangedListener(changed,listener,handler);listening=true;}access=true;pick(sessions.getActiveSessions(listener));}catch(SecurityException e){access=false;try{sessions.removeOnActiveSessionsChangedListener(changed);}catch(RuntimeException ignored){}listening=false;use(null);}}
    /** The session to show: one that is playing, else the most recent one with a title. */
    private void pick(List<MediaController> list){
        MediaController best=null;int highest=-1;
        if(list!=null)for(MediaController c:list){if(c.getPackageName().equals(getContext().getPackageName()))continue;
            PlaybackState s=c.getPlaybackState();int state=s==null?PlaybackState.STATE_NONE:s.getState();
            int rank=MediaSessionPolicy.rank(state==PlaybackState.STATE_PLAYING,state==PlaybackState.STATE_BUFFERING,state==PlaybackState.STATE_PAUSED,!titleOf(c).isEmpty(),Arrays.asList(MUSIC).contains(c.getPackageName()));
            if(rank>highest){highest=rank;best=c;}}
        use(best);
    }
    private void use(MediaController c){
        if(controller!=null&&c!=null&&controller.getSessionToken().equals(c.getSessionToken())){render();return;}
        if(controller!=null)controller.unregisterCallback(callback);
        controller=c;if(c!=null)c.registerCallback(callback,handler);
        render();
    }
    private static String titleOf(MediaController c){MediaMetadata m=c.getMetadata();return m==null?"":MediaSessionPolicy.first(m.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),m.getText(MediaMetadata.METADATA_KEY_TITLE),m.getDescription().getTitle());}
    private boolean playing(){if(Promo.on)return Promo.playing;PlaybackState s=controller==null?null:controller.getPlaybackState();return s!=null&&(s.getState()==PlaybackState.STATE_PLAYING||s.getState()==PlaybackState.STATE_BUFFERING);}
    private String appName(String pkg){MainActivity.App a=host.app(pkg);if(a!=null)return a.label;try{return getContext().getPackageManager().getApplicationLabel(getContext().getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){return pkg.contains("youtube")?"YouTube":pkg;}}
    private long actions(){PlaybackState state=controller==null?null:controller.getPlaybackState();return state==null?0:state.getActions();}
    private MediaSessionPolicy.Command command(){long a=actions();return MediaSessionPolicy.command(playing(),(a&PlaybackState.ACTION_PLAY)!=0,(a&PlaybackState.ACTION_PAUSE)!=0,(a&PlaybackState.ACTION_PLAY_PAUSE)!=0);}
    private void togglePlayback(){
        if(Promo.on){Promo.toggle();render();return;}if(controller==null)return;
        try{switch(command()){
            case PLAY:controller.getTransportControls().play();break;
            case PAUSE:controller.getTransportControls().pause();break;
            case TOGGLE:
                long now=SystemClock.uptimeMillis();boolean sent=controller.dispatchMediaButtonEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));
                controller.dispatchMediaButtonEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));
                if(!sent){if(playing()&&(actions()&PlaybackState.ACTION_PAUSE)!=0)controller.getTransportControls().pause();else if(!playing()&&(actions()&PlaybackState.ACTION_PLAY)!=0)controller.getTransportControls().play();}
                break;
            default:break;
        }}catch(RuntimeException ignored){repick();}
    }
    private void skip(boolean forward){if(controller==null)return;long flag=forward?PlaybackState.ACTION_SKIP_TO_NEXT:PlaybackState.ACTION_SKIP_TO_PREVIOUS;if((actions()&flag)==0)return;try{if(forward)controller.getTransportControls().skipToNext();else controller.getTransportControls().skipToPrevious();}catch(RuntimeException ignored){repick();}}

    /** Updates the widget in place; focus moves to a visible control if its control disappears. */
    private void render(){
        if(Promo.on){promo();return;}
        View focused=findFocus();
        boolean has=controller!=null;
        int media=has?VISIBLE:GONE;
        for(View v:new View[]{prev,play,next,track,fill,elapsed,remaining,openPill})v.setVisibility(media);
        shortcuts.setVisibility(has?GONE:VISIBLE);
        helpPill.setVisibility(has?GONE:VISIBLE);
        if(has){
            MediaMetadata m=controller.getMetadata();String pkg=controller.getPackageName();
            source.setText(appName(pkg).toUpperCase(Locale.ENGLISH));title.setText(MediaSessionPolicy.first(titleOf(controller),getContext().getString(R.string.media_in_app,appName(pkg))));
            artist.setText(m==null?"Waiting for the app's track information":MediaSessionPolicy.first(m.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),m.getText(MediaMetadata.METADATA_KEY_ARTIST),m.getText(MediaMetadata.METADATA_KEY_ALBUM)));
            long actions=actions();
            capability(prev,(actions&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0);capability(next,(actions&PlaybackState.ACTION_SKIP_TO_NEXT)!=0);capability(play,command()!=MediaSessionPolicy.Command.NONE);
            prev.setAlpha((actions&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0?1:.4f);next.setAlpha((actions&PlaybackState.ACTION_SKIP_TO_NEXT)!=0?1:.4f);
            playGlyph.kind(playing()?"pause":"play");
            openPill.setText("Open "+appName(pkg));
            if(m!=null)artwork(m);else{artKey="";artImage.setImageDrawable(null);}progress();
            handler.removeCallbacks(tick);if(active&&playing())handler.postDelayed(tick,1000);
        } else {
            artKey="";artImage.setImageDrawable(null);source.setText("NOW PLAYING");
            title.setText(access?"Nothing playing":"See what's playing");
            artist.setText(access?"Apps must keep playing and share a media session on Home.":"Allow notification access, or wait for it to connect.");
            fillShortcuts();
        }
        if(focused!=null&&(focused.getVisibility()!=VISIBLE||focused.getParent()==null||!focused.isShown()||!focused.isEnabled())){View first=firstControl();if(first!=null)first.requestFocus();}
    }
    /** The promo demo's song (Promo): the same layout as a real session, on a made-up clock. */
    private void promo(){
        for(View v:new View[]{prev,play,next,track,fill,elapsed,remaining,openPill})v.setVisibility(VISIBLE);
        shortcuts.setVisibility(GONE);helpPill.setVisibility(GONE);capability(prev,true);capability(play,true);capability(next,true);
        source.setText(Promo.SOURCE.toUpperCase(Locale.ENGLISH));title.setText(Promo.SONG);artist.setText(Promo.ARTIST);
        playGlyph.kind(Promo.playing?"pause":"play");
        openPill.setText("Open "+Promo.SOURCE);
        if(!artKey.equals("promo")){artKey="promo";fade(Promo.artwork(px(ART)*2));}
        progress();
        handler.removeCallbacks(tick);if(active&&Promo.playing)handler.postDelayed(tick,1000);
    }
    private void capability(View v,boolean enabled){v.setEnabled(enabled);v.setFocusable(enabled);v.setAlpha(enabled?1:.4f);}
    private View firstControl(){for(View v:new View[]{play,prev,next,openPill})if(v.isShown()&&v.isEnabled())return v;return shortcuts.getChildCount()>0?shortcuts.getChildAt(0):helpPill;}
    private String shortcutKey="";
    private void fillShortcuts(){
        ArrayList<String> apps=new ArrayList<>();
        if(!access)apps.add("@allow");
        else for(String pkg:MUSIC)if(host.app(pkg)!=null&&apps.size()<2)apps.add(pkg);
        String key=String.join(",",apps);if(key.equals(shortcutKey))return;shortcutKey=key;
        boolean hadFocus=shortcuts.hasFocus();shortcuts.removeAllViews();
        for(int i=0;i<apps.size();i++){final String pkg=apps.get(i);
            String[] hero=pkg.equals("@allow")?null:new String[]{"music:open:"+pkg,appName(pkg),"Open "+appName(pkg)+" and start something. It will show up here, with play, pause and skip."};
            TextView p=pill(pkg.equals("@allow")?"Allow access":appName(pkg),i,hero,()->{
                if(pkg.equals("@allow"))host.start(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));else host.launch(pkg);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,px(32));lp.rightMargin=px(8);lp.topMargin=px(1);shortcuts.addView(p,lp);}
        if(hadFocus&&shortcuts.getChildCount()>0)shortcuts.getChildAt(0).requestFocus();
    }
    private void progress(){
        if(Promo.on){long d=Promo.SONG_LENGTH,p=Promo.position();track.setVisibility(VISIBLE);fill.setVisibility(VISIBLE);fill.setScaleX(p/(float)d);elapsed.setText(time(p));remaining.setText("−"+time(d-p));return;}
        if(controller==null)return;PlaybackState s=controller.getPlaybackState();MediaMetadata m=controller.getMetadata();
        long duration=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if(s==null||duration<=0){fill.setScaleX(0);track.setVisibility(INVISIBLE);fill.setVisibility(INVISIBLE);elapsed.setText("");remaining.setText("");return;}
        long position=s.getPosition();if(s.getState()==PlaybackState.STATE_PLAYING&&s.getLastPositionUpdateTime()>0)position+=(long)((SystemClock.elapsedRealtime()-s.getLastPositionUpdateTime())*s.getPlaybackSpeed());
        position=Math.max(0,Math.min(duration,position));
        track.setVisibility(VISIBLE);fill.setVisibility(VISIBLE);fill.setScaleX(position/(float)duration);
        elapsed.setText(time(position));remaining.setText("−"+time(duration-position));
    }
    private static String time(long ms){long s=ms/1000;return s>=3600?String.format(Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(Locale.US,"%d:%02d",s/60,s%60);}
    private void artwork(MediaMetadata m){
        Bitmap b=m.getBitmap(MediaMetadata.METADATA_KEY_ART);if(b==null)b=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(b==null)b=m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        String link=null;for(String k:new String[]{MediaMetadata.METADATA_KEY_ART_URI,MediaMetadata.METADATA_KEY_ALBUM_ART_URI,MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI})if(link==null)link=m.getString(k);
        String key=b!=null?"bitmap:"+b.getGenerationId()+":"+b.getWidth()+titleOf(controller):link==null?"":link;
        if(key.equals(artKey))return;artKey=key;
        if(b!=null){fade(b);return;}
        artImage.setImageDrawable(null);if(link==null)return;
        final String url=link;final Context context=getContext().getApplicationContext();
        WidgetBoard.ART.execute(()->{Bitmap loaded=null;
            try{byte[] bytes;Uri uri=Uri.parse(url);String scheme=uri.getScheme();
                if("http".equals(scheme)||"https".equals(scheme))bytes=RemoteData.get(url,3_000_000,true);
                else if("content".equals(scheme)||"android.resource".equals(scheme))bytes=RemoteData.read(context.getContentResolver().openInputStream(uri),3_000_000);
                else throw new java.io.IOException("Unsupported artwork");
                BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
                o.inSampleSize=RemoteData.imageSample(o.outWidth,o.outHeight,px(ART)*2);o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;loaded=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
            }catch(Exception|OutOfMemoryError ignored){}
            final Bitmap result=loaded;if(result!=null)post(()->{if(url.equals(artKey))fade(result);});});
    }
    private void fade(Bitmap b){artImage.setImageBitmap(b);if(motion()){artImage.setAlpha(0f);artImage.animate().alpha(1).setDuration(300).start();}}
    private String[] hero(){
        if(Promo.on)return new String[]{"music:promo",Promo.SONG,Promo.ARTIST+" · "+Promo.SOURCE};
        if(controller!=null&&!titleOf(controller).isEmpty())return new String[]{"music:"+titleOf(controller),titleOf(controller),UpNextWidget.join(artist.getText().toString(),appName(controller.getPackageName()))};
        return access?new String[]{"music:none","Now Playing","Start music in an app and control it from Home."}:new String[]{"music:allow","Now Playing","Allow notification access so Gunner's can show and control the music playing on this TV. Gunner's does not read your notifications."};
    }
    @Override protected void onDetachedFromWindow(){setActive(false);handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}

