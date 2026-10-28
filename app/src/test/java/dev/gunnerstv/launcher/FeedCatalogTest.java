package dev.gunnerstv.launcher;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class FeedCatalogTest {
    private ContentFeedPolicy.Item item(String pkg,String id){ContentFeedPolicy.Item i=new ContentFeedPolicy.Item();i.pkg=pkg;i.title=id;i.intent="https://example.test/"+id;i.watchType=0;return i;}
    @Test public void busyProviderDoesNotConsumeAnotherProvidersSixSlots(){
        ContentFeedReader.Result feed=new ContentFeedReader.Result();
        for(int n=0;n<100;n++)feed.recommendations.add(item(FeedCatalog.APPLE,"a"+n));
        for(int n=0;n<6;n++)feed.recommendations.add(item(FeedCatalog.PRIME,"p"+n));
        assertEquals(6,FeedCatalog.row("prime_picks").select(feed).size());
        assertTrue(FeedCatalog.row("prime_resume").select(feed).isEmpty());
    }
    @Test public void emptyResumeRowsStayHiddenWithoutUsingRecommendations(){
        ContentFeedReader.Result feed=new ContentFeedReader.Result();feed.recommendations.add(item(FeedCatalog.YOUTUBE,"video"));
        assertEquals(Collections.singleton("youtube_picks"),FeedCatalog.available(feed));
    }
    @Test public void youtubeMusicNeedsExplicitMetadata(){
        ContentFeedPolicy.Item i=item(FeedCatalog.YOUTUBE,"Music news and playlist tutorial");
        i.type=10;assertFalse(ContentFeedPolicy.music(i)); // A generic playlist is not necessarily music.
        i.type=7;assertTrue(ContentFeedPolicy.music(i));
        i.type=4;i.genre="MUSIC";assertTrue(ContentFeedPolicy.music(i));
        i.genre="";i.channelTitle="Music";assertTrue(ContentFeedPolicy.music(i));
        i.channelTitle="Recommended";assertFalse(ContentFeedPolicy.music(i));
    }
    @Test public void youtubeMusicPackageAndSpotifyFeedsNeedNoGuessedTitles(){
        assertTrue(ContentFeedPolicy.music(item("com.google.android.youtube.tvmusic","a")));
        assertTrue(ContentFeedPolicy.music(item(FeedCatalog.SPOTIFY,"b")));
        ContentFeedReader.Result feed=new ContentFeedReader.Result();feed.recommendations.add(item("com.google.android.youtube.tvmusic","a"));
        assertEquals(Collections.singleton("youtube_music"),FeedCatalog.available(feed));
    }
    @Test public void distinctContentIdsSurviveSameDisplayTitle(){
        ContentFeedPolicy.Item one=item(FeedCatalog.YOUTUBE,"Live"),two=item(FeedCatalog.YOUTUBE,"Live");one.contentId="1";two.contentId="2";
        assertEquals(2,ContentFeedPolicy.recommendations(Arrays.asList(one,two),6).size());
        two.contentId="1";assertEquals(1,ContentFeedPolicy.recommendations(Arrays.asList(one,two),6).size());
    }
    @Test public void zeroLimitMeansNoItems(){assertTrue(ContentFeedPolicy.recommendations(Arrays.asList(item("a","a")),0).isEmpty());assertTrue(ContentFeedPolicy.continuing(Arrays.asList(item("a","a")),0).isEmpty());}
}
