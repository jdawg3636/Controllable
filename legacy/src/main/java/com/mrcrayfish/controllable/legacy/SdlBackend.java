package com.mrcrayfish.controllable.legacy;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Owns only the joystick/gamecontroller subsystems, never Minecraft's window or audio. */
public final class SdlBackend {
    private static final int FLAGS = 0x200 | 0x2000;
    private Sdl sdl;
    private Pointer controller;
    private int selected = -1;
    private long nextScan;

    public void init(File gameDirectory) throws IOException {
        String filename = Platform.isWindows() ? "SDL2.dll" : Platform.isMac() ? "libSDL2.dylib" : "libSDL2.so";
        String resource = "/" + Platform.RESOURCE_PREFIX + "/" + filename;
        Path directory = new File(gameDirectory, "controllable_natives/2.32.10/" + Platform.RESOURCE_PREFIX).toPath();
        Files.createDirectories(directory);
        Path library = directory.resolve(filename);
        // Atomic replacement allows concurrent game instances to extract safely.
        if (!Files.exists(library)) {
            InputStream input = SdlBackend.class.getResourceAsStream(resource);
            if (input == null) throw new IOException("Unsupported SDL platform: " + Platform.RESOURCE_PREFIX);
            Path temporary = Files.createTempFile(directory, "sdl-", ".tmp");
            try {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                try { Files.move(temporary, library, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ignored) {
                    try { Files.move(temporary, library); }
                    catch (IOException failure) { if (!Files.exists(library)) throw failure; }
                }
                catch (IOException failure) {
                    // Another instance may have installed and loaded the same Windows DLL.
                    if (!Files.exists(library)) throw failure;
                }
            } finally { input.close(); Files.deleteIfExists(temporary); }
        }
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(LibraryOptions.ENCODING, "UTF-8");
        Sdl loaded = (Sdl) Native.loadLibrary(library.toAbsolutePath().toString(), Sdl.class, options);
        loaded.SDL_SetHint("SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS", "0");
        if (loaded.SDL_InitSubSystem(FLAGS) != 0) throw new IOException(loaded.SDL_GetError());
        sdl = loaded;
        InputStream mappings = SdlBackend.class.getResourceAsStream("/gamecontrollerdb.txt");
        if (mappings != null) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192]; int count;
                while ((count = mappings.read(buffer)) != -1) bytes.write(buffer, 0, count);
                byte[] data = bytes.toByteArray();
                Memory memory = new Memory(data.length);
                memory.write(0, data, 0, data.length);
                sdl.SDL_GameControllerAddMappingsFromRW(sdl.SDL_RWFromConstMem(memory, data.length), 1);
            } finally { mappings.close(); }
        }
    }

    public ControllerState poll(int index) {
        ControllerState state = new ControllerState();
        if (sdl == null) return state;
        sdl.SDL_GameControllerUpdate();
        if (selected != index || (controller != null && sdl.SDL_GameControllerGetAttached(controller) == 0)) {
            disconnect(); selected = index; nextScan = 0;
        }
        if (controller == null && System.currentTimeMillis() >= nextScan) {
            nextScan = System.currentTimeMillis() + 1000;
            int ordinal = 0;
            for (int i = 0; i < sdl.SDL_NumJoysticks(); i++) {
                if (sdl.SDL_IsGameController(i) == 0) continue;
                if (ordinal++ == index) { controller = sdl.SDL_GameControllerOpen(i); break; }
            }
        }
        if (controller != null) {
            state.connected = true;
            state.name = sdl.SDL_GameControllerName(controller);
            for (int i = 0; i < state.axes.length; i++) state.axes[i] = ControllerState.normalize(sdl.SDL_GameControllerGetAxis(controller, i));
            for (int i = 0; i < state.buttons.length; i++) state.buttons[i] = sdl.SDL_GameControllerGetButton(controller, i) != 0;
        }
        return state;
    }

    public void rumble(float strength, int milliseconds) {
        if (controller != null) {
            short amplitude = (short) (Math.max(0, Math.min(1, strength)) * 65535);
            sdl.SDL_GameControllerRumble(controller, amplitude, amplitude, milliseconds);
        }
    }
    private void disconnect() { if (controller != null) { sdl.SDL_GameControllerClose(controller); controller = null; } }
    public void close() { disconnect(); if (sdl != null) { sdl.SDL_QuitSubSystem(FLAGS); sdl = null; } }
    private static final class LibraryOptions { static final String ENCODING = "string-encoding"; }
}
