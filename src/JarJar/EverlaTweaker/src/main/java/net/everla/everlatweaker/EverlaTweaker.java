package net.everla.everlatweaker;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("everlatweaker")
public class EverlaTweaker {
	public static final Logger LOGGER = LogManager.getLogger(EverlaTweaker.class);
	public static final String MODID = "everlatweaker";

	public EverlaTweaker() {
		LOGGER.info("EverlaTweaker loaded");
	}
}
