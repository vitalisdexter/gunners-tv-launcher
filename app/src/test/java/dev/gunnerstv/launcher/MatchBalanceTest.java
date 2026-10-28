package dev.gunnerstv.launcher;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MatchBalanceTest {
    private ArsenalMatch fixture(String home,String away,String h,String a){ArsenalMatch m=new ArsenalMatch();m.homeId=home;m.awayId=away;m.homeScore=h;m.awayScore=a;m.state="post";return m;}
    @Test public void countsVictoriesRelativeToCurrentTeamsAcrossBothVenues(){
        ArsenalMatch current=fixture("A","B","","");
        assertArrayEquals(new int[]{2,1,1},MatchDetail.balance(current,Arrays.asList(fixture("A","B","2","0"),fixture("B","A","0","1"),fixture("B","A","3","1"),fixture("A","B","1","1"))));
    }
    @Test public void reversingTheCurrentFixtureReversesWinTotals(){
        List<ArsenalMatch> history=Arrays.asList(fixture("A","B","2","0"),fixture("B","A","0","1"),fixture("A","B","1","1"));
        assertArrayEquals(new int[]{0,1,2},MatchDetail.balance(fixture("B","A","",""),history));
    }
    @Test public void excludesUnrelatedUnfinishedAndInvalidResults(){
        ArsenalMatch pre=fixture("A","B","2","0");pre.state="pre";
        assertArrayEquals(new int[]{0,0,0},MatchDetail.balance(fixture("A","B","",""),Arrays.asList(pre,fixture("A","C","2","0"),fixture("A","B","","1"),fixture("B","A","-1","0"),fixture("B","A","N/A","1"))));
    }
    @Test public void missingHistoryHasNoInventedResults(){
        assertArrayEquals(new int[]{0,0,0},MatchDetail.balance(fixture("A","B","",""),Collections.emptyList()));
    }
}
