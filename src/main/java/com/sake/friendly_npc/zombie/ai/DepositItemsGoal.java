// 文件路径: src/main/java/com/sake/npc/zombie/ai/DepositItemsGoal.java
// 用下面的代码完整替换掉旧的 DepositItemsGoal.java

package com.sake.friendly_npc.zombie.ai;

import com.sake.friendly_npc.zombie.ZombieNPCEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;
import java.util.Set;

public class DepositItemsGoal extends MoveToBlockGoal {

    private final ZombieNPCEntity mob;
    private int depositCooldown = 0;
    private boolean hasDeposited = false;

    public DepositItemsGoal(ZombieNPCEntity mob, double speedModifier) {
        super(mob, speedModifier, 16, 8);
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
        // 【修改】让它不要直接走到方块正上方
        this.verticalSearchStart = -1;
    }

    // ... isValidTarget, canUse, canContinueToUse, stop 方法保持不变 ...

    // 【核心修改】我们重写 start() 和 tick() 来实现箱子动画和更好的站位
    @Override
    public void start() {
        this.hasDeposited = false;
        super.start();
    }

    @Override
    public void tick() {
        // 如果目标方块变了（比如被破坏了），就停止
        if (!this.isValidTarget(this.mob.level(), this.blockPos)) {
            stop();
            return;
        }

        super.tick();

        // 离目标足够近时
        if (this.isReachedTarget()) {
            BlockEntity blockEntity = this.mob.level().getBlockEntity(this.blockPos);
            if (blockEntity instanceof ChestBlockEntity chest) {
                // 播放箱子打开动画
                playChestAnimation(chest, true);
                organizeInventory(chest);
                // 播放箱子关闭动画
                playChestAnimation(chest, false);
                this.hasDeposited = true;
            }
            stop();
        }
    }

    /**
     * 【新增】播放箱子打开/关闭动画的方法
     */
    private void playChestAnimation(ChestBlockEntity chest, boolean open) {
        Level level = this.mob.level();
        // 这是一个原版机制，通过发送方块事件来同步客户端动画
        level.blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, open ? 1 : 0);
    }

    /**
     * 【修改】让它寻找箱子旁边的位置，而不是箱子本身
     */
    @Override
    protected BlockPos getMoveToTarget() {
        // 尝试找到箱子正面
        return this.blockPos.relative(this.mob.level().getBlockState(this.blockPos).getValue(ChestBlock.FACING));
    }

    // ... organizeInventory 和其他辅助方法保持不变 ...

    // 为了防止出错，我将所有代码都完整提供在下方

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChestBlockEntity;
    }

    @Override
    public boolean canUse() {
        if (depositCooldown > 0) {
            depositCooldown--;
            return false;
        }
        if (!isInventoryNeedsOrganizing()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !hasDeposited && super.canContinueToUse();
    }

    @Override
    public void stop() {
        super.stop();
        depositCooldown = 100;
    }

    private void organizeInventory(Container chest) {
        SimpleContainer mobInventory = this.mob.getInventory();
        for (int i = 0; i < mobInventory.getContainerSize(); i++) {
            ItemStack stackInMob = mobInventory.getItem(i);
            if (stackInMob.isEmpty()) continue;
            Item item = stackInMob.getItem();
            if (isCrop(item) && !(item instanceof BlockItem)) {
                mobInventory.setItem(i, transferToContainer(chest, stackInMob));
            } else if (isSeed(item) && stackInMob.getCount() > 64) {
                int surplus = stackInMob.getCount() - 64;
                ItemStack toDeposit = stackInMob.split(surplus);
                ItemStack remaining = transferToContainer(chest, toDeposit);
                stackInMob.grow(remaining.getCount());
            }
        }
        checkAndRetrieveSeeds(chest, mobInventory);
    }

    private ItemStack transferToContainer(Container destination, ItemStack stackToTransfer) {
        ItemStack remaining = stackToTransfer.copy();
        for (int i = 0; i < destination.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack stackInSlot = destination.getItem(i);
            if (ItemStack.isSameItemSameTags(stackInSlot, remaining) && stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                int transferable = Math.min(remaining.getCount(), stackInSlot.getMaxStackSize() - stackInSlot.getCount());
                remaining.shrink(transferable);
                stackInSlot.grow(transferable);
            }
        }
        for (int i = 0; i < destination.getContainerSize() && !remaining.isEmpty(); i++) {
            if (destination.getItem(i).isEmpty()) {
                destination.setItem(i, remaining.copy());
                remaining.setCount(0);
            }
        }
        return remaining;
    }

    private boolean isInventoryNeedsOrganizing() {
        SimpleContainer inventory = this.mob.getInventory();
        int emptySlots = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                emptySlots++;
                continue;
            }
            Item item = stack.getItem();
            if (isSeed(item) && stack.getCount() > 64) return true;
            if (isCrop(item) && !(item instanceof BlockItem)) return true;
        }
        return emptySlots < 2;
    }

    private void checkAndRetrieveSeeds(Container chest, SimpleContainer mobInventory) {
        ensureSeed(chest, mobInventory, Items.WHEAT_SEEDS);
        ensureSeed(chest, mobInventory, Items.CARROT);
        ensureSeed(chest, mobInventory, Items.POTATO);
    }

    private void ensureSeed(Container chest, SimpleContainer mobInventory, Item seedItem) {
        if (mobInventory.hasAnyOf(Set.of(seedItem))) return;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stackInChest = chest.getItem(i);
            if (stackInChest.is(seedItem)) {
                ItemStack toRetrieve = stackInChest.split(16);
                transferToContainer(mobInventory, toRetrieve);
                if(stackInChest.isEmpty()){
                    chest.setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private boolean isCrop(Item item) {
        return item == Items.WHEAT || item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT;
    }

    private boolean isSeed(Item item) {
        return item instanceof BlockItem || item == Items.POTATO || item == Items.CARROT;
    }


    }
