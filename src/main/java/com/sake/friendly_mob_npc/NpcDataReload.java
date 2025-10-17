package com.sake.friendly_mob_npc;

import com.sake.friendly_mob_npc.zombie.ZombieNpcTrades;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "frienly_mob_npc", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcDataReload {
    @SubscribeEvent
    public static void onAddReload(AddReloadListenerEvent e) {
        e.addListener(new ZombieNpcTrades());
    }


}