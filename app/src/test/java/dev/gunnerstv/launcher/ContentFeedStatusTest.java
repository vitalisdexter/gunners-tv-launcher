package dev.gunnerstv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContentFeedStatusTest {
    @Test public void missingPermissionAndEmptyRowsStillExposeSetupAction(){ContentFeedStatus s=new ContentFeedStatus();assertTrue(s.needsSetup(true,false));assertFalse(s.needsSetup(false,false));}
    @Test public void successfulPermissionWithNoPublishedDataIsNotReportedAsDenied(){ContentFeedStatus s=new ContentFeedStatus();s.listingsGranted=true;s.readTables=2;assertTrue(s.needsSetup(true,false));assertTrue(s.message().contains("no readable programs"));assertFalse(s.message().contains("required"));}
    @Test public void firmwareDenialDespitePermissionIsDifferentFromMissingAccess(){ContentFeedStatus s=new ContentFeedStatus();s.deniedTables=1;assertTrue(s.message().contains("required"));s.listingsGranted=true;assertTrue(s.message().contains("despite listings access"));}
    @Test public void populatedAccessibleFeedsHideSetupButPartialAccessDoesNot(){ContentFeedStatus s=new ContentFeedStatus();s.listingsGranted=true;assertFalse(s.needsSetup(true,true));s.failedTables=1;assertTrue(s.needsSetup(true,true));}
}
