package net.ahliu22.better_ie_coke_oven.api;

import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Duck interface implemented on {@link blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe}
 * by {@link net.ahliu22.better_ie_coke_oven.mixin.MixinCokeOvenRecipe}.
 * <p>
 * An empty optional (or null {@link #getFluidOutput()}) means the recipe uses the default creosote output.
 */
public interface ICokeOvenRecipe
{
	/**
	 * The raw reference for the custom fluid output, only present if the datapack recipe contains a "fluid" entry.
	 */
	Optional<CokeOvenFluidOutput> getFluidOutputReference();

	void setFluidOutputReference(@Nullable CokeOvenFluidOutput fluid);

	/**
	 * The resolved custom fluid output, or {@code null} if the recipe uses the default creosote output
	 * (or the referenced fluid/tag could not be resolved).
	 */
	@Nullable
	FluidStack getFluidOutput();
}
