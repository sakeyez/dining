package com.sake.npc.warrior;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

public class WarriorNPCEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(WarriorNPCEntity.class, EntityDataSerializers.INT);
    private Set<ResourceLocation> eatenFoods = Sets.newHashSet();

    private int regenCooldown = 0;
    private boolean needsStatUpdate = true;
    private int teleportCheckCooldown = 0;

    // --- 【核心修正】创建一个不可喂食的食物黑名单 ---
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

    public WarriorNPCEntity(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); this.setCanPickUpLoot(false); }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() instanceof WarriorSummonItem) return InteractionResult.PASS;
        if (!this.isOwnedBy(player)) return super.mobInteract(player, hand);
        if (this.level().isClientSide()) return InteractionResult.sidedSuccess(true);

        if (player.isShiftKeyDown()) {
            if (heldItem.isEmpty()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                return InteractionResult.SUCCESS;
            } else if (heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem) {
                ItemStack currentItem = this.getMainHandItem();
                this.setItemInHand(InteractionHand.MAIN_HAND, heldItem.copy());
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, currentItem);
                this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        else if (heldItem.isEdible()) {
            ResourceLocation foodId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());

            // 【核心修正】检查是否在黑名单中
            if (UNFEEDABLE_FOODS.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e这种食物似乎蕴含着强大的力量，直接喂食太浪费了，应该让它以一种更具仪式感的方式享用。"));
                return InteractionResult.FAIL;
            }

            if (isRitualPending()) {
                String requiredFoodName = this.getLevel() == 1 ? "珍珠咕咾肉" : "黄金沙拉";
                player.sendSystemMessage(Component.literal("§c它似乎遇到了瓶颈... (提示: 将 "+ requiredFoodName +" 放置在地上)"));
                return InteractionResult.CONSUME;
            }

            if (eatenFoods.contains(foodId)) {
                player.sendSystemMessage(Component.literal("§e战士伙伴摇了摇头，看来它已经尝过这个味道了。"));
                return InteractionResult.FAIL;
            }

            if (!player.getAbilities().instabuild) heldItem.shrink(1);
            this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, heldItem.copy()), this.getRandomX(0.8), this.getY(0.6), this.getRandomZ(0.8), 10, 0.2, 0.2, 0.2, 0.05);
            }

            eatenFoods.add(foodId);
            applyStats(false);

            int progress = eatenFoods.size();
            int goal = (this.getLevel() == 1) ? 20 : 40;
            player.sendSystemMessage(Component.literal("§a战士伙伴吃下了新食物，它的气息变强了！ (§b" + progress + "§a/" + goal + ")"));

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    public void applyStats(boolean isLevelReset) {
        if (this.level().isClientSide()) return;

        int level = this.getLevel();
        int totalFoodCount = this.eatenFoods.size();

        int foodsThisLevel = 0;
        if (level == 1) foodsThisLevel = totalFoodCount;
        else if (level == 2) foodsThisLevel = totalFoodCount - 20; // 减去第一阶段的20个
        else if (level == 3) foodsThisLevel = totalFoodCount - 40; // 减去前两阶段的40个
        foodsThisLevel = Math.max(0, foodsThisLevel);

        double baseHealth = switch(level) { case 2 -> 40.0; case 3 -> 80.0; default -> 20.0; };
        double baseAttack = switch(level) { case 2 -> 4.0; case 3 -> 6.0; default -> 2.0; };
        double baseSpeed = switch(level) { case 2 -> 0.25; case 3 -> 0.30; default -> 0.20; };

        double bonusHealth = 0, bonusAttack = 0;
        if (level == 1) { bonusHealth = foodsThisLevel * 1.0; bonusAttack = foodsThisLevel * 0.1; }
        else if (level == 2) { bonusHealth = foodsThisLevel * 2.0; bonusAttack = foodsThisLevel * 0.1; }
        else if (level == 3) { bonusHealth = foodsThisLevel * 1.0; bonusAttack = foodsThisLevel * 0.1; }

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth + bonusHealth);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack + bonusAttack);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed);

        if (isLevelReset) this.setHealth(this.getMaxHealth());
        else this.heal(level == 2 ? 2.0f : 1.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        c.putInt("WarriorLevel", getLevel());
        ListTag eatenList = new ListTag();
        eatenFoods.forEach(food -> eatenList.add(StringTag.valueOf(food.toString())));
        c.put("EatenFoods", eatenList);
    }
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        setLevel(c.getInt("WarriorLevel"));
        eatenFoods.clear();
        ListTag eatenList = c.getList("EatenFoods", 8);
        eatenList.forEach(tag -> eatenFoods.add(new ResourceLocation(tag.getAsString())));
        needsStatUpdate = true;
    }

    public boolean isRitualPending() {
        int foodCount = eatenFoods.size();
        int level = this.getLevel();
        // 【核心修正】累积式判断
        return (level == 1 && foodCount >= 20) || (level == 2 && foodCount >= 40);
    }

    // ... (其他所有方法都保持不变)
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
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (teleportCheckCooldown > 0) teleportCheckCooldown--;
        LivingEntity target = this.getTarget();
        if (this.getLevel() >= 2 && target != null && this.distanceToSqr(target) > 25.0) {
            if (teleportCheckCooldown <= 0) {
                this.teleportCheckCooldown = 200;
                teleportNearTarget(target);
            }
        }
        if (this.needsStatUpdate) { applyStats(true); this.needsStatUpdate = false; }
        if (this.getLevel() >= 3 && this.getHealth() < this.getMaxHealth()) { if (++regenCooldown >= 40) { this.heal(3.0F); regenCooldown = 0; } }
    }
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) { double r=switch(getLevel()){case 2->0.2;case 3->0.4;default->0.0;}; return super.hurt(pSource,(float)(pAmount*(1.0-r))); }
    @Override
    public void setTarget(@Nullable LivingEntity pTarget) { if (this.isOwnedBy(pTarget)) return; super.setTarget(pTarget); }
    @Override
    public float getScale() { return switch(getLevel()) { case 2->1.2f; case 3->1.5f; default->1.0f; }; }
    @Override
    public EntityDimensions getDimensions(Pose p) { return super.getDimensions(p).scale(getScale()); }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> k) { if(DATA_LEVEL.equals(k))refreshDimensions(); super.onSyncedDataUpdated(k); }
    public void setLevel(int l) { entityData.set(DATA_LEVEL, l); applyStats(true); refreshDimensions(); }
    @Override
    protected void defineSynchedData() { super.defineSynchedData(); entityData.define(DATA_LEVEL,1); }
    @Override
    public boolean doHurtTarget(Entity e){float m=switch(getLevel()){case 2->1.2f;case 3->1.5f;default->1.0f;};return e.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*m);}
    @Override
    public boolean addEffect(MobEffectInstance i,@Nullable Entity e){return getLevel()<3||i.getEffect().isBeneficial()?super.addEffect(i,e):false;}
    public int getLevel(){return entityData.get(DATA_LEVEL);}
    private void teleportNearTarget(LivingEntity t){Vec3 p=t.position();for(int i=0;i<10;++i){double x=p.x()+(getRandom().nextDouble()-0.5D)*8;double y=p.y()+(getRandom().nextInt(8)-4);double z=p.z()+(getRandom().nextDouble()-0.5D)*8;if(randomTeleport(x,y,z,false)){getNavigation().stop();level().playSound(null,xo,yo,zo,SoundEvents.ENDERMAN_TELEPORT,getSoundSource(),1.0F,1.0F);if(level()instanceof ServerLevel sl)sl.sendParticles(ParticleTypes.PORTAL,getX(),getY(0.5),getZ(),30,0.5,1.0,0.5,0.1);getLookControl().setLookAt(t);return;}}}
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) { return null; }
}