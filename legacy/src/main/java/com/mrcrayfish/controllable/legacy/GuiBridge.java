package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.gui.GuiScreen;
import java.lang.reflect.*;

/** Virtual dispatch is retained, including overridden handlers in modded screens. */
public final class GuiBridge {
    private static Method method(String[] names, Class<?>... types) {
        for(String name:names) try { Method m=GuiScreen.class.getDeclaredMethod(name,types); m.setAccessible(true); return m; }
        catch(NoSuchMethodException ignored) { }
        throw new IllegalStateException("Cannot resolve GuiScreen input handler " + names[0]);
    }
    private static final Method CLICK=method(new String[]{"mouseClicked","func_73864_a","a"},int.class,int.class,int.class);
    private static final Method UP=method(new String[]{"mouseMovedOrUp","func_73879_b","b"},int.class,int.class,int.class);
    private static final Method DRAG=method(new String[]{"func_85041_a","a"},int.class,int.class,int.class,long.class);
    private static final Method KEY=method(new String[]{"keyTyped","func_73869_a","a"},char.class,int.class);
    private static void invoke(Method method, GuiScreen screen,Object... args) {
        try { method.invoke(screen,args); }
        catch(InvocationTargetException e) { throw new RuntimeException("Screen input failed",e.getCause()); }
        catch(IllegalAccessException e) { throw new RuntimeException(e); }
    }
    public static void click(GuiScreen screen,int x,int y,int button) { invoke(CLICK,screen,x,y,button); }
    public static void up(GuiScreen screen,int x,int y,int button) { invoke(UP,screen,x,y,button); }
    public static void drag(GuiScreen screen,int x,int y,int button,long time) { invoke(DRAG,screen,x,y,button,time); }
    public static void key(GuiScreen screen,char character,int key) { invoke(KEY,screen,character,key); }
    public static Object field(Object instance,Class<?> owner,String... names) {
        for(String name:names) try { Field f=owner.getDeclaredField(name); f.setAccessible(true); return f.get(instance); }
        catch(NoSuchFieldException ignored) { } catch(IllegalAccessException e) { throw new RuntimeException(e); }
        throw new IllegalStateException("Cannot resolve " + names[0]);
    }
}
