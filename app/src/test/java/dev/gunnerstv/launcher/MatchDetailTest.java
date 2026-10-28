package dev.gunnerstv.launcher;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class MatchDetailTest {
    private ArsenalMatch match(){ArsenalMatch m=new ArsenalMatch();m.id="now";m.homeId="359";m.awayId="363";m.kickoff=1800000000000L;return m;}
    private JSONObject json()throws Exception{return new JSONObject().put("header",new JSONObject().put("id","now").put("competitions",new JSONArray().put(new JSONObject().put("competitors",new JSONArray()))));}
    private JSONObject team(String id,String side,String score)throws Exception{return new JSONObject().put("homeAway",side).put("score",score).put("team",new JSONObject().put("id",id).put("displayName",id));}
    private JSONObject meeting(String id,String date,String home,String away,String state)throws Exception{return new JSONObject().put("id",id).put("date",date).put("status",state).put("competitors",new JSONArray().put(team(home,"home","2")).put(team(away,"away","1")));}
    @Test public void h2hAcceptsOnlyPastCompletedSamePairAndDeduplicates()throws Exception{
        JSONArray events=new JSONArray().put(meeting("old","2026-01-01T15:00Z","363","359","post")).put(meeting("old","2026-01-01T15:00Z","363","359","post"))
            .put(meeting("wrong","2026-01-01T15:00Z","363","999","post")).put(meeting("future","2029-01-01T15:00Z","363","359","post")).put(meeting("pre","2026-01-01T15:00Z","363","359","pre"));
        MatchDetail d=MatchDetail.parse(match(),json().put("seasonseries",new JSONArray().put(new JSONObject().put("events",events))));
        assertEquals(1,d.h2h.size());assertEquals("old",d.h2h.get(0).id);assertEquals("2 – 1",d.h2h.get(0).score());
    }
    @Test public void missingDataDoesNotInventLineupCommentaryOrVenue()throws Exception{
        MatchDetail d=MatchDetail.parse(match(),json());assertTrue(d.lineups.isEmpty());assertTrue(d.commentary.isEmpty());assertEquals("",d.venue);assertTrue(d.h2h.isEmpty());
    }
    @Test(expected=JSONException.class) public void unrelatedSummaryIsRejected()throws Exception{MatchDetail.parse(match(),json().put("header",new JSONObject().put("id","other")));}
    @Test public void lineupsKeepStartersAndBenchSeparateAndRequireConfirmedXI()throws Exception{
        JSONArray players=new JSONArray();for(int n=1;n<=11;n++)players.put(new JSONObject().put("starter",true).put("jersey",""+n).put("athlete",new JSONObject().put("displayName","Player "+n)));
        players.put(new JSONObject().put("starter",false).put("subbedIn",true).put("athlete",new JSONObject().put("displayName","Sub")));
        JSONObject roster=new JSONObject().put("team",new JSONObject().put("id","359").put("displayName","Arsenal")).put("formation","4-3-3").put("roster",players);
        MatchDetail d=MatchDetail.parse(match(),json().put("rosters",new JSONArray().put(roster)));assertTrue(d.lineups.get(0).confirmed);assertEquals(11,d.lineups.get(0).starters.size());assertEquals("Sub · On",d.lineups.get(0).bench.get(0));
    }
    @Test public void commentaryHasLatestEntriesFirstAndNoMarkup()throws Exception{
        JSONArray events=new JSONArray().put(new JSONObject().put("text","Kickoff").put("time",new JSONObject().put("displayValue","1'"))).put(new JSONObject().put("text","<b>Goal</b>").put("time",new JSONObject().put("displayValue","9'")));
        MatchDetail d=MatchDetail.parse(match(),json().put("commentary",events));assertEquals("9'  ·  Goal",d.commentary.get(0));assertEquals(2,d.commentary.size());
    }
    @Test public void upstreamScoreboardUpdatesOnlyMatchingFixtureAndNeverOverwritesNewerData()throws Exception{
        ArsenalMatch m=match();m.updated=100;m.state="pre";Map<String,ArsenalMatch> map=new HashMap<>();map.put(m.id,m);
        JSONObject competition=new JSONObject().put("competitors",new JSONArray().put(team("359","home","1")).put(team("363","away","0"))).put("status",new JSONObject().put("type",new JSONObject().put("state","in")).put("displayClock","42'"));
        JSONObject board=new JSONObject().put("events",new JSONArray().put(new JSONObject().put("id","now").put("competitions",new JSONArray().put(competition))));
        ScoreboardRefresh.apply(map,board,99);assertEquals("pre",m.state);ScoreboardRefresh.apply(map,board,101);assertTrue(m.live());assertEquals("42'",m.minute);assertEquals("1 – 0",m.score());
        assertEquals(1,map.size());
    }
}
