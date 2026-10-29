package dev.gunnerstv.launcher;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class FootballPolicyTest {
    @Before public void selectClubs(){FootballPolicy.CLUBS=new HashSet<>(Arrays.asList("359","363","382","360","364","367"));}
    @After public void clearClubs(){FootballPolicy.CLUBS=Collections.emptySet();}
    private ArsenalMatch match(String home,String away,String league){ArsenalMatch m=new ArsenalMatch();m.id="1";m.homeId=home;m.awayId=away;m.slug=league;return m;}
    private ArsenalMatch player(){ArsenalMatch m=match("86","87","esp.1");m.playerNames.put("231388","Kylian Mbappé");m.playerTeams.put("231388","86");return m;}
    @Test public void selectedClubsAppearAcrossEveryCompetition(){
        assertTrue(FootballPolicy.accepts(match("359","999","eng.1")));
        assertTrue(FootballPolicy.accepts(match("999","363","uefa.champions")));
        assertTrue(FootballPolicy.accepts(match("359","363","eng.fa")));
        for(String club:FootballPolicy.CLUBS)for(String competition:Arrays.asList("eng.league_cup","fifa.cwc","club.friendly","new.competition"))assertTrue(FootballPolicy.accepts(match("999",club,competition)));
        assertFalse(FootballPolicy.accepts(match("86","817","esp.1")));
    }
    @Test public void playersMayAppearInAnyCompetitionButNotIfKnownAbsent(){
        ArsenalMatch m=player();m.slug="fifa.world";assertTrue(FootballPolicy.accepts(m));
        m.playerStates.put("231388","Not selected");assertFalse(FootballPolicy.accepts(m));
        m.homeId="359";m.slug="uefa.champions";assertTrue(FootballPolicy.accepts(m));
    }
    @Test public void completedPlayerMatchesNeedAnActualAppearance(){
        ArsenalMatch m=player();m.state="post";assertFalse(FootballPolicy.accepts(m));
        m.playerStates.put("231388","Unused substitute");assertFalse(FootballPolicy.accepts(m));
        m.playerStates.put("231388","Played");assertTrue(FootballPolicy.accepts(m));
    }
    private JSONObject summary(ArsenalMatch m,JSONArray squad)throws Exception{
        JSONObject competition=new JSONObject().put("status",new JSONObject().put("type",new JSONObject().put("state",m.state)))
                .put("competitors",new JSONArray().put(new JSONObject().put("homeAway","home").put("team",new JSONObject().put("id","86").put("displayName","Real Madrid")))
                .put(new JSONObject().put("homeAway","away").put("team",new JSONObject().put("id","87").put("displayName","Racing"))));
        return new JSONObject().put("header",new JSONObject().put("id",m.id).put("competitions",new JSONArray().put(competition)))
                .put("rosters",new JSONArray().put(new JSONObject().put("team",new JSONObject().put("id","86")).put("roster",squad)));
    }
    private JSONObject athlete(String id,boolean starter)throws Exception{return new JSONObject().put("athlete",new JSONObject().put("id",id)).put("starter",starter);}
    @Test public void partialRosterDoesNotMeanPlayerIsAbsent()throws Exception{
        ArsenalMatch m=player();FootballSource.applyDetails(m,summary(m,new JSONArray().put(athlete("999",true))));
        assertEquals("Selection pending",m.playerStates.getOrDefault("231388","Selection pending"));assertTrue(FootballPolicy.accepts(m));
    }
    @Test public void fullLineupConfirmsAbsence()throws Exception{
        ArsenalMatch m=player();JSONArray squad=new JSONArray();for(int n=0;n<11;n++)squad.put(athlete("other"+n,true));
        FootballSource.applyDetails(m,summary(m,squad));assertEquals("Not selected",m.playerStates.get("231388"));assertFalse(FootballPolicy.accepts(m));
    }
    @Test public void startingBenchAndSubAppearancesAreDistinct()throws Exception{
        ArsenalMatch m=player();FootballSource.applyDetails(m,summary(m,new JSONArray().put(athlete("231388",true))));assertEquals("Starting",m.playerStates.get("231388"));
        JSONArray full=new JSONArray().put(athlete("231388",false));for(int n=0;n<11;n++)full.put(athlete("other"+n,true));
        FootballSource.applyDetails(m,summary(m,full));assertEquals("On the bench",m.playerStates.get("231388"));
        m.state="in";FootballSource.applyDetails(m,summary(m,new JSONArray().put(athlete("231388",false).put("subbedIn",true))));assertEquals("Played",m.playerStates.get("231388"));
    }
    @Test public void fixturesSortByKickoffWithDeduplication(){
        long now=1793000000000L;ArsenalMatch future=match("359","999","eng.1");future.id="future";future.kickoff=now+86400000L;
        ArsenalMatch live=match("363","999","eng.1");live.id="live";live.kickoff=now-3600000L;live.state="in";
        ArsenalMatch result=match("367","999","eng.1");result.id="result";result.kickoff=now-86400000L;result.state="post";
        assertEquals(Arrays.asList(result,live,future),FootballPolicy.select(Arrays.asList(result,future,live,live),now));
    }
    @Test public void liveWidgetIncludesAllSelectedLiveFixturesAndExcludesStaleOrOtherTeams(){
        long now=1793000000000L;ArsenalMatch a=match("359","999","eng.1"),b=match("363","999","eng.1"),old=match("364","999","eng.1"),other=match("777","999","eng.1"),future=match("367","999","eng.1");
        a.state=b.state=old.state=other.state="in";a.kickoff=now-3600000;b.kickoff=now-1800000;old.kickoff=now-86400000;other.kickoff=now;future.kickoff=now+3600000;
        assertEquals(Arrays.asList(a,b),FootballPolicy.live(Arrays.asList(b,old,other,future,a),now));
    }
    @Test public void unconfirmedSquadListIsNotAConfirmedBench()throws Exception{
        ArsenalMatch m=player();FootballSource.applyDetails(m,summary(m,new JSONArray().put(athlete("231388",false))));
        assertEquals("Selection pending",m.playerStates.get("231388"));
    }
    @Test public void cachedLineupPreservesNewerFinalScoreAndAppearance()throws Exception{
        ArsenalMatch m=player();JSONObject old=summary(m,new JSONArray().put(athlete("231388",true)));
        m.state="post";m.homeScore="3";m.awayScore="1";
        FootballSource.applySelection(m,old);
        assertEquals("post",m.state);assertEquals("3",m.homeScore);assertEquals("Started",m.playerStates.get("231388"));assertTrue(FootballPolicy.accepts(m));
    }
    @Test public void widgetFallsBackToEarliestArsenalGameAcrossCompetitions(){
        long now=1793000000000L;ArsenalMatch other=match("363","999","eng.fa"),later=match("359","999","eng.1"),cup=match("999","359","eng.league_cup");
        other.kickoff=now+1000;later.kickoff=now+200000;cup.kickoff=now+100000;
        assertEquals(Collections.singletonList(cup),FootballPolicy.widgetMatches(Arrays.asList(later,other,cup),now,"359"));
    }
    @Test public void selectedLiveGamesReplaceArsenalPreviewAndPreviewReturnsAfterFullTime(){
        long now=1793000000000L;ArsenalMatch arsenal=match("359","999","eng.1"),live=match("363","999","eng.league_cup");
        arsenal.kickoff=now+100000;live.kickoff=now-1000;live.state="in";
        assertEquals(Collections.singletonList(live),FootballPolicy.widgetMatches(Arrays.asList(arsenal,live),now,"359"));
        live.state="post";assertEquals(Collections.singletonList(arsenal),FootballPolicy.widgetMatches(Arrays.asList(arsenal,live),now,"359"));
    }
    @Test public void arsenalPreviewSkipsPastFinishedPostponedAndCancelledFixtures(){
        long now=1793000000000L;ArsenalMatch old=match("359","999","eng.1"),ended=match("359","999","eng.fa"),postponed=match("359","999","eng.1"),cancelled=match("359","999","club.friendly"),next=match("999","359","uefa.champions");
        old.kickoff=now-1000;ended.kickoff=now+1000;ended.state="post";postponed.kickoff=now+2000;postponed.status="Postponed";cancelled.kickoff=now+3000;cancelled.status="Canceled";next.kickoff=now+4000;
        assertEquals(Collections.singletonList(next),FootballPolicy.widgetMatches(Arrays.asList(old,ended,postponed,cancelled,next),now,"359"));
    }
    @Test public void noArsenalFixtureDoesNotSubstituteAnotherUpcomingClub(){
        long now=1793000000000L;ArsenalMatch other=match("364","999","eng.1");other.kickoff=now+1000;
        assertTrue(FootballPolicy.widgetMatches(Collections.singletonList(other),now,"359").isEmpty());
    }
    @Test public void anotherMainClubAndNoMainAreRespected(){
        long now=1793000000000L;ArsenalMatch club=match("364","999","eng.1");club.kickoff=now+1000;
        assertEquals(Collections.singletonList(club),FootballPolicy.widgetMatches(Collections.singletonList(club),now,"364"));
        assertTrue(FootballPolicy.widgetMatches(Collections.singletonList(club),now,"").isEmpty());
        club.state="in";club.kickoff=now-1000;assertEquals(Collections.singletonList(club),FootballPolicy.widgetMatches(Collections.singletonList(club),now,""));
    }
    @Test public void removingLastClubDoesNotRestoreTheOldPreset(){
        FootballPolicy.CLUBS=Collections.emptySet();assertFalse(FootballPolicy.accepts(match("359","363","eng.1")));
        FootballPolicy.CLUBS=Collections.singleton("83");assertTrue(FootballPolicy.accepts(match("999","83","esp.1")));assertFalse(FootballPolicy.accepts(match("359","363","eng.1")));
    }
}
