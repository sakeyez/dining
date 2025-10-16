package com.sake.npc.wither_warrior;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

// 模型类本身很简单，我们只需要一个标准的类来接收模型部件
public class WitherWarriorNPCModel<T extends WitherWarriorNPCEntity> extends HumanoidModel<T> {
    public WitherWarriorNPCModel(ModelPart root) {
        super(root);
    }
}