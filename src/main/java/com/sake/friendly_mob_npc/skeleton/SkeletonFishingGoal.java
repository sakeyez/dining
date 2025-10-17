package com.sake.friendly_mob_npc.skeleton;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class SkeletonFishingGoal extends Goal {
    private final SkeletonNPCEntity skeleton;
    private final ServerLevel level;
    private BlockPos fishingPos = null;
    private BlockPos waterPos = null;
    private boolean isFishing = false;
    private int fishingTimer = 0;
    private int cooldown = 0;

    private static final ResourceLocation NEPTUNIUM_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "neptunium_fishing_rod");

    private static final class RodStats {
        public final float speedModifier;
        public final float doubleLootChance;
        public RodStats(float speedModifier, float doubleLootChance) {
            this.speedModifier = speedModifier;
            this.doubleLootChance = doubleLootChance;
        }
    }

    private static final Map<ResourceLocation, RodStats> ROD_STATS_MAP = Maps.newHashMap();
    static {
        ROD_STATS_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "fishing_rod"), new RodStats(1.0f, 0));
        ROD_STATS_MAP.put(ResourceLocation.fromNamespaceAndPath("aquaculture", "iron_fishing_rod"), new RodStats(0.8f, 0));
        ROD_STATS_MAP.put(ResourceLocation.fromNamespaceAndPath("aquaculture", "gold_fishing_rod"), new RodStats(0.7f, 0));
        ROD_STATS_MAP.put(ResourceLocation.fromNamespaceAndPath("aquaculture", "diamond_fishing_rod"), new RodStats(0.6f, 0.1f));
        ROD_STATS_MAP.put(NEPTUNIUM_ROD_ID, new RodStats(0.4f, 0.4f));
    }

    private static final Map<Item, EntityType<?>> FISH_MAP = Maps.newHashMap();
    static {
        FISH_MAP.put(Items.COD, EntityType.COD);
        FISH_MAP.put(Items.SALMON, EntityType.SALMON);
        FISH_MAP.put(Items.TROPICAL_FISH, EntityType.TROPICAL_FISH);
        FISH_MAP.put(Items.PUFFERFISH, EntityType.PUFFERFISH);
        String aquaModId = "aquaculture";
        addFishMapping(aquaModId, "atlantic_cod");
        addFishMapping(aquaModId, "blackfish");
        addFishMapping(aquaModId, "pacific_halibut");
        addFishMapping(aquaModId, "atlantic_halibut");
        addFishMapping(aquaModId, "atlantic_herring");
        addFishMapping(aquaModId, "pink_salmon");
        addFishMapping(aquaModId, "pollock");
        addFishMapping(aquaModId, "rainbow_trout");
        addFishMapping(aquaModId, "bayad");
        addFishMapping(aquaModId, "boulti");
        addFishMapping(aquaModId, "capitaine");
        addFishMapping(aquaModId, "synodontis");
        addFishMapping(aquaModId, "smallmouth_bass");
        addFishMapping(aquaModId, "bluegill");
        addFishMapping(aquaModId, "brown_trout");
        addFishMapping(aquaModId, "carp");
        addFishMapping(aquaModId, "catfish");
        addFishMapping(aquaModId, "gar");
        addFishMapping(aquaModId, "minnow");
        addFishMapping(aquaModId, "muskellunge");
        addFishMapping(aquaModId, "perch");
        addFishMapping(aquaModId, "arapaima");
        addFishMapping(aquaModId, "piranha");
        addFishMapping(aquaModId, "tambaqui");
        addFishMapping(aquaModId, "brown_shrooma");
        addFishMapping(aquaModId, "red_shrooma");
        addFishMapping(aquaModId, "jellyfish");
        addFishMapping(aquaModId, "red_grouper");
        addFishMapping(aquaModId, "tuna");
        addFishMapping(aquaModId, "box_turtle");
        addFishMapping(aquaModId, "arrau_turtle");
        addFishMapping(aquaModId, "starshell_turtle");
    }

    public SkeletonFishingGoal(SkeletonNPCEntity skeleton) {
        this.skeleton = skeleton;
        this.level = (ServerLevel) skeleton.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean isDiligentMode() {
        Item heldItem = this.skeleton.getMainHandItem().getItem();
        return ForgeRegistries.ITEMS.getKey(heldItem).equals(NEPTUNIUM_ROD_ID);
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(this.skeleton.getMainHandItem().getItem() instanceof FishingRodItem)) {
            return false;
        }

        Optional<BlockPos> optionalPos = isDiligentMode() ? findWaterNearby() : findFishingSpot();

        if (optionalPos.isPresent()) {
            // 在勤奋模式下，站立点就是骷髅自己的位置, 否则使用找到的位置
            this.fishingPos = isDiligentMode() ? this.skeleton.blockPosition() : optionalPos.get();
            System.out.println("[钓鱼AI-Debug] 找到钓鱼点: " + this.fishingPos + ", 目标水域: " + this.waterPos);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        if (this.fishingPos != null && !isDiligentMode()) { // 勤奋模式不需要移动
            System.out.println("[钓鱼AI-Debug] 开始移动到钓鱼点: " + this.fishingPos);
            this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
        }
    }

    @Override
    public void tick() {
        if (this.fishingPos == null || this.waterPos == null) return;

        // 持续朝向水面
        this.skeleton.getLookControl().setLookAt(Vec3.atCenterOf(this.waterPos));

        // --- 核心AI逻辑修复: 恢复你最初的、有效的移动逻辑 ---
        // 只有在悠闲模式下才需要判断距离并移动
        if (!isDiligentMode()) {
            // distSqr是距离的平方，检查4.0就意味着检查直线距离2格以内
            if (this.skeleton.blockPosition().distSqr(this.fishingPos) > 4.0) {
                // 如果距离还远，并且寻路已停止（可能被卡住），重新尝试寻路
                if (this.skeleton.getNavigation().isDone()) {
                    this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
                }
                return; // 只要还没到“附近”，就一直返回，不执行下面的钓鱼逻辑
            }
        }

        // 只要进入了“附近”(悠闲模式)或处于勤奋模式，就停止移动，开始钓鱼
        this.skeleton.getNavigation().stop();

        if (!isFishing) {
            System.out.println("[钓鱼AI-Debug] 到达钓鱼点附近，准备抛竿。");
            this.skeleton.swing(InteractionHand.MAIN_HAND);
            this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_THROW, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));
            isFishing = true;

            RodStats stats = getRodStats();
            float timeModifier = stats.speedModifier;
            this.fishingTimer = (int) ((100 + this.level.getRandom().nextInt(200)) * timeModifier);
            // 恢复并增强控制台输出
            System.out.println("[钓鱼AI-Debug] 已抛竿，使用鱼竿: " + ForgeRegistries.ITEMS.getKey(this.skeleton.getMainHandItem().getItem()) + "，等待 " + fishingTimer + " ticks.");

        } else {
            fishingTimer--;
            if (this.level.getRandom().nextInt(10) == 0) {
                this.level.sendParticles(ParticleTypes.SPLASH, this.waterPos.getX() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), this.waterPos.getY() + 1.0, this.waterPos.getZ() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), 1, 0, 0, 0, 0);
            }

            if (fishingTimer <= 0) {
                System.out.println("[钓鱼AI-Debug] 时间到，收竿！");
                this.skeleton.swing(InteractionHand.MAIN_HAND);
                this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));
                generateLoot();
                stop();
            }
        }
    }

    @Override
    public void stop() {
        System.out.println("[钓鱼AI-Debug] 停止钓鱼AI。");
        this.skeleton.getNavigation().stop();
        this.fishingPos = null;
        this.waterPos = null;
        this.isFishing = false;
        this.fishingTimer = 0;
        this.cooldown = isDiligentMode() ? 1 : 200 + this.level.getRandom().nextInt(400);
    }

    private void generateLoot() {
        LootParams lootparams = new LootParams.Builder(this.level)
                .withParameter(LootContextParams.ORIGIN, this.skeleton.position())
                .withParameter(LootContextParams.TOOL, this.skeleton.getMainHandItem())
                .withLuck(0)
                .create(LootContextParamSets.FISHING);
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        LootTable loottable = server.getLootData().getLootTable(BuiltInLootTables.FISHING);

        RodStats stats = getRodStats();
        int lootRolls = 1;
        if (this.level.getRandom().nextFloat() < stats.doubleLootChance) {
            lootRolls = 2;
        }
        System.out.println("[钓鱼AI-Debug] 钓鱼次数: " + lootRolls);

        for (int i = 0; i < lootRolls; i++) {
            List<ItemStack> items = loottable.getRandomItems(lootparams);
            System.out.println("[钓鱼AI-Debug] 钓到了 " + items.size() + " 个物品。");

            for (ItemStack itemstack : items) {
                Entity entityToLaunch;
                Item fishedItem = itemstack.getItem();
                if (FISH_MAP.containsKey(fishedItem)) {
                    EntityType<?> fishEntityType = FISH_MAP.get(fishedItem);
                    entityToLaunch = fishEntityType.create(this.level);
                    System.out.println("[钓鱼AI-Debug] 成功匹配到鱼生物: " + fishEntityType.getDescriptionId());
                } else {
                    entityToLaunch = new ItemEntity(this.level, 0, 0, 0, itemstack);
                    System.out.println("[钓鱼AI-Debug] 未匹配到鱼生物，生成物品: " + fishedItem.getDescription().getString());
                }

                if (entityToLaunch != null) {
                    entityToLaunch.setPos(this.waterPos.getX() + 0.5, this.waterPos.getY() + 0.5, this.waterPos.getZ() + 0.5);

                    // --- 核心改动: 全新的抛物线逻辑 ---
                    // 1. 计算从水面指向骷髅的向量
                    Vec3 toSkeleton = this.skeleton.position().subtract(entityToLaunch.position());

                    // 2. 计算骷髅身体朝向的“右”方向向量 (yBodyRot更稳定)
                    float bodyYaw = (float) Math.toRadians(this.skeleton.yBodyRot);
                    Vec3 rightVec = new Vec3(-Math.cos(bodyYaw), 0, Math.sin(bodyYaw));

                    // 3. 随机选择冲向左边还是右边，并与“冲向骷髅”的向量结合
                    double sideImpulse = 1.5 * (this.level.getRandom().nextBoolean() ? 1 : -1);
                    Vec3 initialDir = toSkeleton.add(rightVec.scale(sideImpulse));

                    // 4. 给予一个较高的向上的初速度形成漂亮的弧线
                    Vec3 motion = initialDir.add(0, 6.0, 0);

                    // 5. 归一化并施加一个更快的总体速度
                    entityToLaunch.setDeltaMovement(motion.normalize().scale(0.75));

                    this.level.addFreshEntity(entityToLaunch);
                }
            }
        }
    }

    private RodStats getRodStats() {
        Item heldItem = this.skeleton.getMainHandItem().getItem();
        ResourceLocation rodId = ForgeRegistries.ITEMS.getKey(heldItem);
        return ROD_STATS_MAP.getOrDefault(rodId, ROD_STATS_MAP.get(ResourceLocation.fromNamespaceAndPath("minecraft", "fishing_rod")));
    }

    private Optional<BlockPos> findFishingSpot() {
        BlockPos center = this.skeleton.blockPosition();
        for (BlockPos posToStand : BlockPos.betweenClosed(center.offset(-8, -2, -8), center.offset(8, 2, 8))) {
            if (isStandable(posToStand)) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos adjacentPos = posToStand.relative(direction);
                    BlockPos belowAdjacentPos = adjacentPos.below();
                    if (level.getFluidState(adjacentPos).is(FluidTags.WATER) || level.getFluidState(belowAdjacentPos).is(FluidTags.WATER)) {
                        this.waterPos = level.getFluidState(belowAdjacentPos).is(FluidTags.WATER) ? belowAdjacentPos.immutable() : adjacentPos.immutable();
                        return Optional.of(posToStand.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<BlockPos> findWaterNearby() {
        BlockPos center = this.skeleton.blockPosition();
        for (BlockPos posToCheck : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (isWater(posToCheck)) {
                this.waterPos = posToCheck.immutable();
                return Optional.of(posToCheck);
            }
        }
        return Optional.empty();
    }

    private boolean isWater(BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private boolean isStandable(BlockPos pos) {
        return level.getBlockState(pos.below()).entityCanStandOn(level, pos.below(), skeleton) &&
                level.isEmptyBlock(pos) &&
                level.isEmptyBlock(pos.above());
    }

    private static void addFishMapping(String modId, String fishName) {
        Supplier<Item> itemSupplier = () -> ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(modId, fishName));
        Supplier<EntityType<?>> entitySupplier = () -> ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath(modId, fishName));
        Item fishItem = itemSupplier.get();
        EntityType<?> fishEntity = entitySupplier.get();
        if (fishItem != null && fishEntity != null) {
            FISH_MAP.put(fishItem, fishEntity);
        }
    }

    @Override
    public boolean canContinueToUse() {
        // canContinueToUse 应该只关心AI是否应该持续，而不是是否正在钓鱼。
        // 只要fishingPos 和 waterPos 存在，就应该继续。isFishing是一个内部状态。
        return this.fishingPos != null && this.waterPos != null && this.skeleton.getMainHandItem().getItem() instanceof FishingRodItem;
    }
}