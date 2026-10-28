package com.krplay.protocol;

/** PCM frame budgets, never packet-age deadlines for music. */
public final class AudioPolicy {
    private AudioPolicy() {}
    public static final int DEFAULT_BUFFER_MS=300;
    public static final int MUSIC=0, PHONE=1, ASSISTANT=2, NAVIGATION=3;
    public static int bufferMs(int value){return value==500||value==1000?value:DEFAULT_BUFFER_MS;}
    public static int capacityMs(int purpose,int buffer){return purpose==MUSIC?Math.max(1000,bufferMs(buffer)+500):240;}
    public static int startMs(int purpose,int buffer){return purpose==MUSIC?bufferMs(buffer):40;}
    public static int trackMs(int purpose,int buffer){return purpose==MUSIC?bufferMs(buffer)+200:120;}
    public static int purpose(String audioType,int type){
        if("media".equalsIgnoreCase(audioType))return MUSIC;
        if("telephony".equalsIgnoreCase(audioType))return PHONE;
        if("speechrecognition".equalsIgnoreCase(audioType))return ASSISTANT;
        if("default".equalsIgnoreCase(audioType)||"alert".equalsIgnoreCase(audioType)||"compatibility".equalsIgnoreCase(audioType))return NAVIGATION;
        return type==102?MUSIC:NAVIGATION;
    }
    public static int frames(int rate,int millis){return (int)((long)rate*millis/1000);}
}
