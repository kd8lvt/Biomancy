package com.github.elenterius.biomancy.item.armor;

import com.github.elenterius.biomancy.init.ModSoundEvents;
import com.github.elenterius.biomancy.item.livingtool.LivingTool;
import com.github.elenterius.biomancy.item.livingtool.LivingToolState;
import com.github.elenterius.biomancy.util.SoundUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;

public class MorphballItem extends ArmorItem implements LivingTool {
	public MorphballItem(Properties pProperties) {
		super(new MorphballArmorMaterial(), EquipmentSlot.CHEST, pProperties);
	}

	@Override
	public int getMaxNutrients(ItemStack container) {
		return 250;
	}

	@Override
	public void onArmorTick(ItemStack stack, Level level, Player player) {
		if (stack.getOrCreateTag().getInt("ConsumeTimer") <= 0) {
			stack.getOrCreateTag().putInt("ConsumeTimer",100);
			if (this.consumeNutrients(stack,1))
				SoundUtil.playItemSoundEffect(level,player, ModSoundEvents.CREATOR_EAT.get());
			else
				this.setLivingToolState(stack,LivingToolState.DORMANT);
		}

		if (this.getLivingToolState(stack) == LivingToolState.AWAKENED) {
			stack.getOrCreateTag().putInt("ConsumeTimer",stack.getOrCreateTag().getInt("ConsumeTimer")-1);
			player.setForcedPose(Pose.SWIMMING);
			player.setInvisible(true);
		}

		super.onArmorTick(stack, level, player);
	}

	@Override
	public int getLivingToolActionCost(ItemStack livingTool, LivingToolState state, ToolAction toolAction) {
		return 0;
	}
}
