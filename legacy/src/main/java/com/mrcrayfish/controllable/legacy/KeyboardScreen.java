package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

/** Sends ordinary key events to the original screen, retaining its focused text field. */
public final class KeyboardScreen extends GuiScreen {
    private final GuiScreen target;
    private boolean shift;
    private String typed="";
    private static final String[] ROWS={"1234567890-", "qwertyuiop/", "asdfghjkl;'", "zxcvbnm,.?!", "[]{}=+@#%&*"};
    private static final String[] SHIFT_ROWS={"!@#$%^&*()_", "QWERTYUIOP/", "ASDFGHJKL:\"", "ZXCVBNM<>?!", "[]{}=+@#%&*"};
    public KeyboardScreen(GuiScreen target) { this.target=target; }
    public static void open(net.minecraft.client.Minecraft mc,GuiScreen target) {
        KeyboardScreen keyboard=new KeyboardScreen(target);
        ScaledResolution resolution=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);
        mc.currentScreen=keyboard;
        keyboard.setWorldAndResolution(mc,resolution.getScaledWidth(),resolution.getScaledHeight());
    }
    @Override public void onGuiClosed() { target.onGuiClosed(); }
    @Override public void initGui() {
        buttonList.clear(); int cell=Math.min(25,(width-20)/11),start=width/2-cell*11/2,top=height/2-40;
        int id=0;
        for(int row=0;row<ROWS.length;row++) for(int col=0;col<ROWS[row].length();col++) {
            char c=(shift?SHIFT_ROWS:ROWS)[row].charAt(col);
            buttonList.add(new GuiButton(id++,start+col*cell,top+row*23,cell-1,20,""+c));
        }
        buttonList.add(new GuiButton(100,start,top+120,cell*2,20,"Shift"));
        buttonList.add(new GuiButton(101,start+cell*2,top+120,cell*3,20,"Space"));
        buttonList.add(new GuiButton(102,start+cell*5,top+120,cell*2,20,"Delete"));
        buttonList.add(new GuiButton(103,start+cell*7,top+120,cell*2,20,"Enter"));
        buttonList.add(new GuiButton(104,start+cell*9,top+120,cell*2,20,"Done"));
    }
    private void send(char c,int key) {
        // GuiChat and some mod screens close themselves using Minecraft.currentScreen.
        mc.currentScreen=target;
        GuiBridge.key(target,c,key);
        if(mc.currentScreen==target) mc.currentScreen=this;
        if(key==Keyboard.KEY_BACK && typed.length()>0) typed=typed.substring(0,typed.length()-1);
        else if(c>=32) typed+=c;
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id<100) send(button.displayString.charAt(0),0);
        else switch(button.id) {
            case 100:shift=!shift;initGui();break;
            case 101:send(' ',Keyboard.KEY_SPACE);break;
            case 102:send('\b',Keyboard.KEY_BACK);break;
            case 103:send('\r',Keyboard.KEY_RETURN);break;
            case 104:mc.currentScreen=target;break;
        }
    }
    @Override protected void keyTyped(char c,int key) { if(key==Keyboard.KEY_ESCAPE) mc.currentScreen=target; else send(c,key); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground(); drawCenteredString(fontRenderer,"On-screen keyboard",width/2,20,0xFFFFFF);
        String visible=typed.length()>45?typed.substring(typed.length()-45):typed;
        drawCenteredString(fontRenderer,visible+"_",width/2,height/2-60,0xFFFFAA);
        super.drawScreen(x,y,partial);
    }
    @Override public boolean doesGuiPauseGame() { return target.doesGuiPauseGame(); }
}
