package com.sake.dining.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/**
 * 当一个生物通过 EatCakeBlockGoal 吃完一个方块时触发。
 * 这个事件在服务器端触发。
 */
public class MobFinishedEatingEvent extends Event {

    private final Mob mob;
    private final BlockPos foodPos;
    private final BlockState foodState;

    public MobFinishedEatingEvent(Mob mob, BlockPos foodPos, BlockState foodState) {
        this.mob = mob;
        this.foodPos = foodPos;
        this.foodState = foodState;
    }

    /**
     * 获取吃东西的生物实体。
     */
    public Mob getMob() {
        return mob;
    }

    /**
     * 获取被吃掉的食物方块的位置。
     */
    public BlockPos getFoodPos() {
        return foodPos;
    }

    /**
     * 获取被吃掉的食物方块的状态。
     */
    public BlockState getFoodState() {
        return foodState;
    }
}