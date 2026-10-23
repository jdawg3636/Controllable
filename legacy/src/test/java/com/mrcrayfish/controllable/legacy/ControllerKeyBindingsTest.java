package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.settings.KeyBinding;
import org.junit.Test;
import static org.junit.Assert.*;

public class ControllerKeyBindingsTest {
    private static final ControllerKeyBindings.PhysicalKeys RELEASED = new ControllerKeyBindings.PhysicalKeys() {
        @Override public boolean down(int code) { return false; }
    };
    @Test public void heldJumpTargetsVanillaBindingDespiteLaterModAliases() {
        KeyBinding jump = new KeyBinding("test.minecraft.jump",57);
        KeyBinding vehicleUp = new KeyBinding("test.galacticraft.vehicleUp",57);
        KeyBinding flansUp = new KeyBinding("test.flans.up",57);
        ControllerKeyBindings input = new ControllerKeyBindings();
        input.beginTick();input.hold(jump,true);input.apply(RELEASED);
        assertTrue(jump.pressed);assertEquals(1,jump.pressTime);
        assertFalse(vehicleUp.pressed);assertFalse(flansUp.pressed);
        input.beginTick();input.hold(jump,true);input.apply(RELEASED);
        assertEquals("holding is not a second press",1,jump.pressTime);
        input.beginTick();input.apply(RELEASED);assertFalse(jump.pressed);
    }
    @Test public void pulseAndReleaseDoNotMutateUnownedDuplicateBindings() {
        KeyBinding inventory = new KeyBinding("test.minecraft.inventory",18);
        KeyBinding vehicleExit = new KeyBinding("test.flans.exit",18);
        ControllerKeyBindings.pulse(inventory);
        assertEquals(1,inventory.pressTime);assertEquals(0,vehicleExit.pressTime);
        ControllerKeyBindings input = new ControllerKeyBindings();
        input.beginTick();input.hold(inventory,true);input.apply(RELEASED);
        vehicleExit.pressed=true;
        input.release(RELEASED);assertFalse(inventory.pressed);assertTrue(vehicleExit.pressed);
    }
    @Test public void releasePreservesARealKeyboardHold() {
        KeyBinding jump=new KeyBinding("test.minecraft.physicalJump",57);
        ControllerKeyBindings input=new ControllerKeyBindings();
        input.beginTick();input.hold(jump,true);input.apply(RELEASED);
        input.release(new ControllerKeyBindings.PhysicalKeys() {
            @Override public boolean down(int code) { return code==57; }
        });
        assertTrue(jump.pressed);
    }
}
