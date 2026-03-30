package com.smartzombie.entity.ai;

import com.smartzombie.entity.SmartZombieEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

import java.util.EnumSet;

/**
 * Ranged attack goal for Smart Zombie.
 * The zombie keeps its distance, backs up if the target closes in,
 * and fires arrows at intervals.
 *
 * Only active when holding a bow.
 */
public class SmartRangedAttackGoal extends Goal {

    private final SmartZombieEntity zombie;
    private LivingEntity target;

    // Distance control
    private static final double IDEAL_RANGE = 12.0;       // preferred attack range
    private static final double MIN_RANGE = 7.0;           // too close - back up
    private static final double MAX_RANGE = 20.0;          // too far - move closer
    private static final double IDEAL_RANGE_SQ = IDEAL_RANGE * IDEAL_RANGE;
    private static final double MIN_RANGE_SQ = MIN_RANGE * MIN_RANGE;
    private static final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;

    // Shooting timing
    private int shootChargeTick = 0;
    private static final int CHARGE_TIME = 20; // ticks to charge arrow (like a real bow pull)
    private static final int SHOOT_COOLDOWN = 40; // ticks between shots
    private int cooldownTick = 0;

    public SmartRangedAttackGoal(SmartZombieEntity zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!zombie.isRanged()) return false;
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (!zombie.isRanged()) return false;
        LivingEntity target = zombie.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.target = zombie.getTarget();
        this.shootChargeTick = 0;
        this.cooldownTick = SHOOT_COOLDOWN / 2; // Small initial delay
        zombie.startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public void stop() {
        this.target = null;
        zombie.stopUsingItem();
        zombie.getNavigation().stop();
        zombie.stopRetreating();
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        double distSq = zombie.distanceToSqr(target);

        // Always look at target
        zombie.getLookControl().setLookAt(target, 30f, 30f);

        // ---- POSITION MANAGEMENT ----
        if (distSq < MIN_RANGE_SQ) {
            // Too close - back up!
            backAwayFrom(target);
            zombie.startRetreating(15);
        } else if (distSq > MAX_RANGE_SQ) {
            // Too far - move closer (stop backing up)
            zombie.stopRetreating();
            zombie.getNavigation().moveTo(target, 1.0D);
        } else {
            // Good range - strafe sideways a little to avoid projectiles
            zombie.stopRetreating();
            maybeSideStep();
        }

        // ---- SHOOTING LOGIC ----
        if (cooldownTick > 0) {
            cooldownTick--;
            return;
        }

        // Check line of sight
        if (!zombie.getSensing().hasLineOfSight(target)) {
            // Can't see target - circle to find LOS
            zombie.getNavigation().moveTo(target, 1.0D);
            return;
        }

        // Charge the bow
        shootChargeTick++;

        if (shootChargeTick >= CHARGE_TIME) {
            // Fire!
            float power = BowItem.getPowerForTime(shootChargeTick);
            zombie.fireArrowAt(target, power);
            shootChargeTick = 0;
            cooldownTick = SHOOT_COOLDOWN;
            // Reset bow animation
            zombie.stopUsingItem();
            zombie.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    private void backAwayFrom(LivingEntity target) {
        double dx = zombie.getX() - target.getX();
        double dz = zombie.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) { dx = 1; dz = 0; }
        else { dx /= len; dz /= len; }

        // Back away by ~5 blocks
        double retreatX = zombie.getX() + dx * 5.0;
        double retreatZ = zombie.getZ() + dz * 5.0;
        zombie.getNavigation().moveTo(retreatX, zombie.getY(), retreatZ, 1.2D);
    }

    private void maybeSideStep() {
        // Occasionally strafe sideways for ~20% of ticks to be harder to hit
        if (zombie.getRandom().nextInt(20) == 0) {
            double dx = zombie.getX() - target.getX();
            double dz = zombie.getZ() - target.getZ();
            // Perpendicular (rotate 90 degrees)
            double perpX = -dz;
            double perpZ = dx;
            double len = Math.sqrt(perpX * perpX + perpZ * perpZ);
            if (len > 0.001) {
                perpX /= len;
                perpZ /= len;
            }
            double side = zombie.getRandom().nextBoolean() ? 3.0 : -3.0;
            zombie.getNavigation().moveTo(
                zombie.getX() + perpX * side,
                zombie.getY(),
                zombie.getZ() + perpZ * side,
                0.9D
            );
        }
    }
}
