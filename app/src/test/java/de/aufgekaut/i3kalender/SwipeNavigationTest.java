package de.aufgekaut.i3kalender;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SwipeNavigationTest {
    @Test public void horizontalSwipesChangePeriod() {
        assertEquals(1, SwipeNavigation.direction(-120, 8, 70));
        assertEquals(-1, SwipeNavigation.direction(120, -8, 70));
    }
    @Test public void tapsAndVerticalScrollingDoNotChangePeriod() {
        assertEquals(0, SwipeNavigation.direction(12, 0, 70));
        assertEquals(0, SwipeNavigation.direction(80, 150, 70));
        assertEquals(0, SwipeNavigation.direction(90, 90, 70));
    }
}
