package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

public final class SettingsScreen extends GuiScreen {
    private final GuiScreen parent;
    private int page;
    private Settings.Action waiting;
    private boolean[] wasDown=new boolean[17];
    public SettingsScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() {
        buttonList.clear(); Settings s=ClientProxy.instance.settings;
        int x=width/2-155,y=48;
        if(page==0) {
            add(0,x,y,"Controller support: "+on(s.enabled)); add(1,x+160,y,"Device: "+(s.controllerIndex+1));
            add(2,x,y+24,"Look speed: "+(int)s.sensitivity); add(3,x+160,y+24,"Dead zone: "+(int)(s.deadZone*100)+"%");
            add(4,x,y+48,"Invert Y: "+on(s.invertY)); add(5,x+160,y+48,"Cursor speed: "+(int)s.cursorSpeed);
            add(6,x,y+72,"Hints: "+on(s.hints)); add(7,x+160,y+72,"Rumble: "+on(s.rumble));
            add(8,x,y+96,"Toggle sneak: "+on(s.toggleSneak)); add(9,x+160,y+96,"Analog on servers: "+on(s.analogRemote));
            add(10,x,y+120,"Trigger threshold: "+(int)(s.triggerThreshold*100)+"%");
            add(11,x+160,y+120,"Merge device: "+(s.secondaryIndex<0?"Off":""+(s.secondaryIndex+1)));
        } else if(page==1||page==2) {
            Settings.Action[] actions=Settings.Action.values();
            for(int i=(page-1)*8;i<Math.min(actions.length,page*8);i++) {
                Settings.Action a=actions[i];
                add(100+i,x+(i%2)*160,y+((i%8)/2)*28,a.label+": "+(waiting==a?"Press a button...":Settings.BUTTONS[s.bindings.get(a)]));
            }
        } else {
            for(int i=0;i<s.radial.length;i++) add(200+i,x+(i%2)*160,y+(i/2)*24,"Wheel "+(i+1)+": "+s.radial[i]);
        }
        add(900,x,height-49,page==0?"Button mappings":page==1?"More mappings":page==2?"Radial actions":"Mod key bindings");
        add(901,x+160,height-49,"Done");
    }
    private void add(int id,int x,int y,String text) { buttonList.add(new GuiButton(id,x,y,150,20,text)); }
    private static String on(boolean value) { return value?"On":"Off"; }
    @Override protected void actionPerformed(GuiButton button) {
        Settings s=ClientProxy.instance.settings;
        if(button.id==901) { s.save(); mc.displayGuiScreen(parent); return; }
        if(button.id==900) { if(page==3) { mc.displayGuiScreen(new KeyBindingsScreen(this)); page=0; return; }
            page++; waiting=null; initGui(); return; }
        if(button.id>=100&&button.id<200) { waiting=Settings.Action.values()[button.id-100]; snapshot(); initGui(); return; }
        if(button.id>=200&&button.id<208) {
            String[] choices={"inventory","chat","drop","perspective","screenshot","fullscreen","playerlist","pick","settings"};
            int slot=button.id-200,current=0;
            for(int i=0;i<choices.length;i++) if(choices[i].equals(s.radial[slot])) current=i;
            s.radial[slot]=choices[(current+1)%choices.length];
        }
        switch(button.id) {
            case 11:s.secondaryIndex=s.secondaryIndex>=7?-1:s.secondaryIndex+1;break;
            case 0:s.enabled=!s.enabled;break; case 1:s.controllerIndex=(s.controllerIndex+1)%8;break;
            case 2:s.sensitivity=s.sensitivity>=360?60:s.sensitivity+30;break;
            case 3:s.deadZone=s.deadZone>=.39F?.05F:s.deadZone+.05F;break;
            case 4:s.invertY=!s.invertY;break; case 5:s.cursorSpeed=s.cursorSpeed>=1200?200:s.cursorSpeed+100;break;
            case 6:s.hints=!s.hints;break; case 7:s.rumble=!s.rumble;break; case 8:s.toggleSneak=!s.toggleSneak;break;
            case 9:s.analogRemote=!s.analogRemote;break; case 10:s.triggerThreshold=s.triggerThreshold>=.79F?.15F:s.triggerThreshold+.1F;break;
        }
        s.save(); initGui();
    }
    public boolean isRebinding() { return waiting!=null; }
    private boolean down(int i) { ControllerState s=ClientProxy.instance.state; return i<15?s.buttons[i]:s.axes[i==15?4:5]>.5; }
    private void snapshot() { for(int i=0;i<17;i++) wasDown[i]=down(i); }
    @Override public void updateScreen() {
        if(waiting!=null) for(int i=0;i<17;i++) {
            boolean value=down(i);
            if(value&&!wasDown[i]) { ClientProxy.instance.settings.bindings.put(waiting,i); waiting=null; ClientProxy.instance.settings.save(); initGui(); break; }
            wasDown[i]=value;
        }
    }
    @Override protected void keyTyped(char c,int key) {
        if(waiting!=null) {
            if(key==Keyboard.KEY_DELETE) { ClientProxy.instance.settings.bindings.put(waiting,17); ClientProxy.instance.settings.save(); }
            waiting=null; initGui();
        } else if(key==Keyboard.KEY_ESCAPE) { ClientProxy.instance.settings.save(); mc.displayGuiScreen(parent); }
    }
    @Override public void onGuiClosed() { ClientProxy.instance.settings.save(); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRenderer,"Controllable - Minecraft 1.5.2",width/2,12,0xFFFFFF);
        drawCenteredString(fontRenderer,ClientProxy.instance.status,width/2,29,0xAAAAAA);
        super.drawScreen(x,y,partial);
        if(waiting!=null) drawCenteredString(fontRenderer,"Press a controller button. Delete: unbind. Escape: cancel.",width/2,height-16,0xFFFF55);
        else drawCenteredString(fontRenderer,"D-pad: navigate   A: click   B: back   LS: keyboard   F8: settings",width/2,height-16,0xAAAAAA);
    }
}
