package dev.gunnerstv.launcher;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class ArsenalMatchTest {
    private ArsenalMatch match(String id,String state,long kickoff){ArsenalMatch m=new ArsenalMatch();m.id=id;m.state=state;m.kickoff=kickoff;return m;}
    @Test public void liveMatchTakesPriorityOverUpcomingFixture(){ArsenalMatch live=match("1","in",1000),next=match("2","pre",10000);assertSame(live,ArsenalMatch.featured(Arrays.asList(next,live),2000));}
    @Test public void nextFixtureTakesPriorityOverLastResult(){ArsenalMatch last=match("1","post",1000),next=match("2","pre",10000);assertSame(next,ArsenalMatch.featured(Arrays.asList(last,next),2000));}
    @Test public void staleLiveFlagCannotPinAnOldMatch(){ArsenalMatch stale=match("1","in",0),next=match("2","pre",1000000000);assertSame(next,ArsenalMatch.featured(Arrays.asList(stale,next),900000000));}
    @Test public void mostRecentCompletedMatchIsFallback(){ArsenalMatch a=match("1","post",1000),b=match("2","post",2000);assertSame(b,ArsenalMatch.featured(Arrays.asList(b,a),3000));}
    @Test public void emptyDataHasNoFabricatedFixture(){assertNull(ArsenalMatch.featured(Arrays.asList(),3000));}
    @Test public void onlyNearKickoffMatchesPollEveryMinute(){ArsenalMatch m=match("1","pre",1000);assertEquals(60000,ArsenalMatch.interval(m,2000));m.state="post";assertEquals(1800000,ArsenalMatch.interval(m,2000));}
}
