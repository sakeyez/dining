// 文件路径: src/main/java/com/sake/factions/FactionHandler.java
package com.sake.factions;

import com.sake.dining.DiningItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FactionHandler {

    // ... 其他代码都正确，无需改动 ...

    private static void grantAdvancement(Player player, String factionId) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceLocation advId = ResourceLocation.fromNamespaceAndPath(Factions.MODID, "become_friend_with_" + factionId);
            Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(advId);
            if (advancement != null) {
                AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
                if (!progress.isDone()) {
                    // 当我们完成成就的所有标准后，原版游戏会自动处理弹窗和广播
                    progress.getRemainingCriteria().forEach(progress::grantProgress);

                    // 【最终修复】删除下面这行手动发送消息的代码！
                    // player.sendSystemMessage(Component.translatable("advancements.factions.friendship." + factionId + ".description"));
                }
            }
        }
    }

    // ... 其他所有未改动的方法保持原样 ...

    //为了方便你完整替换，下面是文件的全部内容

    public static class Faction {
        public final String id;
        public final Set<String> memberIds = new HashSet<>();
        public Faction(String id, Collection<String> initialMembers) {
            this.id = id;
            if (initialMembers != null) { this.memberIds.addAll(initialMembers); }
        }
    }

    public static final Map<String, Faction> FACTIONS = new HashMap<>();
    public static final Set<String> FRIENDLY_FACTIONS_POOL = Set.of("zombies", "skeletons", "creepers", "blazes", "slimes", "endermen");
    private static final Set<EntityType<?>> DEFAULT_NEUTRAL_MOBS = new HashSet<>();

    public static void initializeFactions() {
        FACTIONS.clear();
        FACTIONS.put("zombies", new Faction("zombies", Set.of("minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk", "minecraft:drowned")));
        FACTIONS.put("skeletons", new Faction("skeletons", Set.of("minecraft:skeleton", "minecraft:stray", "minecraft:wither_skeleton")));
        FACTIONS.put("slimes", new Faction("slimes", Set.of("minecraft:slime", "minecraft:magma_cube")));
        FACTIONS.put("creepers", new Faction("creepers", Set.of("minecraft:creeper")));
        FACTIONS.put("blazes", new Faction("blazes", Set.of("minecraft:blaze")));
        FACTIONS.put("endermen", new Faction("endermen", Set.of("minecraft:enderman")));

        Set<String> neutralMobIds = Set.of(
                "minecraft:blaze", "minecraft:bogged", "minecraft:creeper", "minecraft:elder_guardian",
                "minecraft:endermite", "minecraft:ghast", "minecraft:guardian", "minecraft:hoglin",
                "minecraft:husk", "minecraft:magma_cube", "minecraft:phantom", "minecraft:piglin_brute",
                "minecraft:shulker", "minecraft:silverfish", "minecraft:skeleton", "minecraft:slime",
                "minecraft:stray", "minecraft:warden", "minecraft:wither_skeleton", "minecraft:zombified_piglin",
                "minecraft:zombie_villager", "minecraft:zombie"
        );
        neutralMobIds.forEach(id -> {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
            if (type != null) {
                DEFAULT_NEUTRAL_MOBS.add(type);
            }
        });
    }

    public static String getFactionIdFor(EntityType<?> type) {
        String entityId = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();
        for (Faction faction : FACTIONS.values()) {
            if (faction.memberIds.contains(entityId)) {
                return faction.id;
            }
        }
        return null;
    }

    private static FactionData PERSISTED_DATA;

    private static class FactionData extends SavedData {
        static final String DATA_NAME = "dining_faction_data";
        public final Map<UUID, Set<String>> playerFriendlyFactions = new HashMap<>();

        public static FactionData load(CompoundTag tag) {
            FactionData data = new FactionData();
            ListTag playerList = tag.getList("PlayerFriendlyFactions", 10);
            for (Tag playerTag : playerList) {
                CompoundTag pTag = (CompoundTag) playerTag;
                UUID uuid = pTag.getUUID("UUID");
                Set<String> friendlySet = new HashSet<>();
                ListTag factionsList = pTag.getList("Factions", 8);
                factionsList.forEach(factionTag -> friendlySet.add(factionTag.getAsString()));
                data.playerFriendlyFactions.put(uuid, friendlySet);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag playerList = new ListTag();
            playerFriendlyFactions.forEach((uuid, friendlySet) -> {
                CompoundTag pTag = new CompoundTag();
                pTag.putUUID("UUID", uuid);
                ListTag factionsList = new ListTag();
                friendlySet.forEach(factionId -> factionsList.add(StringTag.valueOf(factionId)));
                pTag.put("Factions", factionsList);
                playerList.add(pTag);
            });
            tag.put("PlayerFriendlyFactions", playerList);
            return tag;
        }
    }

    public static void load(ServerLevel level) {
        initializeFactions();
        PERSISTED_DATA = level.getDataStorage().computeIfAbsent(FactionData::load, FactionData::new, FactionData.DATA_NAME);
    }

    public static void setDirty() {
        if (PERSISTED_DATA != null) {
            PERSISTED_DATA.setDirty();
        }
    }

    public static boolean isPlayerFriendlyWith(UUID playerId, String factionId) {
        if (PERSISTED_DATA == null || factionId == null) return false;
        return PERSISTED_DATA.playerFriendlyFactions.getOrDefault(playerId, Collections.emptySet()).contains(factionId);
    }

    public static void makePlayerFriendly(Player player, String factionId) {
        if (PERSISTED_DATA == null) return;
        Set<String> friendlySet = PERSISTED_DATA.playerFriendlyFactions.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        if (friendlySet.add(factionId)) {
            setDirty();
            grantAdvancement(player, factionId);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer player) || event.getEntity() instanceof Player) return;
        if (!(event.getEntity() instanceof Mob attacker)) return;

        String factionId = getFactionIdFor(attacker.getType());
        if (factionId != null && FRIENDLY_FACTIONS_POOL.contains(factionId) && isPlayerFriendlyWith(player.getUUID(), factionId)) {
            event.setCanceled(true);
            return;
        }

        if (DEFAULT_NEUTRAL_MOBS.contains(attacker.getType())) {
            LivingEntity lastHurtBy = attacker.getLastHurtByMob();
            if (lastHurtBy == null || !lastHurtBy.getUUID().equals(player.getUUID())) {
                event.setCanceled(true);
            }
        }

        if ("zombies".equals(factionId) && event.getNewTarget() instanceof Villager) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerPickupItem(PlayerEvent.ItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Map<Item, String> tokenToFactionMap = Map.of(
                DiningItems.ZOMBIE_TOKEN.get(), "zombies",
                DiningItems.SKELETON_TOKEN.get(), "skeletons",
                DiningItems.CREEPER_TOKEN.get(), "creepers",
                DiningItems.BLAZE_TOKEN.get(), "blazes",
                DiningItems.SLIME_TOKEN.get(), "slimes",
                DiningItems.ENDERMAN_TOKEN.get(), "endermen"
        );
        String factionId = tokenToFactionMap.get(event.getStack().getItem());
        if (factionId != null) {
            makePlayerFriendly(player, factionId);
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || PERSISTED_DATA == null) return;
        Set<String> friendlyFactions = PERSISTED_DATA.playerFriendlyFactions.getOrDefault(player.getUUID(), Collections.emptySet());
        if (friendlyFactions.isEmpty()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker instanceof Player) return;

        List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0));
        for (Mob mob : nearbyMobs) {
            String mobFactionId = getFactionIdFor(mob.getType());
            if (mobFactionId != null && friendlyFactions.contains(mobFactionId) && mob.getTarget() == null) {
                mob.setTarget(attacker);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("factions")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    listFactions(ctx.getSource());
                                    return 1;
                                }))
                        .then(Commands.literal("check")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    if (PERSISTED_DATA != null) {
                                        Set<String> friendlySet = PERSISTED_DATA.playerFriendlyFactions.getOrDefault(player.getUUID(), Collections.emptySet());
                                        ctx.getSource().sendSuccess(() -> Component.literal("你的友好派系: " + String.join(", ", friendlySet)), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    if (PERSISTED_DATA != null) {
                                        PERSISTED_DATA.playerFriendlyFactions.remove(player.getUUID());
                                        setDirty();
                                        revokeAdvancements(player);
                                        ctx.getSource().sendSuccess(() -> Component.literal("已重置你的所有派系友好度。"), false);
                                    }
                                    return 1;
                                }))
        );
    }

    private static void revokeAdvancements(ServerPlayer player) {
        for (String factionId : FRIENDLY_FACTIONS_POOL) {
            ResourceLocation advId = ResourceLocation.fromNamespaceAndPath(Factions.MODID, "become_friend_with_" + factionId);
            Advancement advancement = player.getServer().getAdvancements().getAdvancement(advId);
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                if (progress.isDone()) {
                    progress.getCompletedCriteria().forEach(criterion -> player.getAdvancements().revoke(advancement, criterion));
                }
            }
        }
        ResourceLocation rootId = ResourceLocation.fromNamespaceAndPath(Factions.MODID, "root");
        Advancement rootAdv = player.getServer().getAdvancements().getAdvancement(rootId);
        if (rootAdv != null) {
            AdvancementProgress rootProgress = player.getAdvancements().getOrStartProgress(rootAdv);
            if (rootProgress.isDone()) {
                rootProgress.getCompletedCriteria().forEach(c -> player.getAdvancements().revoke(rootAdv, c));
            }
        }
    }

    private static void listFactions(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§a=== 可达成友好的派系 ==="), false);
        FACTIONS.forEach((id, faction) -> {
            String members = faction.memberIds.stream().map(s -> s.replace("minecraft:", "")).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal("§e" + id + ": §f" + members), false);
        });
        source.sendSuccess(() -> Component.literal("§c=== 默认中立的生物 ==="), false);
        String neutralMobs = DEFAULT_NEUTRAL_MOBS.stream()
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(Objects::nonNull)
                .map(ResourceLocation::getPath)
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal(neutralMobs), false);
    }
}