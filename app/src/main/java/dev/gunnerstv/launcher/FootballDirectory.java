package dev.gunnerstv.launcher;

import org.json.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/** Bounded, public football search. Never follows links supplied by search results. */
final class FootballDirectory {
    static final String PROFILE="https://site.web.api.espn.com/apis/common/v3/sports/soccer/athletes/";
    static final class Result {
        final String id,name,detail;
        Result(String id,String name,String detail){this.id=id;this.name=name;this.detail=detail;}
        String label(){return name+(detail.isEmpty()?"":" · "+detail);}
        FootballSelection.Team team(){return new FootballSelection.Team(id,name,detail);}
    }
    static List<Result> search(String query,boolean player)throws Exception{
        query=FootballSelection.clean(query);if(query.length()<2||query.length()>80)throw new IllegalArgumentException("Use 2–80 characters");
        JSONObject response=ArsenalSource.get("https://site.web.api.espn.com/apis/search/v2?limit=30&page=1&type="+(player?"player":"team")+"&query="+URLEncoder.encode(query,"UTF-8"));
        if(response.has("errors"))throw new java.io.IOException("Football directory unavailable");return parseSearch(response,player);
    }
    static List<Result> parseSearch(JSONObject json,boolean player){
        LinkedHashMap<String,Result> out=new LinkedHashMap<>();JSONArray groups=json.optJSONArray("results");if(groups==null)return new ArrayList<>();
        String type=player?"player":"team";Pattern pattern=Pattern.compile("(?:^|~)"+(player?"a":"t")+":([1-9][0-9]{0,9})(?:~|$)");
        for(int g=0;g<Math.min(groups.length(),100);g++){JSONObject group=groups.optJSONObject(g);if(group==null||!type.equals(group.optString("type")))continue;JSONArray rows=group.optJSONArray("contents");if(rows==null)continue;
            for(int n=0;n<Math.min(rows.length(),100);n++){JSONObject row=rows.optJSONObject(n);if(row==null||!"soccer".equals(row.optString("sport")))continue;String uid=row.optString("uid");if(!uid.startsWith("s:600~"))continue;Matcher m=pattern.matcher(uid);String name=FootballSelection.clean(row.optString("displayName"));if(!m.find()||name.isEmpty()||name.length()>120)continue;
                out.put(m.group(1),new Result(m.group(1),name,FootballSelection.clean(row.optString("subtitle"))));if(out.size()>=30)return new ArrayList<>(out.values());}
        }return new ArrayList<>(out.values());
    }
    static final class Profile {
        final FootballSelection.Player player;
        final List<FootballSelection.Team> nationals;
        final boolean internationalAvailable;
        Profile(FootballSelection.Player p,List<FootballSelection.Team> n,boolean available){player=p;nationals=n;internationalAvailable=available;}
    }
    static Profile profile(Result result)throws Exception{
        JSONObject athlete=ArsenalSource.get(PROFILE+result.id).getJSONObject("athlete");
        FootballSelection.Player p=parseProfile(result.id,athlete);
        List<FootballSelection.Team> national=new ArrayList<>();boolean available=true;
        try{national=parseNationals(ArsenalSource.get(PROFILE+result.id+"/bio"));}catch(Exception e){available=false;}
        if(athlete.optJSONObject("team")!=null&&athlete.getJSONObject("team").optBoolean("isNational")){
            JSONObject t=athlete.getJSONObject("team");FootballSelection.Team side=providerTeam(t);if(side!=null)national.add(0,side);
        }
        LinkedHashMap<String,FootballSelection.Team> unique=new LinkedHashMap<>();for(FootballSelection.Team t:national)unique.put(t.id,t);national=new ArrayList<>(unique.values());
        if(national.size()==1)p.national=national.get(0);
        p.scope=p.club==null?"international":p.national==null?"club":"both";
        return new Profile(p,national,available);
    }
    static FootballSelection.Player parseProfile(String expected,JSONObject athlete)throws JSONException{
        String uid=athlete.optString("uid");
        if(!FootballSelection.id(expected)||!expected.equals(athlete.optString("id"))||!uid.startsWith("s:600~")||!Pattern.compile("(?:^|~)a:"+expected+"(?:~|$)").matcher(uid).find())throw new JSONException("Wrong football player");
        FootballSelection.Player p=new FootballSelection.Player(expected,athlete.optString("displayName"));JSONObject team=athlete.optJSONObject("team");
        if(team!=null&&!team.optBoolean("isNational"))p.club=providerTeam(team);return p;
    }
    private static FootballSelection.Team providerTeam(JSONObject j){try{return new FootballSelection.Team(j.optString("id"),j.optString("displayName"),"");}catch(IllegalArgumentException e){return null;}}
    static List<FootballSelection.Team> parseNationals(JSONObject bio){
        LinkedHashMap<String,FootballSelection.Team> out=new LinkedHashMap<>();JSONArray history=bio.optJSONArray("teamHistory");if(history==null)return new ArrayList<>();
        for(int n=0;n<Math.min(history.length(),100);n++){JSONObject row=history.optJSONObject(n);if(row==null)continue;boolean national=row.optBoolean("isNational");
            // This is the provider's national-team flag classification, never citizenship matching.
            try{URI logo=new URI(row.optString("logo"));String host=logo.getHost();national|="https".equals(logo.getScheme())&&host!=null&&(host.equals("espncdn.com")||host.endsWith(".espncdn.com"))&&logo.getPath().startsWith("/i/teamlogos/countries/");}catch(Exception ignored){}
            FootballSelection.Team team=providerTeam(row);if(national&&team!=null)out.put(team.id,team);
        }return new ArrayList<>(out.values());
    }
}
