package com.sake.npc;

import com.sake.dining.event.MobFinishedEatingEvent;
import com.sake.npc.warrior.WarriorNPCEntity;
import com.sake.npc.wither_warrior.WitherWarriorNPCEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundTag;
import com.sake.npc.wither_warrior.WitherWarriorSummonItem;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "npc", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcUpgradeHandler {
    // 僵尸战士的升级食物
    private static final ResourceLocation PEARL_MEAT_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "sweet_and_sour_ender_pearls");
    private static final ResourceLocation GOLDEN_SALAD_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "golden_salad");

    // 【核心修改 1】凋零骷髅战士的升级食物
    private static final ResourceLocation FONDANT_SPIDER_EYE_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "fondant_spider_eye");
    private static final ResourceLocation KNIGHT_STEAK_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "pan_seared_knight_steak");

    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        // 僵尸战士的逻辑
        if (event.getMob() instanceof WarriorNPCEntity w && !w.level().isClientSide()) {
            ResourceLocation f = BuiltInRegistries.BLOCK.getKey(event.getFoodState().getBlock());
            Player p = w.level().getNearestPlayer(w, 16);
            int l = w.getLevel();
            int n = -1;
            String m = "";
            if (w.isRitualPending()) {
                if (l == 1 && f.equals(PEARL_MEAT_ID)) {
                    n = 2;
                    m = "§5吞下珍珠的瞬间，它的身影变得飘忽不定，仿佛掌握了空间的力量！";
                } else if (l == 2 && f.equals(GOLDEN_SALAD_ID)) {
                    n = 3;
                    m = "§6金色的光辉包裹了它！它突破了凡物的极限，成为了真正的传奇战士！";
                }
            }
            if (n != -1) {
                w.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                if (w.level() instanceof ServerLevel s)
                    s.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, w.getRandomX(1.0), w.getY(0.5), w.getRandomZ(1.0), 30, 0.3, 0.5, 0.3, 0.5);
                w.setLevel(n);
                if (p != null) p.sendSystemMessage(Component.literal(m));
            }
        }
        // 【核心修改 2】凋零骷髅战士的逻辑
        else if (event.getMob() instanceof WitherWarriorNPCEntity w && !w.level().isClientSide()) {
            ResourceLocation f = BuiltInRegistries.BLOCK.getKey(event.getFoodState().getBlock());
            Player p = w.level().getNearestPlayer(w, 16);
            int l = w.getLevel();
            int n = -1;
            String m = "";
            if (w.isRitualPending()) {
                if (l == 1 && f.equals(FONDANT_SPIDER_EYE_ID)) {
                    n = 2;
                    m = "§8吞下蛛眼的瞬间，它的骨骼渗出剧毒，获得了掌控毒素的力量！";
                } else if (l == 2 && f.equals(KNIGHT_STEAK_ID)) {
                    n = 3;
                    m = "§7黑暗的能量包裹了它！它突破了凋零的诅咒，成为了真正的战场主宰！";
                }
            }
            if (n != -1) {
                w.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 1.0F); // 使用凋零的音效
                if (w.level() instanceof ServerLevel s)
                    s.sendParticles(ParticleTypes.SOUL, w.getRandomX(1.0), w.getY(0.5), w.getRandomZ(1.0), 50, 0.3, 0.5, 0.3, 0.5); // 使用灵魂粒子
                w.setLevel(n);
                if (p != null) p.sendSystemMessage(Component.literal(m));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof TamableAnimal warrior) {
            boolean isCorrectWarriorType = warrior instanceof WarriorNPCEntity || warrior instanceof WitherWarriorNPCEntity;
            if (!isCorrectWarriorType) {
                return;
            }

            if (warrior.getOwner() instanceof Player owner) {
                UUID warriorUUID = warrior.getUUID();

                for (int i = 0; i < owner.getInventory().getContainerSize(); ++i) {
                    ItemStack stack = owner.getInventory().getItem(i);

                    boolean isNormalWarriorSummonItem = stack.getItem() instanceof com.sake.npc.warrior.WarriorSummonItem;
                    boolean isWitherWarriorSummonItem = stack.getItem() instanceof WitherWarriorSummonItem;

                    if (isNormalWarriorSummonItem || isWitherWarriorSummonItem) {
                        CompoundTag tag = stack.getTag();
                        if (tag != null && tag.hasUUID("WarriorUUID") && tag.getUUID("WarriorUUID").equals(warriorUUID)) {

                            ItemStack brokenStack;
                            if (warrior instanceof WarriorNPCEntity) {
                                brokenStack = new ItemStack(NpcItems.BROKEN_WARRIOR_SUMMON_ITEM.get());
                            } else {
                                brokenStack = new ItemStack(NpcItems.BROKEN_WITHER_WARRIOR_SUMMON_ITEM.get());
                            }

                            CompoundTag warriorData = new CompoundTag();
                            warrior.addAdditionalSaveData(warriorData);

                            CompoundTag brokenTag = new CompoundTag();
                            brokenTag.putUUID("WarriorUUID", warriorUUID);
                            brokenTag.put("WarriorData", warriorData);
                            brokenStack.setTag(brokenTag);

                            owner.getInventory().setItem(i, brokenStack);
                            owner.playSound(SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
                            owner.sendSystemMessage(Component.literal("§c你与战士伙伴的灵魂连接断开了..."));
                            return;
                        }
                    }
                }
            }
        }
    }
}