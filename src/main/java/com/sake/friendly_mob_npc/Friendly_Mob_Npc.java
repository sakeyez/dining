package com.sake.friendly_mob_npc;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.sake.friendly_mob_npc.network.NpcPacketHandler;

@Mod("friendly_mob_npc")
public class Friendly_Mob_Npc {
    public void friendly_mob_npc() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NpcEntities.register(eventBus);
        NpcItems.register(eventBus);

        NpcPacketHandler.register();
    }
}