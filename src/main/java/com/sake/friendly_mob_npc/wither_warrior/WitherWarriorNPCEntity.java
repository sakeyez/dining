package com.sake.friendly_mob_npc.wither_warrior;

import com.google.common.collect.Sets;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 凋零骷髅战士伙伴的实体类。
 * 继承自 TamableAnimal，使其拥有可被驯服的特性。
 */
public class WitherWarriorNPCEntity extends TamableAnimal {

    // --- 同步数据定义 ---
    /** 用于同步战士等级的数据访问器 */
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(WitherWarriorNPCEntity.class, EntityDataSerializers.INT);

    // --- 内部状态字段 ---
    /** 记录已经吃过的食物ID，防止重复喂食同一种食物 */
    private Set<ResourceLocation> eatenFoods = Sets.newHashSet();
    /** 一个标记，用于在实体数据加载后强制更新一次属性 */
    private boolean needsStatUpdate = true;
    /** 等级3特殊攻击的冷却计时器 */
    private int specialAttackCooldown = 0;
    /** 等级3特殊攻击的冷却时间（5秒） */
    private static final int SPECIAL_ATTACK_INTERVAL = 100;

    /**
     * 食物黑名单。
     * 这里的食物是用于升级仪式的，通过此列表阻止玩家直接右键喂食。
     */
    private static final Set<ResourceLocation> UNFEEDABLE_FOODS = Sets.newHashSet();
    static {
        Stream.of(
                "kaleidoscope_cookery:fondant_spider_eye",
                "kaleidoscope_cookery:pan_seared_knight_steak"
        ).map(ResourceLocation::tryParse).filter(Objects::nonNull).forEach(UNFEEDABLE_FOODS::add);
    }

    /**
     * 构造函数。
     */
    public WitherWarriorNPCEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false); // 设置为不能拾取地上的物品/装备
    }

    /**
     * 处理玩家与凋零战士伙伴的交互逻辑。
     * @param player 交互的玩家
     * @param hand   交互时使用的手
     * @return 交互结果
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        // 如果玩家手持召唤物，则不进行任何操作
        if (heldItem.getItem() instanceof WitherWarriorSummonItem) {
            return InteractionResult.PASS;
        }
        // 如果玩家不是主人，则执行父类的默认交互
        if (!this.isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }
        // 如果是客户端，则提前返回成功，防止玩家手臂挥动两次
        if (this.level().isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        // --- 潜行状态下的交互 ---
        if (player.isShiftKeyDown()) {
            // 潜行空手右键：切换坐下/站起状态
            if (heldItem.isEmpty()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                return InteractionResult.SUCCESS;
            }
            // 潜行持剑/斧右键：交换武器
            else if (heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem) {
                ItemStack currentItem = this.getMainHandItem();
                this.setItemInHand(InteractionHand.MAIN_HAND, heldItem.copy());
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, currentItem);
                }
                this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        // --- 非潜行状态下的交互 (喂食) ---
        else if (heldItem.isEdible()) {
            ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());

            // 检查食物是否在黑名单中（仪式食物）
            if (UNFEEDABLE_FOODS.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e这种食物似乎蕴含着强大的力量，直接喂食太浪费了，应该让它以一种更具仪式感的方式享用。"));
                return InteractionResult.FAIL;
            }

            // 检查是否已达到升级瓶颈
            if (isRitualPending()) {
                String requiredFoodName = this.getLevel() == 1 ? "翻糖蛛眼" : "香煎骑士牛排";
                player.sendSystemMessage(Component.literal("§c它似乎遇到了瓶颈... (提示: 将 " + requiredFoodName + " 放置在地上)"));
                return InteractionResult.CONSUME;
            }

            // 检查是否已经吃过这种食物
            if (eatenFoods.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e战士伙伴摇了摇头，看来它已经尝过这个味道了。"));
                return InteractionResult.FAIL;
            }

            // 消耗食物并播放效果
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }
            this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, heldItem.copy()), this.getRandomX(0.8), this.getY(0.6), this.getRandomZ(0.8), 10, 0.2, 0.2, 0.2, 0.05);
            }

            // 记录食物并更新属性
            eatenFoods.add(foodId);
            applyStats(false);

            // 发送进度消息
            int progress = eatenFoods.size();
            int goal = (this.getLevel() == 1) ? 20 : 40;
            player.sendSystemMessage(Component.literal("§a凋零伙伴吃下了新食物，它的气息变强了！ (§b" + progress + "§a/" + goal + ")"));

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    /**
     * 每tick执行的逻辑。
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // 读档后更新属性
        if (this.needsStatUpdate) {
            applyStats(true);
            this.needsStatUpdate = false;
        }

        // 等级3特殊攻击的冷却计时器
        if (this.getLevel() >= 3) {
            if (this.specialAttackCooldown > 0) {
                this.specialAttackCooldown--;
            }
        }
    }

    /**
     * 处理攻击命中目标的逻辑，根据等级附加不同的技能效果。
     * @param e 被攻击的实体
     * @return 攻击是否成功
     */
    @Override
    public boolean doHurtTarget(Entity e) {
        boolean result = super.doHurtTarget(e);
        if (result && e instanceof LivingEntity living) {
            int level = getLevel();

            // --- 等级3的特殊攻击伤害 ---
            if (level >= 3 && this.specialAttackCooldown <= 0) {
                float bonusDamage = 0;
                for (MobEffectInstance effectInstance : living.getActiveEffects()) {
                    if (!effectInstance.getEffect().isBeneficial()) {
                        bonusDamage += (effectInstance.getAmplifier() + 1);
                    }
                }
                if (bonusDamage > 0) {
                    living.hurt(damageSources().magic(), bonusDamage * 2.5F);
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SOUL, living.getX(), living.getEyeY(), living.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                    }
                    this.playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.0F);
                }
                this.specialAttackCooldown = SPECIAL_ATTACK_INTERVAL;
            }

            // --- 根据等级施加普通攻击的负面效果 ---
            switch (level) {
                case 1:
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0), this); // 10秒 凋零I
                    break;
                case 2:
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 1), this);       // 15秒 凋零II
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 1), this);       // 15秒 剧毒II
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0), this); // 15秒 缓慢I
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0), this);       // 15秒 虚弱I
                    break;
                case 3:
                    // 【核心修改】计算当前等级上限
                    // 升到3级需要吃40种食物，所以从第41种开始计算
                    int foodsEatenAfterLvl3 = Math.max(0, this.eatenFoods.size() - 40);
                    // 基础上限为4，每多吃10种，上限+2
                    int capBonus = (foodsEatenAfterLvl3 / 10) * 2;
                    // 最终上限不能超过10
                    int effectLevelCap = Math.min(10, 4 + capBonus);

                    // 提升凋零等级（带上限）
                    if (living.hasEffect(MobEffects.WITHER)) {
                        MobEffectInstance existing = living.getEffect(MobEffects.WITHER);
                        // 计算新等级，但不超过上限 (amplifier = level - 1)
                        int newAmplifier = Math.min(effectLevelCap - 1, existing.getAmplifier() + 1);
                        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, newAmplifier), this);
                    } else {
                        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 0), this);
                    }

                    // 提升剧毒等级（带上限）
                    if (living.hasEffect(MobEffects.POISON)) {
                        MobEffectInstance existing = living.getEffect(MobEffects.POISON);
                        // 计算新等级，但不超过上限 (amplifier = level - 1)
                        int newAmplifier = Math.min(effectLevelCap - 1, existing.getAmplifier() + 1);
                        living.addEffect(new MobEffectInstance(MobEffects.POISON, 300, newAmplifier), this);
                    } else {
                        living.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0), this);
                    }

                    // 施加缓慢II和虚弱II
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1), this);
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 1), this);
                    break;
            }
        }
        return result;
    }

    /**
     * 处理被施加药水效果的逻辑。
     */
    @Override
    public boolean addEffect(MobEffectInstance i, @Nullable Entity e) {
        return super.addEffect(i, e);
    }

    /**
     * 根据等级和已吃食物数量，计算并应用凋零战士的属性。
     * @param isLevelReset 如果为true，会直接将生命值设为最大值。
     */
    public void applyStats(boolean isLevelReset) {
        if (this.level().isClientSide()) return;
        int level = this.getLevel();
        int totalFoodCount = this.eatenFoods.size();
        int foodsThisLevel = Math.max(0, totalFoodCount - (level > 1 ? (level - 1) * 20 : 0));
        //生命值、基础攻击、速度
        double baseHealth = switch (level) { case 2 -> 40.0; case 3 -> 80.0; default -> 20.0; };
        double baseAttack = switch (level) { case 2 -> 2.0; case 3 -> 3.0; default -> 1.0; };
        double baseSpeed = switch (level) { case 2 -> 0.25; case 3 -> 0.28; default -> 0.22; };
        //吃东西获得的额外属性
        double bonusHealth = 0, bonusAttack = 0;
        if (level == 1) { bonusHealth = foodsThisLevel * 1.0; bonusAttack = foodsThisLevel * 0.0; }
        else if (level == 2) { bonusHealth = foodsThisLevel * 2.0; bonusAttack = foodsThisLevel * 0.0; }
        else if (level == 3) { bonusHealth = foodsThisLevel * 1.0; bonusAttack = foodsThisLevel * 0.0; }

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth + bonusHealth);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack + bonusAttack);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);

        if (isLevelReset) {
            this.setHealth(this.getMaxHealth());
        } else {
            this.heal(level == 2 ? 2.0f : 1.0f);
        }
    }

    /**
     * 将凋零战士的数据保存到NBT标签中。
     */
    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        c.putInt("WarriorLevel", getLevel());
        ListTag eatenList = new ListTag();
        eatenFoods.forEach(food -> eatenList.add(StringTag.valueOf(food.toString())));
        c.put("EatenFoods", eatenList);
    }

    /**
     * 从NBT标签中读取凋零战士的数据。
     */
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        setLevel(c.getInt("WarriorLevel"));
        eatenFoods.clear();
        ListTag eatenList = c.getList("EatenFoods", 8);
        eatenList.forEach(tag -> {
            ResourceLocation rl = ResourceLocation.tryParse(tag.getAsString());
            if (rl != null) eatenFoods.add(rl);
        });
        needsStatUpdate = true;
    }

    /**
     * 检查是否满足升级仪式的条件。
     */
    public boolean isRitualPending() {
        int foodCount = eatenFoods.size();
        int level = this.getLevel();
        return (level == 1 && foodCount >= 20) || (level == 2 && foodCount >= 40);
    }

    /**
     * 注册AI目标。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers(Monster.class));
    }

    /**
     * 处理受伤逻辑，提供减伤并免疫凋零伤害。
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypes.WITHER)) return false;
        double r = switch(getLevel()){case 2->0.1;case 3->0.2;default->0.0;};
        return super.hurt(pSource,(float)(pAmount*(1.0-r)));
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        if (this.isOwnedBy(pTarget)) return;
        super.setTarget(pTarget);
    }

    @Override
    public float getScale() {
        return 1.25F;
    }

    @Override
    public EntityDimensions getDimensions(Pose p) {
        return super.getDimensions(p).scale(getScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> k) {
        if(DATA_LEVEL.equals(k)) refreshDimensions();
        super.onSyncedDataUpdated(k);
    }

    public void setLevel(int l) {
        entityData.set(DATA_LEVEL, l);
        applyStats(true);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LEVEL,1);
    }

    public int getLevel() {
        return entityData.get(DATA_LEVEL);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }
}