package com.sake.friendly_npc;

import com.sake.friendly_npc.zombie_warrior.BrokenSummonItem; // 【新增】
import com.sake.friendly_npc.zombie_warrior.WarriorSummonItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.sake.friendly_npc.wither_warrior.BrokenWitherWarriorSummonItem;
import com.sake.friendly_npc.wither_warrior.WitherWarriorSummonItem;

public class NpcItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "npc");

    // ... (所有刷怪蛋的注册保持不变) ...
    public static final RegistryObject<Item> ZOMBIE_NPC_SPAWN_EGG = ITEMS.register("zombie_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.ZOMBIE_NPC, 0x00a000, 0x799c65, new Item.Properties()));
    public static final RegistryObject<Item> SKELETON_NPC_SPAWN_EGG = ITEMS.register("skeleton_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.SKELETON_NPC, 0xC1C1C1, 0x494949, new Item.Properties()));
    public static final RegistryObject<Item> BLAZE_NPC_SPAWN_EGG = ITEMS.register("blaze_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.BLAZE_NPC, 0xFFA500, 0xFFFF00, new Item.Properties()));
    public static final RegistryObject<Item> SLIME_NPC_SPAWN_EGG = ITEMS.register("slime_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.SLIME_NPC, 0x7CFC00, 0x006400, new Item.Properties()));
    public static final RegistryObject<Item> CREEPER_NPC_SPAWN_EGG = ITEMS.register("creeper_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.CREEPER_NPC, 0x00FF00, 0x000000, new Item.Properties()));
    public static final RegistryObject<Item> ENDERMAN_NPC_SPAWN_EGG = ITEMS.register("enderman_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.ENDERMAN_NPC, 0x000000, 0x8A2BE2, new Item.Properties()));
    public static final RegistryObject<Item> WARRIOR_NPC_SPAWN_EGG = ITEMS.register("warrior_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.WARRIOR_NPC, 0x3B4C35, 0x6C8263, new Item.Properties()));

    public static final RegistryObject<Item> WARRIOR_SUMMON_ITEM = ITEMS.register("warrior_summon_item",
            () -> new WarriorSummonItem(new Item.Properties().stacksTo(1))
    );

    // 【核心修正】使用我们新的 BrokenSummonItem 类
    public static final RegistryObject<Item> BROKEN_WARRIOR_SUMMON_ITEM = ITEMS.register("broken_warrior_summon_item",
            () -> new BrokenSummonItem(new Item.Properties().stacksTo(1))
    );

    // ... (WARRIOR_NPC_SPAWN_EGG) ...
    public static final RegistryObject<Item> WITHER_WARRIOR_NPC_SPAWN_EGG = ITEMS.register("wither_warrior_npc_spawn_egg", () -> new ForgeSpawnEggItem(NpcEntities.WITHER_WARRIOR_NPC, 0x141414, 0x494949, new Item.Properties()));

    // ... (WARRIOR_SUMMON_ITEM) ...
    public static final RegistryObject<Item> WITHER_WARRIOR_SUMMON_ITEM = ITEMS.register("wither_warrior_summon_item",
            () -> new WitherWarriorSummonItem(new Item.Properties().stacksTo(1))
    );

    // ... (BROKEN_WARRIOR_SUMMON_ITEM) ...
    public static final RegistryObject<Item> BROKEN_WITHER_WARRIOR_SUMMON_ITEM = ITEMS.register("broken_wither_warrior_summon_item",
            () -> new BrokenWitherWarriorSummonItem(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}