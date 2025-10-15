package com.sake.dining;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class DiningItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "dining");

    public static final RegistryObject<Item> ZOMBIE_TOKEN = ITEMS.register("zombie_token", () ->
            new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SKELETON_TOKEN = ITEMS.register("skeleton_token", () ->
            new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> CREEPER_TOKEN = ITEMS.register("creeper_token", () ->
            new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> ENDERMAN_TOKEN = ITEMS.register("enderman_token", () ->
            new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> SLIME_TOKEN = ITEMS.register("slime_token", () ->
            new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> BLAZE_TOKEN = ITEMS.register("blaze_token", () ->
            new Item(new Item.Properties().stacksTo(64)));

    // ------- 这里是修复点，直接传 RegistryObject -------



    public static void register() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
