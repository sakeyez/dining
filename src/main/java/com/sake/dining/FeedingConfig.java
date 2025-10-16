package com.sake.dining;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
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

    // 【核心修正1】创建两个Map，分别存储直接ID的食谱和标签的食谱
    private static final Map<ResourceLocation, List<ResourceLocation>> DIRECT_RECIPES = new HashMap<>();
    private static final Map<TagKey<EntityType<?>>, List<ResourceLocation>> TAG_RECIPES = new HashMap<>();

    private static final Set<ResourceLocation> PLACEABLE_FOODS = new HashSet<>();

    public static synchronized void load() {
        try {
            Path configFile = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

            if (Files.notExists(configFile)) {
                LOGGER.info("[Dining] Config file not found, creating default dining.json...");
                try (InputStream is = FeedingConfig.class.getResourceAsStream("/" + FILE_NAME)) {
                    if (is == null) {
                        throw new IllegalStateException("Default dining.json not found in resources!");
                    }
                    Files.copy(is, configFile);
                    LOGGER.info("[Dining] Default config file created at: {}", configFile);
                } catch (Exception e) {
                    LOGGER.error("[Dining] Failed to create default config file!", e);
                    return;
                }
            }

            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> raw;
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                raw = new Gson().fromJson(reader, type);
            }
            if(raw == null) return;

            // 清空旧数据
            DIRECT_RECIPES.clear();
            TAG_RECIPES.clear();
            PLACEABLE_FOODS.clear();

            for (Map.Entry<String, List<String>> e : raw.entrySet()) {
                String key = e.getKey();
                List<ResourceLocation> foods = e.getValue().stream()
                        .map(ResourceLocation::tryParse)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 【核心修正2】判断键是ID还是标签
                if (key.startsWith("#")) {
                    // 如果是标签，移除'#'后创建TagKey
                    ResourceLocation tagLocation = ResourceLocation.tryParse(key.substring(1));
                    if (tagLocation != null) {
                        TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagLocation);
                        TAG_RECIPES.put(tagKey, foods);
                    }
                } else {
                    // 否则，作为直接ID处理
                    ResourceLocation mobId = ResourceLocation.tryParse(key);
                    if (mobId != null) {
                        DIRECT_RECIPES.put(mobId, foods);
                    }
                }
                PLACEABLE_FOODS.addAll(foods);
            }
            LOGGER.info("[Dining] Config loaded for {} direct mob types and {} mob tags.", DIRECT_RECIPES.size(), TAG_RECIPES.size());

        } catch (Exception ex) {
            LOGGER.error("[Dining] Error loading config!", ex);
            DIRECT_RECIPES.clear();
            TAG_RECIPES.clear();
            PLACEABLE_FOODS.clear();
        }
    }

    public static boolean isKnownPlaceableFood(ResourceLocation itemRL) {
        return PLACEABLE_FOODS.contains(itemRL);
    }

    // 【核心修正3】重构查询逻辑，使其能同时处理ID和标签
    public static List<ResourceLocation> getFoodBlocks(Mob mob) {
        // 优先查询直接ID
        List<ResourceLocation> foods = DIRECT_RECIPES.get(mob.getType().builtInRegistryHolder().key().location());
        if (foods != null && !foods.isEmpty()) {
            return foods;
        }

        // 如果没有直接ID的配置，则遍历所有标签配置
        for (Map.Entry<TagKey<EntityType<?>>, List<ResourceLocation>> entry : TAG_RECIPES.entrySet()) {
            if (mob.getType().is(entry.getKey())) {
                return entry.getValue(); // 找到第一个匹配的标签就返回
            }
        }

        return Collections.emptyList(); // 如果都找不到，返回空列表
    }
}