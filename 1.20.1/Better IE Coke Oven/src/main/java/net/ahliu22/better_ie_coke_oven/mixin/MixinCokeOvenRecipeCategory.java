package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.common.util.compat.jei.JEIHelper;
import blusunrize.immersiveengineering.common.util.compat.jei.cokeoven.CokeOvenRecipeCategory;
import mezz.jei.api.forge.ForgeTypes;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows the custom fluid output (instead of creosote) in the JEI coke oven category.
 */
@Mixin(CokeOvenRecipeCategory.class)
public class MixinCokeOvenRecipeCategory
{
	@Shadow(remap = false)
	@Final
	private IDrawableStatic tankOverlay;

	@Inject(
			method = "setRecipe",
			at = @At(
					value = "INVOKE",
					target = "Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;addSlot(Lmezz/jei/api/recipe/RecipeIngredientRole;II)Lmezz/jei/api/gui/builder/IRecipeSlotBuilder;",
					ordinal = 2,
					remap = false
			),
			cancellable = true,
			remap = false
	)
	private void bicoCustomFluidSlot(IRecipeLayoutBuilder builder, CokeOvenRecipe recipe, IFocusGroup focuses, CallbackInfo ci)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		if(custom == null || custom.isEmpty())
			return;
		int tankSize = Math.max(FluidType.BUCKET_VOLUME, custom.getAmount());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 103, 4)
				.setFluidRenderer(tankSize, false, 16, 47)
				.setOverlay(tankOverlay, 0, 0)
				.addIngredient(ForgeTypes.FLUID_STACK, custom)
				.addTooltipCallback(JEIHelper.fluidTooltipCallback);
		ci.cancel();
	}

	@Inject(method = "setRecipe", at = @At("RETURN"), remap = false)
	private void bicoCustomFluidSlotNoCreosote(IRecipeLayoutBuilder builder, CokeOvenRecipe recipe, IFocusGroup focuses, CallbackInfo ci)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		if(custom == null || custom.isEmpty() || recipe.creosoteOutput > 0)
			return;
		int tankSize = Math.max(FluidType.BUCKET_VOLUME, custom.getAmount());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 103, 4)
				.setFluidRenderer(tankSize, false, 16, 47)
				.setOverlay(tankOverlay, 0, 0)
				.addIngredient(ForgeTypes.FLUID_STACK, custom)
				.addTooltipCallback(JEIHelper.fluidTooltipCallback);
	}
}
