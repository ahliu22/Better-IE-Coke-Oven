package net.ahliu22.better_ie_coke_oven;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BetterIECokeOven.MODID)
public class BetterIECokeOven
{
	// Define mod id in a common place for everything to reference
	public static final String MODID = "better_ie_coke_oven";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();

	public BetterIECokeOven()
	{
	}
}
