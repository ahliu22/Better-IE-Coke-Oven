package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CokeOvenLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CokeOvenLogic.State;
import blusunrize.immersiveengineering.common.register.IEFluids;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the coke oven use the recipe's custom fluid output instead of creosote, when one is
 * configured, while keeping the exact vanilla batch processing logic:
 * <ul>
 *   <li>All reads of {@code CokeOvenRecipe.creosoteOutput} are replaced with the custom fluid
 *       amount, so batch sizes, tank space limits and the capacity check behave as if the recipe
 *       simply produced that amount of fluid.</li>
 *   <li>The actual tank fill swaps the fluid type to the resolved custom fluid.</li>
 *   <li>{@code getRecipe} additionally validates that the tank's existing contents match the
 *       recipe's output fluid, otherwise a wrong fluid left in the tank would be silently refused
 *       by the single-fluid tank and the item processing would still consume input.</li>
 * </ul>
 */
@Mixin(CokeOvenLogic.class)
public class MixinCokeOvenLogic
{
	private static final String CREOSOTE_OUTPUT_FIELD =
			"Lblusunrize/immersiveengineering/api/crafting/CokeOvenRecipe;creosoteOutput:I";

	@Unique
	@Nullable
	private CokeOvenRecipe bicoRecipeBeingProcessed;

	private static int bicoCustomFluidAmount(CokeOvenRecipe recipe)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		return custom != null && !custom.isEmpty() ? custom.getAmount() : recipe.creosoteOutput;
	}

	@Redirect(method = "tickServer", at = @At(value = "FIELD", target = CREOSOTE_OUTPUT_FIELD, ordinal = 0), remap = false)
	private int bicoBatchSizeLimit(CokeOvenRecipe recipe)
	{
		// remember the recipe being processed for the fill redirect below
		this.bicoRecipeBeingProcessed = recipe;
		return bicoCustomFluidAmount(recipe);
	}

	@Redirect(method = "tickServer", at = @At(value = "FIELD", target = CREOSOTE_OUTPUT_FIELD, ordinal = 1), remap = false)
	private int bicoFillAmount(CokeOvenRecipe recipe)
	{
		return bicoCustomFluidAmount(recipe);
	}

	@Redirect(
			method = "tickServer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"
			),
			remap = false
	)
	private int bicoFillCustomFluid(FluidTank tank, FluidStack stack, FluidAction action)
	{
		CokeOvenRecipe recipe = this.bicoRecipeBeingProcessed;
		this.bicoRecipeBeingProcessed = null;
		FluidStack custom = recipe != null ? ((ICokeOvenRecipe)(Object)recipe).getFluidOutput() : null;
		if(custom != null && !custom.isEmpty() && !custom.getFluid().isSame(stack.getFluid()))
			return tank.fill(custom.copyWithAmount(stack.getAmount()), action);
		return tank.fill(stack, action);
	}

	@Redirect(method = "getRecipe", at = @At(value = "FIELD", target = CREOSOTE_OUTPUT_FIELD), remap = false)
	private int bicoCapacityCheckAmount(CokeOvenRecipe recipe)
	{
		return bicoCustomFluidAmount(recipe);
	}

	@Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true, remap = false)
	private void bicoCheckTankFluidType(IMultiblockContext<State> context, CallbackInfoReturnable<CokeOvenRecipe> cir)
	{
		CokeOvenRecipe recipe = cir.getReturnValue();
		if(recipe == null)
			return;
		FluidTank tank = context.getState().getTank();
		if(tank.getFluidAmount() <= 0)
			return;
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		Fluid expected = custom != null && !custom.isEmpty() ? custom.getFluid() : IEFluids.CREOSOTE.getStill();
		if(!tank.getFluid().is(expected))
			cir.setReturnValue(null);
	}
}
