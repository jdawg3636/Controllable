package com.mrcrayfish.controllable.legacy;

import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StringTranslate;
import java.util.*;

/** All registered vanilla and Forge mod key bindings can be mapped, including modded actions. */
public final class KeyBindingsScreen extends GuiScreen {
    private final GuiScreen parent;
    private int page;
    private List<KeyBinding> keys=new ArrayList<KeyBinding>();
    public KeyBindingsScreen(GuiScreen parent) { this.parent=parent; }
    @Override public void initGui() {
        buttonList.clear();keys.clear();Set<String> seen=new HashSet<String>();
        for(KeyBinding binding:mc.gameSettings.keyBindings) if(seen.add(binding.keyDescription))keys.add(binding);
        int start=page*8;
        for(int i=start;i<Math.min(keys.size(),start+8);i++) {
            KeyBinding binding=keys.get(i);Integer id=ClientProxy.instance.settings.customBindings.get(binding.keyDescription);
            String label=StringTranslate.getInstance().translateKey(binding.keyDescription);
            String assigned=Settings.BUTTONS[id==null?17:id];
            while(label.length()>1 && fontRenderer.getStringWidth(label+": "+assigned)>142) label=label.substring(0,label.length()-1);
            buttonList.add(new GuiButton(i,width/2-155+((i-start)%2)*160,50+((i-start)/2)*28,150,20,label+": "+Settings.BUTTONS[id==null?17:id]));
        }
        buttonList.add(new GuiButton(900,width/2-155,height-45,100,20,"Previous"));
        buttonList.add(new GuiButton(901,width/2-50,height-45,100,20,"Next"));
        buttonList.add(new GuiButton(902,width/2+55,height-45,100,20,"Done"));
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==902) { mc.displayGuiScreen(parent);return; }
        if(button.id==900)page=Math.max(0,page-1);
        else if(button.id==901)page=Math.min((keys.size()-1)/8,page+1);
        else {
            Settings settings=ClientProxy.instance.settings;String key=keys.get(button.id).keyDescription;
            Integer id=settings.customBindings.get(key);int next=((id==null?17:id)+1)%18;
            if(next==17)settings.customBindings.remove(key);else settings.customBindings.put(key,next);
            settings.save();
        }
        initGui();
    }
    @Override protected void keyTyped(char c,int key) { if(key==1)mc.displayGuiScreen(parent); }
    @Override public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();drawCenteredString(fontRenderer,"Key bindings - "+(page+1)+" / "+Math.max(1,(keys.size()+7)/8),width/2,16,0xFFFFFF);
        drawCenteredString(fontRenderer,"Click to cycle controller buttons; Unbound disables the mapping.",width/2,32,0xAAAAAA);
        super.drawScreen(x,y,partial);
    }
}
