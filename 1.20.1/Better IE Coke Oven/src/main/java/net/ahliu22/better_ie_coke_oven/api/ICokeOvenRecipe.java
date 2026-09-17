package net.ahliu22.better_ie_coke_oven.api;

import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * Duck interface implemented on {@link blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe}
 * by {@link net.ahliu22.better_ie_coke_oven.mixin.MixinCokeOvenRecipe}.
 * <p>
 * A null (or empty) custom fluid output means the recipe uses the default creosote output.
 */
public interface ICokeOvenRecipe
{
	/**
	 * The resolved custom fluid output, or {@code null} if the recipe uses creosote.
	 */
	@Nullable
	FluidStack getFluidOutput();

	@Nullable
	CokeOvenFluidOutput getFluidOutputReference();

	void setFluidOutputReference(@Nullable CokeOvenFluidOutput fluid);
}
