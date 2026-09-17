package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CokeOvenLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.CokeOvenLogic.State;
import blusunrize.immersiveengineering.common.register.IEFluids;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Makes the coke oven fill its tank with the recipe's custom fluid output instead
 * of creosote, when one is configured.
 * <p>
 * {@code tickServer}: right before the vanilla creosote fill, the custom fluid is
 * filled into the tank first. The tank is single-fluid, so the following vanilla
 * fill with creosote is then refused (returns 0). If the custom fluid happens to be
 * creosote itself, the custom fill is skipped and the vanilla code handles it.
 * <p>
 * {@code getRecipe}: additionally validates tank space/fluid type for custom-fluid
 * recipes, mirroring the vanilla creosote capacity check.
 */
@Mixin(CokeOvenLogic.class)
public class MixinCokeOvenLogic
{
	@Inject(
			method = "tickServer",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I",
					remap = false
			),
			locals = LocalCapture.CAPTURE_FAILHARD,
			remap = false
	)
	private void bicoCustomFluidOutput(
			IMultiblockContext<State> context, CallbackInfo ci,
			State state, BlockState masterBlockState, boolean activeBeforeTick, boolean active,
			CokeOvenRecipe recipe, ItemStack outputStack
	)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		if(custom != null && !custom.isEmpty() && !custom.getFluid().isSame(IEFluids.CREOSOTE.getStill()))
			state.getTank().fill(custom, FluidAction.EXECUTE);
	}

	@Inject(method = "getRecipe", at = @At("RETURN"), cancellable = true, remap = false)
	private void bicoCheckCustomFluidCapacity(IMultiblockContext<State> context, CallbackInfoReturnable<CokeOvenRecipe> cir)
	{
		CokeOvenRecipe recipe = cir.getReturnValue();
		if(recipe == null)
			return;
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		if(custom == null || custom.isEmpty())
			return;
		FluidTank tank = context.getState().getTank();
		if(tank.getFluidAmount() > 0)
		{
			if(!tank.getFluid().isFluidEqual(custom) || tank.getFluidAmount()+custom.getAmount() > tank.getCapacity())
				cir.setReturnValue(null);
		}
		else if(custom.getAmount() > tank.getCapacity())
			cir.setReturnValue(null);
	}
}
