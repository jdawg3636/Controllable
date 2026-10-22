package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.gui.GuiScreen;

public final class RadialScreen extends GuiScreen {
    private int selected=-1;
    @Override public void drawScreen(int x,int y,float partial) {
        drawRect(0,0,width,height,0x88000000);
        ControllerState state=ClientProxy.instance.state;
        float sx=state.axes[0],sy=state.axes[1];
        if(sx*sx+sy*sy>.25F) selected=((int)Math.floor((Math.atan2(sy,sx)+Math.PI/2+Math.PI/8+Math.PI*2)/(Math.PI/4)))%8;
        drawCenteredString(fontRenderer,"Choose with left stick; release "+Settings.BUTTONS[ClientProxy.instance.settings.bindings.get(Settings.Action.RADIAL)],width/2,height/2-12,0xFFFFFF);
        drawCenteredString(fontRenderer,"B: cancel",width/2,height/2+4,0xAAAAAA);
        for(int i=0;i<8;i++) {
            double angle=i*Math.PI/4-Math.PI/2;
            int px=width/2+(int)(Math.cos(angle)*Math.min(110,width/3)),py=height/2+(int)(Math.sin(angle)*65);
            String action=ClientProxy.instance.settings.radial[i];
            drawCenteredString(fontRenderer,action,px,py,selected==i?0xFFFF55:0xFFFFFF);
        }
        if(!ClientProxy.instance.radialHeld()) {
            mc.displayGuiScreen(null);
            if(selected>=0) ClientProxy.instance.runAction(ClientProxy.instance.settings.radial[selected]);
        }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
