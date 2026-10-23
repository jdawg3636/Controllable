package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.settings.KeyBinding;
import java.util.*;

/** Forge 1.5's key-code hash retains only one binding when mods share a key. */
final class ControllerKeyBindings {
    interface PhysicalKeys { boolean down(int keyCode); }
    private final Set<KeyBinding> desired = identitySet();
    private final Set<KeyBinding> owned = identitySet();

    private static Set<KeyBinding> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<KeyBinding, Boolean>());
    }
    void beginTick() { desired.clear(); }
    void hold(KeyBinding binding, boolean down) { if (down) desired.add(binding); }
    static void pulse(KeyBinding binding) { binding.pressTime++; }
    void apply(PhysicalKeys physical) {
        Iterator<KeyBinding> iterator = owned.iterator();
        while (iterator.hasNext()) {
            KeyBinding binding = iterator.next();
            if (!desired.contains(binding)) {
                binding.pressed = physical.down(binding.keyCode);
                iterator.remove();
            }
        }
        for (KeyBinding binding : desired) {
            if (owned.add(binding)) pulse(binding);
            binding.pressed = true;
        }
    }
    void release(PhysicalKeys physical) {
        for (KeyBinding binding : owned) binding.pressed = physical.down(binding.keyCode);
        owned.clear(); desired.clear();
    }
}
