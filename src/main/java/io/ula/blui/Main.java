package io.ula.blui;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main implements ModInitializer {
    private Logger LOGGER = LogManager.getLogger("blui");
    @Override
    public void onInitialize() {
        LOGGER.info("Thanks for using BetterLaunchUI!");
    }
}
