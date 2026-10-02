package com.mrcrayfish.controllable.client.input.sdl3;

import com.mrcrayfish.controllable.client.input.ButtonStates;
import com.mrcrayfish.controllable.client.input.Buttons;
import com.mrcrayfish.controllable.client.input.Controller;
import com.mrcrayfish.controllable.client.input.DeviceInfo;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 *  A wrapper class that aims to reduce the exposure to the underlying controller library. This class
 *  provides simple and straight forward methods to retrieve values about the current state of the
 *  controller.
 */
public class SDL3Controller extends Controller
{
    private final int instanceId;
    private Long gamepad;
    private String cachedName;
    private DeviceInfo info;
    private boolean gyro;

    public SDL3Controller(int deviceIndex, Number jid)
    {
        this.instanceId = jid.intValue();
        this.getName(); //cache the name straight away
    }

    @Override
    public Number getJid()
    {
        return this.instanceId;
    }

    @Override
    public boolean open()
    {
        if(this.gamepad == null)
        {
            this.gamepad = SDLGamepad.SDL_OpenGamepad(this.instanceId);
            this.updateGyroState();
            return this.gamepad != null;
        }
        return true;
    }

    @Override
    public void close()
    {
        if(this.gamepad != null)
        {
            SDLGamepad.SDL_CloseGamepad(this.gamepad);
            this.gamepad = null;
        }
    }

    @Override
    public boolean isOpen()
    {
        return this.gamepad != null && SDLGamepad.SDL_GamepadConnected(this.gamepad);
    }

    @Override
    public ButtonStates captureButtonStates()
    {
        ButtonStates states = new ButtonStates();
        states.setState(Buttons.A, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH));
        states.setState(Buttons.B, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_EAST));
        states.setState(Buttons.X, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST));
        states.setState(Buttons.Y, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH));
        states.setState(Buttons.SELECT, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_BACK));
        states.setState(Buttons.HOME, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE));
        states.setState(Buttons.START, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_START));
        states.setState(Buttons.LEFT_THUMB_STICK, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK));
        states.setState(Buttons.RIGHT_THUMB_STICK, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK));
        states.setState(Buttons.LEFT_BUMPER, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER));
        states.setState(Buttons.RIGHT_BUMPER, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER));
        states.setState(Buttons.LEFT_TRIGGER, this.getLTriggerValue() >= 0.5F);
        states.setState(Buttons.RIGHT_TRIGGER, this.getRTriggerValue() >= 0.5F);
        states.setState(Buttons.DPAD_UP, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP));
        states.setState(Buttons.DPAD_DOWN, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN));
        states.setState(Buttons.DPAD_LEFT, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT));
        states.setState(Buttons.DPAD_RIGHT, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT));
        states.setState(Buttons.MISC, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_MISC1));
        states.setState(Buttons.PADDLE_ONE, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_PADDLE1));
        states.setState(Buttons.PADDLE_TWO, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_PADDLE2));
        states.setState(Buttons.PADDLE_THREE, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1));
        states.setState(Buttons.PADDLE_FOUR, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2));
        states.setState(Buttons.TOUCHPAD, this.readButton(SDLGamepad.SDL_GAMEPAD_BUTTON_TOUCHPAD));
        states.setState(Buttons.LEFT_THUMB_STICK_UP, this.getLThumbStickYValue() <= -0.5F);
        states.setState(Buttons.LEFT_THUMB_STICK_DOWN, this.getLThumbStickYValue() >= 0.5F);
        states.setState(Buttons.LEFT_THUMB_STICK_LEFT, this.getLThumbStickXValue() <= -0.5F);
        states.setState(Buttons.LEFT_THUMB_STICK_RIGHT, this.getLThumbStickXValue() >= 0.5F);
        states.setState(Buttons.RIGHT_THUMB_STICK_UP, this.getRThumbStickYValue() <= -0.5F);
        states.setState(Buttons.RIGHT_THUMB_STICK_DOWN, this.getRThumbStickYValue() >= 0.5F);
        states.setState(Buttons.RIGHT_THUMB_STICK_LEFT, this.getRThumbStickXValue() <= -0.5F);
        states.setState(Buttons.RIGHT_THUMB_STICK_RIGHT, this.getRThumbStickXValue() >= 0.5F);
        return states;
    }

    private boolean readButton(int button)
    {
        return SDLGamepad.SDL_GetGamepadButton(this.gamepad, button);
    }

    @Override
    public String getName()
    {
        if(this.cachedName == null)
        {
            this.cachedName = SDLGamepad.SDL_GetGamepadNameForID(this.instanceId);
        }
        return this.cachedName;
    }

    @Override
    public boolean supportsRumble()
    {
        if(this.gamepad == null)
            return false;
        int props = SDLGamepad.SDL_GetGamepadProperties(this.gamepad);
        return SDLProperties.SDL_GetBooleanProperty(props, SDLGamepad.SDL_PROP_GAMEPAD_CAP_RUMBLE_BOOLEAN, false);
    }

    @Override
    protected boolean internalRumble(float lowFrequency, float highFrequency, int timeInMs)
    {
        lowFrequency = Mth.clamp(lowFrequency, 0.0F, 1.0F);
        highFrequency = Mth.clamp(highFrequency, 0.0F, 1.0F);
        return SDLGamepad.SDL_RumbleGamepad(this.gamepad, (short) (0xFFFF * lowFrequency), (short) (0xFFFF * highFrequency), timeInMs);
    }

    @Override
    protected float internalGetLTriggerValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, 0, 1);
    }

    @Override
    protected float internalGetRTriggerValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, 0, 1);
    }

    @Override
    protected float internalGetLThumbStickXValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, -1, 1);
    }

    @Override
    protected float internalGetLThumbStickYValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, -1, 1);
    }

    @Override
    protected float internalGetRThumbStickXValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, -1, 1);
    }

    @Override
    protected float internalGetRThumbStickYValue()
    {
        return Mth.clamp(SDLGamepad.SDL_GetGamepadAxis(this.gamepad, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY) / (float) SDLJoystick.SDL_JOYSTICK_AXIS_MAX, -1, 1);
    }

    @Override
    public DeviceInfo getInfo()
    {
        if(this.info == null)
        {
            String name = SDLGamepad.SDL_GetGamepadName(this.gamepad);
            String serial = SDLGamepad.SDL_GetGamepadSerial(this.gamepad);
            int type = SDLGamepad.SDL_GetGamepadType(this.gamepad);
            short vendor = SDLGamepad.SDL_GetGamepadVendor(this.gamepad);
            short product = SDLGamepad.SDL_GetGamepadProduct(this.gamepad);
            short productVersion = SDLGamepad.SDL_GetGamepadProductVersion(this.gamepad);
            short firmware = SDLGamepad.SDL_GetGamepadFirmwareVersion(this.gamepad);
            int buttons = SDLJoystick.SDL_GetNumJoystickButtons(this.gamepad);
            int axes = SDLJoystick.SDL_GetNumJoystickAxes(this.gamepad);
            this.info = new DeviceInfo(name, this.getGuidString(), serial, type, vendor, product, productVersion, firmware, buttons, axes);
        }
        return this.info;
    }

    private String getGuidString()
    {
        try(MemoryStack stack = MemoryStack.stackPush())
        {
            SDL_GUID guid = SDLJoystick.SDL_GetJoystickGUIDForID(this.instanceId, SDL_GUID.create());
            ByteBuffer buffer = stack.malloc(33);
            SDLGUID.SDL_GUIDToString(guid, buffer);
            return MemoryUtil.memASCII(buffer);
        }
    }

    private void updateGyroState()
    {
        this.gyro = false;
        if(this.gamepad == null)
            return;
        if(!SDLGamepad.SDL_GamepadHasSensor(this.gamepad, SDLSensor.SDL_SENSOR_GYRO))
            return;
        if(!SDLGamepad.SDL_SetGamepadSensorEnabled(this.gamepad, SDLSensor.SDL_SENSOR_GYRO, true))
            return;
        this.gyro = true;
    }

    @Override
    public boolean supportsGyroscope()
    {
        return this.gyro;
    }

    @Override
    public Vector3f getGyroscope()
    {
        try(MemoryStack stack = MemoryStack.stackPush())
        {
            FloatBuffer gyro = stack.mallocFloat(3);
            if(SDLGamepad.SDL_GetGamepadSensorData(this.gamepad, SDLSensor.SDL_SENSOR_GYRO, gyro))
            {
                float x = gyro.get(0);
                float y = gyro.get(1);
                float z = gyro.get(2);
                return new Vector3f(x, y, z);
            }
        }
        return super.getGyroscope();
    }
}
