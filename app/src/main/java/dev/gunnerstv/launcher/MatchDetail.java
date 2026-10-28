package dev.gunnerstv.launcher;

import org.json.*;
import java.time.OffsetDateTime;
import java.util.*;

/** Bounded, validated public match details. Missing provider fields remain empty. */
final class MatchDetail {
    String venue="",location="",referee="",homeForm="",awayForm="";
    final List<ArsenalMatch> h2h=new ArrayList<>();
    final List<Lineup> lineups=new ArrayList<>();
    final List<String> commentary=new ArrayList<>();
    static final class Lineup {
        String team="",teamId="",formation="";boolean confirmed;
        final List<String> starters=new ArrayList<>(),bench=new ArrayList<>();
    }
    static MatchDetail parse(ArsenalMatch match,JSONObject json)throws JSONException{
        if(!match.id.equals(json.getJSONObject("header").optString("id")))throw new JSONException("Wrong event");
        MatchDetail out=new MatchDetail();JSONObject info=json.optJSONObject("gameInfo");
        if(info!=null){JSONObject venue=info.optJSONObject("venue");if(venue!=null){out.venue=venue.optString("fullName");JSONObject address=venue.optJSONObject("address");if(address!=null)out.location=join(address.optString("city"),address.optString("country"));}
            JSONArray officials=info.optJSONArray("officials");if(officials!=null&&officials.length()>0){JSONObject official=officials.optJSONObject(0);if(official!=null)out.referee=official.optString("displayName",official.optString("fullName"));}}
        JSONArray competitors=json.getJSONObject("header").getJSONArray("competitions").getJSONObject(0).optJSONArray("competitors");
        if(competitors!=null)for(int i=0;i<competitors.length();i++){JSONObject c=competitors.optJSONObject(i);if(c==null)continue;if("home".equals(c.optString("homeAway")))out.homeForm=c.optString("form");else out.awayForm=c.optString("form");}
        Object series=json.opt("seasonseries");JSONArray groups=series instanceof JSONArray?(JSONArray)series:series instanceof JSONObject?new JSONArray().put(series):new JSONArray();
        Set<String> seen=new HashSet<>();
        for(int group=0;group<groups.length();group++){
            JSONObject g=groups.optJSONObject(group);if(g==null)continue;JSONArray events=g.optJSONArray("events");if(events==null)continue;
            for(int i=0;i<events.length();i++)try{
                JSONObject e=events.getJSONObject(i);ArsenalMatch m=new ArsenalMatch();m.id=e.getString("id");
                if(m.id.equals(match.id)||seen.contains(m.id))continue;m.kickoff=OffsetDateTime.parse(e.getString("date")).toInstant().toEpochMilli();
                if(m.kickoff>=match.kickoff)continue;
                JSONArray teams=e.getJSONArray("competitors");if(teams.length()!=2)continue;
                JSONObject copy=new JSONObject(e.toString());Object state=e.opt("status");
                if(state instanceof String)copy.put("status",new JSONObject().put("type",new JSONObject().put("state",state)));
                ArsenalSource.readCompetition(m,copy);
                boolean same=(match.homeId.equals(m.homeId)&&match.awayId.equals(m.awayId))||(match.homeId.equals(m.awayId)&&match.awayId.equals(m.homeId));
                if(!same||!"post".equals(m.state)||m.homeScore.isEmpty()||m.awayScore.isEmpty())continue;
                m.league=e.optString("competitionName");out.h2h.add(m);seen.add(m.id);
            }catch(Exception ignored){}
        }
        out.h2h.sort(Comparator.comparingLong((ArsenalMatch m)->m.kickoff).reversed());while(out.h2h.size()>5)out.h2h.remove(out.h2h.size()-1);
        JSONArray rosters=json.optJSONArray("rosters");
        if(rosters!=null)for(int n=0;n<rosters.length();n++){
            JSONObject r=rosters.optJSONObject(n);if(r==null)continue;JSONObject team=r.optJSONObject("team");if(team==null)continue;
            if(!Arrays.asList(match.homeId,match.awayId).contains(team.optString("id")))continue;
            Lineup lineup=new Lineup();lineup.team=team.optString("displayName");lineup.teamId=team.optString("id");lineup.formation=r.optString("formation");JSONArray squad=r.optJSONArray("roster");
            if(squad!=null)for(int i=0;i<Math.min(40,squad.length());i++){
                JSONObject player=squad.optJSONObject(i);if(player==null)continue;JSONObject athlete=player.optJSONObject("athlete");if(athlete==null)continue;
                String name=athlete.optString("displayName",athlete.optString("fullName"));if(name.isEmpty())continue;
                JSONObject position=player.optJSONObject("position");String line=(player.optString("jersey").isEmpty()?"":player.optString("jersey")+"  ")+name;
                if(position!=null&&!position.optString("abbreviation").isEmpty())line+=" · "+position.optString("abbreviation");
                if(player.optBoolean("subbedIn"))line+=" · On";if(player.optBoolean("subbedOut"))line+=" · Off";
                (player.optBoolean("starter")?lineup.starters:lineup.bench).add(line);
            }
            lineup.confirmed=lineup.starters.size()>=11;out.lineups.add(lineup);
        }
        JSONArray commentary=json.optJSONArray("commentary");
        if(commentary!=null)for(int i=commentary.length()-1;i>=0&&out.commentary.size()<100;i--){
            JSONObject event=commentary.optJSONObject(i);if(event==null)continue;String text=event.optString("text").replaceAll("<[^>]*>","").trim();if(text.isEmpty())continue;
            JSONObject time=event.optJSONObject("time");String minute=time==null?"":time.optString("displayValue");out.commentary.add(minute.isEmpty()?text:minute+"  ·  "+text);
        }
        return out;
    }
    static String join(String a,String b){return a.isEmpty()?b:b.isEmpty()?a:a+" · "+b;}
    /** Counts wins relative to the current home/away teams, regardless of past venue. */
    static int[] balance(ArsenalMatch current,List<ArsenalMatch> meetings){
        int[] totals=new int[3];
        for(ArsenalMatch m:meetings)try{
            boolean same=current.homeId.equals(m.homeId)&&current.awayId.equals(m.awayId),reverse=current.homeId.equals(m.awayId)&&current.awayId.equals(m.homeId);
            if(!"post".equals(m.state)||(!same&&!reverse))continue;
            int h=Integer.parseInt(m.homeScore),a=Integer.parseInt(m.awayScore);if(h<0||a<0)continue;
            if(h==a)totals[1]++;else if((h>a)==same)totals[0]++;else totals[2]++;
        }catch(NumberFormatException ignored){}
        return totals;
    }
}
