package com.github.elenterius.biomancy.serum;

import com.github.elenterius.biomancy.BiomancyMod;
import com.github.elenterius.biomancy.item.SerumItem;
import com.github.elenterius.biomancy.item.injector.InjectorItem;
import com.github.elenterius.biomancy.styles.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class CompoundSerum extends BasicSerum {

	public CompoundSerum() {
		super(PotionUtils.getColor(Potions.EMPTY));
	}

	@Override
	public CompoundTag getDataTag(ItemStack stack) {
		if (stack.getItem() instanceof InjectorItem) return super.getDataTag(((InjectorItem) stack.getItem()).getSerumItemStack(stack));
		return super.getDataTag(stack);
	}

	public static List<MobEffectInstance> getEffects(CompoundTag tag) {
		ArrayList<MobEffectInstance> ret = new ArrayList<>();
		CompoundTag targetTag = tag;
		if (!tag.contains("effects")) targetTag = tag.getCompound("inventory").getCompound("Item").getCompound("tag").getCompound("serum_data");
		ListTag effectsTag = targetTag.getList("effects",ListTag.TAG_COMPOUND);
		for(int i = 0; i < effectsTag.size(); ++i) {
			CompoundTag effect = effectsTag.getCompound(i);
			MobEffectInstance instance = MobEffectInstance.load(effect);
			if (instance != null) {
				ret.add(instance);
			}
		}
		return ret;
	}

	public static ItemStack setEffects(ItemStack serum, List<MobEffectInstance> effects) {
		ItemStack ret = serum.copy();
		ListTag effectsTag = (((SerumItem)serum.getItem()).getSerum()).getDataTag(serum).getList("effects",ListTag.TAG_COMPOUND);
		for (int i=0;i<effects.size();i++) {
			CompoundTag tag = effects.get(i).save(new CompoundTag());
			if (i<effectsTag.size()) effectsTag.set(i,tag);
			else effectsTag.add(i,tag);
		}
		ret.getOrCreateTag().getCompound(CompoundSerum.DATA_TAG_KEY).put("effects",effectsTag);
		return ret;
	}

	public static List<MobEffectInstance> getEffects(ItemStack stack) {
		return getEffects(((SerumItem)stack.getItem()).getSerum().getDataTag(stack));
	}

	private static final Predicate<Holder<Item>> IS_INITIAL_POTION_INGREDIENT = stack -> {
		return BrewingRecipeRegistry.canBrew(NonNullList.of(ItemStack.EMPTY,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.AWKWARD)),new ItemStack(stack,1),new int[]{0});
	};
	private static final ItemStack AWKWARD_POTION = PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.AWKWARD);
	//IBrewingRecipe reference:
	// input=Bottom 3 slots
	// ingredient=Top slot

	public static int calculateCraftingTime(ItemStack stack) {
		int BASE_TIME = 20;
		List<MobEffectInstance> effects = getEffects(stack);
		AtomicInteger ret = new AtomicInteger(BASE_TIME+(effects.size()*10));
		effects.forEach(instance->{
			ret.addAndGet((instance.getDuration()/20)*(instance.getAmplifier()+1));
			ret.set((int) (ret.intValue()*Math.log(instance.getAmplifier()+1)));
		});
		return ret.get();
	}

	public static int calculateNutrientCost(ItemStack stack) {
		int BASE_COST = 5;
		List<MobEffectInstance> effects = getEffects(stack);
		AtomicInteger ret = new AtomicInteger(BASE_COST+(effects.size()*10));
		effects.forEach(instance->{
			ret.set((int) (ret.intValue()*Math.log(instance.getAmplifier()+1)));
		});
		return ret.get();
	}

	public static ItemStack addIngredient(ItemStack serum,ItemStack ingredient) {
		List<MobEffectInstance> currentEffects = getEffects(serum);
		if (ingredient.is(IS_INITIAL_POTION_INGREDIENT)) {
			currentEffects.addAll(PotionUtils.getPotion(BrewingRecipeRegistry.getOutput(AWKWARD_POTION,ingredient)).getEffects());
		} else {
			MobEffectInstance targetEffect = currentEffects.get(currentEffects.size()-1);
			if (ingredient.is(Items.REDSTONE)) targetEffect = new MobEffectInstance(targetEffect.getEffect(),targetEffect.getDuration()+60,targetEffect.getAmplifier());
			if (ingredient.is(Items.GLOWSTONE)) targetEffect = new MobEffectInstance(targetEffect.getEffect(),targetEffect.getDuration(),targetEffect.getAmplifier()+1);
			currentEffects.set(currentEffects.size()-1,targetEffect);
		}
		serum = setEffects(serum,currentEffects);
		return serum;
	}

	@Override
	public void affectEntity(ServerLevel level, CompoundTag tag, @Nullable LivingEntity source, LivingEntity target) {
		getEffects(tag).forEach(mobEffectInstance -> target.addEffect(mobEffectInstance,source));
	}

	@Override
	public void affectPlayerSelf(CompoundTag tag, ServerPlayer targetSelf) {
		BiomancyMod.LOGGER.debug(PotionUtils.getCustomEffects(tag.getCompound("effects")));
		getEffects(tag).forEach(mobEffectInstance -> targetSelf.addEffect(mobEffectInstance,targetSelf));
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendTooltip(stack, level, tooltip, flag);
		if (Screen.hasAltDown()) {
			tooltip.addAll(TextComponentUtil.getTooltipText("when_injected").withStyle(ChatFormatting.GRAY).toFlatList());
			PotionUtils.addPotionTooltip(getEffects(getDataTag(stack)),tooltip,1f);
		}
	}
}
