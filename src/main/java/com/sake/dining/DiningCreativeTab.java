package com.sake.dining;

import com.sake.npc.NpcItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class DiningCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dining.MODID);

    public static final RegistryObject<CreativeModeTab> DINING_TAB = TABS.register("dining_tab", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(DiningItems.ZOMBIE_TOKEN.get()))
                    .title(Component.literal("Dining Mod"))
                    .displayItems((params, output) -> {
                        // Dining本mod的物品
                        output.accept(DiningItems.ZOMBIE_TOKEN.get());
                        output.accept(DiningItems.SKELETON_TOKEN.get());
                        output.accept(DiningItems.CREEPER_TOKEN.get());
                        output.accept(DiningItems.ENDERMAN_TOKEN.get());
                        output.accept(DiningItems.SLIME_TOKEN.get());
                        output.accept(DiningItems.BLAZE_TOKEN.get());
                        output.accept(DiningItems.COIN.get());
                        output.accept(DiningItems.POWDER.get());

                        // 添加NPC的刷怪蛋
                        output.accept(NpcItems.ZOMBIE_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.SKELETON_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.BLAZE_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.SLIME_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.CREEPER_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.ENDERMAN_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.WARRIOR_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.WARRIOR_SUMMON_ITEM.get());
                        output.accept(NpcItems.WITHER_WARRIOR_NPC_SPAWN_EGG.get());
                        output.accept(NpcItems.WITHER_WARRIOR_SUMMON_ITEM.get());
                    })
                    .build()
    );

    public static void register() {
        TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}