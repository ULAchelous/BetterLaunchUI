package io.ula.blui.client.mixin;

import io.ula.blui.client.screens.CustomLoadingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    @Final
    private FontManager fontManager;

    @Redirect(method = "<init>",at = @At(value = "NEW", target = "net/minecraft/client/gui/screens/LoadingOverlay"))
    private LoadingOverlay redirect(Minecraft minecraft, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> consumer, boolean bl){
        return new CustomLoadingOverlay(minecraft,fontManager,reloadInstance,consumer,bl);
    }
}
