package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import net.ahliu22.better_ie_coke_oven.api.CokeOvenFluidOutput;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

/**
 * Adds an optional custom fluid output reference to the coke oven recipe, populated from the
 * optional "fluid" entry of the datapack recipe JSON (see {@link MixinCokeOvenRecipeSerializer}).
 */
@Mixin(CokeOvenRecipe.class)
public abstract class MixinCokeOvenRecipe implements ICokeOvenRecipe
{
	@Unique
	@Nullable
	private CokeOvenFluidOutput bicoFluidOutput;

	@Override
	public Optional<CokeOvenFluidOutput> getFluidOutputReference()
	{
		return Optional.ofNullable(bicoFluidOutput);
	}

	@Override
	public void setFluidOutputReference(@Nullable CokeOvenFluidOutput fluid)
	{
		this.bicoFluidOutput = fluid;
	}

	@Override
	@Nullable
	public FluidStack getFluidOutput()
	{
		CokeOvenFluidOutput ref = bicoFluidOutput;
		return ref != null ? ref.resolve() : null;
	}
}
