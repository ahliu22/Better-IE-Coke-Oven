package net.ahliu22.better_ie_coke_oven.mixin;

import blusunrize.immersiveengineering.api.crafting.CokeOvenRecipe;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.common.crafting.serializers.CokeOvenRecipeSerializer;
import malte0811.dualcodecs.DualCodec;
import malte0811.dualcodecs.DualCodecs;
import malte0811.dualcodecs.DualCompositeMapCodecs;
import malte0811.dualcodecs.DualMapCodec;
import net.ahliu22.better_ie_coke_oven.api.CokeOvenFluidOutput;
import net.ahliu22.better_ie_coke_oven.api.ICokeOvenRecipe;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends the vanilla coke oven recipe serializer with the optional "fluid" entry.
 * <p>
 * The dual codec (JSON datapack loading + network sync to clients) is replaced by a composite that
 * includes the extra optional "fluid" field. Recipes without a "fluid" entry keep the default
 * creosote output. The field definitions of the vanilla serializer are mirrored here, so a change
 * of those fields in a future IE version will fail loudly at mixin application time.
 */
@Mixin(CokeOvenRecipeSerializer.class)
public class MixinCokeOvenRecipeSerializer
{
	private static final Logger LOGGER = LoggerFactory.getLogger("better_ie_coke_oven");

	private static final DualCodec<ByteBuf, CokeOvenFluidOutput> BICO_FLUID_CODEC = new DualCodec<>(
			CokeOvenFluidOutput.JSON_CODEC, CokeOvenFluidOutput.STREAM_CODEC
	);

	@Inject(method = "codecs", at = @At("RETURN"), cancellable = true, remap = false)
	private void bicoAddFluidOutputField(CallbackInfoReturnable<DualMapCodec<RegistryFriendlyByteBuf, CokeOvenRecipe>> cir)
	{
		cir.setReturnValue(DualCompositeMapCodecs.composite(
				TagOutput.CODECS.fieldOf("result"), r -> r.output,
				IngredientWithSize.CODECS.fieldOf("input"), r -> r.input,
				DualCodecs.INT.optionalFieldOf("time", 200), r -> r.time,
				DualCodecs.INT.fieldOf("creosote"), r -> r.creosoteOutput,
				BICO_FLUID_CODEC.optionalFieldOf("fluid"), r -> ((ICokeOvenRecipe)(Object)r).getFluidOutputReference(),
				(output, input, time, creosote, fluid) -> {
					CokeOvenRecipe recipe = new CokeOvenRecipe(output, input, time, creosote);
					if(fluid.isPresent())
					{
						CokeOvenFluidOutput ref = fluid.get();
						((ICokeOvenRecipe)(Object)recipe).setFluidOutputReference(ref);
						LOGGER.info("[Better IECokeOven] Coke oven recipe has custom fluid output: {} mb of {} (overrides creosote)",
								ref.getAmount(), ref.getId());
					}
					return recipe;
				}
		));
	}
}
