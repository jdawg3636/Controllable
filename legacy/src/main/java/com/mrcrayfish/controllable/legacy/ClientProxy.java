package com.mrcrayfish.controllable.legacy;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import java.util.*;
import static com.mrcrayfish.controllable.legacy.Settings.Action;

public final class ClientProxy extends CommonProxy implements ITickHandler {
    public static ClientProxy instance;
    public Settings settings;
    public ControllerState state = new ControllerState();
    public String status = "Starting SDL2";
    private SdlBackend secondary;
    private final SdlBackend backend = new SdlBackend();
    private final boolean[] previous = new boolean[18];
    private final boolean[] guiPrevious = new boolean[18];
    private final Set<Integer> desiredKeys = new HashSet<Integer>();
    private final Set<Integer> ownedKeys = new HashSet<Integer>();
    private Minecraft mc;
    private boolean initialized, f8, active, sneakToggle;
    private GuiScreen lastScreen;
    private float moveX, moveY;
    private long lastFrame, clickTime, nextNavigation, nextScroll;
    private boolean leftClick, rightClick;
    private float cursorX, cursorY;
    private int lastHealth;

    @Override public void init() {
        instance=this;
        TickRegistry.registerTickHandler(this, Side.CLIENT);
    }
    private void initialize() {
        initialized=true; mc=Minecraft.getMinecraft(); settings=new Settings(mc.mcDataDir); settings.save();
        try {
            backend.init(mc.mcDataDir); status="No controller connected";
            Runtime.getRuntime().addShutdownHook(new Thread("Controllable SDL shutdown") { @Override public void run() { backend.close(); if(secondary!=null)secondary.close(); } });
        } catch(Throwable error) {
            status="SDL2 unavailable: "+error.getMessage();
            System.err.println("[Controllable] "+status); error.printStackTrace();
        }
    }
    private boolean button(int id) {
        return id<15 ? id>=0 && state.buttons[id] : id==15 ? state.axes[4]>settings.triggerThreshold : id==16 && state.axes[5]>settings.triggerThreshold;
    }
    private boolean held(Action action) { return button(settings.bindings.get(action)); }
    private boolean pressed(Action action) { int id=settings.bindings.get(action); return button(id) && !previous[id]; }
    private boolean guiPressed(int id) { return button(id) && !guiPrevious[id]; }
    private void snapshot(boolean[] target) { for(int i=0;i<target.length;i++) target[i]=button(i); }
    private boolean focused() { return Display.isActive(); }

    @Override public void tickStart(EnumSet<TickType> ticks,Object... data) {
        if(!initialized) initialize();
        if(ticks.contains(TickType.RENDER)) frame();
        if(ticks.contains(TickType.CLIENT)) gameTick();
        ModKeyBridge.dispatch(this,ticks,false);
    }
    @Override public void tickEnd(EnumSet<TickType> ticks,Object... data) {
        if(initialized)ModKeyBridge.dispatch(this,ticks,true);
        if(ticks.contains(TickType.RENDER)) MouseBridge.restore();
        if(ticks.contains(TickType.RENDER) && initialized && settings.hints && active && mc.currentScreen==null && mc.thePlayer!=null && !mc.gameSettings.hideGUI) {
            ScaledResolution size=new ScaledResolution(mc.gameSettings,mc.displayWidth,mc.displayHeight);
            String attack=mc.objectMouseOver!=null && mc.objectMouseOver.entityHit!=null?"Attack":"Mine";
            String line=hint(Action.INVENTORY,"Inventory")+hint(Action.JUMP,"Jump")+hint(Action.USE,"Use")+hint(Action.ATTACK,attack)+hint(Action.RADIAL,"Wheel");
            if(mc.fontRenderer.getStringWidth(line)>size.getScaledWidth()-12) {
                mc.fontRenderer.drawStringWithShadow(hint(Action.INVENTORY,"Inventory")+hint(Action.JUMP,"Jump")+hint(Action.RADIAL,"Wheel"),6,size.getScaledHeight()-51,0xFFFFFF);
                line=hint(Action.USE,"Use")+hint(Action.ATTACK,attack);
            }
            mc.fontRenderer.drawStringWithShadow(line,6,size.getScaledHeight()-39,0xFFFFFF);
        }
    }
    private String hint(Action action,String label) {
        int button=settings.bindings.get(action);
        return button==17?"":Settings.BUTTONS[button]+": "+label+"  ";
    }
    private void frame() {
        long now=System.nanoTime(); float dt=lastFrame==0?0:Math.min(.05F,(now-lastFrame)/1e9F); lastFrame=now;
        state=backend.poll(settings.controllerIndex);
        if(settings.secondaryIndex>=0 && settings.secondaryIndex!=settings.controllerIndex) {
            if(secondary==null) {
                try { secondary=new SdlBackend();secondary.init(mc.mcDataDir); }
                catch(Throwable error) { status="Secondary controller unavailable: "+error.getMessage();secondary=null;settings.secondaryIndex=-1; }
            }
            if(secondary!=null)state=ControllerState.merge(state,secondary.poll(settings.secondaryIndex));
        } else if(secondary!=null) { secondary.close();secondary=null; }
        if(state.connected) status=state.name;
        else if(!status.startsWith("SDL2 unavailable")) status="No controller connected";
        boolean settingsKey=Keyboard.isKeyDown(Keyboard.KEY_F8);
        if(settingsKey&&!f8) openSettings(); f8=settingsKey;
        active=(settings.enabled || mc.currentScreen instanceof SettingsScreen) && state.connected && focused();
        if(!active) { MouseBridge.restore(); release(); snapshot(guiPrevious); snapshot(previous); return; }
        if(!(mc.currentScreen instanceof SettingsScreen && ((SettingsScreen)mc.currentScreen).isRebinding()) && (guiPressed(settings.bindings.get(Action.SETTINGS)) || button(4)&&button(6)&&(guiPressed(4)||guiPressed(6)))) { openSettings(); snapshot(guiPrevious); return; }
        float[] left=ControllerState.stick(state.axes[0],state.axes[1],settings.deadZone);
        float[] right=ControllerState.stick(state.axes[2],state.axes[3],settings.deadZone);
        moveX=-left[0]; moveY=-left[1];
        if(mc.currentScreen==null && mc.thePlayer!=null && mc.inGameHasFocus) {
            mc.thePlayer.setAngles(right[0]*settings.sensitivity*dt/0.15F, -right[1]*settings.sensitivity*dt*(settings.invertY?-1:1)/0.15F);
        } else if(mc.currentScreen!=null) {
            releaseKeys(); moveX=moveY=0;
            handleGui(dt,left,right);
        }
        snapshot(guiPrevious);
    }
    private void gameTick() {
        desiredKeys.clear();
        if(!active) return;
        if(mc.currentScreen==null && mc.thePlayer!=null && mc.inGameHasFocus) {
            if(!(mc.thePlayer.movementInput instanceof ControllerMovement)) mc.thePlayer.movementInput=new ControllerMovement(mc.thePlayer.movementInput);
            key(mc.gameSettings.keyBindAttack,held(Action.ATTACK)); key(mc.gameSettings.keyBindUseItem,held(Action.USE));
            key(mc.gameSettings.keyBindJump,held(Action.JUMP));
            if(settings.toggleSneak && pressed(Action.SNEAK)) sneakToggle=!sneakToggle;
            key(mc.gameSettings.keyBindSneak,settings.toggleSneak?sneakToggle:held(Action.SNEAK));
            key(mc.gameSettings.keyBindPickBlock,held(Action.PICK));
            if(held(Action.SPRINT) && moveY>0 && !mc.thePlayer.isSneaking()) mc.thePlayer.setSprinting(true);
            if(pressed(Action.PREVIOUS)) mc.thePlayer.inventory.currentItem=(mc.thePlayer.inventory.currentItem+8)%9;
            if(pressed(Action.NEXT)) mc.thePlayer.inventory.currentItem=(mc.thePlayer.inventory.currentItem+1)%9;
            if(pressed(Action.INVENTORY)) pulse(mc.gameSettings.keyBindInventory);
            if(pressed(Action.DROP)||pressed(Action.SWAP)) pulse(mc.gameSettings.keyBindDrop);
            if(pressed(Action.CHAT)) openChat();
            if(pressed(Action.PERSPECTIVE)) mc.gameSettings.thirdPersonView=(mc.gameSettings.thirdPersonView+1)%3;
            if(pressed(Action.PAUSE)) mc.displayInGameMenu();
            if(pressed(Action.RADIAL)) mc.displayGuiScreen(new RadialScreen());
            for(KeyBinding binding:mc.gameSettings.keyBindings) {
                Integer id=settings.customBindings.get(binding.keyDescription);
                if(id!=null)key(binding,button(id));
            }
            int health=mc.thePlayer.getHealth();
            if(settings.rumble && lastHealth>health) { backend.rumble(.5F,180);if(secondary!=null)secondary.rumble(.5F,180); }
            lastHealth=health;
        } else { releaseKeys(); moveX=moveY=0; }
        if(mc.currentScreen==null) applyKeys(); else releaseKeys();
        snapshot(previous);
    }
    private void key(KeyBinding binding,boolean down) {
        if(down) desiredKeys.add(binding.keyCode);
    }
    private void applyKeys() {
        for(Integer code:new HashSet<Integer>(ownedKeys)) if(!desiredKeys.contains(code)) { KeyBinding.setKeyBindState(code,physical(code));ownedKeys.remove(code); }
        for(Integer code:desiredKeys) { if(!ownedKeys.contains(code))KeyBinding.onTick(code); KeyBinding.setKeyBindState(code,true);ownedKeys.add(code); }
    }
    private static boolean physical(int code) { return code>=0 ? code<Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code) : code+100>=0 && code+100<Mouse.getButtonCount() && Mouse.isButtonDown(code+100); }
    private void pulse(KeyBinding binding) { KeyBinding.onTick(binding.keyCode); }
    private void releaseKeys() { for(Integer code:ownedKeys) KeyBinding.setKeyBindState(code,physical(code)); ownedKeys.clear(); }
    private void release() {
        releaseKeys(); moveX=moveY=0; sneakToggle=false; lastHealth=0;
        if(lastScreen!=null) { if(leftClick) GuiBridge.up(lastScreen,(int)cursorX,(int)cursorY,0); if(rightClick) GuiBridge.up(lastScreen,(int)cursorX,(int)cursorY,1); }
        leftClick=rightClick=false;
    }
    public void openSettings() { mc.displayGuiScreen(new SettingsScreen(mc.currentScreen)); }
    private void openChat() { GuiChat chat=new GuiChat(); mc.displayGuiScreen(chat); KeyboardScreen.open(mc,chat); }
    public void runAction(String action) {
        if("settings".equals(action)) { openSettings(); return; }
        if(mc.thePlayer==null) return;
        if("inventory".equals(action)) pulse(mc.gameSettings.keyBindInventory);
        else if("chat".equals(action)) openChat();
        else if("drop".equals(action)) pulse(mc.gameSettings.keyBindDrop);
        else if("perspective".equals(action)) mc.gameSettings.thirdPersonView=(mc.gameSettings.thirdPersonView+1)%3;
        else if("screenshot".equals(action)) mc.thePlayer.addChatMessage(ScreenShotHelper.saveScreenshot(mc.mcDataDir,mc.displayWidth,mc.displayHeight));
        else if("fullscreen".equals(action)) mc.toggleFullscreen();
        else if("pick".equals(action)) pulse(mc.gameSettings.keyBindPickBlock);
        else if("playerlist".equals(action)) mc.displayGuiScreen(new PlayerListScreen());
    }
    private void handleGui(float dt,float[] left,float[] right) {
        GuiScreen screen=mc.currentScreen;
        if(screen!=lastScreen) {
            release(); lastScreen=screen;
            cursorX=screen.width/2F; cursorY=screen.height/2F;
            snapshot(guiPrevious); // opening button cannot click the newly opened screen
        }
        if(screen instanceof RadialScreen) return;
        if(screen instanceof SettingsScreen && ((SettingsScreen)screen).isRebinding()) { snapshot(guiPrevious); return; }
        float scale=(float)mc.displayWidth/screen.width;
        cursorX=Math.max(0,Math.min(screen.width-1,cursorX+left[0]*settings.cursorSpeed*dt/scale));
        cursorY=Math.max(0,Math.min(screen.height-1,cursorY+left[1]*settings.cursorSpeed*dt/scale));
        navigate(screen);
        Mouse.setCursorPosition((int)(cursorX*mc.displayWidth/screen.width),(int)((screen.height-1-cursorY)*mc.displayHeight/screen.height));
        boolean l=button(0)||button(16),r=button(2)||button(15);
        if(l&&!leftClick) { clickTime=System.currentTimeMillis(); GuiBridge.click(screen,(int)cursorX,(int)cursorY,0); }
        if(!l&&leftClick) GuiBridge.up(screen,(int)cursorX,(int)cursorY,0);
        if(l&&leftClick) GuiBridge.drag(screen,(int)cursorX,(int)cursorY,0,System.currentTimeMillis()-clickTime);
        if(r&&!rightClick) { clickTime=System.currentTimeMillis(); GuiBridge.click(screen,(int)cursorX,(int)cursorY,1); }
        if(!r&&rightClick) GuiBridge.up(screen,(int)cursorX,(int)cursorY,1);
        if(r&&rightClick) GuiBridge.drag(screen,(int)cursorX,(int)cursorY,1,System.currentTimeMillis()-clickTime);
        leftClick=l; rightClick=r;
        if(mc.currentScreen!=screen) return;
        MouseBridge.hold(l,r);
        int scroll=button(9)?1:button(10)?-1:right[1]<-.4F?1:right[1]>.4F?-1:0;
        if(scroll!=0 && System.currentTimeMillis()>=nextScroll) {
            MouseBridge.scroll((int)(cursorX*mc.displayWidth/screen.width),(int)((screen.height-1-cursorY)*mc.displayHeight/screen.height),scroll*120);
            nextScroll=System.currentTimeMillis()+130;
        }
        if(guiPressed(1)||guiPressed(6)) GuiBridge.key(screen,'\0',Keyboard.KEY_ESCAPE);
        if(guiPressed(7) && !(screen instanceof KeyboardScreen)) KeyboardScreen.open(mc,screen);
        if(guiPressed(3)&&screen instanceof GuiContainer) {
            // Quick move through the container's public click API; works in SMP and mod containers.
            GuiContainer container=(GuiContainer)screen;
            int[] origin=containerOrigin(container);
            for(Object object:container.inventorySlots.inventorySlots) {
                Slot slot=(Slot)object;
                if(cursorX>=origin[0]+slot.xDisplayPosition && cursorX<origin[0]+slot.xDisplayPosition+16 && cursorY>=origin[1]+slot.yDisplayPosition && cursorY<origin[1]+slot.yDisplayPosition+16) {
                    mc.playerController.windowClick(container.inventorySlots.windowId,slot.slotNumber,0,1,mc.thePlayer); break;
                }
            }
        }

    }
    private static int[] containerOrigin(GuiContainer screen) {
        return new int[]{(Integer)GuiBridge.field(screen,GuiContainer.class,"guiLeft","field_74198_m","e"),(Integer)GuiBridge.field(screen,GuiContainer.class,"guiTop","field_74197_n","o")};
    }
    private void navigate(GuiScreen screen) {
        int dx=button(14)?1:button(13)?-1:0,dy=button(12)?1:button(11)?-1:0;
        if(dx==0&&dy==0) { nextNavigation=0; return; }
        long now=System.currentTimeMillis(); if(now<nextNavigation) return;
        nextNavigation=now+(nextNavigation==0?350:120);
        List<float[]> points=new ArrayList<float[]>();
        List buttons=(List)GuiBridge.field(screen,GuiScreen.class,"buttonList","field_73887_h","k");
        for(Object object:buttons) { GuiButton b=(GuiButton)object; if(b.enabled&&b.drawButton) points.add(new float[]{b.xPosition+(Integer)GuiBridge.field(b,GuiButton.class,"width","field_73747_a","a")/2F,b.yPosition+10}); }
        if(screen instanceof GuiContainer) {
            GuiContainer container=(GuiContainer)screen; int[] origin=containerOrigin(container);
            for(Object object:container.inventorySlots.inventorySlots) { Slot slot=(Slot)object; points.add(new float[]{origin[0]+slot.xDisplayPosition+8,origin[1]+slot.yDisplayPosition+8}); }
        }
        float best=Float.MAX_VALUE; float[] target=null;
        for(float[] point:points) {
            float x=point[0]-cursorX,y=point[1]-cursorY,along=x*dx+y*dy,across=Math.abs(x*dy-y*dx);
            if(along<=2) continue;
            float score=along+across*4;
            if(score<best) { best=score; target=point; }
        }
        if(target!=null) { cursorX=target[0]; cursorY=target[1]; }
        else { cursorX=Math.max(0,Math.min(screen.width-1,cursorX+dx*20)); cursorY=Math.max(0,Math.min(screen.height-1,cursorY+dy*20)); }
    }
    public boolean customHeld(String description) {
        Integer id=settings.customBindings.get(description);
        return active && mc.currentScreen==null && id!=null && button(id);
    }
    public boolean radialHeld() { return held(Action.RADIAL); }
    @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT,TickType.RENDER); }
    @Override public String getLabel() { return "Controllable"; }
    private final class ControllerMovement extends MovementInput {
        private final MovementInput keyboard;
        ControllerMovement(MovementInput keyboard) { this.keyboard=keyboard; }
        @Override public void updatePlayerMoveState() {
            keyboard.updatePlayerMoveState(); moveStrafe=keyboard.moveStrafe; moveForward=keyboard.moveForward; jump=keyboard.jump; sneak=keyboard.sneak;
            if(active && mc.currentScreen==null) {
                float x=moveX,y=moveY;
                if(!settings.analogRemote&&!mc.isSingleplayer()) { x=x>.3F?1:x<-.3F?-1:0; y=y>.3F?1:y<-.3F?-1:0; }
                if(sneak) { x*=.3F; y*=.3F; }
                moveStrafe=Math.max(-1,Math.min(1,moveStrafe+x)); moveForward=Math.max(-1,Math.min(1,moveForward+y));
            }
        }
    }
}
