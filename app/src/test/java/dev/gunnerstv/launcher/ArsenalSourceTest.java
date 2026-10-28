package dev.gunnerstv.launcher;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class ArsenalSourceTest {
    private JSONObject competition()throws Exception{
        return new JSONObject("{\"status\":{\"type\":{\"state\":\"in\",\"description\":\"Second Half\"},\"displayClock\":\"72'\"},\"competitors\":[{\"homeAway\":\"home\",\"team\":{\"displayName\":\"Arsenal\"},\"score\":{\"displayValue\":\"2\"}},{\"homeAway\":\"away\",\"team\":{\"displayName\":\"Everton\"},\"score\":\"0\"}]}");
    }
    @Test public void handlesBothScheduleScoreObjectsAndSummaryScoreStrings()throws Exception{
        ArsenalMatch m=new ArsenalMatch();ArsenalSource.readCompetition(m,competition());assertEquals("2 – 0",m.score());assertEquals("Arsenal v Everton",m.title());assertEquals("72'",m.minute);assertTrue(m.live());
    }
    @Test public void goalTimelineUsesProviderMinutesAndSkipsNonScoringEvents()throws Exception{
        ArsenalMatch m=new ArsenalMatch();m.id="123";
        JSONObject json=new JSONObject().put("header",new JSONObject().put("id","123").put("competitions",new JSONArray().put(competition())))
            .put("keyEvents",new JSONArray("[{\"scoringPlay\":false},{\"scoringPlay\":true,\"clock\":{\"displayValue\":\"45'+2'\"},\"shortText\":\"Player Goal\"}]"));
        ArsenalSource.applySummary(m,json);assertEquals(1,m.goals.size());assertEquals("45'+2'   Player Goal",m.goals.get(0));
    }
    @Test(expected=JSONException.class) public void unrelatedSummaryCannotOverwriteMatch()throws Exception{
        ArsenalMatch m=new ArsenalMatch();m.id="123";ArsenalSource.applySummary(m,new JSONObject("{\"header\":{\"id\":\"999\"}}"));
    }
    @Test public void malformedEventsAreSkippedWithoutLosingGoodFixtures()throws Exception{
        JSONObject event=new JSONObject().put("id","123").put("date","2026-11-01T16:30Z").put("competitions",new JSONArray().put(competition()));
        ArsenalSource.Snapshot s=ArsenalSource.decode(new JSONArray().put(new JSONObject()).put(event));assertEquals(1,s.matches.size());assertEquals("123",s.matches.get(0).id);
    }
    @Test public void scheduleCrestsStayWithTheirHomeAndAwayClubs()throws Exception{
        JSONObject comp=competition();JSONArray teams=comp.getJSONArray("competitors");
        teams.getJSONObject(0).getJSONObject("team").put("abbreviation","ARS").put("logos",new JSONArray().put(new JSONObject().put("href","https://a.espncdn.com/i/teamlogos/soccer/500/359.png")));
        teams.getJSONObject(1).getJSONObject("team").put("abbreviation","EVE").put("logos",new JSONArray().put(new JSONObject().put("href","https://a.espncdn.com/i/teamlogos/soccer/500/368.png")));
        // ESPN does not promise the competitors array is ordered home first.
        comp.put("competitors",new JSONArray().put(teams.getJSONObject(1)).put(teams.getJSONObject(0)));
        JSONObject event=new JSONObject().put("id","123").put("date","2026-11-01T16:30Z").put("competitions",new JSONArray().put(comp));
        ArsenalMatch m=ArsenalSource.decode(new JSONArray().put(event)).matches.get(0);
        assertEquals("Arsenal",m.home);assertEquals("ARS",m.homeShort);assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/359.png",m.homeLogo);
        assertEquals("Everton",m.away);assertEquals("EVE",m.awayShort);assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/368.png",m.awayLogo);
    }
    @Test public void summaryReadsItsSingleLogoFieldWhileUpdatingTheScore()throws Exception{
        ArsenalMatch m=new ArsenalMatch();m.id="123";JSONObject comp=competition();
        comp.getJSONArray("competitors").getJSONObject(0).getJSONObject("team").put("logo","https://a.espncdn.com/i/teamlogos/soccer/500-dark/359.png");
        comp.getJSONArray("competitors").getJSONObject(1).getJSONObject("team").put("logo","https://a.espncdn.com/i/teamlogos/soccer/500-dark/368.png");
        ArsenalSource.applySummary(m,new JSONObject().put("header",new JSONObject().put("id","123").put("competitions",new JSONArray().put(comp))));
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500-dark/359.png",m.homeLogo);assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500-dark/368.png",m.awayLogo);
        assertEquals("2 – 0",m.score());assertEquals("72'",m.minute);
    }
    @Test public void summaryWithoutArtworkKeepsTheCachedScheduleCrests()throws Exception{
        ArsenalMatch m=new ArsenalMatch();m.id="123";m.homeLogo="https://a.espncdn.com/i/teamlogos/soccer/500/359.png";m.awayLogo="https://a.espncdn.com/i/teamlogos/soccer/500/368.png";
        ArsenalSource.applySummary(m,new JSONObject().put("header",new JSONObject().put("id","123").put("competitions",new JSONArray().put(competition()))));
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/359.png",m.homeLogo);assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/368.png",m.awayLogo);
        assertEquals("Arsenal v Everton",m.title());assertTrue(m.live());assertEquals("72'",m.minute);
    }
}
