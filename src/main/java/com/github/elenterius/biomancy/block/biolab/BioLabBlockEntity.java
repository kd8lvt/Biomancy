package com.github.elenterius.biomancy.block.biolab;

import com.github.elenterius.biomancy.BiomancyMod;
import com.github.elenterius.biomancy.block.base.MachineBlock;
import com.github.elenterius.biomancy.block.base.MachineBlockEntity;
import com.github.elenterius.biomancy.client.util.ClientLoopingSoundHelper;
import com.github.elenterius.biomancy.crafting.recipe.BioLabRecipe;
import com.github.elenterius.biomancy.crafting.recipe.IngredientStack;
import com.github.elenterius.biomancy.crafting.recipe.SimpleRecipeType;
import com.github.elenterius.biomancy.init.*;
import com.github.elenterius.biomancy.inventory.BehavioralInventory;
import com.github.elenterius.biomancy.inventory.SimpleInventory;
import com.github.elenterius.biomancy.inventory.itemhandler.HandlerBehaviors;
import com.github.elenterius.biomancy.menu.BioLabMenu;
import com.github.elenterius.biomancy.serum.CompoundSerum;
import com.github.elenterius.biomancy.styles.TextComponentUtil;
import com.github.elenterius.biomancy.util.ILoopingSoundHelper;
import com.github.elenterius.biomancy.util.SoundUtil;
import com.github.elenterius.biomancy.util.fuel.FluidFuelConsumerHandler;
import com.github.elenterius.biomancy.util.fuel.FuelHandler;
import com.github.elenterius.biomancy.util.fuel.IFuelHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BioLabBlockEntity extends MachineBlockEntity<BioLabRecipe, Container, BioLabStateData> implements MenuProvider, GeoBlockEntity {

	public static final int FUEL_SLOTS = 1;
	public static final int INPUT_SLOTS = BioLabRecipe.MAX_INGREDIENTS + BioLabRecipe.MAX_REACTANT;
	public static final int OUTPUT_SLOTS = 1;
	private static final ArrayList<Item> bannedAlchemyInputs = new ArrayList<>(List.of(Items.GUNPOWDER,Items.DRAGON_BREATH,Items.TURTLE_HELMET));


	public static final int MAX_FUEL = 1_000;

	public static final RegistryObject<SimpleRecipeType.ItemStackRecipeType<BioLabRecipe>> RECIPE_TYPE = ModRecipes.BIO_BREWING_RECIPE_TYPE;

	protected static final RawAnimation WORKING_ANIM = RawAnimation.begin().thenLoop("bio_lab.working");
	protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("bio_lab.idle");

	private final BioLabStateData stateData;
	private final FuelHandler fuelHandler;
	private final BehavioralInventory<?> fuelInventory;
	private final SimpleInventory inputInventory;
	private final BehavioralInventory<?> outputInventory;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private ILoopingSoundHelper loopingSoundHelper = ILoopingSoundHelper.NULL;

	private LazyOptional<IItemHandler> optionalCombinedInventory;
	private LazyOptional<IFluidHandler> optionalFluidConsumer;

	public BioLabBlockEntity(BlockPos worldPosition, BlockState blockState) {
		super(ModBlockEntities.BIO_LAB.get(), worldPosition, blockState);

		inputInventory = SimpleInventory.createServerContents(INPUT_SLOTS, this::canPlayerOpenInv, this::setChanged);
		outputInventory = BehavioralInventory.createServerContents(OUTPUT_SLOTS, HandlerBehaviors::denyInput, this::canPlayerOpenInv, this::setChanged);
		fuelInventory = BehavioralInventory.createServerContents(FUEL_SLOTS, HandlerBehaviors::filterFuel, this::canPlayerOpenInv, this::setChanged);

		optionalCombinedInventory = createCombinedInventory();

		fuelHandler = FuelHandler.createNutrientFuelHandler(MAX_FUEL, this::setChanged);
		stateData = new BioLabStateData(fuelHandler,worldPosition);
		optionalFluidConsumer = LazyOptional.of(() -> new FluidFuelConsumerHandler(fuelHandler));
	}

	private LazyOptional<IItemHandler> createCombinedInventory() {
		return LazyOptional.of(() -> new CombinedInvWrapper(
				fuelInventory.getItemHandlerWithBehavior(),
				new RangedWrapper(inputInventory.getItemHandler(), inputInventory.getContainerSize() - 1, inputInventory.getContainerSize())
		));
	}

	@Override
	public void onLoad() {
		if (level != null && level.isClientSide) {
			loopingSoundHelper = new ClientLoopingSoundHelper();
		}
	}

	@Override
	public Component getDisplayName() {
		return getName();
	}

	@Override
	public Component getName() {
		return TextComponentUtil.getTranslationText("container", "bio_lab");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return BioLabMenu.createServerMenu(containerId, playerInventory, fuelInventory, inputInventory, outputInventory, stateData);
	}

	@Override
	protected BioLabStateData getStateData() {
		return stateData;
	}

	@Override
	protected Container getInputInventory() {
		return inputInventory;
	}

	@Override
	protected IFuelHandler getFuelHandler() {
		return fuelHandler;
	}

	@Override
	public ItemStack getStackInFuelSlot() {
		return fuelInventory.getItem(0);
	}

	@Override
	public void setStackInFuelSlot(ItemStack stack) {
		fuelInventory.setItem(0, stack);
	}

	@Override
	protected boolean doesRecipeResultFitIntoOutputInv(BioLabRecipe craftingGoal, ItemStack stackToCraft) {
		return outputInventory.getItem(0).isEmpty() || outputInventory.doesItemStackFit(0, stackToCraft);
	}


	/*
	 * Alright time to do some bootleg runtime recipe spoofing, because I have zero clue how to do it otherwise without flooding the registry with nonsense! Yay! >.>
	 * Ugh, after modifying like 4 other classes that seem completely unrelated at first glance, this seems to work.
	 * But please for the love of the Makyr's tell me how to do this in a less janktastic way so I can fix it...
	 * I hate this method. The whole thing's vibes are absolutely vile.
	 * I've spent literally four days getting JUST THIS METHOD to work. I don't like the feature as a whole anymore because of this stupid block of code.
	 * Don't get me wrong - I like the idea, I just don't like how much mental gymnastics I had to do to produce such a seemingly simple feature 🙃
	 * -Kd
	 */
	protected @Nullable BioLabRecipe detectAndGenerateAlchemicalSerumRecipe(Level level) {
		ArrayList<Item> REQUIRED_STATIC_REAGENTS = new ArrayList<>(List.of(ModItems.EXOTIC_DUST.get(),ModItems.HORMONE_SECRETION.get())); //Static for now because lazy
		HashMap<String,ItemStack> taggedInputs = new HashMap<>();
		ArrayList<ItemStack> inputs = new ArrayList<>();
		for (int i=0;i< inputInventory.getContainerSize();i++) {
			if (inputInventory.getItem(i).is(ModItems.ALCHEMICAL_SERUM.get()) && !taggedInputs.containsKey("SERUM")) {taggedInputs.put("SERUM", inputInventory.getItem(i));}
			if (!bannedAlchemyInputs.contains(inputInventory.getItem(i).getItem())) {
				inputs.add(inputInventory.getItem(i));
				continue;
			}
			return null;
		}

		//Do the inputs resemble an "alchemy" recipe?
		boolean DOES_RECIPE_HAVE_REQUIRED_STATIC_REAGENTS = REQUIRED_STATIC_REAGENTS.stream().allMatch(reagent->
			inputs.stream().anyMatch(stack->
				stack.is(reagent) //I'd do .containsAll() but unfortunately Item != ItemStack, and Count can prevent equality.
			)
		);
		if (!DOES_RECIPE_HAVE_REQUIRED_STATIC_REAGENTS) return null;

		ArrayList<ItemStack> filteredInputs = new ArrayList<>(inputs.stream().filter(stack-> BrewingRecipeRegistry.isValidInput(stack) || PotionBrewing.hasMix(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD),stack)).toList());
		if (filteredInputs.isEmpty()) return null;
		//If this was 1.20.3, the `or` operation above wouldn't have to be there, because potions are data-driven, and instead of Forge letting us register directly to Vanilla's EXISTING POTION REGISTRY they made their own.
		//Totally unrelated, but NeoForge is a fantastic change of pace btw.
		taggedInputs.put("INPUT",filteredInputs.get(0));
		ItemStack result = taggedInputs.get("SERUM").copy();
		result.setCount(1);
		result = CompoundSerum.addIngredient(result,taggedInputs.get("INPUT"));
		return new BioLabRecipe(
				BiomancyMod.createRL("temp_alchemy"),
				result,
				CompoundSerum.calculateCraftingTime(result),
				CompoundSerum.calculateNutrientCost(result),
				inputs.stream().map(stack->new IngredientStack(Ingredient.of(stack),1)).toList(), //Why can I not just throw ItemStacks at BioLabRecipe and call it a day. Why do I need to do this.
				Ingredient.of(ModItems.EXOTIC_DUST.get())
		);
	}

	@Nullable
	@Override
	protected BioLabRecipe resolveRecipeFromInput(Level level) {
		return RECIPE_TYPE.get().getRecipeFromContainer(level, inputInventory).orElse(detectAndGenerateAlchemicalSerumRecipe(level));
	}

	@Override
	protected boolean doesRecipeMatchInput(BioLabRecipe recipeToTest, Level level) {
		return recipeToTest.matches(inputInventory, level);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		stateData.serialize(tag);
		tag.put("Fuel", fuelHandler.serializeNBT());
		tag.put("FuelSlots", fuelInventory.serializeNBT());
		tag.put("InputSlots", inputInventory.serializeNBT());
		tag.put("OutputSlots", outputInventory.serializeNBT());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		stateData.deserialize(tag);
		fuelHandler.deserializeNBT(tag.getCompound("Fuel"));
		fuelInventory.deserializeNBT(tag.getCompound("FuelSlots"));
		inputInventory.deserializeNBT(tag.getCompound("InputSlots"));
		outputInventory.deserializeNBT(tag.getCompound("OutputSlots"));
	}

	@Override
	public void dropAllInvContents(Level level, BlockPos pos) {
		Containers.dropContents(level, pos, fuelInventory);
		Containers.dropContents(level, pos, inputInventory);
		Containers.dropContents(level, pos, outputInventory);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (remove) return super.getCapability(cap, side);

		if (cap == ModCapabilities.ITEM_HANDLER) {
			if (side == null || side == Direction.DOWN) return outputInventory.getOptionalItemHandler().cast();
			if (side == Direction.UP) return inputInventory.getOptionalItemHandler().cast();
			return optionalCombinedInventory.cast();
		}

		if (cap == ModCapabilities.FLUID_HANDLER) {
			return optionalFluidConsumer.cast();
		}

		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		fuelInventory.invalidate();
		inputInventory.invalidate();
		outputInventory.invalidate();
		optionalCombinedInventory.invalidate();
		optionalFluidConsumer.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		fuelInventory.revive();
		inputInventory.revive();
		outputInventory.revive();
		optionalCombinedInventory = createCombinedInventory();
		optionalFluidConsumer = LazyOptional.of(() -> new FluidFuelConsumerHandler(fuelHandler));
	}

	@Override
	protected boolean craftRecipe(BioLabRecipe recipeToCraft, Level level) {
		ItemStack result = recipeToCraft.getResultItem(level.registryAccess()).copy();
		if (result.isEmpty() || !doesRecipeResultFitIntoOutputInv(recipeToCraft, result)) {
			return false;
		}

		//get ingredients cost
		List<IngredientStack> ingredients = recipeToCraft.getIngredientQuantities();
		int[] ingredientCost = new int[ingredients.size()];
		for (int i = 0; i < ingredients.size(); i++) {
			ingredientCost[i] = ingredients.get(i).count();
		}

		//consume reactant
		final int lastIndex = inputInventory.getContainerSize() - 1;
		inputInventory.removeItem(lastIndex, 1);

		//consume ingredients
		for (int idx = 0; idx < lastIndex; idx++) {
			final ItemStack foundStack = inputInventory.getItem(idx); //do not modify this stack
			if (!foundStack.isEmpty()) {
				for (int i = 0; i < ingredients.size(); i++) {
					int remainingCost = ingredientCost[i];
					if (remainingCost > 0 && ingredients.get(i).testItem(foundStack)) {
						int amount = Math.min(remainingCost, foundStack.getCount());
						inputInventory.removeItem(idx, amount);
						ingredientCost[i] -= amount;
						break;
					}
				}
			}
		}

		//output result
		outputInventory.insertItemStack(0, result);

		SoundUtil.broadcastBlockSound((ServerLevel) level, getBlockPos(), ModSoundEvents.BIO_LAB_CRAFTING_COMPLETED);

		setChanged();
		return true;
	}

	private <T extends BioLabBlockEntity> PlayState handleAnimationState(AnimationState<T> event) {
		boolean isCrafting = Boolean.TRUE.equals(getBlockState().getValue(MachineBlock.CRAFTING));

		if (isCrafting) {
			event.getController().setAnimation(WORKING_ANIM);
			loopingSoundHelper.startLoop(this, ModSoundEvents.BIO_LAB_CRAFTING.get(), 0.65f);
		}
		else {
			event.getController().setAnimation(IDLE_ANIM);
			loopingSoundHelper.stopLoop();
		}

		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "main", 0, this::handleAnimationState));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public void setRemoved() {
		if (level != null && level.isClientSide) {
			loopingSoundHelper.clear();
		}
		super.setRemoved();
	}

}
