package com.sake.npc;

import com.sake.npc.warrior.WarriorNPCRenderer;
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

    // 【新增】为战士伙伴的盔甲创建专属的“渲染蓝图”位置
    public static final ModelLayerLocation WARRIOR_INNER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "warrior_npc_inner_armor"), "main");
    public static final ModelLayerLocation WARRIOR_OUTER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("npc", "warrior_npc_outer_armor"), "main");


    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // (其他NPC的注册保持不变)
        event.registerEntityRenderer(NpcEntities.ZOMBIE_NPC.get(), ZombieRenderer::new);
        event.registerEntityRenderer(NpcEntities.SKELETON_NPC.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(NpcEntities.BLAZE_NPC.get(), BlazeRenderer::new);
        event.registerEntityRenderer(NpcEntities.SLIME_NPC.get(), SlimeRenderer::new);
        event.registerEntityRenderer(NpcEntities.CREEPER_NPC.get(), CreeperRenderer::new);
        event.registerEntityRenderer(NpcEntities.ENDERMAN_NPC.get(), EndermanRenderer::new);
        event.registerEntityRenderer(NpcEntities.WARRIOR_NPC.get(), WarriorNPCRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册主模型
        event.registerLayerDefinition(WARRIOR_NPC_LAYER, () -> LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));

        // 【新增】注册盔甲模型的具体形状
        // 我们使用标准的盔甲形状，只是给它一个我们自己的ID
        event.registerLayerDefinition(WARRIOR_INNER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0f), 64, 32));
        event.registerLayerDefinition(WARRIOR_OUTER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0f), 64, 32));
    }
}