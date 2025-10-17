package com.sake.friendly_mob_npc.zombie_warrior;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

// 我们使用 BipedModel，这是玩家、骷髅等使用的标准两足模型
// 它不包含僵尸特有的举手动作
public class WarriorNPCModel<T extends WarriorNPCEntity> extends HumanoidModel<T> {
    public WarriorNPCModel(ModelPart root) {
        super(root);
    }
}