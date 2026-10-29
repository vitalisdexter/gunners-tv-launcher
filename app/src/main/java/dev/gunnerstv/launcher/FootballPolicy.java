package dev.gunnerstv.launcher;

import java.util.*;

final class FootballPolicy {
    static volatile Set<String> CLUBS=Collections.emptySet();
    static boolean accepts(ArsenalMatch match){
        if(CLUBS.contains(match.homeId)||CLUBS.contains(match.awayId))return true;
        for(String id:match.playerNames.keySet()){
            String state=match.playerStates.getOrDefault(id,"Selection pending");
            if(!"Not selected".equals(state)&&!"Unused substitute".equals(state)
                    &&(!"post".equals(match.state)||"Started".equals(state)||"Played".equals(state)))return true;
        }
        return false;
    }
    static List<ArsenalMatch> select(List<ArsenalMatch> matches,long now){
        Map<String,ArsenalMatch> unique=new LinkedHashMap<>();
        for(ArsenalMatch m:matches)if(accepts(m)&&m.kickoff>=now-2*86400000L&&m.kickoff<=now+28*86400000L)unique.put(m.slug+":"+m.id,m);
        List<ArsenalMatch> out=new ArrayList<>(unique.values());
        out.sort(Comparator.comparingLong((ArsenalMatch m)->m.kickoff).thenComparing(m->m.id));
        return out;
    }
    static List<ArsenalMatch> live(List<ArsenalMatch> matches,long now){List<ArsenalMatch> out=new ArrayList<>();for(ArsenalMatch m:matches)if(accepts(m)&&m.live()&&Math.abs(now-m.kickoff)<8*3600000L)out.add(m);out.sort(Comparator.comparingLong(m->m.kickoff));return out;}
    static List<ArsenalMatch> widgetMatches(List<ArsenalMatch> matches,long now,String mainClub){
        List<ArsenalMatch> out=live(matches,now);if(!out.isEmpty())return out;
        if(!FootballSelection.id(mainClub))return out;
        ArsenalMatch next=null;
        for(ArsenalMatch m:matches){
            if(!(mainClub.equals(m.homeId)||mainClub.equals(m.awayId))||!"pre".equals(m.state)||m.kickoff<now)continue;
            String status=m.status.toLowerCase(Locale.ROOT);if(status.contains("postpon")||status.contains("cancel")||status.contains("abandon"))continue;
            if(next==null||m.kickoff<next.kickoff)next=m;
        }
        if(next!=null)out.add(next);return out;
    }
}
