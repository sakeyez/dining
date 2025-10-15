package com.sake.npc.creeper;

// 注意：这里不再需要 import 任何 create 模组的东西
import com.sake.npc.creeper.CreeperActivateHandCrankGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;


public class CreeperNPCEntity extends Creeper {
    public CreeperNPCEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 添加我们新的AI，优先级设为1
        this.goalSelector.addGoal(1, new CreeperActivateHandCrankGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);


        Item hat = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("create", "goggles"));
        Item wrenchItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("create", "wrench"));

        // 如果找到了扳手物品（即Create模组已加载）
        if (wrenchItem != null) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(wrenchItem));

        if (hat != null) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(hat));



        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        return data;
    }
}