package com.mrcrayfish.controllable.legacy;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;

public class ControllerTest {
    @Rule public TemporaryFolder directory=new TemporaryFolder();
    @Test public void radialDeadZonePreservesDirectionAndRange() {
        assertArrayEquals(new float[]{0,0},ControllerState.stick(.1F,.1F,.18F),.0001F);
        assertArrayEquals(new float[]{1,0},ControllerState.stick(1,0,.18F),.0001F);
        float[] diagonal=ControllerState.stick(1,1,.18F);
        assertEquals(1,Math.sqrt(diagonal[0]*diagonal[0]+diagonal[1]*diagonal[1]),.0001);
        assertEquals(diagonal[0],diagonal[1],0);
        assertEquals(-1,ControllerState.normalize(Short.MIN_VALUE),0);
        assertEquals(1,ControllerState.normalize(Short.MAX_VALUE),0);
    }
    @Test public void mergeCombinesButtonsWithoutRotatingEitherStick() {
        ControllerState one=new ControllerState(),two=new ControllerState();
        one.connected=true;one.name="one";two.connected=true;two.name="two";
        one.axes[0]=.8F;one.axes[1]=.2F;two.axes[0]=-.1F;two.axes[1]=-.5F;
        one.axes[2]=.1F;two.axes[2]=.7F;two.axes[3]=-.3F;
        one.axes[4]=.4F;two.axes[4]=.8F;one.buttons[0]=true;two.buttons[1]=true;
        ControllerState merged=ControllerState.merge(one,two);
        assertTrue(merged.connected);assertTrue(merged.buttons[0]);assertTrue(merged.buttons[1]);
        assertEquals(.8F,merged.axes[0],0);assertEquals(.2F,merged.axes[1],0);
        assertEquals(.7F,merged.axes[2],0);assertEquals(-.3F,merged.axes[3],0);assertEquals(.8F,merged.axes[4],0);
    }
    @Test public void activeStickCannotBeCancelledByAnOppositeKeyboardFlag() {
        assertEquals(-.7F,ControllerState.movementAxis(1,-.7F),0);
        assertEquals(.7F,ControllerState.movementAxis(-1,.7F),0);
        assertEquals(-1,ControllerState.movementAxis(-1,0),0);
        assertEquals(1,ControllerState.movementAxis(1,0),0);
        assertEquals(0,ControllerState.movementAxis(0,0),0);
    }
    @Test public void settingsRoundTripBindingsAndRadial() throws Exception {
        Settings original=new Settings(directory.getRoot()); original.deadZone=.3F;
        original.bindings.put(Settings.Action.JUMP,3); original.radial[2]="settings"; original.controllerIndex=2; original.save();
        Settings copy=new Settings(directory.getRoot()); assertEquals(.3F,copy.deadZone,0);
        assertEquals(Integer.valueOf(3),copy.bindings.get(Settings.Action.JUMP)); assertEquals("settings",copy.radial[2]); assertEquals(2,copy.controllerIndex);
    }
    @Test public void malformedSettingsCannotProduceInvalidAxesOrBindings() throws Exception {
        File config=new File(directory.getRoot(),"config"); config.mkdirs();
        FileWriter file=new FileWriter(new File(config,"controllable.properties"));
        file.write("deadZone=NaN\nsensitivity=Infinity\ncontrollerIndex=-10\nbinding.JUMP=500\ntriggerThreshold=not-a-number\n");file.close();
        Settings settings=new Settings(directory.getRoot());
        assertEquals(.18F,settings.deadZone,0);assertEquals(180,settings.sensitivity,0);assertEquals(0,settings.controllerIndex);
        assertEquals(Integer.valueOf(17),settings.bindings.get(Settings.Action.JUMP)); assertEquals(.35F,settings.triggerThreshold,0);
    }
}
