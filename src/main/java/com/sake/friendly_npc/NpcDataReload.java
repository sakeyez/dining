package com.sake.friendly_npc;

import com.sake.friendly_npc.zombie.ZombieNpcTrades;
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