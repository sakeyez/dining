package com.sake.friendly_mob_npc.zombie_warrior;

import com.sake.friendly_mob_npc.NpcEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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

public class WarriorSummonItem extends Item {
    public WarriorSummonItem(Properties properties) {
        super(properties);
    }

    // --- 核心修改：全新的召唤/释放逻辑 ---
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrCreateTag();

        // 如果物品已绑定，但战士在外面，则阻止召唤新的
        if (tag.hasUUID("WarriorUUID") && !tag.contains("WarriorData")) {
            player.sendSystemMessage(Component.literal("§c你已经召唤出你的战士伙伴了！"));
            return InteractionResult.FAIL;
        }

        WarriorNPCEntity warrior = NpcEntities.WARRIOR_NPC.get().create(level);
        if (warrior == null) return InteractionResult.FAIL;

        // 如果物品已绑定且存有数据，则释放
        if (tag.hasUUID("WarriorUUID") && tag.contains("WarriorData")) {
            warrior.readAdditionalSaveData(tag.getCompound("WarriorData"));
            player.sendSystemMessage(Component.literal("§a战士伙伴已召唤！"));
        }
        // 否则，召唤一个全新的
        else {
            player.sendSystemMessage(Component.literal("§b一个忠诚的战士响应了你的召唤！"));
        }

        warrior.setPos(context.getClickedPos().above().getCenter());
        warrior.tame(player);
        // 【灵魂绑定】将新战士的UUID存入物品
        tag.putUUID("WarriorUUID", warrior.getUUID());
        // 清除存储的数据（因为战士现在在外面）
        tag.remove("WarriorData");

        level.addFreshEntity(warrior);
        player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    // --- 核心修改：全新的收回逻辑 ---
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (player.level().isClientSide) return InteractionResult.PASS;

        if (interactionTarget instanceof WarriorNPCEntity warrior && warrior.isOwnedBy(player)) {
            CompoundTag tag = stack.getOrCreateTag();

            // 如果物品未绑定，则绑定到这个战士身上
            if (!tag.hasUUID("WarriorUUID")) {
                tag.putUUID("WarriorUUID", warrior.getUUID());
            }

            // 【灵魂校验】检查目标的UUID是否和物品绑定的UUID一致
            if (warrior.getUUID().equals(tag.getUUID("WarriorUUID"))) {
                CompoundTag warriorData = new CompoundTag();
                warrior.addAdditionalSaveData(warriorData);
                tag.put("WarriorData", warriorData);

                warrior.discard();
                player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
                player.sendSystemMessage(Component.literal("§e战士伙伴已收回。"));
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("§c这不是你这个召唤核心绑定的伙伴。"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    // --- 核心修改：全新的提示信息 ---
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