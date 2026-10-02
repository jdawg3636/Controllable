package com.mrcrayfish.controllable.client.input.sdl3;

import com.mojang.datafixers.util.Pair;
import com.mrcrayfish.controllable.Config;
import com.mrcrayfish.controllable.Constants;
import com.mrcrayfish.controllable.client.input.AdaptiveControllerManager;
import com.mrcrayfish.controllable.client.input.Controller;
import com.mrcrayfish.controllable.client.input.DeviceInfo;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.sdl.SDLHints.*;

/**
 * Author: MrCrayfish
 */
public class SDL3ControllerManager extends AdaptiveControllerManager
{
    private boolean lastBackgroundInput;

    @Override
    public void init()
    {
        if(Config.CLIENT.options.backgroundInput.get())
        {
            SDL_SetHint(SDLHints.SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1");
            this.lastBackgroundInput = true;
        }
        SDL_SetHint(SDL_HINT_JOYSTICK_HIDAPI_VERTICAL_JOY_CONS, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_HIDAPI_PS4, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_HIDAPI_PS5, "1");
        SDL_SetHint(SDL_HINT_JOYSTICK_RAWINPUT, "0");
        if(Config.CLIENT.options.advanced.rawInput.get())
        {
            SDL_SetHint(SDL_HINT_JOYSTICK_RAWINPUT, "1");
            SDL_SetHint(SDL_HINT_JOYSTICK_RAWINPUT_CORRELATE_XINPUT, "1");
        }
    }

    @Override
    public void dispose()
    {
        // Minecraft does this automatically
    }

    @Override
    public SDL3Controller createController(int deviceIndex, Number jid)
    {
        return new SDL3Controller(deviceIndex, jid);
    }

    @Override
    protected int getRawControllerCount()
    {
        var buffer = SDLJoystick.SDL_GetJoysticks();
        if(buffer == null)
            return 0;
        int count = 0;
        for(int i = 0; i < buffer.remaining(); i++)
        {
            int instanceId = buffer.get(i);
            if(SDLGamepad.SDL_IsGamepad(instanceId))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    protected Map<Number, Pair<Integer, String>> createRawControllerMap()
    {
        var buffer = SDLJoystick.SDL_GetJoysticks();
        if(buffer == null)
            return Map.of();

        Map<Number, Pair<Integer, String>> controllers = new HashMap<>();
        for(int i = 0; i < buffer.remaining(); i++)
        {
            int instanceId = buffer.get(i);
            if(SDLGamepad.SDL_IsGamepad(instanceId))
            {
                String controllerName = SDLJoystick.SDL_GetJoystickNameForID(instanceId);
                int index = SDLJoystick.SDL_GetJoystickPlayerIndexForID(instanceId);
                controllers.put(instanceId, Pair.of(index, controllerName));
            }
        }
        return controllers;
    }

    @Override
    @Nullable
    public Controller connectToBestGameController()
    {
        List<DeviceInfo> lastDevices = this.getLastDevices();
        if(!lastDevices.isEmpty())
        {
            List<SDL3Controller> selectedControllers = new ArrayList<>();
            List<SDL3Controller> availableControllers = new ArrayList<>();

            var buffer = SDLJoystick.SDL_GetJoysticks();
            if(buffer != null)
            {
                for(int i = 0; i < buffer.remaining(); i++)
                {
                    int instanceId = buffer.get(i);
                    if(SDLGamepad.SDL_IsGamepad(instanceId))
                    {
                        availableControllers.add(new SDL3Controller(0, buffer.get(i)));
                    }
                }

            }

            for(DeviceInfo info : lastDevices)
            {
                Iterator<SDL3Controller> it = availableControllers.iterator();
                while(it.hasNext())
                {
                    SDL3Controller controller = it.next();
                    controller.open();
                    if(controller.getInfo().equals(info))
                    {
                        selectedControllers.add(controller);
                        it.remove();
                    }
                    controller.close();
                }
            }
            selectedControllers.forEach(this::addActiveController);

            Controller controller = this.getActiveController();
            if(controller != null)
            {
                return controller;
            }
        }

        var buffer = SDLJoystick.SDL_GetJoysticks();
        if(buffer != null)
        {
            for(int i = 0; i < buffer.remaining(); i++)
            {
                int instanceId = buffer.get(i);
                if(SDLGamepad.SDL_IsGamepad(instanceId))
                {
                    SDL3Controller controller = new SDL3Controller(0, instanceId);
                    if(this.setActiveController(controller))
                    {
                        return controller;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void updateMappings(InputStream is) throws IOException
    {
        byte[] bytes = is.readAllBytes();
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        try
        {
            buffer.put(bytes).flip();
            long io = SDLIOStream.SDL_IOFromConstMem(buffer);
            if(io == MemoryUtil.NULL)
            {
                Constants.LOG.error("Error while reading mappings: {}", SDLError.SDL_GetError());
                return;
            }
            int count = SDLGamepad.SDL_AddGamepadMappingsFromIO(io, true);
            if(count == -1)
            {
                Constants.LOG.error("Error while reading mappings: {}", SDLError.SDL_GetError());
                return;
            }
            if(count > 0)
            {
                Constants.LOG.info("Successfully updated {} gamepad mappings", count);
                return;
            }
        }
        catch(Exception e)
        {
            Constants.LOG.error("Error while reading mappings", e);
            return;
        }
        Constants.LOG.info("No gamepad mappings were updated");
    }

    @Override
    public void tick()
    {
        super.tick();

        // Updates the sdl hint for background events
        if(this.lastBackgroundInput != Config.CLIENT.options.backgroundInput.get())
        {
            this.lastBackgroundInput = Config.CLIENT.options.backgroundInput.get();
            String value = this.lastBackgroundInput ? "1" : "0";
            SDL_SetHint(SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, value);
        }
    }
}
