package net.ahliu22.better_ie_coke_oven.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Reference to a custom fluid output of a coke oven recipe. The actual fluid is
 * resolved lazily (and cached) on first use, because during a data reload the
 * recipes are parsed in parallel with the tag loading, so tags may not be bound
 * yet when the recipe JSON is read.
 */
public class CokeOvenFluidOutput
{
	private static final Logger LOGGER = LoggerFactory.getLogger("better_ie_coke_oven");

	private final ResourceLocation id;
	private final boolean isTag;
	private final int amount;
	@Nullable
	private FluidStack resolved;

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
	 * Resolves the referenced fluid to a {@link FluidStack}. Returns {@code null}
	 * (without caching the failure) while the fluid/tag cannot be resolved yet.
	 */
	@Nullable
	public FluidStack resolve()
	{
		if(resolved != null)
			return resolved;
		Fluid fluid;
		if(isTag)
		{
			ITag<Fluid> tag = ForgeRegistries.FLUIDS.tags().getTag(TagKey.create(Registries.FLUID, id));
			if(tag == null || tag.isEmpty())
			{
				LOGGER.warn("[Better IECokeOven] Fluid tag '{}' is empty or missing, falling back to creosote", id);
				return null;
			}
			fluid = tag.iterator().next();
		}
		else
		{
			fluid = ForgeRegistries.FLUIDS.getValue(id);
			if(fluid == null || fluid.isSame(Fluids.EMPTY))
			{
				LOGGER.warn("[Better IECokeOven] Fluid '{}' does not exist, falling back to creosote", id);
				return null;
			}
		}
		resolved = new FluidStack(fluid, amount);
		return resolved;
	}
}
