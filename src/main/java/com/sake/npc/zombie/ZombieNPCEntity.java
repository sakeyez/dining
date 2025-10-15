// 文件路径: src/main/java/com/sake/npc/zombie/ZombieNPCEntity.java

package com.sake.npc.zombie;

import com.sake.npc.zombie.ai.ZombieFarmerGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer; // 引入背包
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import com.sake.npc.zombie.ai.DepositItemsGoal;

public class ZombieNPCEntity extends Zombie implements Merchant {

    // --- 交易相关的变量 ---
    private MerchantOffers offers = new MerchantOffers();
    private Player tradingPlayer;
    private int xp = 0;

    // --- 新增的背包变量 ---
    private final SimpleContainer inventory = new SimpleContainer(8);

    public ZombieNPCEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    /**
     * 这里是注册AI的核心方法。
     * 我们清除了之前重复和混乱的注册，换上了清晰的优先级。
     */
    @Override
    protected void registerGoals() {
        // System.out.println("ZombieNPCEntity: 注册AI Goal！"); // 这行可以保留用于调试

        // 优先级 0: 溺水时上浮 (最重要，保命)
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 优先级 1: 执行我们编写的农夫AI (最重要的工作)
        // 注意：这里的 ZombieFarmerGoal 是我们下一个文件要创建的
        this.goalSelector.addGoal(1, new ZombieFarmerGoal(this, 0.8D));

        this.goalSelector.addGoal(2, new DepositItemsGoal(this, 0.8D));
        // 存物品
        // 优先级 8: 看向附近的玩家 (比较次要的行为)
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));

        // 优先级 9: 随机闲逛 (没事做的时候才做)
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.6D));
    }

    /**
     * 让这个NPC不会因为距离玩家太远而消失
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // =======================================================================
    // 下面是你已经实现的交易功能，我们完整保留
    // =======================================================================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (this.offers.isEmpty()) {
                // 你可以保留这个默认交易，或者之后通过JSON文件加载
                this.offers.add(new MerchantOffer(
                        new ItemStack(Items.ROTTEN_FLESH, 5),
                        new ItemStack(Items.EMERALD, 1),
                        10, 2, 0.05F
                ));
            }
            this.setTradingPlayer(player);
            this.openTradingScreen(player, Component.literal("僵尸农夫"), 1);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        this.playSound(getNotifyTradeSound(), 1.0F, 1.0F);
    }

    @Override
    public void notifyTradeUpdated(ItemStack itemStack) {}

    @Override
    public int getVillagerXp() {
        return this.xp;
    }

    @Override
    public void overrideXp(int xp) {
        this.xp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    // =======================================================================
    // 新增的背包数据存取功能
    // =======================================================================

    /**
     * 保存实体数据时，把背包内容也保存进去
     */
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("Inventory", this.inventory.createTag());
    }

    /**
     * 读取实体数据时，把背包内容也读取出来
     */
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.inventory.fromTag(compound.getList("Inventory", 10));
    }

    /**
     * 提供一个公共方法，让AI代码可以访问到这个实体的背包
     * @return 实体内部的 SimpleContainer 背包实例
     */
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    // =======================================================================
    // 出生时的设置
    // =======================================================================

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType,
                                        @Nullable SpawnGroupData groupData,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);

        // --- 你原来的装备设置代码，完全保留 ---
        Item hat = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "straw_hat_flower"));
        Item chest = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_chest_plate"));
        Item legs = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_leggings"));
        Item boots = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "farmer_boots"));

        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));

        if (hat != null) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(hat));
        if (chest != null) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        if (legs != null) this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        if (boots != null) this.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
        // --- 装备代码结束 ---

        // --- 新增：给背包里预装一些种子 ---
        this.inventory.addItem(new ItemStack(Items.WHEAT_SEEDS, 32));
        this.inventory.addItem(new ItemStack(Items.POTATO, 32));
        this.inventory.addItem(new ItemStack(Items.CARROT, 32));

        return data;
    }
}