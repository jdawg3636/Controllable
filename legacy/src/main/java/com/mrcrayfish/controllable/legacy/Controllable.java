package com.mrcrayfish.controllable.legacy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@cpw.mods.fml.common.network.NetworkMod(clientSideRequired=false, serverSideRequired=false)
@Mod(modid="controllable", name="Controllable", version="0.1.1", acceptedMinecraftVersions="[1.5.2]")
public final class Controllable {
    @SidedProxy(clientSide="com.mrcrayfish.controllable.legacy.ClientProxy", serverSide="com.mrcrayfish.controllable.legacy.CommonProxy")
    public static CommonProxy proxy;
    @Mod.Init public void init(FMLInitializationEvent event) { proxy.init(); }
}
