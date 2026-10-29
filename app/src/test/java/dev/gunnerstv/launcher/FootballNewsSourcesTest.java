package dev.gunnerstv.launcher;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FootballNewsSourcesTest {
    private FootballSelection clubs(String... ids){FootballSelection s=new FootballSelection();for(String id:ids)s.add(new FootballSelection.Team(id,"Club "+id,""));return s;}
    @Test public void catalogueNeverCreatesFollowedClubs(){FootballSelection s=clubs();assertTrue(FootballNewsSources.list(s,Collections.emptySet(),"[]").isEmpty());assertTrue(s.empty());}
    @Test public void defaultsOnlyMatchExplicitlyFollowedClubs(){List<FootballNewsSources.Source> s=FootballNewsSources.list(clubs("359","86"),Collections.emptySet(),"[]");assertEquals(1,s.size());assertEquals("AFTV",s.get(0).name);}
    @Test public void removedDefaultStaysRemoved(){FootballNewsSources.Source first=FootballNewsSources.DEFAULTS[0];assertTrue(FootballNewsSources.list(clubs("359"),Collections.singleton(first.key()),"[]").isEmpty());}
    @Test public void customSourcesAreValidatedDeduplicatedAndClubScoped(){
        String channel=FootballNewsSources.DEFAULTS[0].channel;
        String raw="[{\"club\":\"86\",\"channel\":\""+channel+"\",\"name\":\"Creator\"},{\"club\":\"86\",\"channel\":\""+channel+"\",\"name\":\"Updated\"},{\"club\":\"359\",\"channel\":\""+channel+"\",\"name\":\"Unfollowed\"},{\"club\":\"86\",\"channel\":\"bad\",\"name\":\"Invalid\"}]";
        List<FootballNewsSources.Source> result=FootballNewsSources.list(clubs("86"),Collections.emptySet(),raw);assertEquals(1,result.size());assertEquals("Updated",result.get(0).name);
    }
}
