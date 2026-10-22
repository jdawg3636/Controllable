package com.mrcrayfish.controllable.legacy;

import com.sun.jna.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.File;

/** Exercises the actual shipped SDL library with SDL's virtual game-controller driver. */
public class NativeControllerTest {
    interface VirtualSdl extends Library {
        int SDL_JoystickAttachVirtual(int type,int axes,int buttons,int hats);
        Pointer SDL_JoystickOpen(int index);
        int SDL_JoystickSetVirtualAxis(Pointer joystick,int axis,short value);
        int SDL_JoystickSetVirtualButton(Pointer joystick,int button,byte value);
        void SDL_JoystickClose(Pointer joystick);
        int SDL_JoystickDetachVirtual(int index);
    }
    @Rule public TemporaryFolder directory=new TemporaryFolder();
    @Test public void bundledNativeReadsAxesButtonsAndHotplug() throws Exception {
        SdlBackend backend=new SdlBackend(); backend.init(directory.getRoot());
        String filename=Platform.isWindows()?"SDL2.dll":Platform.isMac()?"libSDL2.dylib":"libSDL2.so";
        File nativeFile=new File(directory.getRoot(),"controllable_natives/2.32.10/"+Platform.RESOURCE_PREFIX+"/"+filename);
        assertTrue(nativeFile.isFile());
        VirtualSdl sdl=(VirtualSdl)Native.loadLibrary(nativeFile.getAbsolutePath(),VirtualSdl.class);
        int index=sdl.SDL_JoystickAttachVirtual(1,6,15,0); assertTrue("virtual joystick attaches",index>=0);
        Pointer joystick=sdl.SDL_JoystickOpen(index); assertNotNull(joystick);
        try {
            ControllerState state=backend.poll(0); assertTrue("virtual controller recognized",state.connected);
            assertEquals(0,sdl.SDL_JoystickSetVirtualAxis(joystick,0,(short)16384));
            assertEquals(0,sdl.SDL_JoystickSetVirtualAxis(joystick,1,Short.MIN_VALUE));
            assertEquals(0,sdl.SDL_JoystickSetVirtualButton(joystick,0,(byte)1));
            sdl.SDL_JoystickSetVirtualAxis(joystick,4,Short.MIN_VALUE);
            sdl.SDL_JoystickSetVirtualAxis(joystick,5,Short.MAX_VALUE);
            state=backend.poll(0); assertEquals(0,state.axes[4],.001);assertEquals(1,state.axes[5],.001); assertEquals(.5F,state.axes[0],.001); assertEquals(-1,state.axes[1],.001); assertTrue(state.buttons[0]);
            sdl.SDL_JoystickSetVirtualButton(joystick,0,(byte)0); state=backend.poll(0); assertFalse(state.buttons[0]);
            backend.rumble(.5F,20); // Unsupported haptics must be harmless.
            sdl.SDL_JoystickClose(joystick); joystick=null;
            assertEquals(0,sdl.SDL_JoystickDetachVirtual(index)); assertFalse(backend.poll(0).connected);
        } finally { if(joystick!=null) sdl.SDL_JoystickClose(joystick); backend.close(); }
    }
}
