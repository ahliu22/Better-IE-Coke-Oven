package net.ahliu22.better_ie_coke_oven.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Parses the optional "fluid" entry of a coke oven datapack recipe:
 * <pre>
 * "fluid": { "amount": 500, "tag": "minecraft:water" }
 * "fluid": { "amount": 500, "fluid": "minecraft:water" }
 * </pre>
 * Returns {@code null} when the entry is missing or invalid, in which case the
 * recipe falls back to the default creosote output. The referenced fluid/tag is
 * only resolved on first use (see {@link CokeOvenFluidOutput#resolve()}), since
 * tags may not be loaded yet while recipes are being parsed.
 */
public final class JsonFluidParser
{
	private static final Logger LOGGER = LoggerFactory.getLogger("better_ie_coke_oven");

	private JsonFluidParser()
	{
	}

	@Nullable
	public static CokeOvenFluidOutput parseFluidOutput(@Nullable JsonObject recipeJson)
	{
		if(recipeJson == null)
			return null;
		JsonElement fluidElement = recipeJson.get("fluid");
		if(fluidElement == null || fluidElement.isJsonNull())
			return null;
		if(!fluidElement.isJsonObject())
		{
			LOGGER.warn("[Better IECokeOven] 'fluid' must be a JSON object, falling back to creosote");
			return null;
		}
		JsonObject fluidJson = fluidElement.getAsJsonObject();

		int amount = GsonHelper.getAsInt(fluidJson, "amount", 1000);
		if(amount <= 0)
		{
			LOGGER.warn("[Better IECokeOven] Invalid fluid 'amount' {}, falling back to creosote", amount);
			return null;
		}

		String tagName = GsonHelper.getAsString(fluidJson, "tag", null);
		if(tagName != null)
			return reference(tagName, true, amount);

		String fluidName = GsonHelper.getAsString(fluidJson, "fluid", null);
		if(fluidName != null)
			return reference(fluidName, false, amount);

		LOGGER.warn("[Better IECokeOven] 'fluid' entry needs a 'tag' or 'fluid' field, falling back to creosote");
		return null;
	}

	@Nullable
	private static CokeOvenFluidOutput reference(String name, boolean isTag, int amount)
	{
		try
		{
			return new CokeOvenFluidOutput(new ResourceLocation(name), isTag, amount);
		}
		catch(Exception e)
		{
			LOGGER.warn("[Better IECokeOven] Invalid {} name '{}', falling back to creosote", isTag ? "tag" : "fluid", name);
			return null;
		}
	}
}
