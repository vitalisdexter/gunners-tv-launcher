package dev.gunnerstv.launcher;

import java.util.*;

/** Provider-reported match state. Minutes are never extrapolated by the launcher. */
final class ArsenalMatch {
    String id="",league="",slug="",home="",away="",homeScore="",awayScore="",state="pre",status="",minute="";
    String homeLogo="",awayLogo="",homeShort="",awayShort="";
    String homeId="",awayId="",provider="ESPN";
    final Map<String,String> playerNames=new LinkedHashMap<>(),playerTeams=new LinkedHashMap<>(),playerStates=new LinkedHashMap<>();
    String following(){
        List<String> labels=new ArrayList<>();for(String id:playerNames.keySet())labels.add(playerNames.get(id)+" · "+playerStates.getOrDefault(id,"Selection pending"));
        return String.join(" / ",labels);
    }
    long kickoff,updated;
    MatchDetail detail;
    final List<String> goals=new ArrayList<>();
    boolean live(){return "in".equals(state);}
    String title(){return home+" v "+away;}
    String score(){return homeScore.isEmpty()||awayScore.isEmpty()?"v":homeScore+" – "+awayScore;}
    static ArsenalMatch featured(List<ArsenalMatch> matches,long now){
        ArsenalMatch next=null,last=null;
        for(ArsenalMatch m:matches){
            if(m.live()&&Math.abs(now-m.kickoff)<8*3600000L)return m;
            if(!"post".equals(m.state)&&m.kickoff>=now-3*3600000L&&(next==null||m.kickoff<next.kickoff))next=m;
            if("post".equals(m.state)&&m.kickoff<=now&&(last==null||m.kickoff>last.kickoff))last=m;
        }
        return next!=null?next:last;
    }
    static long interval(ArsenalMatch m,long now){
        return m!=null&&Math.abs(now-m.kickoff)<4*3600000L&& !"post".equals(m.state)?60000L:30*60000L;
    }
}
