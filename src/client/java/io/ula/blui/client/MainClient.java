package io.ula.blui.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MainClient implements ClientModInitializer {

    Logger LOGGER = LogManager.getLogger("betterlaunchui");
    @Override
    public void onInitializeClient() {
        LOGGER.info("Thanks you for using BLUI!");
    }
}
