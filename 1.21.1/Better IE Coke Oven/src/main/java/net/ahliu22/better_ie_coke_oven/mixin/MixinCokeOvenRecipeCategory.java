package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.common.util.compat.jei.JEIHelper;
import blusunrize.immersiveengineering.common.util.compat.jei.cokeoven.CokeOvenRecipeCategory;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Shows the custom fluid output (instead of creosote) in the JEI coke oven category, both for the
 * static recipe display and for the batch-quantity cycling display.
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
		bicoAddFluidSlot(builder, custom);
		ci.cancel();
	}

	@Inject(method = "setRecipe", at = @At("RETURN"), remap = false)
	private void bicoCustomFluidSlotWithoutCreosote(IRecipeLayoutBuilder builder, CokeOvenRecipe recipe, IFocusGroup focuses, CallbackInfo ci)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe).getFluidOutput();
		if(custom == null || custom.isEmpty() || recipe.creosoteOutput > 0)
			return;
		bicoAddFluidSlot(builder, custom);
	}

	@Unique
	private void bicoAddFluidSlot(IRecipeLayoutBuilder builder, FluidStack custom)
	{
		int tankSize = Math.max(FluidType.BUCKET_VOLUME, custom.getAmount());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 103, 4)
				.setFluidRenderer(tankSize, false, 16, 47)
				.setOverlay(tankOverlay, 0, 0)
				.addIngredient(NeoForgeTypes.FLUID_STACK, custom)
				.addRichTooltipCallback(JEIHelper.fluidTooltipCallback);
	}

	@Inject(method = "onDisplayedIngredientsUpdate", at = @At("HEAD"), cancellable = true, remap = false)
	private void bicoCustomBatchDisplay(RecipeHolder<CokeOvenRecipe> recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses, CallbackInfo ci)
	{
		FluidStack custom = ((ICokeOvenRecipe)(Object)recipe.value()).getFluidOutput();
		if(custom == null || custom.isEmpty())
			return;
		// timing shenanigans, this is the same formula that JEI uses internally for cycling
		long now = System.currentTimeMillis();
		long index = now/1000L%100000L;

		// calculate qty to display
		int batchSize = recipe.value().input.getCount();
		int qty = 1+(Math.toIntExact(index)%batchSize);

		// adjust input quantities
		IRecipeSlotDrawable inputSlot = recipeSlots.getFirst();
		inputSlot.createDisplayOverrides().addItemStacks(inputSlot.getItemStacks().map(stack -> stack.copyWithCount(qty)).toList());
		// adjust output quantities
		IRecipeSlotDrawable outputSlot = recipeSlots.get(1);
		outputSlot.createDisplayOverrides().addItemStacks(outputSlot.getItemStacks().map(stack -> stack.copyWithCount(stack.getCount()*qty)).toList());
		// adjust fluid display
		if(recipeSlots.size() > 2)
		{
			IRecipeSlotDrawable tank = recipeSlots.get(2);
			tank.createDisplayOverrides().addFluidStack(custom.getFluid(), (long)custom.getAmount()*qty);
		}
		ci.cancel();
	}
}
