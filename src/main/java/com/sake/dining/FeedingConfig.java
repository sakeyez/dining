package com.sake.dining;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FeedingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Dining/Config");
    private static final String FILE_NAME = "dining.json";
    private static final Map<ResourceLocation, List<ResourceLocation>> RECIPES = new HashMap<>();

    // 【新增】一个集合，用于快速查询所有已知的可放置食物
    private static final Set<ResourceLocation> PLACEABLE_FOODS = new HashSet<>();

    public static synchronized void load() {
        // ... (你原有的 load 方法上半部分保持不变)
        try {
            Path configFile = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
            // ... (创建文件的逻辑)

            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> raw;
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                raw = new Gson().fromJson(reader, type);
            }
            if(raw == null) return;

            Map<ResourceLocation, List<ResourceLocation>> parsed = new HashMap<>();
            PLACEABLE_FOODS.clear(); // 每次加载时清空

            for (Map.Entry<String, List<String>> e : raw.entrySet()) {
                ResourceLocation mobId = ResourceLocation.tryParse(e.getKey());
                if (mobId == null) continue;

                List<ResourceLocation> foods = e.getValue().stream()
                        .map(ResourceLocation::tryParse)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                parsed.put(mobId, foods);
                PLACEABLE_FOODS.addAll(foods); // 【新增】将所有食物都加入到快速查询集合中
            }

            RECIPES.clear();
            RECIPES.putAll(parsed);
            LOGGER.info("[Dining] Config loaded for {} mob types.", RECIPES.size());

        } catch (Exception ex) {
            LOGGER.error("[Dining] Error loading config!", ex);
            RECIPES.clear();
            PLACEABLE_FOODS.clear();
        }
    }

    /**
     * 【新增】一个公共静态方法，供 WarriorNPCEntity 调用
     * @param itemRL 物品的 ResourceLocation
     * @return 这个物品是否在 dining.json 的任何一个食谱中出现过
     */
    public static boolean isKnownPlaceableFood(ResourceLocation itemRL) {
        return PLACEABLE_FOODS.contains(itemRL);
    }

    public static List<ResourceLocation> getFoodBlocks(ResourceLocation entityType) {
        return RECIPES.getOrDefault(entityType, List.of());
    }
}