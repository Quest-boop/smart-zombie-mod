package com.smartzombie.entity.ai;

import com.smartzombie.entity.SmartZombieEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Handles the physical retreat movement for Smart Zombie.
 * Works in tandem with melee/ranged goals - when zombie.isRetreating() is true,
 * this goal keeps the zombie moving away from its target.
 */
public class SmartZombieRetreatGoal extends Goal {

    private final SmartZombieEntity zombie;
    private LivingEntity retreatTarget;
    private int recalcTimer = 0;

    public SmartZombieRetreatGoal(SmartZombieEntity zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return zombie.isRetreating() && zombie.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return zombie.isRetreating() && retreatTarget != null && retreatTarget.isAlive();
    }

    @Override
    public void start() {
        retreatTarget = zombie.getTarget();
        recalcTimer = 0;
        moveAwayFromTarget();
    }

    @Override
    public void stop() {
        retreatTarget = null;
    }

    @Override
    public void tick() {
        if (retreatTarget == null) return;

        recalcTimer--;
        if (recalcTimer <= 0) {
            recalcTimer = 8; // Recalculate path every 8 ticks
            moveAwayFromTarget();
        }
    }

    private void moveAwayFromTarget() {
        if (retreatTarget == null) return;

        double dx = zombie.getX() - retreatTarget.getX();
        double dz = zombie.getZ() - retreatTarget.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);

        if (len < 0.001) {
            // Directly on top - flee in random direction
            double angle = zombie.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= len;
            dz /= len;
        }

        // Try to move 5-7 blocks away
        double retreatDist = 5.0 + zombie.getRandom().nextDouble() * 2.0;
        double retreatX = zombie.getX() + dx * retreatDist;
        double retreatZ = zombie.getZ() + dz * retreatDist;

        zombie.getNavigation().moveTo(retreatX, zombie.getY(), retreatZ, 1.3D);
    }
}
