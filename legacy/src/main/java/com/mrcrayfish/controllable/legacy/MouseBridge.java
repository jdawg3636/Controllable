package com.mrcrayfish.controllable.legacy;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/** LWJGL 2 has no public event injection API. Scope virtual held buttons to one render. */
public final class MouseBridge {
    private static final Field BUTTONS=field("buttons"), EVENTS=field("readBuffer");
    private static byte physicalLeft,physicalRight;
    private static boolean overridden;
    private static Field field(String name) {
        try { Field f=Mouse.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch(Exception e) { throw new IllegalStateException("Unsupported LWJGL 2 Mouse API",e); }
    }
    public static void hold(boolean left,boolean right) {
        restore();
        try {
            ByteBuffer buttons=(ByteBuffer)BUTTONS.get(null);
            physicalLeft=buttons.get(0);physicalRight=buttons.get(1);
            if(left)buttons.put(0,(byte)1); if(right)buttons.put(1,(byte)1);
            overridden=true;
        } catch(IllegalAccessException e) { throw new RuntimeException(e); }
    }
    public static void restore() {
        if(!overridden)return;
        try { ByteBuffer buttons=(ByteBuffer)BUTTONS.get(null); buttons.put(0,physicalLeft);buttons.put(1,physicalRight);overridden=false; }
        catch(IllegalAccessException e) { throw new RuntimeException(e); }
    }
    public static void scroll(int x,int y,int delta) {
        try {
            ByteBuffer pending=(ByteBuffer)EVENTS.get(null);
            ByteBuffer combined=BufferUtils.createByteBuffer(pending.remaining()+Mouse.EVENT_SIZE);
            combined.put(pending.duplicate());
            combined.put((byte)-1).put((byte)0).putInt(x).putInt(y).putInt(delta).putLong(System.nanoTime());
            combined.flip(); EVENTS.set(null,combined);
        } catch(IllegalAccessException e) { throw new RuntimeException(e); }
    }
}
