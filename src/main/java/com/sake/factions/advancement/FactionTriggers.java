package com.sake.factions.advancement;

import net.minecraft.advancements.CriteriaTriggers;

public class FactionTriggers {
    // 创建一个新的触发器实例
    public static final BecomeFriendlyWithFactionTrigger BECAME_FRIENDLY = new BecomeFriendlyWithFactionTrigger();

    /**
     * 这个方法需要在你的模组主类的构造函数中被调用
     */
    public static void register() {
        CriteriaTriggers.register(BECAME_FRIENDLY);
    }
}