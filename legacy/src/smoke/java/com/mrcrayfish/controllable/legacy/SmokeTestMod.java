package com.mrcrayfish.controllable.legacy;

import com.sun.jna.*;
import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.world.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.opengl.Display;

@Mod(modid="controllable_smoke",name="Controllable integration test",version="1")
public final class SmokeTestMod implements ITickHandler {
    interface Virtual extends Library {
        int SDL_JoystickAttachVirtual(int type,int axes,int buttons,int hats);
        Pointer SDL_JoystickOpen(int index);
        int SDL_JoystickSetVirtualAxis(Pointer joystick,int axis,short value);
        int SDL_JoystickSetVirtualButton(Pointer joystick,int button,byte value);
        void SDL_JoystickClose(Pointer joystick);
        int SDL_JoystickDetachVirtual(int index);
    }
    interface X11 extends Library {
        Pointer XOpenDisplay(String name);
        int XMapRaised(Pointer display,NativeLong window);
        int XSetInputFocus(Pointer display,NativeLong window,int revert,NativeLong time);
        int XSync(Pointer display,boolean discard);
        int XCloseDisplay(Pointer display);
    }
    private Minecraft mc; private Virtual sdl; private Pointer joystick;
    private Pointer secondJoystick;private int primaryIndex,secondIndex,modStart,modEnd,modUp;
    private int ticks;private int stage;private long start=System.currentTimeMillis();
    private SmokeListScreen listScreen;
    private float yaw;private double x,z;
    @Mod.Init public void init(FMLInitializationEvent event) {
        TickRegistry.registerTickHandler(this,Side.CLIENT);
        final net.minecraft.client.settings.KeyBinding key=new net.minecraft.client.settings.KeyBinding("key.controllable_smoke",65);
        cpw.mods.fml.client.registry.KeyBindingRegistry.registerKeyBinding(new cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler(new net.minecraft.client.settings.KeyBinding[]{key},new boolean[]{false}) {
            @Override public void keyDown(EnumSet<TickType> type,net.minecraft.client.settings.KeyBinding binding,boolean end,boolean repeat){if(end)modEnd++;else modStart++;}
            @Override public void keyUp(EnumSet<TickType> type,net.minecraft.client.settings.KeyBinding binding,boolean end){if(end)modUp++;}
            @Override public EnumSet<TickType> ticks(){return EnumSet.of(TickType.CLIENT);}
            @Override public String getLabel(){return "Controller smoke key";}
        });
    }
    private void check(boolean result,String description) { if(!result) throw new AssertionError(description); System.out.println("[SMOKE] PASS: "+description); }
    private void button(int button,boolean down) { sdl.SDL_JoystickSetVirtualButton(joystick,button,(byte)(down?1:0)); }
    @Override public void tickStart(EnumSet<TickType> types,Object... data) {}
    @Override public void tickEnd(EnumSet<TickType> types,Object... data) {
        if(!types.contains(TickType.CLIENT)) return;
        try { step(); } catch(Throwable error) { error.printStackTrace();finish(false,error.toString()); }
    }
    private void step() throws Exception {
        mc=Minecraft.getMinecraft();
        if(System.currentTimeMillis()-start>180000) throw new AssertionError("Timed out at stage "+stage);
        if(ClientProxy.instance==null || ClientProxy.instance.settings==null) return;
        ticks++;
        if(!Display.isActive() && Platform.isLinux()) {
            Class<?> linux=Class.forName("org.lwjgl.opengl.LinuxDisplay");
            Field window=linux.getDeclaredField("current_window"); window.setAccessible(true);
            X11 x11=(X11)Native.loadLibrary("X11",X11.class);
            Pointer display=x11.XOpenDisplay(null);
            x11.XMapRaised(display,new NativeLong(window.getLong(null)));
            x11.XSetInputFocus(display,new NativeLong(window.getLong(null)),1,new NativeLong(0));
            x11.XSync(display,false);x11.XCloseDisplay(display);
        }
        if(stage==0 && ticks>30) {
            String filename=Platform.isWindows()?"SDL2.dll":Platform.isMac()?"libSDL2.dylib":"libSDL2.so";
            File library=new File(mc.mcDataDir,"controllable_natives/2.32.10/"+Platform.RESOURCE_PREFIX+"/"+filename);
            sdl=(Virtual)Native.loadLibrary(library.getAbsolutePath(),Virtual.class);
            int index=sdl.SDL_JoystickAttachVirtual(1,6,15,0);primaryIndex=index;check(index>=0,"SDL virtual gamepad attached");joystick=sdl.SDL_JoystickOpen(index);
            sdl.SDL_JoystickSetVirtualAxis(joystick,4,Short.MIN_VALUE);sdl.SDL_JoystickSetVirtualAxis(joystick,5,Short.MIN_VALUE);
            next();
        } else if(stage==1 && ticks>40) {
            check(ClientProxy.instance.state.connected,"Minecraft receives virtual controller");
            check(Display.isActive(),"Minecraft window is focused");
            ClientProxy.instance.openSettings(); next();
        } else if(stage==2 && ticks>5) {
            check(mc.currentScreen instanceof SettingsScreen,"settings screen opens");
            net.minecraft.util.ScreenShotHelper.func_74292_a(mc.mcDataDir,"controllable-settings.png",mc.displayWidth,mc.displayHeight);
            GuiBridge.key(mc.currentScreen,'\0',1); check(!(mc.currentScreen instanceof SettingsScreen),"GUI bridge dispatches Escape");
            WorldSettings world=new WorldSettings(1234,EnumGameType.CREATIVE,false,false,WorldType.FLAT);
            mc.launchIntegratedServer("controllable-smoke","Controller smoke test",world); next();
        } else if(stage==3 && mc.thePlayer!=null && ticks>60) {
            mc.displayGuiScreen(null);mc.setIngameFocus();
            x=mc.thePlayer.posX;z=mc.thePlayer.posZ;yaw=mc.thePlayer.rotationYaw;
            sdl.SDL_JoystickSetVirtualAxis(joystick,1,(short)-24000);sdl.SDL_JoystickSetVirtualAxis(joystick,2,(short)18000);next();
        } else if(stage==4 && ticks>30) {
            check(Math.abs(mc.thePlayer.posX-x)+Math.abs(mc.thePlayer.posZ-z)>.5,"analog stick moves player");
            check(Math.abs(mc.thePlayer.rotationYaw-yaw)>5,"right stick rotates camera");
            sdl.SDL_JoystickSetVirtualAxis(joystick,1,(short)0);sdl.SDL_JoystickSetVirtualAxis(joystick,2,(short)0);
            button(2,true);next();
        } else if(stage==5 && ticks>3) {
            button(2,false);check(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer,"X opens inventory");next();
        } else if(stage==6 && ticks>3) {
            button(1,true);next();
        } else if(stage==7 && ticks>3) {
            button(1,false);check(mc.currentScreen==null,"B closes inventory");button(4,true);next();
        } else if(stage==8 && ticks>3) {
            check(mc.currentScreen instanceof RadialScreen,"Back opens radial wheel");
            sdl.SDL_JoystickSetVirtualAxis(joystick,1,Short.MIN_VALUE);next();
        } else if(stage==9 && ticks>3) {
            button(4,false);sdl.SDL_JoystickSetVirtualAxis(joystick,1,(short)0);next();
        } else if(stage==10 && ticks>5) {
            check(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer,"radial release opens selected inventory action");
            mc.displayGuiScreen(null);next();
        } else if(stage==11 && ticks>3) {
            button(13,true);next();
        } else if(stage==12 && ticks>3) {
            button(13,false);check(mc.currentScreen instanceof KeyboardScreen,"Chat opens on-screen keyboard");
            net.minecraft.util.ScreenShotHelper.func_74292_a(mc.mcDataDir,"controllable-keyboard.png",mc.displayWidth,mc.displayHeight);
            GuiBridge.key(mc.currentScreen,'h',35);GuiBridge.key(mc.currentScreen,'i',23);GuiBridge.key(mc.currentScreen,'\r',28);next();
        } else if(stage==13 && ticks>5) {
            check(mc.currentScreen==null,"on-screen keyboard sends chat and closes");
            listScreen=new SmokeListScreen();mc.displayGuiScreen(listScreen);next();
        } else if(stage==14 && ticks>4) {
            button(0,true);next();
        } else if(stage==15 && ticks>4) {
            button(0,false);check(listScreen.selected>=0,"A selects a vanilla GuiSlot list entry");
            sdl.SDL_JoystickSetVirtualAxis(joystick,3,(short)24000);next();
        } else if(stage==16 && ticks>10) {
            sdl.SDL_JoystickSetVirtualAxis(joystick,3,(short)0);
            check(listScreen.list.func_77210_c(listScreen.width/2,listScreen.height/2)!=listScreen.selected,"right stick scrolls vanilla list");
            mc.displayGuiScreen(null); mc.setIngameFocus();next();
        } else if(stage==17 && ticks>4) {
            sdl.SDL_JoystickSetVirtualAxis(joystick,5,Short.MAX_VALUE);next();
        } else if(stage==18 && ticks>3) {
            check(mc.gameSettings.keyBindAttack.pressed,"right trigger holds attack");
            sdl.SDL_JoystickSetVirtualAxis(joystick,5,Short.MIN_VALUE);sdl.SDL_JoystickSetVirtualAxis(joystick,4,Short.MAX_VALUE);next();
        } else if(stage==19 && ticks>3) {
            check(!mc.gameSettings.keyBindAttack.pressed,"attack releases with trigger");
            check(mc.gameSettings.keyBindUseItem.pressed,"left trigger holds use");
            sdl.SDL_JoystickSetVirtualAxis(joystick,4,Short.MIN_VALUE);button(0,true);next();
        } else if(stage==20 && ticks>3) {
            check(mc.gameSettings.keyBindJump.pressed,"A holds jump");button(0,false);button(1,true);next();
        } else if(stage==21 && ticks>3) {
            check(mc.gameSettings.keyBindSneak.pressed,"B holds sneak");button(1,false);next();
        } else if(stage==22 && ticks>3) {
            check(!mc.gameSettings.keyBindSneak.pressed && !mc.gameSettings.keyBindJump.pressed && !mc.gameSettings.keyBindUseItem.pressed,"held inputs release cleanly");
            ClientProxy.instance.settings.customBindings.put("key.controllable_smoke",14);button(14,true);next();
        } else if(stage==23 && ticks>4) {
            check(modStart>0 && modEnd>0,"mapped Forge mod key receives both tick callbacks");button(14,false);next();
        } else if(stage==24 && ticks>4) {
            check(modUp>0,"mapped Forge mod key receives release callback");
            ClientProxy.instance.settings.customBindings.remove("key.controllable_smoke");
            secondIndex=sdl.SDL_JoystickAttachVirtual(1,6,15,0);secondJoystick=sdl.SDL_JoystickOpen(secondIndex);
            sdl.SDL_JoystickSetVirtualAxis(secondJoystick,4,Short.MIN_VALUE);sdl.SDL_JoystickSetVirtualAxis(secondJoystick,5,Short.MIN_VALUE);
            sdl.SDL_JoystickSetVirtualButton(secondJoystick,0,(byte)1);
            sdl.SDL_JoystickSetVirtualAxis(joystick,1,(short)-24000);
            ClientProxy.instance.settings.secondaryIndex=1;next();
        } else if(stage==25 && ticks>8) {
            ControllerState merged=ClientProxy.instance.state;
            check(merged.buttons[0] && merged.axes[1]<-.5F && mc.gameSettings.keyBindJump.pressed,"two controllers merge movement and buttons");
            ClientProxy.instance.settings.secondaryIndex=-1;
            sdl.SDL_JoystickClose(secondJoystick);sdl.SDL_JoystickDetachVirtual(secondIndex);secondJoystick=null;
            sdl.SDL_JoystickSetVirtualAxis(joystick,1,(short)0);sdl.SDL_JoystickSetVirtualAxis(joystick,5,Short.MAX_VALUE);next();
        } else if(stage==26 && ticks>5) {
            check(mc.gameSettings.keyBindAttack.pressed,"attack held before disable");
            ClientProxy.instance.settings.enabled=false;next();
        } else if(stage==27 && ticks>5) {
            check(!mc.gameSettings.keyBindAttack.pressed,"disabling controller releases held attack");
            ClientProxy.instance.settings.enabled=true;next();
        } else if(stage==28 && ticks>5) {
            check(mc.gameSettings.keyBindAttack.pressed,"enabling restores held controller input");
            sdl.SDL_JoystickClose(joystick);sdl.SDL_JoystickDetachVirtual(primaryIndex);joystick=null;next();
        } else if(stage==29 && ticks>5) {
            check(!ClientProxy.instance.state.connected && !mc.gameSettings.keyBindAttack.pressed,"disconnect releases held attack");
            finish(true,"All integration checks passed");
        }
    }
    public static final class SmokeListScreen extends GuiScreen {
        int selected=-1;GuiSlot list;
        @Override public void initGui() {
            list=new GuiSlot(mc,width,height,30,height-30,20) {
                @Override protected int getSize(){return 30;}
                @Override protected void elementClicked(int index,boolean twice){selected=index;}
                @Override protected boolean isSelected(int index){return index==selected;}
                @Override protected void drawBackground(){}
                @Override protected void drawSlot(int index,int x,int y,int size,net.minecraft.client.renderer.Tessellator t){fontRenderer.drawString("Test item "+index,x,y,0xFFFFFF);}
            };
        }
        @Override public void drawScreen(int x,int y,float partial){drawDefaultBackground();list.drawScreen(x,y,partial);}
    }
    private void next() { stage++;ticks=0; }
    private void finish(boolean success,String message) {
        try { FileWriter out=new FileWriter(new File(mc.mcDataDir,"smoke-result.txt"));out.write((success?"PASS":"FAIL")+": "+message);out.close(); }
        catch(IOException e) { e.printStackTrace(); }
        System.out.println("[SMOKE] "+(success?"PASS":"FAIL")+": "+message);
        mc.shutdown();stage=100;
    }
    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
    @Override public String getLabel() { return "Controllable smoke test"; }
}
