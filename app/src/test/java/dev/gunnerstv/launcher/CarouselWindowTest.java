package dev.gunnerstv.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CarouselWindowTest {
    @Test public void sixTitlesAdvanceWithoutAddingFakeTitles() {
        int start = 0;
        int[] expected = {1, 2, 3, 0, 1};
        for (int next : expected) {
            start = CarouselWindow.advance(start, 6, 3);
            assertEquals(next, start);
        }
    }

    @Test public void shortFeedsStayStillAndWithinTheirRealTitles() {
        for (int count = 0; count <= 3; count++) {
            assertEquals(0, CarouselWindow.advance(4, count, 3));
            assertEquals(0, CarouselWindow.clamp(4, count, 3));
        }
    }

    @Test public void fourthAndSixthTitlesScrollIntoViewOnManualFocus() {
        assertEquals(1, CarouselWindow.reveal(0, 3, 6, 3));
        assertEquals(3, CarouselWindow.reveal(1, 5, 6, 3));
        assertEquals(2, CarouselWindow.reveal(3, 2, 6, 3));
    }

    @Test public void returningToAnAlreadyVisibleTitleKeepsTheViewport() {
        assertEquals(2, CarouselWindow.reveal(2, 3, 6, 3));
    }

    @Test public void feedShrinkClampsTheSavedViewport() {
        assertEquals(1, CarouselWindow.clamp(3, 4, 3));
        assertEquals(0, CarouselWindow.clamp(3, 2, 3));
    }

    @Test public void missingSelectedTitleDoesNotMoveTheViewport() {
        assertEquals(2, CarouselWindow.reveal(2, -1, 6, 3));
        assertEquals(2, CarouselWindow.reveal(2, 9, 6, 3));
    }
}
