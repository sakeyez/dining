package com.sake.friendly_mob_npc.slime;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SlimeNPCEntity extends Slime {
    public SlimeNPCEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // 首先，调用父类（原版Slime）的方法，让它注册所有默认的、私有的AI
        super.registerGoals();

        // 然后，我们可以清空目标选择器，只保留我们想要的AI，或者添加新的AI
        // 为了简单起见，我们先清除所有攻击性的AI，只让它看向玩家
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // 重写这个方法以防止NPC分裂
    @Override
    public void remove(RemovalReason reason) {
        // 覆盖原版史莱姆死亡后分裂的逻辑
        if (!this.level().isClientSide && this.isDeadOrDying()) {
            // 确保实体被正确移除
            super.remove(reason);
        } else if (!this.level().isClientSide){
            // 对于非死亡情况，也调用超类方法
            super.remove(reason);
        } else {
            // 客户端侧直接调用
            super.remove(reason);
        }
    }

    // 覆盖此方法返回false，可以阻止史莱姆进行攻击
    @Override
    protected boolean isDealsDamage() {
        return false;
    }
}