package com.smartzombie.entity;

import com.smartzombie.entity.ai.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class SmartZombieEntity extends Monster {

    // Synced data: 0 = melee, 1 = ranged
    private static final EntityDataAccessor<Boolean> IS_RANGED =
            SynchedEntityData.defineId(SmartZombieEntity.class, EntityDataSerializers.BOOLEAN);

    private int bowCooldown = 0;
    private int retreatCooldown = 0;
    private boolean isRetreating = false;

    public SmartZombieEntity(EntityType<? extends SmartZombieEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_RANGED, false);
    }

    public boolean isRanged() {
        return this.entityData.get(IS_RANGED);
    }

    public void setRanged(boolean ranged) {
        this.entityData.set(IS_RANGED, ranged);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // Basic survival goals
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Smart melee: hit then back up
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this));

        // Smart ranged: back up while shooting
        this.goalSelector.addGoal(2, new SmartRangedAttackGoal(this));

        // Retreat movement goal
        this.goalSelector.addGoal(3, new SmartZombieRetreatGoal(this));

        // Movement fallback
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Target selectors
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Update ranged/melee mode based on held item
            ItemStack mainHand = this.getItemInHand(InteractionHand.MAIN_HAND);
            boolean hasbow = mainHand.getItem() instanceof BowItem;
            this.setRanged(hasbow);

            // Count down cooldowns
            if (bowCooldown > 0) bowCooldown--;
            if (retreatCooldown > 0) {
                retreatCooldown--;
                if (retreatCooldown == 0) isRetreating = false;
            }
        }
    }

    public boolean isBowCooldownReady() {
        return bowCooldown <= 0;
    }

    public void resetBowCooldown(int ticks) {
        this.bowCooldown = ticks;
    }

    public void startRetreating(int ticks) {
        this.isRetreating = true;
        this.retreatCooldown = ticks;
    }

    public boolean isRetreating() {
        return this.isRetreating;
    }

    public void stopRetreating() {
        this.isRetreating = false;
        this.retreatCooldown = 0;
    }

    /**
     * Fire an arrow at the target.
     */
    public void fireArrowAt(LivingEntity target, float power) {
        ItemStack arrowItem = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowItem, power);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.2D, dz, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Smart zombie spawns with a sword or bow
        super.populateDefaultEquipmentSlots(random, difficulty);

        // Give it a weapon - 40% bow, 60% sword
        if (random.nextFloat() < 0.4f) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            // Give arrows to inventory
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 16));
        } else {
            // Pick a sword tier based on difficulty
            float diffFactor = difficulty.getSpecialMultiplier();
            Item sword;
            if (diffFactor > 0.75f) {
                sword = Items.DIAMOND_SWORD;
            } else if (diffFactor > 0.5f) {
                sword = Items.IRON_SWORD;
            } else {
                sword = Items.STONE_SWORD;
            }
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(sword));
        }
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        Item item = stack.getItem();
        // Smart zombie can pick up swords and bows
        if (item instanceof SwordItem || item instanceof BowItem) {
            // Only pick up if better than current
            ItemStack current = this.getItemInHand(InteractionHand.MAIN_HAND);
            if (current.isEmpty()) return true;
            if (item instanceof SwordItem && current.getItem() instanceof SwordItem) {
                // Compare damage
                return getSwordDamage(item) > getSwordDamage(current.getItem());
            }
            return current.isEmpty();
        }
        return super.canHoldItem(stack);
    }

    private float getSwordDamage(Item item) {
        if (item instanceof SwordItem sword) {
            return (float) sword.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE).stream()
                    .mapToDouble(mod -> mod.getAmount())
                    .sum();
        }
        return 0f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        this.populateDefaultEquipmentEnchantments(this.random, difficulty);
        return data;
    }

    @Override
    public boolean isSunSensitive() {
        // Half-human, so only partially sensitive - burns slower but still burns
        return false; // Smart enough to find shade / retain some human resistance
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.74F;
    }
}
