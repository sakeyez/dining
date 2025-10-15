package com.sake.npc;

import com.sake.dining.event.MobFinishedEatingEvent;
import com.sake.npc.warrior.WarriorNPCEntity;
import com.sake.npc.warrior.WarriorSummonItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "npc", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NpcUpgradeHandler {
    // ... (onMobFinishedEating 方法保持不变) ...
    private static final ResourceLocation PEARL_MEAT_ID = new ResourceLocation("kaleidoscope_cookery", "sweet_and_sour_ender_pearls");
    private static final ResourceLocation GOLDEN_SALAD_ID = new ResourceLocation("kaleidoscope_cookery", "golden_salad");
    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        if(event.getMob()instanceof WarriorNPCEntity w&&!w.level().isClientSide()){ResourceLocation f=BuiltInRegistries.BLOCK.getKey(event.getFoodState().getBlock());Player p=w.level().getNearestPlayer(w,16);int l=w.getLevel(),n=-1;String m="";if(w.isRitualPending()){if(l==1&&f.equals(PEARL_MEAT_ID)){n=2;m="§5吞下珍珠的瞬间，它的身影变得飘忽不定，仿佛掌握了空间的力量！";}else if(l==2&&f.equals(GOLDEN_SALAD_ID)){n=3;m="§6金色的光辉包裹了它！它突破了凡物的极限，成为了真正的传奇战士！";}}if(n!=-1){w.playSound(SoundEvents.PLAYER_LEVELUP,1.0F,1.0F);if(w.level()instanceof ServerLevel s)s.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,w.getRandomX(1.0),w.getY(0.5),w.getRandomZ(1.0),30,0.3,0.5,0.3,0.5);w.setLevel(n);if(p!=null)p.sendSystemMessage(Component.literal(m));}}
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof WarriorNPCEntity warrior)) {
            return;
        }

        if (warrior.getOwner() instanceof Player owner) {
            UUID warriorUUID = warrior.getUUID();

            for (int i = 0; i < owner.getInventory().getContainerSize(); ++i) {
                ItemStack stack = owner.getInventory().getItem(i);
                if (stack.getItem() instanceof WarriorSummonItem) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.hasUUID("WarriorUUID") && tag.getUUID("WarriorUUID").equals(warriorUUID)) {
                        // 找到了绑定的召唤物！
                        ItemStack brokenStack = new ItemStack(NpcItems.BROKEN_WARRIOR_SUMMON_ITEM.get());

                        // 【核心修正】将战士的所有数据保存到破碎的核心里
                        CompoundTag warriorData = new CompoundTag();
                        warrior.addAdditionalSaveData(warriorData); // 保存战士的等级、装备、记忆等

                        CompoundTag brokenTag = new CompoundTag();
                        brokenTag.putUUID("WarriorUUID", warriorUUID); // 保存灵魂ID
                        brokenTag.put("WarriorData", warriorData);   // 保存完整的身体数据
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