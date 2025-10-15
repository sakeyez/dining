package com.sake.npc.skeleton;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class SkeletonNPCEntity extends Skeleton {
    // --- 核心修正: 使用新的 ResourceLocation 写法 ---
    private static final ResourceLocation NEPTUNIUM_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "neptunium_fishing_rod");

    public SkeletonNPCEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.getAvailableGoals().stream()
                .filter(goal -> goal.getGoal() instanceof WaterAvoidingRandomStrollGoal)
                .collect(Collectors.toList())
                .forEach(this.goalSelector::removeGoal);

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SkeletonFishingGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Pufferfish) {
            Item heldItem = this.getMainHandItem().getItem();
            if (ForgeRegistries.ITEMS.getKey(heldItem).equals(NEPTUNIUM_ROD_ID)) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack playerStack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && playerStack.getItem() instanceof FishingRodItem) {
            if (!this.level().isClientSide) {
                ItemStack skeletonStack = this.getMainHandItem();
                this.setItemSlot(EquipmentSlot.MAINHAND, playerStack.copy());
                player.setItemInHand(hand, skeletonStack);
                this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));

        Item hat = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "straw_hat_flower"));
        Item chest = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_chest_plate"));
        Item legs = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_leggings"));
        Item boots = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_boots"));

        if (hat != null) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(hat));
        if (chest != null) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        if (legs != null) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        if (boots != null) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        return data;
    }
}