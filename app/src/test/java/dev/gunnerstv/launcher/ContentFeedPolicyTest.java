package dev.gunnerstv.launcher;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ContentFeedPolicyTest {
    @Test public void onlyRequestedStreamingSourcesAreAccepted() {
        for(String pkg:new String[]{"com.google.android.youtube.tv","com.netflix.ninja","com.amazon.amazonvideo.livingroom","com.apple.atve.androidtv.appletv"})assertTrue(ContentFeedPolicy.supported(pkg));
        for(String pkg:new String[]{"tv.pluto.android","ar.tvplayer.tv","topic.election.sector","other.app"})assertFalse(ContentFeedPolicy.supported(pkg));
    }
    private ContentFeedPolicy.Item item(String pkg, String title, long engaged) {
        ContentFeedPolicy.Item item = new ContentFeedPolicy.Item();
        item.pkg = pkg; item.title = title; item.intent = "https://example.test/" + title;
        item.engaged = engaged; item.watchType = 0; return item;
    }
    @Test public void multipleProvidersGetVisibleSlotsEvenWhenOnePublishesHundreds() {
        ContentFeedPolicy.Item a = item("a", "a1", 0), b = item("a", "a2", 0), c = item("a", "a3", 0), d = item("b", "b1", 0);
        List<ContentFeedPolicy.Item> selected = ContentFeedPolicy.recommendations(Arrays.asList(a,b,c,d), 3);
        assertEquals(Arrays.asList(a,d,b), selected);
    }
    @Test public void duplicatesAndItemsWithoutPlayableLinksDoNotConsumeSlots() {
        ContentFeedPolicy.Item a = item("a", "same", 0), duplicate = item("a", "same", 0), missing = item("b", "missing", 0);
        missing.intent = "";
        assertEquals(1, ContentFeedPolicy.recommendations(Arrays.asList(a,duplicate,missing), 10).size());
    }
    @Test public void resumeSortsByActualEngagementAndExcludesWatchlistOnlyItems() {
        ContentFeedPolicy.Item old = item("a", "old", 10), recent = item("b", "recent", 20), saved = item("c", "saved", 30);
        saved.watchType = 3;
        assertEquals(Arrays.asList(recent,old), ContentFeedPolicy.continuing(Arrays.asList(old,recent,saved), 10));
    }
    @Test public void plutoLiveExcludesOnDemandTitlesAndOtherProviders() {
        ContentFeedPolicy.Item movie = item("tv.pluto.android", "Movie", 0);
        assertFalse(ContentFeedPolicy.plutoLive(movie, "On Demand"));
        assertTrue(ContentFeedPolicy.plutoLive(movie, "Featured Channels"));
        ContentFeedPolicy.Item other = item("other.provider", "Channel", 0);
        other.live = true;
        assertFalse(ContentFeedPolicy.plutoLive(other, "Live"));
    }
    @Test public void explicitPlutoLiveFlagWorksWithoutAChannelName() {
        ContentFeedPolicy.Item channel = item("tv.pluto.android", "Channel", 0);
        channel.live = true;
        assertTrue(ContentFeedPolicy.plutoLive(channel, ""));
    }
    @Test public void progressHandlesMissingDurationsAndOutOfRangePositions() {
        assertEquals(-1, ContentFeedPolicy.progress(100, 0));
        assertEquals(0, ContentFeedPolicy.progress(-10, 100));
        assertEquals(50, ContentFeedPolicy.progress(50, 100));
        assertEquals(100, ContentFeedPolicy.progress(Long.MAX_VALUE, 100));
    }
}
