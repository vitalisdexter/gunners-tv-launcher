package dev.gunnerstv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaSessionPolicyTest {
    @Test public void emptyDisplayTitleFallsThroughToRealTrackTitle(){assertEquals("A track",MediaSessionPolicy.first("  ","A track",null));}
    @Test public void playingYoutubeProxyOutranksPausedMusicSession(){assertTrue(MediaSessionPolicy.rank(true,false,false,true,true)>MediaSessionPolicy.rank(false,false,true,true,true));}
    @Test public void bufferingAndMissingInitialMetadataRemainEligible(){assertTrue(MediaSessionPolicy.rank(false,true,false,false,true)>0);assertEquals(-1,MediaSessionPolicy.rank(false,false,false,false,true));}
    @Test public void toggleOnlyProvidersUseMediaButtonEvents(){assertEquals(MediaSessionPolicy.Command.TOGGLE,MediaSessionPolicy.command(true,false,false,true));assertEquals(MediaSessionPolicy.Command.TOGGLE,MediaSessionPolicy.command(false,false,false,true));}
    @Test public void explicitControlsAndUnavailableActionsAreHonoured(){assertEquals(MediaSessionPolicy.Command.PAUSE,MediaSessionPolicy.command(true,false,true,false));assertEquals(MediaSessionPolicy.Command.PLAY,MediaSessionPolicy.command(false,true,false,false));assertEquals(MediaSessionPolicy.Command.NONE,MediaSessionPolicy.command(false,false,false,false));}
    @Test public void currentlyPlayingWithoutMetadataOutranksBufferingWithMetadata(){assertTrue(MediaSessionPolicy.rank(true,false,false,false,false)>MediaSessionPolicy.rank(false,true,false,true,true));}
    @Test public void wrongStateDoesNotInventTheOppositeControl(){assertEquals(MediaSessionPolicy.Command.NONE,MediaSessionPolicy.command(true,true,false,false));assertEquals(MediaSessionPolicy.Command.NONE,MediaSessionPolicy.command(false,false,true,false));}
}
