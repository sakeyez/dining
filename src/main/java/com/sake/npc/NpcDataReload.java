package com.sake.npc;

import com.sake.npc.zombie.ZombieNpcTrades;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "npc", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcDataReload {
    @SubscribeEvent
    public static void onAddReload(AddReloadListenerEvent e) {
        e.addListener(new ZombieNpcTrades());
    }


}