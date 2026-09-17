package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.common.crafting.serializers.CokeOvenRecipeSerializer;
import com.google.gson.JsonObject;
import net.ahliu22.better_ie_coke_oven.api.CokeOvenFluidOutput;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import net.ahliu22.better_ie_coke_oven.api.JsonFluidParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends the vanilla coke oven recipe serializer with the optional "fluid" entry:
 * <ul>
 *   <li>{@code readFromJson}: reads the custom fluid output reference from the datapack JSON.</li>
 *   <li>{@code toNetwork}/{@code fromNetwork}: syncs the custom fluid reference to clients.</li>
 * </ul>
 * Recipes without a "fluid" entry keep the default creosote output.
 */
@Mixin(CokeOvenRecipeSerializer.class)
public class MixinCokeOvenRecipeSerializer
{
	private static final Logger LOGGER = LoggerFactory.getLogger("better_ie_coke_oven");

	@Inject(method = "readFromJson", at = @At("RETURN"), remap = false)
	private void bicoReadFluidFromJson(ResourceLocation recipeId, JsonObject json, IContext context, CallbackInfoReturnable<CokeOvenRecipe> cir)
	{
		CokeOvenRecipe recipe = cir.getReturnValue();
		if(recipe == null)
			return;
		CokeOvenFluidOutput fluid = JsonFluidParser.parseFluidOutput(json);
		if(fluid != null)
		{
			((ICokeOvenRecipe)(Object)recipe).setFluidOutputReference(fluid);
			LOGGER.info("[Better IECokeOven] Coke oven recipe '{}' has custom fluid output: {} mb of {} (overrides creosote)", recipeId, fluid.getAmount(), fluid.getId());
		}
	}

	@Inject(method = "fromNetwork", at = @At("RETURN"), remap = false)
	private void bicoReadFluidFromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer, CallbackInfoReturnable<CokeOvenRecipe> cir)
	{
		CokeOvenRecipe recipe = cir.getReturnValue();
		if(recipe == null)
			return;
		if(buffer.readBoolean())
		{
			boolean isTag = buffer.readBoolean();
			String id = buffer.readUtf();
			int amount = buffer.readInt();
			try
			{
				((ICokeOvenRecipe)(Object)recipe).setFluidOutputReference(new CokeOvenFluidOutput(new ResourceLocation(id), isTag, amount));
			}
			catch(Exception e)
			{
				LOGGER.warn("[Better IECokeOven] Received invalid custom fluid '{}' for recipe '{}', falling back to creosote", id, recipeId);
			}
		}
	}

	@Inject(method = "toNetwork", at = @At("RETURN"), remap = false)
	private void bicoWriteFluidToNetwork(FriendlyByteBuf buffer, CokeOvenRecipe recipe, CallbackInfo ci)
	{
		CokeOvenFluidOutput fluid = ((ICokeOvenRecipe)(Object)recipe).getFluidOutputReference();
		buffer.writeBoolean(fluid != null);
		if(fluid != null)
		{
			buffer.writeBoolean(fluid.isTag());
			buffer.writeUtf(fluid.getId().toString());
			buffer.writeInt(fluid.getAmount());
		}
	}
}
