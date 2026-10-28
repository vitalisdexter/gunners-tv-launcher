package dev.gunnerstv.launcher;

import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Single-date refresh for selected fixtures. Component provenance: THIRD_PARTY_NOTICES.md. */
final class ScoreboardRefresh {
    static Set<String> dates(long at){
        Set<String> dates=new LinkedHashSet<>();
        for(String zone:new String[]{"UTC","America/New_York"}){
            SimpleDateFormat f=new SimpleDateFormat("yyyyMMdd",Locale.US);f.setTimeZone(TimeZone.getTimeZone(zone));dates.add(f.format(new Date(at)));
        }
        return dates;
    }
    static boolean refresh(Map<String,ArsenalMatch> matches,SharedPreferences cache,long now){
        Set<String> pairs=new LinkedHashSet<>();boolean failed=false;
        for(ArsenalMatch m:matches.values())if(Math.abs(now-m.kickoff)<4*3600000L&&!"post".equals(m.state))
            for(String date:dates(m.kickoff))pairs.add(m.slug+"@"+date);
        for(String pair:pairs){
            long at=cache.getLong("score_at_"+pair,0);String saved=cache.getString("score_"+pair,"");
            if(now-cache.getLong("score_try_"+pair,0)>=60000L){
                cache.edit().putLong("score_try_"+pair,now).apply();
                try{
                    int split=pair.indexOf('@');saved=ArsenalSource.get(ArsenalSource.BASE+pair.substring(0,split)+"/scoreboard?dates="+pair.substring(split+1)).toString();at=now;
                    cache.edit().putString("score_"+pair,saved).putLong("score_at_"+pair,at).apply();
                }catch(Exception e){failed=true;}
            }
            if(!saved.isEmpty())try{apply(matches,new JSONObject(saved),at);}catch(JSONException e){failed=true;}
        }
        return failed;
    }
    static void apply(Map<String,ArsenalMatch> matches,JSONObject json,long at)throws JSONException{
        JSONArray events=json.optJSONArray("events");if(events==null)return;
        for(int i=0;i<events.length();i++){
            JSONObject event=events.optJSONObject(i);if(event==null)continue;ArsenalMatch m=matches.get(event.optString("id"));
            if(m==null||at<m.updated)continue;
            ArsenalSource.readCompetition(m,event.getJSONArray("competitions").getJSONObject(0));m.updated=at;
        }
    }
}
