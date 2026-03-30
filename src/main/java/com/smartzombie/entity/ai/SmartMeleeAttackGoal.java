package com.smartzombie.entity.ai;

import com.smartzombie.entity.SmartZombieEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.EnumSet;

/**
 * Melee attack goal for Smart Zombie.
 * The zombie approaches to strike, attacks, then retreats.
 * Only active when holding a sword (not a bow).
 */
public class SmartMeleeAttackGoal extends Goal {

    private final SmartZombieEntity zombie;
    private LivingEntity target;
    private int attackCooldown = 0;
    private int hitAndRunPhase = 0; // 0=approach, 1=hit, 2=retreat
    private static final int ATTACK_COOLDOWN = 30; // ticks between attacks
    private static final int RETREAT_DURATION = 25; // ticks to retreat after hitting

    public SmartMeleeAttackGoal(SmartZombieEntity zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (zombie.isRanged()) return false; // Don't use melee when holding bow
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        ItemStack held = zombie.getItemInHand(InteractionHand.MAIN_HAND);
        return held.getItem() instanceof SwordItem;
    }

    @Override
    public boolean canContinueToUse() {
        if (zombie.isRanged()) return false;
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.target = zombie.getTarget();
        this.hitAndRunPhase = 0;
        this.attackCooldown = 0;
        zombie.setAggressive(true);
    }

    @Override
    public void stop() {
        this.target = null;
        zombie.setAggressive(false);
        zombie.stopRetreating();
        zombie.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        zombie.getLookControl().setLookAt(target, 30f, 30f);

        attackCooldown--;

        double distSq = zombie.distanceToSqr(target);
        double attackRangeSq = getAttackReachSq();
        double retreatRangeSq = 8.0 * 8.0; // back up to ~8 blocks

        switch (hitAndRunPhase) {
            case 0 -> { // APPROACH
                zombie.stopRetreating();
                zombie.getNavigation().moveTo(target, 1.15D);
                if (distSq <= attackRangeSq && attackCooldown <= 0) {
                    // In range - execute the hit
                    doAttack();
                    hitAndRunPhase = 1;
                }
            }
            case 1 -> { // JUST HIT - start retreating
                zombie.startRetreating(RETREAT_DURATION);
                hitAndRunPhase = 2;
                attackCooldown = ATTACK_COOLDOWN;
                retreatFrom(target);
            }
            case 2 -> { // RETREAT
                if (!zombie.isRetreating() || distSq > retreatRangeSq) {
                    hitAndRunPhase = 0; // Far enough - go back to approaching
                    zombie.stopRetreating();
                }
            }
        }
    }

    private void doAttack() {
        zombie.swing(InteractionHand.MAIN_HAND);
        zombie.doHurtTarget(target);
        zombie.playAttackSound();
    }

    private void retreatFrom(LivingEntity target) {
        double dx = zombie.getX() - target.getX();
        double dz = zombie.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) {
            dx = 1;
            dz = 0;
        } else {
            dx /= len;
            dz /= len;
        }
        double retreatX = zombie.getX() + dx * 6.0;
        double retreatZ = zombie.getZ() + dz * 6.0;
        zombie.getNavigation().moveTo(retreatX, zombie.getY(), retreatZ, 1.3D);
    }

    private double getAttackReachSq() {
        return (zombie.getBbWidth() * 2.0f * zombie.getBbWidth() * 2.0f) + target.getBbWidth();
    }
}
