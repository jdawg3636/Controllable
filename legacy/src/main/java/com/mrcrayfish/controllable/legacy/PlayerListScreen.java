package com.mrcrayfish.controllable.legacy;
import net.minecraft.client.gui.*;
public final class PlayerListScreen extends GuiScreen {
    @Override public void initGui() { buttonList.add(new GuiButton(0,width/2-75,height-30,150,20,"Done")); }
    @Override protected void actionPerformed(GuiButton button) { mc.displayGuiScreen(null); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();drawCenteredString(fontRenderer,"Players",width/2,15,0xFFFFFF);int row=40;
        if(mc.getNetHandler()!=null) for(Object object:mc.getNetHandler().playerInfoList) {
            GuiPlayerInfo info=(GuiPlayerInfo)object;drawCenteredString(fontRenderer,info.name+"  "+info.responseTime+" ms",width/2,row,0xFFFFFF); row+=12;
        }
        super.drawScreen(x,y,partial);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
