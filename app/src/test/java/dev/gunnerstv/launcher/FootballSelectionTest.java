package dev.gunnerstv.launcher;

import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;

public class FootballSelectionTest {
    private FootballSelection.Team team(String id){return new FootballSelection.Team(id,"Club "+id,"");}
    @Test public void emptySelectionRoundTripsWithoutPersonalDefaults(){FootballSelection s=FootballSelection.decode(new FootballSelection().encode());assertTrue(s.empty());assertEquals("",s.main);}
    @Test public void mainMustBeFollowedAndRemovingItClearsFallback(){FootballSelection s=new FootballSelection();assertTrue(s.main(team("83")));assertEquals("83",s.main);assertTrue(s.clubs.containsKey("83"));s.removeClub("83");assertEquals("",s.main);}
    @Test public void arbitraryPlayerAndScopeSurviveRestart(){
        FootballSelection s=new FootballSelection();FootballSelection.Player p=new FootballSelection.Player("280555","Bukayo Saka");p.club=team("359");p.national=team("448");p.scope="international";s.add(p);
        FootballSelection.Player copy=FootballSelection.decode(s.encode()).players.get("280555");assertFalse(copy.club());assertTrue(copy.international());assertEquals("448",copy.national.id);assertEquals("Bukayo Saka",copy.name);
    }
    @Test public void duplicatesAndLimitsBoundRequests(){
        FootballSelection s=new FootballSelection();for(int i=1;i<=FootballSelection.MAX_CLUBS;i++)assertTrue(s.add(team(""+i)));assertTrue(s.add(team("1")));assertFalse(s.add(team("900")));assertFalse(s.main(team("901")));assertEquals("",s.main);
        for(int i=1;i<=FootballSelection.MAX_PLAYERS;i++){FootballSelection.Player p=new FootballSelection.Player(""+i,"Player "+i);p.scope="club";p.club=team("359");assertTrue(s.add(p));}FootballSelection.Player extra=new FootballSelection.Player("999","Extra");extra.scope="club";extra.club=team("359");assertFalse(s.add(extra));
    }
    @Test public void malformedAndFutureSchemasAreNotTreatedAsDefaults(){assertTrue(FootballSelection.decode("broken").empty());assertTrue(FootballSelection.decode("{\"schema\":99}").empty());}
    @Test public void invalidRowsAndOrphanMainAreDiscarded()throws Exception{
        JSONObject j=new JSONObject().put("schema",1).put("main","359").put("clubs",new JSONArray().put(new JSONObject().put("id","../359").put("name","Bad")).put(team("83").json()));
        FootballSelection s=FootballSelection.decode(j.toString());assertEquals(1,s.clubs.size());assertEquals("",s.main);assertTrue(s.clubs.containsKey("83"));
    }
    @Test public void draftEditingDoesNotMutateSavedSelection(){FootballSelection original=new FootballSelection();original.main(team("83"));FootballSelection copy=FootballSelection.decode(original.encode());copy.removeClub("83");assertEquals("83",original.main);assertTrue(copy.empty());}
    @Test public void unsupportedPlayerScopeDoesNotCauseHiddenTeamRequests()throws Exception{
        JSONObject p=new JSONObject().put("id","280555").put("name","Bukayo Saka").put("scope","everything");assertTrue(FootballSelection.decode(new JSONObject().put("schema",1).put("players",new JSONArray().put(p)).toString()).players.isEmpty());
    }
    @Test public void selectedScopeRequiresConfirmedTeamAssociations(){FootballSelection s=new FootballSelection();FootballSelection.Player p=new FootballSelection.Player("280555","Bukayo Saka");assertFalse(s.add(p));p.club=team("359");assertFalse(s.add(p));p.scope="club";assertTrue(s.add(p));}
}
