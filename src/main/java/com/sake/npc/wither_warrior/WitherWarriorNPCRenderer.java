package com.sake.npc.wither_warrior;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.npc.NpcClientEventSubscriber;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class WitherWarriorNPCRenderer extends HumanoidMobRenderer<WitherWarriorNPCEntity, WitherWarriorNPCModel<WitherWarriorNPCEntity>> {

    // 【修复】使用正确的构造函数
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");

    public WitherWarriorNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new WitherWarriorNPCModel<>(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(NpcClientEventSubscriber.WITHER_WARRIOR_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(NpcClientEventSubscriber.WITHER_WARRIOR_OUTER_ARMOR_LAYER)),
                context.getModelManager()
        ));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(WitherWarriorNPCEntity entity) {
        return TEXTURE_LOCATION;
    }

    @Override
    protected void setupRotations(WitherWarriorNPCEntity warrior, PoseStack poseStack, float ageInTicks, float bodyYaw, float partialTicks) {
        super.setupRotations(warrior, poseStack, ageInTicks, bodyYaw, partialTicks);
        float scale = warrior.getScale();
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}