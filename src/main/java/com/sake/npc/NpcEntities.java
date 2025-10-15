package com.sake.npc;

import com.sake.npc.blaze.BlazeNPCEntity;
import com.sake.npc.creeper.CreeperNPCEntity;
import com.sake.npc.enderman.EndermanNPCEntity;
import com.sake.npc.skeleton.SkeletonNPCEntity;
import com.sake.npc.slime.SlimeNPCEntity;
import com.sake.npc.zombie.ZombieNPCEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class NpcEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "npc");

    // ... (其他NPC的注册保持不变)
    public static final RegistryObject<EntityType<ZombieNPCEntity>> ZOMBIE_NPC = ENTITIES.register("zombie_npc",
            () -> EntityType.Builder.<ZombieNPCEntity>of(ZombieNPCEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("zombie_npc"));
    public static final RegistryObject<EntityType<SkeletonNPCEntity>> SKELETON_NPC = ENTITIES.register("skeleton_npc",
            () -> EntityType.Builder.<SkeletonNPCEntity>of(SkeletonNPCEntity::new, MobCategory.CREATURE).sized(0.6F, 1.99F).build("skeleton_npc"));
    public static final RegistryObject<EntityType<BlazeNPCEntity>> BLAZE_NPC = ENTITIES.register("blaze_npc",
            () -> EntityType.Builder.<BlazeNPCEntity>of(BlazeNPCEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("blaze_npc"));
    public static final RegistryObject<EntityType<SlimeNPCEntity>> SLIME_NPC = ENTITIES.register("slime_npc",
            () -> EntityType.Builder.<SlimeNPCEntity>of(SlimeNPCEntity::new, MobCategory.CREATURE).sized(2.04F, 2.04F).build("slime_npc"));
    public static final RegistryObject<EntityType<CreeperNPCEntity>> CREEPER_NPC = ENTITIES.register("creeper_npc",
            () -> EntityType.Builder.<CreeperNPCEntity>of(CreeperNPCEntity::new, MobCategory.CREATURE).sized(0.6F, 1.7F).build("creeper_npc"));
    public static final RegistryObject<EntityType<EndermanNPCEntity>> ENDERMAN_NPC = ENTITIES.register("enderman_npc",
            () -> EntityType.Builder.<EndermanNPCEntity>of(EndermanNPCEntity::new, MobCategory.CREATURE).sized(0.6F, 2.9F).build("enderman_npc"));

    // --- 【核心修正】 ---
    // 我们将注册方法改回 .sized()，这会修复编译错误
    public static final RegistryObject<EntityType<com.sake.npc.warrior.WarriorNPCEntity>> WARRIOR_NPC = ENTITIES.register("warrior_npc",
            () -> EntityType.Builder.<com.sake.npc.warrior.WarriorNPCEntity>of(com.sake.npc.warrior.WarriorNPCEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F) // 使用 .sized() 定义基础尺寸
                    .build("warrior_npc"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}