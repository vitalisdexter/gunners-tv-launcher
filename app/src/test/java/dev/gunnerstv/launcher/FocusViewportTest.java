package dev.gunnerstv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public class FocusViewportTest {
    @Test public void lastOfManyRowsIsRevealedWithHalo(){assertEquals(802,FocusViewport.target(0,340,1200,1064,1118,24));}
    @Test public void movingBackUpRevealsFirstRow(){assertEquals(0,FocusViewport.target(800,340,1140,10,64,10));}
    @Test public void visibleRowDoesNotJump(){assertEquals(100,FocusViewport.target(100,340,1140,150,204,10));}
    @Test public void shortListAndOversizedRowsStayBounded(){
        assertEquals(0,FocusViewport.target(900,340,120,10,64,10));
        assertEquals(90,FocusViewport.target(0,100,500,100,300,10));
    }
    @Test public void bottomClampAndReorderUseNewPosition(){
        assertEquals(800,FocusViewport.target(0,340,1140,1080,1134,10));
        assertEquals(190,FocusViewport.target(800,340,1140,200,254,10));
    }
}
