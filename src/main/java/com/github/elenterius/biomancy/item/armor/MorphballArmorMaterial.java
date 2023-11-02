package com.github.elenterius.biomancy.item.armor;

import com.github.elenterius.biomancy.BiomancyMod;
import com.github.elenterius.biomancy.chat.ComponentUtil;
import com.github.elenterius.biomancy.init.ModItems;
import com.github.elenterius.biomancy.init.ModSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;


public class MorphballArmorMaterial implements ArmorMaterial {
	@Override
	public int getDurabilityForSlot(EquipmentSlot pSlot) {
		return 8192;
	}

	@Override
	public int getDefenseForSlot(EquipmentSlot pSlot) {
		return 0;
	}

	@Override
	public int getEnchantmentValue() {
		return -1;
	}

	@Override
	public SoundEvent getEquipSound() {
		return ModSoundEvents.UI_MENU_OPEN.get();
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(ModItems.HEALING_ADDITIVE.get());
	}

	@Override
	public String getName() {
		return "morphball";
	}

	@Override
	public float getToughness() {
		return 0;
	}

	@Override
	public float getKnockbackResistance() {
		return 0;
	}
}
