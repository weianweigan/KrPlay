package com.krplay.protocol;
/** Distinct listeners: AirPlay's advertised port must not collide with the vehicle worker hub. */
public final class Ports {
    private Ports() {}
    public static final int AUDIO = 8083;
    public static final int PROBE = 8084;
    public static final int AIRPLAY = 7100;
    public static final int REVERSE_WORKERS = 7000;
}
