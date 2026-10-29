package dev.gunnerstv.launcher;

import org.junit.Test;
import java.time.Instant;
import static org.junit.Assert.*;

public class YouTubeFeedsTest {
    private static final String CID="UCBTy8j2cPy6zw68godcE7MQ";
    private final long now=Instant.parse("2026-10-29T20:00:00Z").toEpochMilli();
    private String entry(String id,String cid,String date,String title){return "<entry><yt:videoId>"+id+"</yt:videoId><yt:channelId>"+cid+"</yt:channelId><title>"+title+"</title><published>"+date+"</published><updated>2026-10-29T19:00:00Z</updated></entry>";}
    private String feed(String cid,String entries){return "<feed xmlns='http://www.w3.org/2005/Atom' xmlns:yt='http://www.youtube.com/xml/schemas/2015'><yt:channelId>"+cid+"</yt:channelId><title>Creator</title>"+entries+"</feed>";}
    @Test public void publicFeedSupportsBothChannelIdRepresentations()throws Exception{assertEquals(1,YouTubeFeeds.parse(feed(CID.substring(2),entry("abcdefghijk",CID,"2026-10-29T19:00:00Z","Real title")),CID,now).videos.size());}
    @Test public void onlyRecentPublishedUploadsAppearNotOldEditedOrFutureItems()throws Exception{
        String rows=entry("abcdefghijk",CID,"2026-10-29T19:00:00Z","Recent")+entry("abcdefghijl",CID,"2026-10-01T19:00:00Z","Old edited today")+entry("abcdefghijm",CID,"2026-10-30T19:00:00Z","Future");assertEquals(1,YouTubeFeeds.parse(feed(CID,rows),CID,now).videos.size());
    }
    @Test public void deduplicatesAndOrdersByUploadDate()throws Exception{String old=entry("abcdefghijk",CID,"2026-10-28T19:00:00Z","Older"),recent=entry("abcdefghijl",CID,"2026-10-29T19:00:00Z","Newer");YouTubeFeeds.Feed parsed=YouTubeFeeds.parse(feed(CID,old+recent+recent),CID,now);assertEquals(2,parsed.videos.size());assertEquals("Newer",parsed.videos.get(0).title);}
    @Test public void wrongChannelAndBadVideoIdsAreRejected()throws Exception{String other="UCMmVPVb0BwSIOWVeDwlPocQ";try{YouTubeFeeds.parse(feed(other,""),CID,now);fail();}catch(java.io.IOException expected){}assertTrue(YouTubeFeeds.parse(feed(CID,entry("../evil",CID,"2026-10-29T19:00:00Z","Bad")+entry("abcdefghijk",other,"2026-10-29T19:00:00Z","Other")),CID,now).videos.isEmpty());}
    @Test public void externalEntityDeclarationsAreNeverParsed()throws Exception{try{YouTubeFeeds.parse("<!DOCTYPE feed [<!ENTITY x SYSTEM 'file:///does-not-exist'>]>"+feed(CID,""),CID,now);fail();}catch(java.io.IOException expected){}}
    @Test public void acceptsOnlyKnownPublicChannelForms(){assertEquals(CID,YouTubeFeeds.directId(CID));assertEquals(CID,YouTubeFeeds.directId("https://www.youtube.com/channel/"+CID));assertEquals("",YouTubeFeeds.directId("https://www.youtube.com/@AFTVmedia"));for(String bad:new String[]{"https://youtube.com.evil.test/channel/"+CID,"https://user:pass@www.youtube.com/channel/"+CID,"http://www.youtube.com/channel/"+CID,"file:///channel/"+CID})try{YouTubeFeeds.directId(bad);fail(bad);}catch(IllegalArgumentException expected){}}
    @Test public void rssDiscoveryRequiresMatchingCanonicalAndFeedOwner(){String html="<link href='https://www.youtube.com/channel/"+CID+"' rel='canonical'><link title='RSS' href='https://www.youtube.com/feeds/videos.xml?channel_id="+CID+"' type='application/rss+xml' rel='alternate'>";assertEquals(CID,YouTubeFeeds.discoveryId(html));try{YouTubeFeeds.discoveryId(html.replace("channel_id="+CID,"channel_id=UCMmVPVb0BwSIOWVeDwlPocQ"));fail();}catch(IllegalArgumentException expected){}}
    @Test public void videoLinksCannotBeRedirectedOutsideYoutube(){assertEquals("https://www.youtube.com/watch?v=abcdefghijk",YouTubeFeeds.watch("abcdefghijk"));try{YouTubeFeeds.watch("javascript:alert(1)");fail();}catch(IllegalArgumentException expected){}}
    @Test public void refreshIsCachedAndManualRefreshCannotSpamEndpoints(){long now=2_000_000_000L;assertTrue(FootballNewsSource.fetchDue(now,0,0,false));assertFalse(FootballNewsSource.fetchDue(now,now-600000,now-600000,false));assertFalse(FootballNewsSource.fetchDue(now,0,now-1000,true));assertTrue(FootballNewsSource.fetchDue(now,now-600000,now-600000,true));}
}
