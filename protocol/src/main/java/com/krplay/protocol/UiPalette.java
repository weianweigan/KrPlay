package com.krplay.protocol;
/** Shared Material palette for KrPlay and KrPlay-Client, supporting light (default) and dark styles. */
public final class UiPalette {
    private UiPalette() {}

    // Dark palette (retained for backward compatibility and dark mode)
    public static final int BACKGROUND = 0xff11161a, SURFACE = 0xff1c2428, BORDER = 0xff414941;
    public static final int ACCENT = 0xffb4d9b1, ON_ACCENT = 0xff20391f, TEXT = 0xffe2e8e3, MUTED = 0xffc1c9be, WARNING = 0xffefbe90;
    public static final int SELECTED = 0xff364b35;

    // Light palette (Default style)
    public static final int LIGHT_BACKGROUND = 0xfff4f6f3, LIGHT_SURFACE = 0xffffffff, LIGHT_BORDER = 0xffc4cec2;
    public static final int LIGHT_ACCENT = 0xff3b6939, LIGHT_ON_ACCENT = 0xffffffff, LIGHT_TEXT = 0xff171d17, LIGHT_MUTED = 0xff546253, LIGHT_WARNING = 0xff994d00;
    public static final int LIGHT_SELECTED = 0xffd3e9cf;

    // Overlay surface colors (translucent for HUD, dock, waiting panel)
    public static final int DARK_OVERLAY_SURFACE = 0xf01c2428;
    public static final int DARK_OVERLAY_CARD = 0xee1c2428;
    public static final int DARK_OVERLAY_BADGE = 0xe61c2428;

    public static final int LIGHT_OVERLAY_SURFACE = 0xf2ffffff;
    public static final int LIGHT_OVERLAY_CARD = 0xf5ffffff;
    public static final int LIGHT_OVERLAY_BADGE = 0xebf2f5f1;

    // Android Configuration.UI_MODE_NIGHT_MASK = 0x30, UI_MODE_NIGHT_YES = 0x20
    public static boolean isDark(int uiMode) {
        return (uiMode & 0x30) == 0x20;
    }

    public static int background(boolean dark) { return dark ? BACKGROUND : LIGHT_BACKGROUND; }
    public static int surface(boolean dark) { return dark ? SURFACE : LIGHT_SURFACE; }
    public static int border(boolean dark) { return dark ? BORDER : LIGHT_BORDER; }
    public static int accent(boolean dark) { return dark ? ACCENT : LIGHT_ACCENT; }
    public static int onAccent(boolean dark) { return dark ? ON_ACCENT : LIGHT_ON_ACCENT; }
    public static int text(boolean dark) { return dark ? TEXT : LIGHT_TEXT; }
    public static int muted(boolean dark) { return dark ? MUTED : LIGHT_MUTED; }
    public static int warning(boolean dark) { return dark ? WARNING : LIGHT_WARNING; }
    public static int selected(boolean dark) { return dark ? SELECTED : LIGHT_SELECTED; }

    public static int overlaySurface(boolean dark) { return dark ? DARK_OVERLAY_SURFACE : LIGHT_OVERLAY_SURFACE; }
    public static int overlayCard(boolean dark) { return dark ? DARK_OVERLAY_CARD : LIGHT_OVERLAY_CARD; }
    public static int overlayBadge(boolean dark) { return dark ? DARK_OVERLAY_BADGE : LIGHT_OVERLAY_BADGE; }
}
