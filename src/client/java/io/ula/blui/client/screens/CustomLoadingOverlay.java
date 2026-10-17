package io.ula.blui.client.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class CustomLoadingOverlay extends LoadingOverlay{

    public static final Identifier MOJANG_STUDIOS_LOGO_LOCATION = Identifier.withDefaultNamespace("textures/gui/title/mojangstudios.png");
    public static final Identifier MINECRAFT_LOGO_LOCATION = Identifier.withDefaultNamespace("textures/gui/title/minecraft.png");
    private static final int LOGO_BACKGROUND_COLOR = ARGB.color(255, 105, 105, 105);
    private static final int LOGO_BACKGROUND_COLOR_DARK = ARGB.color(255, 0, 0, 0);
    private static final IntSupplier BRAND_BACKGROUND = () -> (Boolean)Minecraft.getInstance().options.darkMojangStudiosBackground().get() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;
    private FontManager fontManager;
    public CustomLoadingOverlay(Minecraft minecraft,FontManager fontManager, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> consumer, boolean bl) {
        super(minecraft,reloadInstance,consumer,bl);
        this.minecraft = minecraft;
        this.reload = reloadInstance;
        this.onFinish = consumer;
        this.fadeIn = bl;
        this.fontManager = fontManager;
        registerTextures(minecraft.getTextureManager());
    }


    public static void registerTextures(TextureManager textureManager) {
        textureManager.registerAndLoad(MOJANG_STUDIOS_LOGO_LOCATION, new CustomLoadingOverlay.LogoTexture(MOJANG_STUDIOS_LOGO_LOCATION));

        textureManager.registerAndLoad(MINECRAFT_LOGO_LOCATION,new LogoTexture(MINECRAFT_LOGO_LOCATION));
    }

    private static int replaceAlpha(final int color, final int alpha) {
        return color & 16777215 | alpha << 24;
    }

    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        boolean isFontLoaded = fontManager.fontSets.containsKey(Minecraft.DEFAULT_FONT);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }

        float fadeOutAnim = this.fadeOutStart > -1L ? (float)(now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float)(now - this.fadeInStart) / 500.0F : -1.0F;
        float logoAlpha;
        if (fadeOutAnim >= 1.0F) {
            if (this.minecraft.gui.screen() != null) {
                this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, 0, 0, a);
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }

            int alpha = Mth.ceil((1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F)) * 255.0F);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), alpha));
            logoAlpha = 1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            if (this.minecraft.gui.screen() != null && fadeInAnim < 1.0F) {
                this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }

            int alpha = Mth.ceil(Mth.clamp((double)fadeInAnim, 0.15, (double)1.0F) * (double)255.0F);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), alpha));
            logoAlpha = Mth.clamp(fadeInAnim, 0.0F, 1.0F);
        } else {
            ARGB.setVector4fFromARGB32(this.minecraft.gameRenderer.gameRenderState().guiRenderState.clearColorOverride, BRAND_BACKGROUND.getAsInt());
            logoAlpha = 1.0F;
        }
        int progressBar_MaxY = graphics.guiHeight();//进度条位置（高度）
        float reloadProgress = this.reload.getActualProgress();

        int logo_width = (int)Math.ceil(width * 0.2);
        int logo_height = (int)Math.ceil(logo_width*0.25);
        int logo_MinX = (int)(width * 0.99) - logo_width;
        int logo_MinY = (int)(progressBar_MaxY * 0.99) - logo_height;
        int s = ARGB.white(logoAlpha);
        int textColor = this.fadeOutStart > -1L ?ARGB.color(Math.round(1.0F - Mth.clamp(fadeOutAnim, 0.0F, 1.0F) * 255.0F),255,255,255) : ARGB.color(255,255,255);
        graphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX, logo_MinY, 0.0F, 0.0F, logo_width/2, logo_height, 120, 60, 120, 120, s);
        graphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX + logo_width/2 , logo_MinY, 0.0F, 60.0F, logo_width/2, logo_height, 120, 60, 120, 120, s);
        if(isFontLoaded) {
            Component present = Component.translatableWithFallback("betterlaunchui.loading.present", "resented By");
            graphics.text(minecraft.font, present, logo_MinX - 70, logo_MinY, s);
        }

        int mcLogo_width = (int)(width*0.6);
        int mcLogo_height = (int)(mcLogo_width*0.25);
        int mcLogo_MinX = (int)(width * 0.02);
        int mcLogo_MinY = (int)(height * 0.5 - mcLogo_height * 0.5);
        graphics.blit(RenderPipelines.MOJANG_LOGO,MINECRAFT_LOGO_LOCATION,mcLogo_MinX,mcLogo_MinY,0,0,mcLogo_width,mcLogo_height,1024,512,1024,512,s);
        if(isFontLoaded) {
            graphics.text(
                    minecraft.font,
                    Component.translatableWithFallback("betterlaunchui.loading.greeting", "Welcome, %s", minecraft.getGameProfile().name()),
                    mcLogo_MinX + mcLogo_width + 20,
                    mcLogo_MinY + mcLogo_height - 30,
                    s
            );

            graphics.text(
                    minecraft.font,
                    String.format("Minecraft %s", SharedConstants.getCurrentVersion().name()),
                    0,
                    0,
                    s
            );

            if (!Minecraft.getInstance().options.darkMojangStudiosBackground().get()) {
                graphics.text(
                        minecraft.font,
                        Component.translatableWithFallback("betterlaunchui.loading.advice", "Enable \"Monochrome Logo\" to get better experience!"),
                        0,
                        10,
                        s
                );
            }
        }


        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + reloadProgress * 0.050000012F, 0.0F, 1.0F);
        if (fadeOutAnim < 2.0F) {
            if(isFontLoaded) {
                graphics.centeredText(
                        this.minecraft.font,
                        Component.translatableWithFallback("betterlaunchui.loading.progress", "Loading... %s%%", Integer.toString ((int)(this.currentProgress * 100))),
                        (int) (width * 0.1),
                        progressBar_MaxY - 20,
                        s
                );
            }
            int progressBarAlpha = Mth.ceil((1.0F - Mth.clamp(fadeOutAnim - 1.0F, 0.0F, 1.0F)) * 255.0F);
            this.extractProgressBar(graphics, 0, progressBar_MaxY-3, width, progressBar_MaxY,progressBarAlpha);


        }

        if (fadeOutAnim >= 2.0F) {
            this.minecraft.gui.setOverlay(null);
        }

    }


    public void tick() {
        if (this.fadeOutStart == -1L && this.reload.isDone() && this.isReadyToFadeOut()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable t) {
                this.onFinish.accept(Optional.of(t));
            }

            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.gui.screen() != null) {
                Window window = this.minecraft.getWindow();
                this.minecraft.gui.screen().init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }

    }

    private boolean isReadyToFadeOut() {
        return (!this.fadeIn && Util.getMillis() - this.fadeInStart >= 5000L) || this.fadeInStart > -1L && Util.getMillis() - this.fadeInStart >= 5000L;
    }

    private void extractProgressBar(GuiGraphicsExtractor guiGraphics, int frontX, int topY, int endX, int bottomY,int alpha) {
        int progress = Mth.ceil((endX - frontX - 2) * this.currentProgress);
        int color = ARGB.color(255, 255, 255, 255);
        guiGraphics.fill(frontX, topY, frontX + progress, bottomY, replaceAlpha(color,alpha));
    }

    static class LogoTexture extends ReloadableTexture {
        private Identifier identifier;
        public LogoTexture(Identifier id) {
            super(id);
            identifier = id;
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            ResourceProvider resourceProvider = Minecraft.getInstance().getVanillaPackResources().asProvider();
            InputStream inputStream = resourceProvider.open(identifier);

            TextureContents var4;
            try {
                var4 = new TextureContents(NativeImage.read(inputStream), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
            } catch (Throwable var7) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var4;
        }
    }
}
