package com.sake.friendly_mob_npc.enderman;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.EnderMan; // <-- 修改点 1
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EndermanNPCEntity extends EnderMan { // <-- 修改点 2
    public EndermanNPCEntity(EntityType<? extends EnderMan> type, Level level) { // <-- 修改点 3
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}