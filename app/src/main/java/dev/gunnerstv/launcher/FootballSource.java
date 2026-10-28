package dev.gunnerstv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Owner's club/player feed. Public ESPN fallback; no account, subscription or hidden app data. */
final class FootballSource extends ArsenalSource {
    static final String[] PLAYER_IDS={"22774","231388"};
    static final String[] PLAYER_NAMES={"Cristiano Ronaldo","Kylian Mbappé"};
    static final String[] NATIONAL_TEAMS={"482","478"};
    private final SharedPreferences store;
    private final Set<String> followedPlayers;
    static volatile String diagnostic="Not loaded";
    private static FootballSource shared;
    static synchronized FootballSource shared(Context c){if(shared==null)shared=new FootballSource(c.getApplicationContext());return shared;}
    private Snapshot latest;private long loadedAt;
    FootballSource(Context context){super(context);store=context.getSharedPreferences("football_cache",0);
        FootballPolicy.CLUBS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.followed_club_ids))));
        followedPlayers=new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.followed_player_ids)));}
    @Override synchronized Snapshot load(boolean network){
        long now=System.currentTimeMillis();if(network&&latest!=null&&now-loadedAt<45000L)return latest;
        Snapshot result=new Snapshot();List<String> issues=new ArrayList<>();
        Map<String,List<String>> teamPlayers=new LinkedHashMap<>();
        for(String club:FootballPolicy.CLUBS)teamPlayers.put(club,new ArrayList<>());
        for(int i=0;i<PLAYER_IDS.length;i++){
            if(!followedPlayers.contains(PLAYER_IDS[i]))continue;
            String id=PLAYER_IDS[i],team=store.getString("club_"+id,"");
            if(network&&now-store.getLong("profile_attempt_"+id,0)>(team.isEmpty()?60000L:86400000L)){
                store.edit().putLong("profile_attempt_"+id,now).apply();
                try{
                    JSONObject athlete=get("https://site.web.api.espn.com/apis/common/v3/sports/soccer/athletes/"+id).getJSONObject("athlete");
                    if(!id.equals(athlete.getString("id")))throw new JSONException("Wrong athlete");
                    team=athlete.getJSONObject("team").getString("id");
                    if(!team.matches("[0-9]+"))throw new JSONException("Invalid team");
                    store.edit().putString("club_"+id,team).apply();
                }catch(Exception e){result.offline=true;issues.add("Profile "+id+": "+e.getClass().getSimpleName());}
            }
            if(!team.isEmpty())teamPlayers.computeIfAbsent(team,k->new ArrayList<>()).add(id);
            teamPlayers.computeIfAbsent(NATIONAL_TEAMS[i],k->new ArrayList<>()).add(id);
        }
        Map<String,ArsenalMatch> merged=new LinkedHashMap<>();
        for(Map.Entry<String,List<String>> entry:teamPlayers.entrySet()){
            String team=entry.getKey();long at=store.getLong("schedule_at_"+team,0);
            JSONArray events=array(store.getString("schedule_"+team,"[]"));
            if(network&&now-at>=6*3600000L&&now-store.getLong("attempt_"+team,0)>=5*60000L){
                store.edit().putLong("attempt_"+team,now).apply();
                try{
                    JSONArray future=get(BASE+"all/teams/"+team+"/schedule?fixture=true").getJSONArray("events");
                    JSONArray past=get(BASE+"all/teams/"+team+"/schedule").getJSONArray("events");
                    JSONArray fresh=new JSONArray();
                    for(JSONArray list:new JSONArray[]{past,future})for(int n=0;n<list.length();n++){
                        JSONObject event=list.getJSONObject(n);JSONObject league=event.optJSONObject("league");
                        if(league==null)continue;String slug=league.optString("slug");if(!slug.matches("[a-z0-9._-]+"))continue;
                        event.put("personalLeague",slug);
                        long date=java.time.OffsetDateTime.parse(event.getString("date")).toInstant().toEpochMilli();
                        if(date>=now-2*86400000L&&date<=now+28*86400000L)fresh.put(event);
                    }
                    events=fresh;at=now;store.edit().putString("schedule_"+team,events.toString()).putLong("schedule_at_"+team,at).apply();
                }catch(Exception e){result.offline=true;issues.add("Schedule "+team+": "+e.getClass().getSimpleName());}
            }
            if(now-at>7*3600000L)result.offline=true;
            for(ArsenalMatch m:decode(events).matches){
                ArsenalMatch target=merged.get(m.id);if(target==null){target=m;target.updated=at;merged.put(m.id,target);}
                for(String player:entry.getValue()){
                    target.playerNames.put(player,name(player));target.playerTeams.put(player,team);
                    target.playerStates.put(player,"Selection pending");
                }
            }
            if(at>0)result.updated=result.updated==0?at:Math.min(result.updated,at);
        }
        if(network&&ScoreboardRefresh.refresh(merged,store,now))result.offline=true;
        // Near kickoff and during play, refresh each relevant match; never extrapolate match minutes.
        for(ArsenalMatch match:merged.values()){
            String saved=store.getString("details_"+match.id,"");
            if(!saved.isEmpty())try{JSONObject cached=new JSONObject(saved);long at=store.getLong("details_at_"+match.id,0);if(at>=match.updated){applyDetails(match,cached);match.updated=at;}else{applySelection(match,cached);match.detail=MatchDetail.parse(match,cached);}}catch(Exception ignored){}
            boolean near=Math.abs(now-match.kickoff)<4*3600000L;
            boolean needsPlayerResult="post".equals(match.state)&&!match.playerNames.isEmpty()&&saved.isEmpty();
            if(network&&(near||needsPlayerResult)&&now-store.getLong("details_attempt_"+match.id,0)>=ArsenalMatch.interval(match,now)){
                try{details(match);}catch(Exception e){result.offline=true;}
            }
        }
        result.matches.addAll(FootballPolicy.select(new ArrayList<>(merged.values()),now));
        long followed=result.matches.stream().filter(m->!m.playerNames.isEmpty()).count();
        diagnostic="matches="+result.matches.size()+" playerMatches="+followed+" teams="+teamPlayers.keySet()+" issues="+issues;
        if(result.matches.isEmpty())result.error=result.offline?"Fixtures unavailable. Try again shortly.":"No matching fixtures in the next 28 days.";
        latest=result;loadedAt=now;return result;
    }
    @Override synchronized ArsenalMatch details(ArsenalMatch match)throws Exception{
        long now=System.currentTimeMillis();store.edit().putLong("details_attempt_"+match.id,now).apply();
        JSONObject json=get(BASE+match.slug+"/summary?event="+match.id);applyDetails(match,json);match.updated=now;
        store.edit().putString("details_"+match.id,json.toString()).putLong("details_at_"+match.id,now).apply();return match;
    }
    static void applyDetails(ArsenalMatch match,JSONObject json)throws JSONException{
        applySummary(match,json);
        applySelection(match,json);
        match.detail=MatchDetail.parse(match,json);
    }
    static void applySelection(ArsenalMatch match,JSONObject json){
        JSONArray rosters=json.optJSONArray("rosters");if(rosters==null)return;
        for(String player:match.playerNames.keySet()){
            for(int n=0;n<rosters.length();n++){
                JSONObject roster=rosters.optJSONObject(n);if(roster==null)continue;JSONObject team=roster.optJSONObject("team");
                if(team==null||!match.playerTeams.get(player).equals(team.optString("id")))continue;
                JSONArray squad=roster.optJSONArray("roster");if(squad==null)continue;
                boolean found=false;int starters=0;
                for(int i=0;i<squad.length();i++){JSONObject row=squad.optJSONObject(i);if(row!=null&&row.optBoolean("starter"))starters++;}
                for(int i=0;i<squad.length();i++){
                    JSONObject row=squad.optJSONObject(i);if(row==null)continue;
                    JSONObject athlete=row.optJSONObject("athlete");if(athlete==null||!player.equals(athlete.optString("id")))continue;
                    found=true;String state=row.optBoolean("starter")?"Started":row.optBoolean("subbedIn")?"Played":starters<11?"Selection pending":"post".equals(match.state)?"Unused substitute":"On the bench";
                    if("pre".equals(match.state)&&row.optBoolean("starter"))state="Starting";
                    match.playerStates.put(player,state);
                }
                // Only a complete confirmed XI establishes absence. Partial lists stay unconfirmed.
                if(!found&&starters>=11)match.playerStates.put(player,"Not selected");
            }
        }
    }
    private static String name(String id){for(int n=0;n<PLAYER_IDS.length;n++)if(PLAYER_IDS[n].equals(id))return PLAYER_NAMES[n];return id;}
    private static JSONArray array(String value){try{return new JSONArray(value);}catch(Exception e){return new JSONArray();}}
}
