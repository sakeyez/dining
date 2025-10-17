package com.sake.friendly_npc.zombie_warrior;

import com.google.common.collect.Sets;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;
import java.util.stream.Stream;

/**
 * 僵尸战士伙伴的实体类。
 * 继承自 TamableAnimal，使其拥有可被驯服的特性。
 */
public class WarriorNPCEntity extends TamableAnimal {

    // --- 同步数据定义 ---
    /** 用于同步战士等级的数据访问器 */
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(WarriorNPCEntity.class, EntityDataSerializers.INT);

    // --- 内部状态字段 ---
    /** 记录已经吃过的食物ID，防止重复喂食同一种食物 */
    private Set<ResourceLocation> eatenFoods = Sets.newHashSet();
    /** 等级3技能的冷却计时器 */
    private int regenCooldown = 0;
    /** 一个标记，用于在实体数据加载后强制更新一次属性 */
    private boolean needsStatUpdate = true;
    /** 等级2传送技能的冷却计时器 */
    private int teleportCheckCooldown = 0;

    /**
     * 食物黑名单。
     * 这里的食物是用于升级仪式的，通过此列表阻止玩家直接右键喂食。
     */
    private static final Set<ResourceLocation> UNFEEDABLE_FOODS = Sets.newHashSet();
    static {
        Stream.of(
                "kaleidoscope_cookery:dark_cuisine",
                "kaleidoscope_cookery:suspicious_stir_fry",
                "kaleidoscope_cookery:slime_ball_meal",
                "kaleidoscope_cookery:fondant_pie",
                "kaleidoscope_cookery:dongpo_pork",
                "kaleidoscope_cookery:fondant_spider_eye",
                "kaleidoscope_cookery:chorus_fried_egg",
                "kaleidoscope_cookery:braised_fish",
                "kaleidoscope_cookery:golden_salad",
                "kaleidoscope_cookery:spicy_chicken",
                "kaleidoscope_cookery:yakitori",
                "kaleidoscope_cookery:pan_seared_knight_steak",
                "kaleidoscope_cookery:stargazy_pie",
                "kaleidoscope_cookery:sweet_and_sour_ender_pearls",
                "kaleidoscope_cookery:crystal_lamb_chop",
                "kaleidoscope_cookery:blaze_lamb_chop",
                "kaleidoscope_cookery:frost_lamb_chop",
                "kaleidoscope_cookery:nether_style_sashimi",
                "kaleidoscope_cookery:end_style_sashimi",
                "kaleidoscope_cookery:desert_style_sashimi",
                "kaleidoscope_cookery:tundra_style_sashimi",
                "kaleidoscope_cookery:cold_style_sashimi",
                "kaleidoscope_cookery:shengjian_mantou"
        ).map(ResourceLocation::new).forEach(UNFEEDABLE_FOODS::add);
    }

    /**
     * 构造函数。
     */
    public WarriorNPCEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false); // 设置为不能拾取地上的物品/装备
    }

    /**
     * 处理玩家与战士伙伴的交互逻辑。
     * @param player 交互的玩家
     * @param hand   交互时使用的手
     * @return 交互结果
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        // 如果玩家手持召唤物，则不进行任何操作，允许召唤物本身的逻辑执行
        if (heldItem.getItem() instanceof WarriorSummonItem) {
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
                this.getNavigation().stop(); // 切换状态时停止移动
                return InteractionResult.SUCCESS;
            }
            // 潜行持剑/斧右键：交换武器
            else if (heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem) {
                ItemStack currentItem = this.getMainHandItem();
                this.setItemInHand(InteractionHand.MAIN_HAND, heldItem.copy());
                // 如果不是创造模式，则将战士原先的武器给玩家
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

            // 检查食物是否在黑名单中
            if (UNFEEDABLE_FOODS.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e这种食物似乎蕴含着强大的力量，直接喂食太浪费了，应该让它以一种更具仪式感的方式享用。"));
                return InteractionResult.FAIL;
            }

            // 检查是否已达到升级瓶颈
            if (isRitualPending()) {
                String requiredFoodName = this.getLevel() == 1 ? "珍珠咕咾肉" : "黄金沙拉";
                player.sendSystemMessage(Component.literal("§c它似乎遇到了瓶颈... (提示: 将 " + requiredFoodName + " 放置在地上)"));
                return InteractionResult.CONSUME;
            }

            // 检查是否已经吃过这种食物
            if (eatenFoods.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e战士伙伴摇了摇头，看来它已经尝过这个味道了。"));
                return InteractionResult.FAIL;
            }

            // 消耗食物
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }
            // 播放音效和粒子效果
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
            player.sendSystemMessage(Component.literal("§a战士伙伴吃下了新食物，它的气息变强了！ (§b" + progress + "§a/" + goal + ")"));

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    /**
     * 根据等级和已吃食物数量，计算并应用战士的属性。
     * @param isLevelReset 如果为true，通常在升级或读档后调用，会直接将生命值设为最大值。
     */
    public void applyStats(boolean isLevelReset) {
        if (this.level().isClientSide()) {
            return;
        }

        int level = this.getLevel();
        int totalFoodCount = this.eatenFoods.size();

        // 计算当前等级下吃过的食物数量
        int foodsThisLevel = 0;
        if (level == 1) {
            foodsThisLevel = totalFoodCount;
        } else if (level == 2) {
            foodsThisLevel = totalFoodCount - 20; // 减去第一阶段的20个
        } else if (level == 3) {
            foodsThisLevel = totalFoodCount - 40; // 减去前两阶段的40个
        }
        foodsThisLevel = Math.max(0, foodsThisLevel);

        // --- 基础属性 ---
        double baseHealth = switch(level) { case 2 -> 40.0; case 3 -> 80.0; default -> 20.0; };
        double baseAttack = switch(level) { case 2 -> 5.0; case 3 -> 7.0; default -> 3.0; };
        double baseSpeed  = switch(level) { case 2 -> 0.27; case 3 -> 0.31; default -> 0.23; };

        // --- 喂食带来的额外属性 ---
        double bonusHealth = 0, bonusAttack = 0;
        if (level == 1) {
            bonusHealth = foodsThisLevel * 1.0;
            bonusAttack = foodsThisLevel * 0.1;
        } else if (level == 2) {
            bonusHealth = foodsThisLevel * 2.0;
            bonusAttack = foodsThisLevel * 0.1;
        } else if (level == 3) {
            bonusHealth = foodsThisLevel * 1.0;
            bonusAttack = foodsThisLevel * 0.2;
        }

        // --- 应用最终属性 ---
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth + bonusHealth);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack + bonusAttack);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);

        // --- 生命值处理 ---
        if (isLevelReset) {
            this.setHealth(this.getMaxHealth()); // 直接回满血
        } else {
            this.heal(level == 2 ? 2.0f : 1.0f); // 喂食少量回血
        }
    }

    /**
     * 将战士的数据保存到NBT标签中。
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
     * 从NBT标签中读取战士的数据。
     */
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        setLevel(c.getInt("WarriorLevel"));
        eatenFoods.clear();
        ListTag eatenList = c.getList("EatenFoods", 8);
        eatenList.forEach(tag -> eatenFoods.add(new ResourceLocation(tag.getAsString())));
        needsStatUpdate = true; // 标记为需要更新属性
    }

    /**
     * 检查是否满足升级仪式的条件（吃够了当前阶段的食物）。
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
        // 优先级 0: 溺水时上浮
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 优先级 1: 根据主人命令坐下
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        // 优先级 2: 近战攻击
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        // 优先级 4: 跟随主人
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        // 优先级 5: 随机闲逛，会避开水
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        // 优先级 6: 看向附近的玩家
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 优先级 7: 随机环顾四周
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // --- 目标选择AI (决定攻击谁) ---
        // 优先级 1: 当主人被攻击时，攻击攻击者
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        // 优先级 2: 当主人攻击某个目标时，也攻击该目标
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        // 优先级 3: 当自己被攻击时，进行反击
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers(Monster.class));
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

        // --- 冷却计时器 ---
        if (teleportCheckCooldown > 0) {
            teleportCheckCooldown--;
        }

        // --- 技能逻辑 ---
        LivingEntity target = this.getTarget();

        // 等级2技能：当距离目标过远时，传送到目标附近
        if (this.getLevel() >= 2 && target != null && this.distanceToSqr(target) > 25.0) {
            if (teleportCheckCooldown <= 0) {
                this.teleportCheckCooldown = 200; // 10秒冷却
                teleportNearTarget(target);
            }
        }

        // 读档后更新属性
        if (this.needsStatUpdate) {
            applyStats(true);
            this.needsStatUpdate = false;
        }

        if (this.getLevel() >= 3 && this.getHealth() < this.getMaxHealth() && target != null) {
            if (++regenCooldown >= 160) { // 8秒 * 20 ticks/秒 = 160
                // 恢复20点生命值 (10颗心)
                this.heal(20.0F);
                // 获得5秒的抗性提升II
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1)); // 5秒 * 20 ticks/秒 = 100, 等级2 (amplifier为1)

                // 播放音效和粒子效果
                this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0F, 1.2F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    // 创建一个绿色的dust粒子效果
                    DustParticleOptions dustOptions = new DustParticleOptions(new Vector3f(0.2F, 0.8F, 0.2F), 1.0F);
                    serverLevel.sendParticles(dustOptions, this.getRandomX(1.0), this.getY(0.5), this.getRandomZ(1.0), 50, 0.3, 0.5, 0.3, 0.1);
                }
                regenCooldown = 0; // 重置冷却
            }
        }
    }

    /**
     * 处理受伤逻辑，根据等级提供减伤。
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        double damageReduction = switch(getLevel()){
            case 2 -> 0.2; // 2级减伤20%
            case 3 -> 0.4; // 3级减伤40%
            default -> 0.0;
        };
        return super.hurt(pSource, (float)(pAmount * (1.0 - damageReduction)));
    }

    /**
     * 防止战士意外攻击自己的主人。
     */
    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        if (this.isOwnedBy(pTarget)) {
            return;
        }
        super.setTarget(pTarget);
    }

    /**
     * 根据等级获取模型的缩放比例。
     */
    @Override
    public float getScale() {
        return switch(getLevel()) {
            case 2 -> 1.2f;
            case 3 -> 1.5f;
            default -> 1.0f;
        };
    }

    /**
     * 根据缩放比例调整实体的碰撞箱大小。
     */
    @Override
    public EntityDimensions getDimensions(Pose p) {
        return super.getDimensions(p).scale(getScale());
    }

    /**
     * 当同步数据（如等级）更新时，刷新实体的尺寸。
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> k) {
        if(DATA_LEVEL.equals(k)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(k);
    }

    /**
     * 设置战士的等级。
     */
    public void setLevel(int l) {
        entityData.set(DATA_LEVEL, l);
        applyStats(true);
        refreshDimensions();
    }

    /**
     * 定义需要同步到客户端的数据。
     */
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LEVEL,1);
    }

    /**
     * 处理攻击命中目标的逻辑，根据等级附加额外伤害。
     */
    @Override
    public boolean doHurtTarget(Entity e) {
        float damageMultiplier = switch(getLevel()) {
            case 2 -> 1.2f;
            case 3 -> 1.5f;
            default -> 1.0f;
        };
        return e.hurt(damageSources().mobAttack(this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier);
    }

    /**
     * 处理被施加药水效果的逻辑，3级时免疫大部分负面效果。
     */
    @Override
    public boolean addEffect(MobEffectInstance i, @Nullable Entity e) {
        return getLevel() < 3 || i.getEffect().isBeneficial() ? super.addEffect(i,e) : false;
    }

    /**
     * 获取战士的当前等级。
     */
    public int getLevel() {
        return entityData.get(DATA_LEVEL);
    }

    /**
     * 等级2技能：传送到目标附近。
     */
    private void teleportNearTarget(LivingEntity t) {
        Vec3 p = t.position();
        for(int i = 0; i < 10; ++i) {
            double x = p.x() + (getRandom().nextDouble() - 0.5D) * 8;
            double y = p.y() + (getRandom().nextInt(8) - 4);
            double z = p.z() + (getRandom().nextDouble() - 0.5D) * 8;
            if (randomTeleport(x, y, z, false)) {
                getNavigation().stop();
                level().playSound(null, xo, yo, zo, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.0F, 1.0F);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, getX(), getY(0.5), getZ(), 30, 0.5, 1.0, 0.5, 0.1);
                }
                getLookControl().setLookAt(t);
                return;
            }
        }
    }

    /**
     * 该实体不能繁殖。
     */
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }
}