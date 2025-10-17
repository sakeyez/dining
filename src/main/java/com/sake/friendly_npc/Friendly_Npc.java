package com.sake.friendly_npc;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.sake.friendly_npc.network.NpcPacketHandler;

@Mod("friendly_npc")
public class Friendly_Npc {
    public void friendly_npc() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NpcEntities.register(eventBus);
        NpcItems.register(eventBus);

        NpcPacketHandler.register();
    }
}