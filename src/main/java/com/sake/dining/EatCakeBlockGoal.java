package com.sake.dining;

import com.sake.dining.event.MobFinishedEatingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class EatCakeBlockGoal extends Goal {
    private final Mob mob;
    private BlockPos targetPos;
    private int eatingCooldown = 0;
    private int retargetCooldown = 0;

    public EatCakeBlockGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return isSupportedEater(mob) && findNearestFoodBlock().isPresent();
    }

    @Override
    public void start() {
        findNearestFoodBlock().ifPresent(pos -> this.targetPos = pos);
        this.retargetCooldown = 0;
    }

    @Override
    public void tick() {
        if (retargetCooldown > 0) {
            retargetCooldown--;
        } else {
            Optional<BlockPos> newTarget = findNearestFoodBlock();
            if (newTarget.isPresent() && !newTarget.get().equals(targetPos)) {
                targetPos = newTarget.get();
            }
            retargetCooldown = 60;
        }
        if (targetPos == null) {
            mob.removeEffect(MobEffects.GLOWING);
            return;
        }

        mob.getLookControl().setLookAt(Vec3.atCenterOf(targetPos));

        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        double dx = mob.getX() - targetCenter.x();
        double dz = mob.getZ() - targetCenter.z();
        double horizontalDistSq = dx * dx + dz * dz;

        if (horizontalDistSq > 1.5 * 1.5 || Math.abs(mob.getY() - targetPos.getY()) > 1) {
            if (!mob.hasEffect(MobEffects.GLOWING)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
            mob.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.0);
            return;
        } else {
            mob.getNavigation().stop();
            mob.removeEffect(MobEffects.GLOWING);
        }

        if (eatingCooldown > 0) {
            eatingCooldown--;
            return;
        }
        eatingCooldown = 10;

        Level world = mob.level();
        BlockState state = world.getBlockState(targetPos);

        spawnEatingParticles(world, targetPos, state);

        if (canBiteMultiple(state)) {
            Property<Integer> bitesProp = (Property<Integer>) getBitesProperty(state);
            int bites = state.getValue(bitesProp);
            int maxBites = bitesProp.getPossibleValues().size() - 1;
            if (bites < maxBites) {
                BlockState newState = state.setValue(bitesProp, bites + 1);
                world.setBlock(targetPos, newState, 3);
                onEatFood(world, targetPos, state, false); // 传递 state
            } else {
                world.removeBlock(targetPos, false);
                onEatFood(world, targetPos, state, true); // 传递 state
                targetPos = null;
            }
        } else {
            world.removeBlock(targetPos, false);
            onEatFood(world, targetPos, state, true); // 传递 state
            targetPos = null;
        }
    }

    private void spawnEatingParticles(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        int particleCount = 10;
        double particleSpeed = 0.1D;
        serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                particleCount,
                0.3, 0.2, 0.3,
                particleSpeed
        );
    }

    private boolean canBiteMultiple(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("bites")) {
                return true;
            }
        }
        return false;
    }

    private Property<?> getBitesProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("bites")) {
                return prop;
            }
        }
        return null;
    }

    private Optional<BlockPos> findNearestFoodBlock() {
        Level world = mob.level();
        int range = 6;
        int verticalRange = 3;
        ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();
        List<ResourceLocation> allowedFoods = FeedingConfig.getFoodBlocks(mobId);
        if (allowedFoods == null || allowedFoods.isEmpty()) return Optional.empty();
        return BlockPos.findClosestMatch(mob.blockPosition(), range, verticalRange, pos -> {
            BlockState state = world.getBlockState(pos);
            ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();
            return allowedFoods.contains(blockId);
        });
    }

    public static boolean isSupportedEater(Mob mob) {
        ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();
        List<ResourceLocation> allowedFoods = FeedingConfig.getFoodBlocks(mobId);
        return allowedFoods != null && !allowedFoods.isEmpty();
    }

    // [修改点 1]: 修改方法签名，增加 BlockState 参数
    private void onEatFood(Level world, BlockPos pos, BlockState eatenState, boolean finished) {
        if (!world.isClientSide) {
            mob.heal(2.0F);
            mob.getPersistentData().putBoolean("dining_fed", true);

            if (finished) {
                // [修改点 2]: 在这里广播我们的自定义事件！
                MinecraftForge.EVENT_BUS.post(new MobFinishedEatingEvent(mob, pos, eatenState));

                // 保留你原有的掉落物逻辑，如果你不需要可以让 getThankYouToken() 返回 null
                ItemStack token = getThankYouToken();
                if (token != null) {
                    world.addFreshEntity(new ItemEntity(world, mob.getX(), mob.getY() + 0.5, mob.getZ(), token));
                }
            }
        }
    }

    // [修改点 3]: 新增一个方法来获取掉落物，方便管理
    private ItemStack getThankYouToken() {
        if (mob.getType() == EntityType.ZOMBIE) {
            return new ItemStack(DiningItems.ZOMBIE_TOKEN.get());
        } else if (mob.getType() == EntityType.SKELETON) {
            return new ItemStack(DiningItems.SKELETON_TOKEN.get());
        } else if (mob.getType() == EntityType.CREEPER) {
            return new ItemStack(DiningItems.CREEPER_TOKEN.get());
        } else if (mob.getType() == EntityType.ENDERMAN) {
            return new ItemStack(DiningItems.ENDERMAN_TOKEN.get());
        } else if (mob.getType() == EntityType.SLIME) {
            return new ItemStack(DiningItems.SLIME_TOKEN.get());
        } else if (mob.getType() == EntityType.BLAZE) {
            return new ItemStack(DiningItems.BLAZE_TOKEN.get());
        }
        return null; // 默认不掉落
    }
}