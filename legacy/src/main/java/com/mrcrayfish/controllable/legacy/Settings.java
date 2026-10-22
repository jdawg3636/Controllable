package com.mrcrayfish.controllable.legacy;

import java.io.*;
import java.util.*;

public final class Settings {
    public enum Action {
        JUMP("Jump",0), SNEAK("Sneak",1), INVENTORY("Inventory",2), SWAP("Drop item",3),
        PREVIOUS("Previous slot",9), NEXT("Next slot",10), SPRINT("Sprint",7), PICK("Pick block",8),
        PERSPECTIVE("Perspective",11), DROP("Drop item",12), CHAT("Chat",13), RADIAL("Radial wheel",4),
        PAUSE("Pause / back",6), ATTACK("Attack / mine",16), USE("Use / place",15), SETTINGS("Settings",5);
        public final String label; public final int defaultButton;
        Action(String label, int button) { this.label = label; this.defaultButton = button; }
    }
    public static final String[] BUTTONS = {"A", "B", "X", "Y", "Back", "Guide", "Start", "LS", "RS", "LB", "RB", "D-pad up", "D-pad down", "D-pad left", "D-pad right", "LT", "RT", "Unbound"};
    public final EnumMap<Action,Integer> bindings = new EnumMap<Action,Integer>(Action.class);
    public final Map<String,Integer> customBindings = new LinkedHashMap<String,Integer>();
    public int secondaryIndex = -1;
    public final String[] radial = {"inventory", "chat", "drop", "perspective", "screenshot", "fullscreen", "playerlist", "pick"};
    public boolean enabled = true, invertY, hints = true, rumble = true, toggleSneak, analogRemote;
    public float deadZone = .18F, sensitivity = 180, cursorSpeed = 650, triggerThreshold = .35F;
    public int controllerIndex;
    private final File file;
    public Settings(File directory) { file = new File(directory, "config/controllable.properties"); for (Action action : Action.values()) bindings.put(action, action.defaultButton); load(); }
    private void load() {
        Properties p = new Properties();
        if (!file.isFile()) return;
        try {
            FileInputStream in = new FileInputStream(file);
            try { p.load(in); } finally { in.close(); }
            enabled = bool(p,"enabled",true); invertY = bool(p,"invertY",false); hints = bool(p,"hints",true);
            rumble = bool(p,"rumble",true); toggleSneak = bool(p,"toggleSneak",false); analogRemote = bool(p,"analogRemote",false);
            deadZone = number(p,"deadZone",.18F,.05F,.8F); sensitivity = number(p,"sensitivity",180,30,720);
            cursorSpeed = number(p,"cursorSpeed",650,100,2000); triggerThreshold = number(p,"triggerThreshold",.35F,.05F,.95F);
            controllerIndex = (int) number(p,"controllerIndex",0,0,15);
            secondaryIndex=(int)number(p,"secondaryIndex",-1,-1,15);
            for(String key:p.stringPropertyNames()) if(key.startsWith("custom.")) customBindings.put(key.substring(7),(int)number(p,key,17,0,17));
            for (Action action : Action.values()) bindings.put(action,(int)number(p,"binding."+action.name(),action.defaultButton,0,17));
            for (int i=0;i<8;i++) radial[i]=p.getProperty("radial."+i,radial[i]);
        } catch (IOException e) { System.err.println("[Controllable] Could not load settings: " + e); }
    }
    private static boolean bool(Properties p,String key,boolean fallback) { return Boolean.parseBoolean(p.getProperty(key,Boolean.toString(fallback))); }
    private static float number(Properties p,String key,float fallback,float min,float max) {
        try { float value=Float.parseFloat(p.getProperty(key,"")); return Float.isNaN(value) || Float.isInfinite(value) ? fallback : Math.max(min,Math.min(max,value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    public void save() {
        Properties p=new Properties();
        p.setProperty("secondaryIndex",""+secondaryIndex);
        for(Map.Entry<String,Integer> entry:customBindings.entrySet()) p.setProperty("custom."+entry.getKey(),""+entry.getValue());
        p.setProperty("enabled",""+enabled); p.setProperty("invertY",""+invertY); p.setProperty("hints",""+hints);
        p.setProperty("rumble",""+rumble); p.setProperty("toggleSneak",""+toggleSneak); p.setProperty("analogRemote",""+analogRemote);
        p.setProperty("deadZone",""+deadZone); p.setProperty("sensitivity",""+sensitivity); p.setProperty("cursorSpeed",""+cursorSpeed);
        p.setProperty("triggerThreshold",""+triggerThreshold); p.setProperty("controllerIndex",""+controllerIndex);
        for(Action action:Action.values()) p.setProperty("binding."+action.name(),""+bindings.get(action));
        for(int i=0;i<8;i++) p.setProperty("radial."+i,radial[i]);
        file.getParentFile().mkdirs();
        try { FileOutputStream out=new FileOutputStream(file); try { p.store(out,"Controllable 1.5.2 - SDL standard button IDs; 17 = unbound. Radial actions: inventory, chat, drop, perspective, screenshot, fullscreen, playerlist, pick, settings"); } finally { out.close(); } }
        catch(IOException e) { System.err.println("[Controllable] Could not save settings: "+e); }
    }
}
