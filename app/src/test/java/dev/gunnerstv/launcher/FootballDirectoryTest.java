package dev.gunnerstv.launcher;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

public class FootballDirectoryTest {
    private JSONObject row(String uid,String sport,String name)throws Exception{return new JSONObject().put("uid",uid).put("id","not-a-provider-number").put("sport",sport).put("displayName",name);}
    private JSONObject search(String type,JSONArray rows)throws Exception{return new JSONObject().put("results",new JSONArray().put(new JSONObject().put("type",type).put("contents",rows)));}
    @Test public void playerLookupUsesFootballUidInsteadOfSearchGuid()throws Exception{
        List<FootballDirectory.Result> out=FootballDirectory.parseSearch(search("player",new JSONArray().put(row("s:600~a:280555","soccer","Bukayo Saka")).put(row("s:40~a:123","basketball","Other"))),true);
        assertEquals(1,out.size());assertEquals("280555",out.get(0).id);
    }
    @Test public void similarlyNamedTeamsKeepDistinctIdsAndLeagueLabels()throws Exception{
        JSONObject mens=row("s:600~t:359","soccer","Arsenal").put("subtitle","Premier League"),womens=row("s:600~t:19973","soccer","Arsenal").put("subtitle","Women's Super League");
        List<FootballDirectory.Result> out=FootballDirectory.parseSearch(search("team",new JSONArray().put(mens).put(womens).put(mens)),false);assertEquals(2,out.size());assertNotEquals(out.get(0).label(),out.get(1).label());
    }
    @Test public void wrongCategoryAndForgedSportUidAreRejected()throws Exception{
        assertTrue(FootballDirectory.parseSearch(search("player",new JSONArray().put(row("s:600~a:1","soccer","Name"))),false).isEmpty());
        assertTrue(FootballDirectory.parseSearch(search("team",new JSONArray().put(row("s:40~t:1","soccer","Name")).put(row("s:600~t:../1","soccer","Bad"))),false).isEmpty());
    }
    @Test public void profilesSupportAnyFootballPlayerButMustMatchRequestedId()throws Exception{
        JSONObject athlete=new JSONObject().put("id","280555").put("uid","s:600~a:280555").put("displayName","Bukayo Saka").put("team",new JSONObject().put("id","359").put("displayName","Arsenal"));
        assertEquals("359",FootballDirectory.parseProfile("280555",athlete).club.id);
        athlete.put("uid","s:600~a:999");try{FootballDirectory.parseProfile("280555",athlete);fail();}catch(JSONException expected){}
    }
    @Test public void nationalSidesComeFromProviderClassificationNotFormerClubs()throws Exception{
        JSONArray rows=new JSONArray().put(new JSONObject().put("id","448").put("displayName","England").put("logo","https://a.espncdn.com/i/teamlogos/countries/500/eng.png"))
            .put(new JSONObject().put("id","359").put("displayName","Arsenal").put("logo","https://a.espncdn.com/i/teamlogos/soccer/500/359.png"))
            .put(new JSONObject().put("id","999").put("displayName","Forged").put("logo","https://espncdn.com.evil.test/i/teamlogos/countries/500/x.png"));
        List<FootballSelection.Team> out=FootballDirectory.parseNationals(new JSONObject().put("teamHistory",rows));assertEquals(1,out.size());assertEquals("448",out.get(0).id);
    }
    @Test public void multipleReportedNationalSidesRequireChoiceRatherThanCitizenshipGuess()throws Exception{
        JSONArray rows=new JSONArray().put(new JSONObject().put("id","441").put("displayName","Ireland").put("isNational",true)).put(new JSONObject().put("id","448").put("displayName","England").put("isNational",true));assertEquals(2,FootballDirectory.parseNationals(new JSONObject().put("teamHistory",rows)).size());
    }
}
