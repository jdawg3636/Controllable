package com.mrcrayfish.controllable.legacy;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/** Small Java 7 ABI binding; SDL's game-controller API provides standardized Xbox button IDs. */
public interface Sdl extends Library {
    int SDL_InitSubSystem(int flags);
    void SDL_QuitSubSystem(int flags);
    int SDL_SetHint(String name, String value);
    String SDL_GetError();
    int SDL_NumJoysticks();
    int SDL_IsGameController(int index);
    Pointer SDL_GameControllerOpen(int index);
    void SDL_GameControllerClose(Pointer controller);
    int SDL_GameControllerGetAttached(Pointer controller);
    String SDL_GameControllerName(Pointer controller);
    void SDL_GameControllerUpdate();
    short SDL_GameControllerGetAxis(Pointer controller, int axis);
    byte SDL_GameControllerGetButton(Pointer controller, int button);
    int SDL_GameControllerRumble(Pointer controller, short low, short high, int milliseconds);
    int SDL_GameControllerAddMappingsFromRW(Pointer rw, int free);
    Pointer SDL_RWFromConstMem(Pointer memory, int size);
}
