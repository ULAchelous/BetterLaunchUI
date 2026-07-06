package mixin;

import io.ula.blui.client.screens.CustomLoadingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Redirect(method = "<init>",at = @At(value = "INVOKE", target = "net/minecraft/client/Minecraft.setOverlay (Lnet/minecraft/client/gui/screens/Overlay;)V"))
    private void RedirectSetOverlay(Minecraft minecraft,Overlay overlay){
        CustomLoadingOverlay overlay1 = (CustomLoadingOverlay) overlay;
        minecraft.setOverlay(overlay1);
    }
}
