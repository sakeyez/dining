package com.sake.npc;

import com.sake.npc.blaze.BlazeNPCEntity;
import com.sake.npc.creeper.CreeperNPCEntity;
import com.sake.npc.enderman.EndermanNPCEntity;
import com.sake.npc.skeleton.SkeletonNPCEntity;
import com.sake.npc.slime.SlimeNPCEntity;
import com.sake.npc.zombie_warrior.WarriorNPCEntity;
import com.sake.npc.wither_warrior.WitherWarriorNPCEntity;
import com.sake.npc.zombie.ZombieNPCEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class NpcEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "npc");

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
    public static final RegistryObject<EntityType<WarriorNPCEntity>> WARRIOR_NPC = ENTITIES.register("warrior_npc",
            () -> EntityType.Builder.of(WarriorNPCEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build("warrior_npc"));

    public static final RegistryObject<EntityType<WitherWarriorNPCEntity>> WITHER_WARRIOR_NPC = ENTITIES.register("wither_warrior_npc",
            () -> EntityType.Builder.of(WitherWarriorNPCEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.4F)
                    .build("wither_warrior_npc"));


    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}