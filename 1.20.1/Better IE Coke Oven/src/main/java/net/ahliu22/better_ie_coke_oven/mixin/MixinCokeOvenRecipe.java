package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import net.ahliu22.better_ie_coke_oven.api.CokeOvenFluidOutput;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(CokeOvenRecipe.class)
public abstract class MixinCokeOvenRecipe implements ICokeOvenRecipe
{
	@Unique
	private CokeOvenFluidOutput bicoFluidOutput;

	@Override
	@Nullable
	public FluidStack getFluidOutput()
	{
		CokeOvenFluidOutput ref = bicoFluidOutput;
		return ref != null ? ref.resolve() : null;
	}

	@Override
	@Nullable
	public CokeOvenFluidOutput getFluidOutputReference()
	{
		return bicoFluidOutput;
	}

	@Override
	public void setFluidOutputReference(@Nullable CokeOvenFluidOutput fluid)
	{
		this.bicoFluidOutput = fluid;
	}
}
