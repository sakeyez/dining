// 文件路径: main/java/com/sake/friendly_mob_npc/Friendly_Mob_Npc.java

package com.sake.friendly_mob_npc;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.sake.friendly_mob_npc.network.NpcPacketHandler;

@Mod("friendly_mob_npc")
public class Friendly_Mob_Npc {
    // 【核心修正】将 public void friendly_mob_npc() 修改为正确的构造函数 public Friendly_Mob_Npc()
    public Friendly_Mob_Npc() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        NpcEntities.register(eventBus);
        NpcItems.register(eventBus);

        NpcPacketHandler.register();
    }
}