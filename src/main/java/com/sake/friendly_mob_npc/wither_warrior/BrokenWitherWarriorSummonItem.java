package com.sake.friendly_mob_npc.wither_warrior;

import com.sake.friendly_mob_npc.NpcItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.sake.friendly_mob_npc.network.ClientboundDisplayItemActivationPacket;
import com.sake.friendly_mob_npc.network.NpcPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class BrokenWitherWarriorSummonItem extends Item {
    public BrokenWitherWarriorSummonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack brokenStack = player.getItemInHand(hand);
        ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);

        if (!level.isClientSide && offhandStack.is(Items.TOTEM_OF_UNDYING)) {
            if (brokenStack.hasTag()) {
                ItemStack repairedStack = new ItemStack(NpcItems.WITHER_WARRIOR_SUMMON_ITEM.get());
                repairedStack.setTag(brokenStack.getTag().copy());

                if (!player.getAbilities().instabuild) {
                    offhandStack.shrink(1);
                }

                player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.2), player.getZ(), 80, 0.5, 0.8, 0.5, 0.15);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    NpcPacketHandler.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new ClientboundDisplayItemActivationPacket(repairedStack)
                    );
                }
                player.sendSystemMessage(Component.literal("§a灵魂连接被重塑了！"));
                return InteractionResultHolder.success(repairedStack);
            }
        }
        return InteractionResultHolder.fail(brokenStack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("WarriorData")) {
            CompoundTag warriorData = tag.getCompound("WarriorData");
            int warriorLevel = warriorData.getInt("WarriorLevel");
            tooltip.add(Component.literal("一个沉睡的灵魂:").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  等级 " + warriorLevel).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("一个失去了灵魂连接的核心。").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("§7(副手持不死图腾右键修复)").withStyle(ChatFormatting.DARK_GRAY));
    }
}