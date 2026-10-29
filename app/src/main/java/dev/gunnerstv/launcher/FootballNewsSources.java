package dev.gunnerstv.launcher;

import android.content.*;
import org.json.*;
import java.util.*;

/** Editable club-to-creator mappings; the catalogue never follows a club on the user's behalf. */
final class FootballNewsSources {
    static final int MAX=12;
    static final class Source {
        final String club,channel,name;
        Source(String club,String channel,String name){if(!FootballSelection.id(club)||!YouTubeFeeds.channel(channel))throw new IllegalArgumentException("Invalid source");this.club=club;this.channel=channel;this.name=YouTubeFeeds.clean(name,120);}
        String key(){return club+":"+channel;}
        JSONObject json()throws JSONException{return new JSONObject().put("club",club).put("channel",channel).put("name",name);}
    }
    static final Source[] DEFAULTS={
        new Source("359","UCBTy8j2cPy6zw68godcE7MQ","AFTV"),
        new Source("363","UCxGkVbPmzcMLbPj7OPcRAWg","Blue Lions TV"),
        new Source("382","UCSZfggT235t8_uaMREZ8IPQ","City Xtra"),
        new Source("360","UCMmVPVb0BwSIOWVeDwlPocQ","The United Stand"),
        new Source("364","UCc5C5dNupCMbyutNatfBujQ","The Anfield Wrap"),
        new Source("367","UCUz_XIKFQOrliSqPZWJBq2g","WeAreTottenhamTV")};
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("gunners",0);}
    static List<Source> list(Context c){
        return list(FootballSettings.load(c),prefs(c).getStringSet("news_disabled",Collections.emptySet()),prefs(c).getString("news_sources","[]"));
    }
    static List<Source> list(FootballSelection followed,Set<String> disabled,String raw){
        LinkedHashMap<String,Source> out=new LinkedHashMap<>();
        for(Source s:DEFAULTS)if(followed.clubs.containsKey(s.club)&&!disabled.contains(s.key()))out.put(s.key(),s);
        for(Source s:custom(raw))if(followed.clubs.containsKey(s.club))out.put(s.key(),s);
        return new ArrayList<>(out.values()).subList(0,Math.min(MAX,out.size()));
    }
    static List<Source> custom(String raw){List<Source> out=new ArrayList<>();try{JSONArray a=new JSONArray(raw);for(int i=0;i<Math.min(a.length(),100);i++)try{JSONObject j=a.getJSONObject(i);out.add(new Source(j.optString("club"),j.optString("channel"),j.optString("name")));}catch(Exception ignored){}}catch(JSONException ignored){}return out;}
    private static void write(Context c,List<Source> sources,Set<String> disabled){JSONArray a=new JSONArray();for(Source s:sources)try{a.put(s.json());}catch(JSONException ignored){}prefs(c).edit().putString("news_sources",a.toString()).putStringSet("news_disabled",disabled).apply();}
    static boolean add(Context c,Source source){List<Source> active=list(c);boolean exists=false;for(Source s:active)if(s.key().equals(source.key()))exists=true;if(!exists&&active.size()>=MAX)return false;
        List<Source> custom=custom(prefs(c).getString("news_sources","[]"));custom.removeIf(s->s.key().equals(source.key()));custom.add(source);Set<String> off=new HashSet<>(prefs(c).getStringSet("news_disabled",Collections.emptySet()));off.remove(source.key());write(c,custom,off);return true;}
    static void remove(Context c,Source source){List<Source> custom=custom(prefs(c).getString("news_sources","[]"));custom.removeIf(s->s.key().equals(source.key()));Set<String> off=new HashSet<>(prefs(c).getStringSet("news_disabled",Collections.emptySet()));off.add(source.key());write(c,custom,off);}
}
