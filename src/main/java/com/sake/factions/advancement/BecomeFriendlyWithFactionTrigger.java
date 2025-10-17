package com.sake.factions.advancement;

import com.google.gson.JsonObject;
import com.sake.factions.Factions;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

// 1. 定义我们自己的成就触发器，继承自 SimpleCriterionTrigger
public class BecomeFriendlyWithFactionTrigger extends SimpleCriterionTrigger<BecomeFriendlyWithFactionTrigger.TriggerInstance> {

    // 2. 给这个触发器一个唯一的ID，和你的模组ID绑定
    static final ResourceLocation ID = new ResourceLocation(Factions.MODID, "become_friendly_with_faction");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    // 3. 定义如何从JSON文件中解析这个触发器的条件
    @Override
    public BecomeFriendlyWithFactionTrigger.TriggerInstance createInstance(JsonObject pJson, ContextAwarePredicate pPlayer, DeserializationContext pContext) {
        // 从JSON中读取 "faction" 字段，如果没有就默认为空字符串
        String factionId = pJson.has("faction") ? pJson.get("faction").getAsString() : "";
        return new BecomeFriendlyWithFactionTrigger.TriggerInstance(pPlayer, factionId);
    }

    /**
     * 这是核心方法，我们会在代码中调用它来“激活”这个触发器
     * @param player 触发成就的玩家
     * @param factionId 玩家与之变得友好的派系ID
     */
    public void trigger(ServerPlayer player, String factionId) {
        // this.trigger(...) 是父类的方法，它会自动检查所有监听此触发器的成就
        // 并对满足条件的成就授予进度
        this.trigger(player, (triggerInstance) -> triggerInstance.matches(factionId));
    }

    // 4. 定义触发器的“实例”，它代表了JSON文件中 "criteria" 里的一个具体条件
    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String factionId;

        public TriggerInstance(ContextAwarePredicate pPlayer, String factionId) {
            super(BecomeFriendlyWithFactionTrigger.ID, pPlayer);
            this.factionId = factionId;
        }

        /**
         * 这是一个静态工厂方法，方便我们在数据生成器中使用
         */
        public static BecomeFriendlyWithFactionTrigger.TriggerInstance becameFriendly(String factionId) {
            return new BecomeFriendlyWithFactionTrigger.TriggerInstance(ContextAwarePredicate.ANY, factionId);
        }

        /**
         * 检查传入的派系ID是否与此实例中定义的派系ID匹配
         */
        public boolean matches(String factionId) {
            return Objects.equals(this.factionId, factionId);
        }

        /**
         * 将此实例的条件序列化为JSON
         */
        @Override
        public JsonObject serializeToJson(SerializationContext pContext) {
            JsonObject jsonobject = super.serializeToJson(pContext);
            jsonobject.addProperty("faction", this.factionId);
            return jsonobject;
        }
    }
}