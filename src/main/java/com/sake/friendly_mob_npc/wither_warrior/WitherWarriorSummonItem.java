package com.sake.friendly_mob_npc.wither_warrior;

import com.sake.friendly_mob_npc.NpcEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WitherWarriorSummonItem extends Item {
    public WitherWarriorSummonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.hasUUID("WarriorUUID") && !tag.contains("WarriorData")) {
            player.sendSystemMessage(Component.literal("§c你已经召唤出你的凋零战士伙伴了！"));
            return InteractionResult.FAIL;
        }

        WitherWarriorNPCEntity warrior = NpcEntities.WITHER_WARRIOR_NPC.get().create(level);
        if (warrior == null) return InteractionResult.FAIL;

        if (tag.hasUUID("WarriorUUID") && tag.contains("WarriorData")) {
            warrior.readAdditionalSaveData(tag.getCompound("WarriorData"));
            player.sendSystemMessage(Component.literal("§a凋零战士伙伴已召唤！"));
        } else {
            player.sendSystemMessage(Component.literal("§b一个忠诚的凋零战士响应了你的召唤！"));
        }

        warrior.setPos(context.getClickedPos().above().getCenter());
        warrior.tame(player);
        tag.putUUID("WarriorUUID", warrior.getUUID());
        tag.remove("WarriorData");

        level.addFreshEntity(warrior);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (player.level().isClientSide) return InteractionResult.PASS;

        if (interactionTarget instanceof WitherWarriorNPCEntity warrior && warrior.isOwnedBy(player)) {
            CompoundTag tag = stack.getOrCreateTag();

            if (!tag.hasUUID("WarriorUUID")) {
                tag.putUUID("WarriorUUID", warrior.getUUID());
            }

            if (warrior.getUUID().equals(tag.getUUID("WarriorUUID"))) {
                CompoundTag warriorData = new CompoundTag();
                warrior.addAdditionalSaveData(warriorData);
                tag.put("WarriorData", warriorData);

                warrior.discard();
                player.sendSystemMessage(Component.literal("§e凋零战士伙伴已收回。"));
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("§c这不是你这个召唤核心绑定的伙伴。"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // 检查物品是否有NBT数据，并且数据中是否【不包含】"WarriorData"
        // 注意：我们还需要确保它绑定了一个UUID，这样全新的空核心就不会发光
        return stack.hasTag() && stack.getTag().hasUUID("WarriorUUID") && !stack.getTag().contains("WarriorData");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID("WarriorUUID")) {
            if (tag.contains("WarriorData")) {
                int warriorLevel = tag.getCompound("WarriorData").getInt("WarriorLevel");
                tooltip.add(Component.literal("存储的伙伴:").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("  等级 " + warriorLevel).withStyle(ChatFormatting.AQUA));
            } else {
                tooltip.add(Component.literal("伙伴已召唤").withStyle(ChatFormatting.GREEN));
            }
            tooltip.add(Component.literal("灵魂绑定").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            tooltip.add(Component.literal("一个空的召唤核心。").withStyle(ChatFormatting.GRAY));
        }
    }
}