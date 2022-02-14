package com.github.elenterius.biomancy.world.entity.ai.goal.controllable;

import com.github.elenterius.biomancy.world.entity.ownable.IControllableMob;
import com.github.elenterius.biomancy.world.entity.ownable.IOwnableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Optional;

public class CopyOwnerRevengeTargetGoal<T extends Mob & IOwnableMob & IControllableMob> extends TargetGoal {

	private final T entity;
	private LivingEntity attacker;
	private int lastRevengeTime;

	public CopyOwnerRevengeTargetGoal(T goalOwner) {
		super(goalOwner, false);
		entity = goalOwner;
		setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (!entity.isGolemInactive() && entity.getGolemCommand() == IControllableMob.Command.DEFEND_OWNER) {
			Optional<Player> owner = entity.getOwner();
			if (owner.isPresent()) {
				attacker = owner.get().getLastHurtByMob();
				int revengeTimer = owner.get().getLastHurtByMobTimestamp();
				return revengeTimer != lastRevengeTime && canAttack(attacker, TargetingConditions.DEFAULT) && entity.shouldAttackEntity(attacker, owner.get());
			}
		}
		return false;
	}

	@Override
	public void start() {
		mob.setTarget(attacker);
		Optional<Player> optional = entity.getOwner();
		optional.ifPresent(player -> lastRevengeTime = player.getLastHurtByMobTimestamp());
		super.start();
	}
}
