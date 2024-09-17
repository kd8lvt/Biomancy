package com.github.elenterius.biomancy.block.biolab;

import com.github.elenterius.biomancy.BiomancyMod;
import com.github.elenterius.biomancy.crafting.recipe.BioLabRecipe;
import com.github.elenterius.biomancy.crafting.state.FuelConsumingRecipeCraftingStateData;
import com.github.elenterius.biomancy.util.fuel.IFuelHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class BioLabStateData extends FuelConsumingRecipeCraftingStateData<BioLabRecipe, Container> {
	private final BlockPos worldPosition;
	public BioLabStateData(IFuelHandler fuelHandler) {
		super(fuelHandler);
		this.worldPosition = null;
	}
	public BioLabStateData(IFuelHandler fuelHandler, BlockPos worldPosition) {
		super(fuelHandler);
		this.worldPosition = worldPosition;
	}

	@Override
	protected boolean isRecipeOfInstance(Recipe<?> recipe) {
		return recipe instanceof BioLabRecipe;
	}

	@Override
	public Optional<BioLabRecipe> getCraftingGoalRecipe(Level level) {
		Optional<BioLabRecipe> ret = super.getCraftingGoalRecipe(level);
		if (ret.isPresent()) return ret;
		if (recipeId == null) return Optional.empty();
		if (!recipeId.equals(BiomancyMod.createRL("temp_alchemy"))) return Optional.empty();
		if(this.worldPosition == null) return Optional.empty();
		return Optional.ofNullable(((BioLabBlockEntity)level.getBlockEntity(worldPosition)).detectAndGenerateAlchemicalSerumRecipe(level));
	}
}
