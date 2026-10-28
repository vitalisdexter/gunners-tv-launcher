package dev.gunnerstv.launcher;

import android.content.*;
import android.net.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Control: the TV's connection, picture and sound, accessories and settings as four tiles, in the spirit of
 *  Control Center. Each shows its state and opens the matching page of the TV's own Settings. */
final class ControlWidget extends Widget {
    static final float PAD=18,GAP=12;
    final float tileW=(w-PAD*2-GAP)/2,tileH=(WidgetBoard.ROW_H-PAD*2-GAP)/2;
    final Tile network,picture,accessories,settings;
    final Runnable tick=new Runnable(){public void run(){if(!active)return;update();postDelayed(this,15000);}};

    ControlWidget(Context context,WidgetBoard.Host host){
        super(context,host,WidgetBoard.width("control"));
        network=tile("wifi","Wi-Fi",0,0,0,()->open(Settings.ACTION_WIFI_SETTINGS));
        picture=tile("speaker","Audio Output",1,1,0,this::openAudioOutput);
        accessories=tile("home","Google Home",2,0,1,this::openGoogleHome);
        settings=tile("gear","Settings",3,1,1,()->open(Settings.ACTION_SETTINGS));
        accessories.set("Smart home controls","Open your Google Home lights, cameras and other linked devices.",false);
        settings.set("Apps, storage and more","All of the TV's settings: apps, storage, date, language and more.",false);
        update();
    }
    private Tile tile(String kind,String title,int index,int col,int row,Runnable action){Tile t=new Tile(kind,title,tileW,tileH,index,action);addView(t,pos(tileW,tileH,PAD+col*(tileW+GAP),PAD+row*(tileH+GAP)));return t;}
    private void open(String action){try{host.start(new Intent(action).setPackage("com.android.tv.settings"));}catch(RuntimeException e){host.start(new Intent(Settings.ACTION_SETTINGS));}}
    private void openGoogleHome(){
        try{host.activity().startActivity(new Intent("com.google.android.libraries.tv.smarthome.intent.action.OPEN_SMART_HOME").setPackage("com.google.android.apps.tv.dreamx").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(android.content.ActivityNotFoundException|SecurityException e){Toast.makeText(getContext(),"Google Home controls are unavailable on this box.",Toast.LENGTH_LONG).show();}
    }
    private void openAudioOutput(){
        try{host.activity().startActivity(new Intent("com.android.systemui.TV_MEDIA_OUTPUT_DIALOG").setPackage("com.android.systemui"));}
        catch(android.content.ActivityNotFoundException|SecurityException e){Toast.makeText(getContext(),"The audio-output picker is unavailable on this box.",Toast.LENGTH_LONG).show();}
    }
    @Override void setActive(boolean value){boolean was=active;super.setActive(value);removeCallbacks(tick);if(value&&!was)post(tick);}
    @Override void refresh(){update();}

    @SuppressWarnings("deprecation")
    void update(){
        Context c=getContext();
        try{
            ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
            if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){network.glyph.kind("ethernet");network.name.setText("Ethernet");network.set("Connected","Connected by cable. OK opens network settings.",true);}
            else if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                WifiInfo info=((WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
                String ssid=info==null?null:info.getSSID();if(ssid!=null){ssid=ssid.replace("\"","");if(ssid.startsWith("<")||ssid.isEmpty())ssid=null;}
                int level=info==null?0:WifiManager.calculateSignalLevel(info.getRssi(),4);String signal=new String[]{"Weak","Fair","Good","Excellent"}[Math.max(0,Math.min(3,level))];
                boolean online=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                network.glyph.kind("wifi");network.name.setText("Wi-Fi");
                network.set(ssid!=null?ssid:online?signal+" signal":"No internet",(ssid!=null?"Connected to "+ssid+". ":"Connected. ")+signal+" signal"+(online?"":", no internet")+". OK opens Wi-Fi settings.",true);
            } else {network.glyph.kind("offline");network.name.setText("Wi-Fi");network.set("Not connected","The TV is offline. OK opens Wi-Fi settings to connect.",false);}
        }catch(RuntimeException e){network.set("Network settings","OK opens network settings.",false);}
        picture.set("TV & Bluetooth speakers","Choose TV/HDMI, a saved speaker, or connect another audio device.",false);
    }
}

