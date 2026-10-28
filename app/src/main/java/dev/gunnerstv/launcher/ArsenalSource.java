package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/** Free public ESPN responses, no login or key. Only the visible widget requests updates. */
class ArsenalSource {
    static final String BASE="https://site.api.espn.com/apis/site/v2/sports/soccer/";
    static final String[] LEAGUES={"eng.1","uefa.champions","eng.fa","eng.league_cup"};
    static final class Snapshot {
        final List<ArsenalMatch> matches=new ArrayList<>();
        long updated; boolean offline; String error="";
        ArsenalMatch featured(){return ArsenalMatch.featured(matches,System.currentTimeMillis());}
    }
    private final SharedPreferences cache;
    ArsenalSource(Context c){cache=c.getSharedPreferences("arsenal_cache",0);}
    synchronized Snapshot load(boolean network){
        JSONArray events;
        try{events=new JSONArray(cache.getString("events","[]"));}catch(JSONException e){events=new JSONArray();}
        long now=System.currentTimeMillis(),scheduleAt=cache.getLong("schedule_at",0);
        boolean failed=false;
        if(network&&(events.length()==0||now-scheduleAt>6*3600000L)){
            Map<String,JSONObject> all=new LinkedHashMap<>();boolean complete=true;
            // Keep a league's previous cache if that league temporarily fails.
            for(int i=0;i<events.length();i++){JSONObject e=events.optJSONObject(i);if(e!=null)all.put(e.optString("id"),e);}
            for(String league:LEAGUES)for(boolean future:new boolean[]{false,true}){
                try{
                    JSONArray list=get(BASE+league+"/teams/359/schedule"+(future?"?fixture=true":"")).optJSONArray("events");
                    if(list==null)throw new IOException("Missing events");
                    for(int i=0;i<list.length();i++){JSONObject event=list.getJSONObject(i);event.put("personalLeague",league);all.put(event.getString("id"),event);}
                }catch(Exception e){complete=false;failed=true;android.util.Log.w("Gunner'sArsenal","Schedule unavailable: "+league+" ("+e.getClass().getSimpleName()+")");}
            }
            if(!all.isEmpty()){
                events=new JSONArray();for(JSONObject e:all.values())events.put(e);
                SharedPreferences.Editor edit=cache.edit().putString("events",events.toString());
                // Retry partial failures in five minutes, rather than six hours.
                edit.putLong("schedule_at",complete?now:now-6*3600000L+5*60000L);
                if(complete)edit.putLong("good_schedule",now);
                edit.apply();
            }
        }
        Snapshot s=decode(events);s.offline=failed;
        for(ArsenalMatch m:s.matches)try{applySummary(m,new JSONObject(cache.getString("match_"+m.id,"")));m.updated=cache.getLong("at_"+m.id,0);}catch(JSONException ignored){}
        ArsenalMatch featured=s.featured();
        if(featured!=null){
            long summaryAt=cache.getLong("at_"+featured.id,0);
            if(network&&now-summaryAt>=ArsenalMatch.interval(featured,now)){
                try{JSONObject json=get(BASE+featured.slug+"/summary?event="+featured.id);applySummary(featured,json);cache.edit().putString("match_"+featured.id,json.toString()).putLong("at_"+featured.id,now).apply();summaryAt=now;featured.updated=now;}
                catch(Exception e){s.offline=true;}
            }
            try{applySummary(featured,new JSONObject(cache.getString("match_"+featured.id,"")));}catch(JSONException ignored){}
            s.updated=summaryAt>0?summaryAt:cache.getLong("good_schedule",0);
        }
        if(s.matches.isEmpty())s.error="Match data unavailable. Try again later.";
        return s;
    }
    ArsenalMatch details(ArsenalMatch match)throws Exception{
        JSONObject json=get(BASE+match.slug+"/summary?event="+match.id);applySummary(match,json);
        match.updated=System.currentTimeMillis();cache.edit().putString("match_"+match.id,json.toString()).putLong("at_"+match.id,match.updated).apply();return match;
    }
    static Snapshot decode(JSONArray events){
        Snapshot s=new Snapshot();
        for(int i=0;i<events.length();i++){
            try{
                JSONObject e=events.getJSONObject(i),comp=e.getJSONArray("competitions").getJSONObject(0);
                ArsenalMatch m=new ArsenalMatch();m.id=e.getString("id");m.kickoff=OffsetDateTime.parse(e.getString("date")).toInstant().toEpochMilli();
                m.slug=e.optString("personalLeague","eng.1");JSONObject league=e.optJSONObject("league");m.league=league==null?leagueName(m.slug):league.optString("name",leagueName(m.slug));
                readCompetition(m,comp);if(!m.home.isEmpty()&&!m.away.isEmpty())s.matches.add(m);
            }catch(Exception ignored){}
        }
        s.matches.sort(Comparator.comparingLong(m->m.kickoff));return s;
    }
    private static String leagueName(String slug){switch(slug){case "eng.1":return "Premier League";case "uefa.champions":return "Champions League";case "esp.1":return "LaLiga";case "ksa.1":return "Saudi Pro League";case "eng.fa":return "FA Cup";case "eng.league_cup":return "Carabao Cup";default:return slug;}}
    static void readCompetition(ArsenalMatch m,JSONObject comp)throws JSONException{
        JSONObject status=comp.optJSONObject("status");
        if(status!=null){JSONObject type=status.optJSONObject("type");if(type!=null){m.state=type.optString("state","pre");m.status=type.optString("description",type.optString("shortDetail"));}m.minute=status.optString("displayClock");}
        JSONArray teams=comp.getJSONArray("competitors");
        for(int i=0;i<teams.length();i++){
            JSONObject t=teams.getJSONObject(i),team=t.getJSONObject("team");String name=team.optString("shortDisplayName",team.optString("displayName"));
            String logo=team.optString("logo"),shortName=team.optString("abbreviation",name);
            JSONArray logos=team.optJSONArray("logos");if(logo.isEmpty()&&logos!=null&&logos.length()>0){JSONObject first=logos.optJSONObject(0);if(first!=null)logo=first.optString("href");}
            Object score=t.opt("score");String value=score instanceof JSONObject?((JSONObject)score).optString("displayValue"):score==null||score==JSONObject.NULL?"":String.valueOf(score);
            if("home".equals(t.optString("homeAway"))){m.home=name;m.homeId=team.optString("id");m.homeScore=value;if(!logo.isEmpty())m.homeLogo=logo;m.homeShort=shortName;}else{m.away=name;m.awayId=team.optString("id");m.awayScore=value;if(!logo.isEmpty())m.awayLogo=logo;m.awayShort=shortName;}
        }
    }
    static void applySummary(ArsenalMatch m,JSONObject summary)throws JSONException{
        JSONObject header=summary.getJSONObject("header");
        // An erroneous or unrelated event response must not overwrite this match.
        if(!m.id.equals(header.optString("id")))throw new JSONException("Wrong event");
        readCompetition(m,header.getJSONArray("competitions").getJSONObject(0));m.goals.clear();
        JSONArray events=summary.optJSONArray("keyEvents");if(events==null)return;
        for(int i=0;i<events.length();i++){
            JSONObject e=events.getJSONObject(i);if(!e.optBoolean("scoringPlay"))continue;
            JSONObject clock=e.optJSONObject("clock");String minute=clock==null?"":clock.optString("displayValue");
            String text=e.optString("shortText",e.optString("text","Goal"));m.goals.add(minute+"   "+text);
        }
    }
    static JSONObject get(String url)throws Exception{
        return new JSONObject(new String(RemoteData.get(url,2_500_000,false),StandardCharsets.UTF_8));
    }
}
