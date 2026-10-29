package dev.gunnerstv.launcher;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AppArrangementTest {
    private AppArrangement grid(String selected){List<String> all=Arrays.asList("a","b","c","d","e","f","g","h");return new AppArrangement(all,all,selected,6);}
    @Test public void leftRightRetainsSelectedApp(){AppArrangement a=grid("b");assertTrue(a.move(1,0));assertEquals("b",a.items().get(2));assertEquals("c",a.items().get(1));assertTrue(a.move(-1,0));assertEquals(1,a.index());}
    @Test public void upDownMovesAcrossGridRows(){AppArrangement a=grid("b");assertTrue(a.move(0,1));assertEquals(7,a.index());assertEquals("h",a.items().get(1));assertTrue(a.move(0,-1));assertEquals(1,a.index());}
    @Test public void horizontalEdgesDoNotWrap(){AppArrangement a=grid("f");assertFalse(a.move(1,0));AppArrangement b=grid("g");assertFalse(b.move(-1,0));assertEquals(6,b.index());}
    @Test public void incompleteLastRowAndVerticalEdgesStayPut(){AppArrangement a=grid("c");assertFalse(a.move(0,1));assertFalse(a.move(0,-1));AppArrangement b=grid("h");assertFalse(b.move(1,0));assertFalse(b.move(0,1));}
    @Test public void hiddenOrUnavailablePackagesKeepTheirSlots(){List<String> all=Arrays.asList("a","hidden","b","removed","c");AppArrangement a=new AppArrangement(all,Arrays.asList("a","b","c"),"a",6);assertTrue(a.move(1,0));assertEquals(Arrays.asList("b","hidden","a","removed","c"),a.order());}
    @Test public void cancellingDraftLeavesInputsUntouched(){List<String> original=new ArrayList<>(Arrays.asList("a","b")),visible=new ArrayList<>(original);AppArrangement a=new AppArrangement(original,visible,"a",6);a.move(1,0);assertEquals(Arrays.asList("a","b"),original);assertEquals(original,visible);List<String> saved=a.order();saved.clear();assertEquals(2,a.order().size());}
    @Test public void homeDraftCannotMoveIntoUndisplayedApps(){List<String> all=Arrays.asList("a","b","c","d","e","f","g");AppArrangement a=new AppArrangement(all,all.subList(0,6),"a",6);assertFalse(a.move(0,1));assertEquals(all,a.order());}
    @Test public void invalidDraftAndDiagonalInputAreRejected(){AppArrangement a=grid("a");assertFalse(a.move(1,1));try{new AppArrangement(Arrays.asList("a","a"),Arrays.asList("a"),"a",6);fail();}catch(IllegalArgumentException expected){}}
}
