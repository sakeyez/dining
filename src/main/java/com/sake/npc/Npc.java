package com.sake.npc;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("npc")
public class Npc {
    public Npc() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NpcEntities.register(eventBus);
        NpcItems.register(eventBus);


    }
}