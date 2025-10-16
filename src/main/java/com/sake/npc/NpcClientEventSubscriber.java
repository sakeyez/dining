package com.sake.npc;

import com.sake.npc.warrior.WarriorNPCRenderer;
import com.sake.npc.wither_warrior.WitherWarriorNPCRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "npc", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NpcClientEventSubscriber {

    public static final ModelLayerLocation WARRIOR_NPC_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "warrior_npc"), "main");
    public static final ModelLayerLocation WARRIOR_INNER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "warrior_npc_inner_armor"), "main");
    public static final ModelLayerLocation WARRIOR_OUTER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "warrior_npc_outer_armor"), "main");

    // 凋零骷髅战士只需要盔甲层，主体模型我们用原版的
    public static final ModelLayerLocation WITHER_WARRIOR_INNER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "wither_warrior_npc_inner_armor"), "main");
    public static final ModelLayerLocation WITHER_WARRIOR_OUTER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "wither_warrior_npc_outer_armor"), "main");


    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NpcEntities.ZOMBIE_NPC.get(), ZombieRenderer::new);
        event.registerEntityRenderer(NpcEntities.SKELETON_NPC.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(NpcEntities.BLAZE_NPC.get(), BlazeRenderer::new);
        event.registerEntityRenderer(NpcEntities.SLIME_NPC.get(), SlimeRenderer::new);
        event.registerEntityRenderer(NpcEntities.CREEPER_NPC.get(), CreeperRenderer::new);
        event.registerEntityRenderer(NpcEntities.ENDERMAN_NPC.get(), EndermanRenderer::new);
        event.registerEntityRenderer(NpcEntities.WARRIOR_NPC.get(), WarriorNPCRenderer::new);
        event.registerEntityRenderer(NpcEntities.WITHER_WARRIOR_NPC.get(), WitherWarriorNPCRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 僵尸战士模型
        event.registerLayerDefinition(WARRIOR_NPC_LAYER, () -> LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
        event.registerLayerDefinition(WARRIOR_INNER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0f), 64, 32));
        event.registerLayerDefinition(WARRIOR_OUTER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0f), 64, 32));

        // 【核心修正】我们不再需要为凋零骷髅战士的主体注册模型层，因为它现在使用原版的层

        // 凋零骷髅战士盔甲模型
        event.registerLayerDefinition(WITHER_WARRIOR_INNER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0f), 64, 32));
        event.registerLayerDefinition(WITHER_WARRIOR_OUTER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0f), 64, 32));
    }
}