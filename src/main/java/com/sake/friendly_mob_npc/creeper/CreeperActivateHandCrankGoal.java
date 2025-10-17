package com.sake.friendly_mob_npc.creeper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Stream;

public class CreeperActivateHandCrankGoal extends Goal {
    private final CreeperNPCEntity creeper;
    private BlockPos targetPos;
    private Block handCrankBlock;

    // 我们要寻找的方块的ID
    private static final ResourceLocation HAND_CRANK_ID = new ResourceLocation("create", "hand_crank");
    // 我们要调用的方法的名字
    private static final String TURN_METHOD_NAME = "turn";

    private Method turnMethod;

    public CreeperActivateHandCrankGoal(CreeperNPCEntity creeper) {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 如果找不到手摇曲柄方块（比如没装Create模组），就不执行
        if (handCrankBlock == null) {
            handCrankBlock = ForgeRegistries.BLOCKS.getValue(HAND_CRANK_ID);
            if (handCrankBlock == null) {
                return false;
            }
        }
        // 如果正在走路，就不寻找
        if (!this.creeper.getNavigation().isDone()) {
            return false;
        }
        // 寻找最近的曲柄
        this.targetPos = findNearestHandCrank();
        return this.targetPos != null;
    }

    @Override
    public void start() {
        if (this.targetPos != null) {
            this.creeper.getNavigation().moveTo(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.targetPos = null;
        this.creeper.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        // 如果目标方块不再是曲柄，或者已经走到了，就停止
        return this.targetPos != null && !this.creeper.getNavigation().isDone() && this.creeper.level().getBlockState(this.targetPos).is(this.handCrankBlock);
    }

    @Override
    public void tick() {
        if (this.targetPos != null) {
            // 看着目标
            this.creeper.getLookControl().setLookAt(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5);
            // 如果距离小于2格，就认为是“站在旁边”，开始工作
            if (this.creeper.blockPosition().closerThan(this.targetPos, 2.0)) {
                work();
            }
        }
    }

    /**
     * 让曲柄工作
     */
    private void work() {
        BlockEntity blockEntity = this.creeper.level().getBlockEntity(this.targetPos);
        if (blockEntity != null) {
            try {
                // 如果是第一次，就获取"turn"方法
                if (turnMethod == null) {
                    turnMethod = blockEntity.getClass().getMethod(TURN_METHOD_NAME, boolean.class);
                }
                // 调用"turn"方法，让曲柄转动
                turnMethod.invoke(blockEntity, this.creeper.getRandom().nextBoolean());
            } catch (Exception e) {
                // 如果发生错误（比如Create模组更新了方法名），打印错误信息并重置
                System.err.println("Failed to turn hand crank via reflection: " + e.getMessage());
                turnMethod = null;
            }
        }
    }

    private BlockPos findNearestHandCrank() {
        int radius = 16;
        BlockPos creeperPos = this.creeper.blockPosition();

        try (Stream<BlockPos> positions = BlockPos.betweenClosedStream(creeperPos.offset(-radius, -5, -radius), creeperPos.offset(radius, 5, radius))) {
            Optional<BlockPos> nearestCrank = positions
                    .filter(pos -> this.creeper.level().getBlockState(pos).is(this.handCrankBlock))
                    .min(Comparator.comparingDouble(pos -> pos.distSqr(creeperPos)));

            return nearestCrank.orElse(null);
        }
    }
}