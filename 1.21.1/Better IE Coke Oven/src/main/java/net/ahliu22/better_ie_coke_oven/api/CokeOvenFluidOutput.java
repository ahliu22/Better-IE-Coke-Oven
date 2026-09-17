package net.ahliu22.better_ie_coke_oven.api;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Reference to a custom fluid output of a coke oven recipe. The actual fluid is resolved lazily
 * (and cached) on first use, because during a data reload the recipes are parsed in parallel with
 * the tag loading, so tags may not be bound yet when the recipe JSON is read.
 * <p>
 * Supported datapack format (either form):
 * <pre>
 * "fluid": { "amount": 500, "tag": "minecraft:water" }
 * "fluid": { "amount": 500, "fluid": "immersiveengineering:creosote" }
 * </pre>
 */
public final class CokeOvenFluidOutput
{
	private static final Logger LOGGER = LoggerFactory.getLogger("better_ie_coke_oven");

	public static final int DEFAULT_AMOUNT = 1000;

	private static final Codec<CokeOvenFluidOutput> TAG_FORM = RecordCodecBuilder.<CokeOvenFluidOutput>create(
			inst -> inst.group(
					ResourceLocation.CODEC.fieldOf("tag").forGetter(o -> o.id),
					Codec.INT.optionalFieldOf("amount", DEFAULT_AMOUNT).forGetter(CokeOvenFluidOutput::getAmount)
			).apply(inst, (id, amount) -> new CokeOvenFluidOutput(id, true, amount))
	);

	private static final Codec<CokeOvenFluidOutput> FLUID_FORM = RecordCodecBuilder.<CokeOvenFluidOutput>create(
			inst -> inst.group(
					ResourceLocation.CODEC.fieldOf("fluid").forGetter(o -> o.id),
					Codec.INT.optionalFieldOf("amount", DEFAULT_AMOUNT).forGetter(CokeOvenFluidOutput::getAmount)
			).apply(inst, (id, amount) -> new CokeOvenFluidOutput(id, false, amount))
	);

	public static final Codec<CokeOvenFluidOutput> JSON_CODEC = Codec.either(TAG_FORM, FLUID_FORM)
			.xmap(
					e -> e.map(l -> l, r -> r),
					o -> o.isTag ? Either.left(o) : Either.right(o)
			)
			.flatXmap(CokeOvenFluidOutput::validate, CokeOvenFluidOutput::validate);

	public static final StreamCodec<ByteBuf, CokeOvenFluidOutput> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, CokeOvenFluidOutput::getId,
			ByteBufCodecs.BOOL, CokeOvenFluidOutput::isTag,
			ByteBufCodecs.VAR_INT, CokeOvenFluidOutput::getAmount,
			CokeOvenFluidOutput::new
	);

	private static DataResult<CokeOvenFluidOutput> validate(CokeOvenFluidOutput o)
	{
		return o.amount > 0
				? DataResult.success(o)
				: DataResult.error(() -> "Coke oven fluid output 'amount' must be positive");
	}

	private final ResourceLocation id;
	private final boolean isTag;
	private final int amount;
	@Nullable
	private FluidStack resolved;
	private boolean warned;

	public CokeOvenFluidOutput(ResourceLocation id, boolean isTag, int amount)
	{
		this.id = id;
		this.isTag = isTag;
		this.amount = amount;
	}

	public boolean isTag()
	{
		return isTag;
	}

	public ResourceLocation getId()
	{
		return id;
	}

	public int getAmount()
	{
		return amount;
	}

	/**
	 * Resolves the referenced fluid to a {@link FluidStack}. Returns {@code null} (without caching the
	 * failure) while the fluid/tag cannot be resolved yet, falling back to the default creosote output.
	 */
	@Nullable
	public FluidStack resolve()
	{
		if(resolved != null)
			return resolved;
		@Nullable Fluid fluid;
		if(isTag)
		{
			Optional<? extends HolderSet.Named<Fluid>> tag = BuiltInRegistries.FLUID.asLookup().get(TagKey.create(Registries.FLUID, id));
			if(tag.isEmpty() || tag.get().size() == 0)
			{
				warnOnce("Fluid tag '{}' is empty or missing, falling back to creosote", id);
				return null;
			}
			fluid = tag.get().stream().findFirst().map(Holder::value).orElse(null);
		}
		else
		{
			fluid = BuiltInRegistries.FLUID.getHolder(id).map(Holder::value).orElse(null);
			if(fluid == null || fluid.isSame(Fluids.EMPTY))
			{
				warnOnce("Fluid '{}' does not exist, falling back to creosote", id);
				return null;
			}
		}
		if(fluid == null)
			return null;
		resolved = new FluidStack(fluid, amount);
		return resolved;
	}

	private void warnOnce(String format, ResourceLocation id)
	{
		if(!warned)
		{
			warned = true;
			LOGGER.warn("[Better IECokeOven] "+format, id);
		}
	}
}
