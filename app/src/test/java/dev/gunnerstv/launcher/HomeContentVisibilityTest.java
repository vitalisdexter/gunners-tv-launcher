package dev.gunnerstv.launcher;

import android.content.SharedPreferences;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class HomeContentVisibilityTest {
    @After public void clear(){FeedCatalog.latest=null;}
    private ContentFeedReader.Result feed(){ContentFeedReader.Result r=new ContentFeedReader.Result();ContentFeedPolicy.Item i=new ContentFeedPolicy.Item();i.pkg=FeedCatalog.YOUTUBE;i.title="Real shared title";i.intent="https://www.youtube.com/watch?v=abcdefghijk";r.recommendations.add(i);return r;}
    @Test public void realTitlesWinOverEmptyStartupVisibilityCache(){
        FeedCatalog.latest=feed();Prefs p=new Prefs();p.values.put("available_feeds",Collections.emptySet());List<ArrayList<String>> rows=WidgetBoard.section(p,false,false,false);assertTrue(rows.stream().anyMatch(r->r.contains("youtube_picks")));
    }
    @Test public void staleCachedRowsAreNotKeptWhenCurrentFeedIsEmpty(){FeedCatalog.latest=new ContentFeedReader.Result();Prefs p=new Prefs();p.values.put("available_feeds",Collections.singleton("youtube_picks"));assertFalse(WidgetBoard.section(p,false,false,false).stream().anyMatch(r->r.contains("youtube_picks")));}
    @Test public void explicitHiddenRowIsRespectedEvenWithRealTitles(){FeedCatalog.latest=feed();Prefs p=new Prefs();p.values.put("widget_off","youtube_picks");assertFalse(WidgetBoard.section(p,false,false,false).stream().anyMatch(r->r.contains("youtube_picks")));}
    private static final class Prefs implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>();
        public Map<String,?> getAll(){return values;}public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){Object v=values.get(k);return v instanceof Set?(Set<String>)v:d;}
        public int getInt(String k,int d){return d;}public long getLong(String k,long d){return d;}public float getFloat(String k,float d){return d;}
        public boolean getBoolean(String k,boolean d){return "content_status_needed".equals(k)?false:d;}public boolean contains(String k){return values.containsKey(k);}
        public Editor edit(){throw new UnsupportedOperationException();}public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    }
}
