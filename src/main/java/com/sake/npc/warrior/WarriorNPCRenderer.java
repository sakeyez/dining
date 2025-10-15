package com.sake.npc.warrior;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.npc.NpcClientEventSubscriber;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer; // 【新增】导入物品渲染层
import net.minecraft.resources.ResourceLocation;

public class WarriorNPCRenderer extends HumanoidMobRenderer<WarriorNPCEntity, WarriorNPCModel<WarriorNPCEntity>> {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

    public WarriorNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new WarriorNPCModel<>(context.bakeLayer(NpcClientEventSubscriber.WARRIOR_NPC_LAYER)), 0.5F);

        // 添加盔甲渲染层 (保持不变)
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(NpcClientEventSubscriber.WARRIOR_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(NpcClientEventSubscriber.WARRIOR_OUTER_ARMOR_LAYER)),
                context.getModelManager()
        ));

        // 【核心修正】手动为这个渲染器添加一个手中物品的渲染层
        // 这会让它能够正确地显示剑、斧、食物等任何拿在手里的东西
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(WarriorNPCEntity entity) {
        return TEXTURE_LOCATION;
    }

    // (体型缩放的代码保持不变)
    @Override
    protected void setupRotations(WarriorNPCEntity warrior, PoseStack poseStack, float ageInTicks, float bodyYaw, float partialTicks) {
        super.setupRotations(warrior, poseStack, ageInTicks, bodyYaw, partialTicks);
        float scale = warrior.getScale();
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}