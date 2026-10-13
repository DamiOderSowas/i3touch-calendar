package de.aufgekaut.i3kalender;

final class SwipeNavigation {
    private SwipeNavigation() { }
    static int direction(float dx, float dy, float threshold) {
        if (Math.abs(dx) < threshold || Math.abs(dx) <= Math.abs(dy) * 1.5f) return 0;
        return dx < 0 ? 1 : -1;
    }
}
