package com.mrcrayfish.controllable.legacy;

/** A complete SDL gamepad snapshot, independent of Minecraft and native code. */
public final class ControllerState {
    public final float[] axes = new float[6];
    public final boolean[] buttons = new boolean[15];
    public boolean connected;
    public String name = "No controller";

    public static ControllerState merge(ControllerState first,ControllerState second) {
        ControllerState result=new ControllerState(); result.connected=first.connected||second.connected;
        result.name=first.connected?(second.connected?first.name+" + "+second.name:first.name):second.name;
        for(int i=0;i<15;i++)result.buttons[i]=first.buttons[i]||second.buttons[i];
        for(int stick=0;stick<4;stick+=2) {
            float[] source=first.axes[stick]*first.axes[stick]+first.axes[stick+1]*first.axes[stick+1]>=second.axes[stick]*second.axes[stick]+second.axes[stick+1]*second.axes[stick+1]?first.axes:second.axes;
            result.axes[stick]=source[stick];result.axes[stick+1]=source[stick+1];
        }
        result.axes[4]=Math.max(first.axes[4],second.axes[4]);result.axes[5]=Math.max(first.axes[5],second.axes[5]);
        return result;
    }

    /** A live stick owns its axis; stale modded keyboard flags must not cancel it. */
    public static float movementAxis(float keyboard, float controller) {
        return Math.max(-1, Math.min(1, controller != 0 ? controller : keyboard));
    }

    public static float normalize(short value) {
        return value < 0 ? value / 32768.0F : value / 32767.0F;
    }

    /** Radial dead zone preserves stick direction and full range at the edge. */
    public static float[] stick(float x, float y, float deadZone) {
        float length = (float) Math.sqrt(x * x + y * y);
        if (length <= deadZone) return new float[] {0, 0};
        float scaled = (Math.min(length, 1) - deadZone) / (1 - deadZone);
        return new float[] {x / length * scaled, y / length * scaled};
    }
}
