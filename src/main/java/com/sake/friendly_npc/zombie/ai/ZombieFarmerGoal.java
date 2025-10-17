// 文件路径: src/main/java/com/sake/npc/zombie/ai/ZombieFarmerGoal.java

package com.sake.friendly_npc.zombie.ai;

import com.sake.friendly_npc.zombie.ZombieNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids; // 【新增】引入流体判断
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.EnumSet;
import java.util.List;

public class ZombieFarmerGoal extends Goal {

    private final ZombieNPCEntity mob;
    private final double speedModifier;
    private final Level level;

    private BlockPos targetPos = BlockPos.ZERO;
    private Task currentTask = Task.NONE;
    private int cooldown = 0;
    private int pathfindingCooldown = 0;

    private enum Task {
        NONE, HARVEST, PLANT, TILL
    }

    public ZombieFarmerGoal(ZombieNPCEntity mob, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }
        return findAndSetClosestTask();
    }

    @Override
    public boolean canContinueToUse() {
        return this.currentTask != Task.NONE && this.pathfindingCooldown > 0;
    }

    @Override
    public void start() {
        this.pathfindingCooldown = 300; // 15秒移动超时
        moveToTarget();
    }

    private void moveToTarget() {
        this.mob.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, this.speedModifier);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.targetPos = BlockPos.ZERO;
        this.currentTask = Task.NONE;
        this.cooldown = 10;
    }

    @Override
    public void tick() {
        this.pathfindingCooldown--;

        // 【核心修改1】更严格的“到达”判断，解决隔墙耕地和耕头顶的问题
        // 只有当僵尸的脚几乎踩在目标方块上时（水平距离），才认为到达
        BlockPos mobPos = this.mob.blockPosition();
        double horizontalDistSq = mobPos.atY(targetPos.getY()).distSqr(targetPos);

        // 如果已经到达或者寻路走完了
        if (horizontalDistSq < 1.1D || this.mob.getNavigation().isDone()) {
            performAndFindNext();
        } else if (this.mob.getNavigation().isStuck()) {
            stop();
        }
    }

    private void performAndFindNext() {
        performTask();
        if (findNextNearbyTask()) {
            this.pathfindingCooldown = 200;
            moveToTarget();
        } else {
            stop();
        }
    }

    private boolean findAndSetClosestTask() {
        // 【核心修改2】扩大水平搜索范围，让他能看到整个农田
        int searchRadius = 32;
        // 【核心修改3】缩小垂直搜索范围，让他专注于脚下
        int verticalRadius = 1;

        BlockPos mobPos = this.mob.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        Task bestTask = Task.NONE;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);

                    // --- 优先级 1: 寻找最近的要收获的作物 ---
                    BlockState cropState = level.getBlockState(mutablePos);
                    if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
                        double distSq = mob.distanceToSqr(Vec3.atCenterOf(mutablePos));
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = mutablePos.immutable();
                            bestTask = Task.HARVEST;
                        }
                    }
                }
            }
        }
        if (bestTask != Task.NONE) {
            this.targetPos = bestPos;
            this.currentTask = bestTask;
            return true;
        }

        // --- 优先级 2 & 3: 寻找种植和耕地的目标 ---
        bestDistSq = Double.MAX_VALUE; // 重置距离，为下一轮搜索做准备
        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                    BlockState groundState = level.getBlockState(mutablePos);

                    // 种植
                    if (hasSeedInInventory() && groundState.is(Blocks.FARMLAND) && level.getBlockState(mutablePos.above()).isAir()) {
                        double distSq = mob.distanceToSqr(Vec3.atCenterOf(mutablePos));
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = mutablePos.immutable();
                            bestTask = Task.PLANT;
                        }
                    }
                    // 【核心修改4】耕地时，加入水源检查
                    else if (hasHoeInHand() && (groundState.is(Blocks.DIRT) || groundState.is(Blocks.GRASS_BLOCK)) && level.getBlockState(mutablePos.above()).isAir()) {
                        if (isNearWater(mutablePos, 4)) { // 只锄离水4格内的地
                            double distSq = mob.distanceToSqr(Vec3.atCenterOf(mutablePos));
                            if (distSq < bestDistSq) {
                                bestDistSq = distSq;
                                bestPos = mutablePos.immutable();
                                bestTask = Task.TILL;
                            }
                        }
                    }
                }
            }
        }

        if (bestTask != Task.NONE) {
            this.targetPos = bestPos;
            this.currentTask = bestTask;
            return true;
        }

        return false;
    }

    /**
     * 【新增辅助方法】检查一个位置附近是否有水源
     * @param pos 要检查的位置
     * @param radius 搜索半径
     * @return 如果在半径内找到水，返回true
     */
    private boolean isNearWater(BlockPos pos, int radius) {
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, 1, radius))) {
            if (level.getFluidState(checkPos).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    // 下面的方法保持不变，为了方便你完整替换，我把它们都留在这里
    private boolean findNextNearbyTask() {
        int searchRadius = 2;
        BlockPos mobPos = this.mob.blockPosition();

        // 优先级1: 收获
        for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
            BlockState cropState = level.getBlockState(pos);
            if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
                this.targetPos = pos.immutable();
                this.currentTask = Task.HARVEST;
                return true;
            }
        }

        // 优先级2: 种植
        if (hasSeedInInventory()) {
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
                if (level.getBlockState(pos).is(Blocks.FARMLAND) && level.getBlockState(pos.above()).isAir()) {
                    this.targetPos = pos.immutable();
                    this.currentTask = Task.PLANT;
                    return true;
                }
            }
        }

        // 优先级3: 耕地
        if (hasHoeInHand()) {
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
                BlockState groundState = level.getBlockState(pos);
                if ((groundState.is(Blocks.DIRT) || groundState.is(Blocks.GRASS_BLOCK)) && level.getBlockState(pos.above()).isAir()) {
                    if (isNearWater(pos, 4)) {
                        this.targetPos = pos.immutable();
                        this.currentTask = Task.TILL;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void performTask() {
        this.mob.getLookControl().setLookAt(Vec3.atCenterOf(this.targetPos));

        switch (this.currentTask) {
            case HARVEST: {
                BlockState cropState = level.getBlockState(this.targetPos);
                if (this.level instanceof ServerLevel serverLevel) {
                    LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.targetPos))
                            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                            .withOptionalParameter(LootContextParams.THIS_ENTITY, this.mob);
                    List<ItemStack> drops = cropState.getDrops(lootBuilder);
                    for (ItemStack drop : drops) {
                        this.mob.getInventory().addItem(drop);
                    }
                }
                this.level.setBlock(this.targetPos, Blocks.AIR.defaultBlockState(), 3);
                break;
            }
            case PLANT: {
                plantSeed(this.targetPos.above());
                break;
            }
            case TILL: {
                this.level.playSound(null, this.targetPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level.setBlock(this.targetPos, Blocks.FARMLAND.defaultBlockState(), 11);
                this.mob.getMainHandItem().hurtAndBreak(1, this.mob, (e) -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                break;
            }
        }
        this.mob.swing(InteractionHand.MAIN_HAND);
    }

    private boolean hasHoeInHand() {
        return this.mob.getMainHandItem().getItem() instanceof HoeItem;
    }

    private boolean hasSeedInInventory() {
        for(int i = 0; i < this.mob.getInventory().getContainerSize(); ++i) {
            Item item = this.mob.getInventory().getItem(i).getItem();
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock) {
                return true;
            }
            if (item == Items.POTATO || item == Items.CARROT) {
                return true;
            }
        }
        return false;
    }

    private void plantSeed(BlockPos pos) {
        for(int i = 0; i < this.mob.getInventory().getContainerSize(); ++i) {
            ItemStack stack = this.mob.getInventory().getItem(i);
            Item item = stack.getItem();
            BlockState seedState = null;

            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock) {
                seedState = ((BlockItem) item).getBlock().defaultBlockState();
            } else if (item == Items.POTATO) {
                seedState = Blocks.POTATOES.defaultBlockState();
            } else if (item == Items.CARROT) {
                seedState = Blocks.CARROTS.defaultBlockState();
            }

            if (seedState != null) {
                this.level.setBlock(pos, seedState, 3);
                stack.shrink(1);
                return;
            }
        }
    }
}