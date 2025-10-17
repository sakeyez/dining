package com.sake.friendly_npc;

import net.minecraft.world.entity.Mob; // 【核心修正】导入 Mob 类
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.monster.WitherSkeleton;

@Mod.EventBusSubscriber(modid = "npc", bus = Mod.EventBusSubscriber.Bus.MOD)
public class NpcEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // ... (其他NPC保持不变)
        event.put(NpcEntities.ZOMBIE_NPC.get(), Zombie.createAttributes().build());
        event.put(NpcEntities.SKELETON_NPC.get(), Skeleton.createAttributes().build());
        event.put(NpcEntities.BLAZE_NPC.get(), Blaze.createAttributes().build());
        event.put(NpcEntities.SLIME_NPC.get(), Monster.createMonsterAttributes().build());
        event.put(NpcEntities.CREEPER_NPC.get(), Creeper.createAttributes().build());
        event.put(NpcEntities.ENDERMAN_NPC.get(), EnderMan.createAttributes().build());


        event.put(NpcEntities.WARRIOR_NPC.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // .add(ForgeMod.ATTACK_RANGE.get(), 0.0) // <-- 移除这一行
                .build());

        event.put(NpcEntities.WITHER_WARRIOR_NPC.get(), WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0) // 凋零骷髅基础攻击力更高
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .build());

    }
}