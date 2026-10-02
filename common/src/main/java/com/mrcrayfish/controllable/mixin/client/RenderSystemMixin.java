package com.mrcrayfish.controllable.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.sdl.SDLInit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderSystem.class)
public class RenderSystemMixin
{
    @ModifyArg(method = "initBackendSystem", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLInit;SDL_Init(I)Z"))
    private static int controllable$ModifySdlInit(int flags)
    {
        return flags | SDLInit.SDL_INIT_JOYSTICK | SDLInit.SDL_INIT_GAMEPAD | SDLInit.SDL_INIT_SENSOR;
    }
}
