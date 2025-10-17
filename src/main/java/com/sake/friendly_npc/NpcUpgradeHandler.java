package com.sake.friendly_npc;

import com.sake.dining.event.MobFinishedEatingEvent;
import com.sake.friendly_npc.wither_warrior.WitherWarriorNPCEntity;
import com.sake.friendly_npc.wither_warrior.WitherWarriorSummonItem;
import com.sake.friendly_npc.zombie_warrior.WarriorNPCEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = "npc", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcUpgradeHandler {

    private static final List<Consumer<TickEvent.ServerTickEvent>> delayedTasks = new ArrayList<>();

    // ... (升级相关的代码保持不变) ...
    private static final ResourceLocation PEARL_MEAT_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "sweet_and_sour_ender_pearls");
    private static final ResourceLocation GOLDEN_SALAD_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "golden_salad");
    private static final ResourceLocation FONDANT_SPIDER_EYE_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "fondant_spider_eye");
    private static final ResourceLocation KNIGHT_STEAK_ID = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "pan_seared_knight_steak");

    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        // ... (这部分代码没有改动，保持原样) ...
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
        } else if (event.getMob() instanceof WitherWarriorNPCEntity w && !w.level().isClientSide()) {
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
                w.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 1.0F);
                if (w.level() instanceof ServerLevel s)
                    s.sendParticles(ParticleTypes.SOUL, w.getRandomX(1.0), w.getY(0.5), w.getRandomZ(1.0), 50, 0.3, 0.5, 0.3, 0.5);
                w.setLevel(n);
                if (p != null) p.sendSystemMessage(Component.literal(m));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }

        if (entity instanceof TamableAnimal warrior) {
            boolean isCorrectWarriorType = warrior instanceof WarriorNPCEntity || warrior instanceof WitherWarriorNPCEntity;
            if (!isCorrectWarriorType) {
                return;
            }

            // 将检查逻辑放入一个延迟任务中
            synchronized (delayedTasks) {
                delayedTasks.add(tick -> {
                    // === 【核心修复】 ===
                    // 在下一个游戏刻，我们检查这个生物体是否“真的死了”。
                    // 真正死亡的生物生命值会 <= 0。
                    // 被复活的生物生命值会 > 0。
                    // isDeadOrDying() 方法本质上就是检查 getHealth() <= 0，这是最可靠的标准。
                    if (warrior.isDeadOrDying()) {
                        handleBrokenCore(warrior);
                    }
                });
            }
        }
    }

    // 监听服务器的每个tick，用于执行我们的延迟任务
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            synchronized (delayedTasks) {
                if (!delayedTasks.isEmpty()) {
                    delayedTasks.forEach(task -> task.accept(event));
                    delayedTasks.clear();
                }
            }
        }
    }

    // 将破碎核心的逻辑提取到一个单独的方法中，保持代码整洁
    private static void handleBrokenCore(TamableAnimal warrior) {
        if (warrior.getOwner() instanceof Player owner) {
            UUID warriorUUID = warrior.getUUID();

            for (int i = 0; i < owner.getInventory().getContainerSize(); ++i) {
                ItemStack stack = owner.getInventory().getItem(i);

                boolean isNormalWarriorSummonItem = stack.getItem() instanceof com.sake.friendly_npc.zombie_warrior.WarriorSummonItem;
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