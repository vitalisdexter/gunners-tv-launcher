package dev.gunnerstv.launcher;

/** The visible window in a finite title row. Manual focus stops at the actual feed boundaries. */
final class CarouselWindow {
    private CarouselWindow() {}

    static int clamp(int start, int count, int visible) {
        return Math.max(0, Math.min(start, Math.max(0, count - visible)));
    }

    static int reveal(int start, int selected, int count, int visible) {
        start = clamp(start, count, visible);
        if (selected < 0 || selected >= count) return start;
        if (selected < start) start = selected;
        else if (selected >= start + visible) start = selected - visible + 1;
        return clamp(start, count, visible);
    }

    static int advance(int start, int count, int visible) {
        int last = Math.max(0, count - visible);
        return last == 0 ? 0 : (clamp(start, count, visible) + 1) % (last + 1);
    }
}
