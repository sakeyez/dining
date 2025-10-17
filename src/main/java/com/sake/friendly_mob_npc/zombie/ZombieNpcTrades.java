package com.sake.friendly_mob_npc.zombie;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZombieNpcTrades extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    // key: 实体ID（例如 npc:zombie_npc）
    private static final Map<ResourceLocation, List<Template>> TRADES = new ConcurrentHashMap<>();

    // 扫描的文件夹：data/*/trades/*.json
    public ZombieNpcTrades() {
        super(GSON, "trades");
    }

    public static List<Template> getTradesFor(ResourceLocation entityId) {
        return TRADES.getOrDefault(entityId, Collections.emptyList());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        TRADES.clear();
        files.forEach((fileId, json) -> {
            try {
                if (!json.isJsonObject()) return;
                JsonObject root = json.getAsJsonObject();
                if (!root.has("entity") || !root.has("trades")) return;

                ResourceLocation entityId = new ResourceLocation(root.get("entity").getAsString());
                List<Template> list = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("trades")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();

                    Template t = new Template(
                            o.get("buyA").getAsString(),
                            o.has("buyACount") ? o.get("buyACount").getAsInt() : 1,
                            o.has("buyB") ? o.get("buyB").getAsString() : null,
                            o.has("buyBCount") ? o.get("buyBCount").getAsInt() : 0,
                            o.get("sell").getAsString(),
                            o.has("sellCount") ? o.get("sellCount").getAsInt() : 1,
                            o.has("maxUses") ? o.get("maxUses").getAsInt() : 10,
                            o.has("xp") ? o.get("xp").getAsInt() : 0,
                            o.has("priceMultiplier") ? o.get("priceMultiplier").getAsFloat() : 0.05F
                    );
                    list.add(t);
                }
                TRADES.put(entityId, list);
                LOGGER.info("[NPC] Loaded {} trades for {}", list.size(), entityId);
            } catch (Exception e) {
                LOGGER.warn("[NPC] Failed reading trades {}: {}", fileId, e.toString());
            }
        });
    }

    // === 数据类 ===
    public record Template(
            String buyA, int buyACount,
            String buyB, int buyBCount,
            String sell, int sellCount,
            int maxUses, int xp, float priceMultiplier
    ) {
        public Optional<MerchantOffer> toOffer() {
            ItemStack a = stackOf(buyA, buyACount);
            if (a.isEmpty()) return Optional.empty();
            ItemStack sellStack = stackOf(sell, sellCount);
            if (sellStack.isEmpty()) return Optional.empty();

            ItemStack b = stackOf(buyB, buyBCount);
            MerchantOffer offer = b.isEmpty()
                    ? new MerchantOffer(a, sellStack, maxUses, xp, priceMultiplier)
                    : new MerchantOffer(a, b, sellStack, maxUses, xp, priceMultiplier);
            return Optional.of(offer);
        }

        private static ItemStack stackOf(String id, int count) {
            if (id == null || id.isEmpty() || count <= 0) return ItemStack.EMPTY;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
        }
    }
}
