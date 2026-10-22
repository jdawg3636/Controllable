package com.mrcrayfish.controllable.legacy;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;
import net.minecraft.client.settings.KeyBinding;
import java.lang.reflect.Field;
import java.util.*;

/** Forge 1.5 key handlers poll raw keyboard state, so deliver their documented callbacks too. */
public final class ModKeyBridge {
    private static final Map<KeyHandler,EnumMap<TickType,boolean[]>> previous=new IdentityHashMap<KeyHandler,EnumMap<TickType,boolean[]>>();
    private static final Field HANDLERS=field(KeyBindingRegistry.class,"keyHandlers");
    private static final Field REPEATS=field(KeyHandler.class,"repeatings");
    private static Field field(Class<?> owner,String name) {
        try { Field f=owner.getDeclaredField(name);f.setAccessible(true);return f; }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    public static void dispatch(ClientProxy client,EnumSet<TickType> types,boolean end) {
        try {
            Set<KeyHandler> handlers=(Set<KeyHandler>)HANDLERS.get(KeyBindingRegistry.instance());
            for(KeyHandler handler:handlers) {
                KeyBinding[] keys=handler.getKeyBindings();
                boolean[] repeats=(boolean[])REPEATS.get(handler);
                if(repeats==null)continue; // Dummy registrations use ordinary KeyBinding state.
                EnumMap<TickType,boolean[]> states=previous.get(handler);
                if(states==null) { states=new EnumMap<TickType,boolean[]>(TickType.class);previous.put(handler,states); }
                for(TickType type:types) {
                    if(!handler.ticks().contains(type))continue;
                    boolean[] was=states.get(type);
                    if(was==null) { was=new boolean[keys.length];states.put(type,was); }
                    for(int i=0;i<keys.length;i++) {
                        boolean down=client.customHeld(keys[i].keyDescription);
                        if(down!=was[i] || down&&repeats[i]) {
                            if(down)handler.keyDown(EnumSet.of(type),keys[i],end,down!=was[i]);
                            else handler.keyUp(EnumSet.of(type),keys[i],end);
                        }
                        if(end)was[i]=down;
                    }
                }
            }
        } catch(IllegalAccessException e) { throw new RuntimeException(e); }
    }
}
