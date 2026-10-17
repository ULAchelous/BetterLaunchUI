package io.ula.blui.client.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class CustomLoadingOverlay extends LoadingOverlay {
    static final ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION = new ResourceLocation("textures/gui/title/mojangstudios.png");
    static final ResourceLocation MINECRAFT_LOGO_LOCATION = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final int LOGO_BACKGROUND_COLOR = FastColor.ARGB32.color(255, 105, 105, 105);
    private static final int LOGO_BACKGROUND_COLOR_DARK = FastColor.ARGB32.color(255, 0, 0, 0);
    private static final IntSupplier BRAND_BACKGROUND = () -> (Boolean) Minecraft.getInstance().options.darkMojangStudiosBackground().get() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
    private static final int LOGO_SCALE = 240;
    private static final float LOGO_QUARTER_FLOAT = 60.0F;
    private static final int LOGO_QUARTER = 60;
    private static final int LOGO_HALF = 120;
    private static final float LOGO_OVERLAP = 0.0625F;
    private static final float SMOOTHING = 0.95F;
    public static final long FADE_OUT_TIME = 1000L;
    public static final long FADE_IN_TIME = 500L;
    private final Minecraft minecraft;
    private final FontManager fontManager;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;
    private long onStart;

    public CustomLoadingOverlay(Minecraft minecraft, FontManager fontManager, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> consumer, boolean bl) {
        super(minecraft,reloadInstance,consumer,bl);
        this.minecraft = minecraft;
        this.fontManager = fontManager;
        this.reload = reloadInstance;
        this.onFinish = consumer;
        this.fadeIn = bl;
        onStart=Util.getMillis();
        registerTextures(minecraft);
    }

    public static void registerTextures(Minecraft minecraft) {
        minecraft.getTextureManager().register(MOJANG_STUDIOS_LOGO_LOCATION, new CustomLoadingOverlay.LogoTexture   (MOJANG_STUDIOS_LOGO_LOCATION));
        minecraft.getTextureManager().register(MINECRAFT_LOGO_LOCATION,new LogoTexture(MINECRAFT_LOGO_LOCATION));
    }

    private static int replaceAlpha(int i, int j) {
        return i & 16777215 | j << 24;
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        boolean isFontLoaded = fontManager.fontSets.containsKey(Minecraft.DEFAULT_FONT);
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        long m = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = m;
        }

        float g = this.fadeOutStart > -1L ? (float)(m - this.fadeOutStart) / 1000.0F : -1.0F;
        float h = this.fadeInStart > -1L ? (float)(m - this.fadeInStart) / 500.0F : -1.0F;
        float o;
        if (g >= 1.0F) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(guiGraphics, 0, 0, f);
            }

            int n = Mth.ceil((1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F)) * 255.0F);
            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = 1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && h < 1.0F) {
                this.minecraft.screen.render(guiGraphics, i, j, f);
            }

            int n = Mth.ceil(Mth.clamp((double)h, 0.15, (double)1.0F) * (double)255.0F);
            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = Mth.clamp(h, 0.0F, 1.0F);
        } else {
            int n = BRAND_BACKGROUND.getAsInt();
            float p = (float)(n >> 16 & 255) / 255.0F;
            float q = (float)(n >> 8 & 255) / 255.0F;
            float r = (float)(n & 255) / 255.0F;
            GlStateManager._clearColor(p, q, r, 1.0F);
            GlStateManager._clear(16384, Minecraft.ON_OSX);
            o = 1.0F;
        }


        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, o);
        int s = FastColor.ARGB32.color((int)(o*255.0F),255,255,255);
//        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, n - u, s - t, u, (int)d, -0.0625F, 0.0F, 120, 60, 120, 120);
//        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, n, s - t, u, (int)d, 0.0625F, 60.0F, 120, 60, 120, 120);

        int progressBar_MaxY = height;
        int logo_width = (int)Math.ceil(width * 0.2);
        int logo_height = (int)Math.ceil(logo_width*0.25);
        int logo_MinX = (int)(width * 0.99) - logo_width;
        int logo_MinY = (int)(progressBar_MaxY * 0.99) - logo_height;
        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX, logo_MinY,  logo_width/2, logo_height,0.0F, 0.0F, 120, 60, 120, 120);
        guiGraphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX + logo_width/2 , logo_MinY,logo_width/2, logo_height, 0.0F, 60.0F,  120, 60, 120, 120);

        int mcLogo_width = (int)(width*0.6);
        int mcLogo_height = (int)(mcLogo_width*0.25);
        int mcLogo_MinX = (int)(width * 0.02);
        int mcLogo_MinY = (int)(height * 0.5 - mcLogo_height * 0.5);
        guiGraphics.blit(MINECRAFT_LOGO_LOCATION,mcLogo_MinX,mcLogo_MinY,mcLogo_width,mcLogo_height,0,0,1024,512,1024,512);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

        if (isFontLoaded) {
            guiGraphics.drawString(minecraft.font, Component.translatableWithFallback("betterlaunchui.loading.present", "Presented By"), logo_MinX - 70, logo_MinY, s);
        }

        if (isFontLoaded) {
            guiGraphics.drawString(
            minecraft.font,
            Component.translatableWithFallback("betterlaunchui.loading.greeting", "Welcome, %s", minecraft.getUser().getName()),
                mcLogo_MinX+mcLogo_width + 20,
                mcLogo_MinY + mcLogo_height -30,
                s
            );
        }

        if (isFontLoaded) {
            guiGraphics.drawString(
                minecraft.font,
                String.format("Minecraft %s", SharedConstants.getCurrentVersion().getName()),
                0,
                0,
                s
            );
        }

        if (isFontLoaded && !Minecraft.getInstance().options.darkMojangStudiosBackground().get()) {
            guiGraphics.drawString(
                    minecraft.font,
                    Component.translatableWithFallback("betterlaunchui.loading.advice", "Enable \"Monochrome Logo\" to get better experience!"),
                    0,
                    10,
                    s
            );
        }

        float w = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + w * 0.050000012F, 0.0F, 1.0F);
        if (g < 2.0F) {
            if (isFontLoaded) {
                guiGraphics.drawCenteredString(
                        this.minecraft.font,
                        Component.translatableWithFallback("betterlaunchui.loading.progress", "Loading... %s%%", (int) (this.currentProgress * 100)),
                        (int) (width * 0.1),
                        progressBar_MaxY - 20,
                        s
                );
            }
            int progressBarAlpha = Mth.ceil((1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F)) * 255.0F);
            this.drawProgressBar(guiGraphics, 0, progressBar_MaxY-3, width, progressBar_MaxY, progressBarAlpha);
        }

        if (g >= 2.0F) {
            this.minecraft.setOverlay((Overlay)null);
        }

        if (this.fadeOutStart == -1L && this.reload.isDone() && ((!this.fadeIn && Util.getMillis() - onStart >= 4000L) || h >= 2.0F)) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.onFinish.accept(Optional.of(throwable));
            }

            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, guiGraphics.guiWidth(), guiGraphics.guiHeight());
            }
        }

    }


    private void drawProgressBar(GuiGraphics guiGraphics, int frontX, int topY, int endX, int bottomY, int alpha) {
        int progress = Mth.ceil((endX - frontX - 2) * this.currentProgress);
        int color = FastColor.ARGB32.color(255, 255, 255, 255);
        guiGraphics.fill(frontX, topY, frontX + progress, bottomY, replaceAlpha(color,alpha));
    }

    public boolean isPauseScreen() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    static class LogoTexture extends SimpleTexture {
        public LogoTexture(ResourceLocation location) {
            super(location);
        }

        protected SimpleTexture.TextureImage getTextureImage(ResourceManager resourceManager) {
            VanillaPackResources vanillaPackResources = Minecraft.getInstance().getVanillaPackResources();
            IoSupplier<InputStream> ioSupplier = vanillaPackResources.getResource(PackType.CLIENT_RESOURCES, location);
            if (ioSupplier == null) {
                return new SimpleTexture.TextureImage(new FileNotFoundException(location.toString()));
            } else {
                try (InputStream inputStream = (InputStream)ioSupplier.get()) {
                    return new SimpleTexture.TextureImage(new TextureMetadataSection(true, true), NativeImage.read(inputStream));
                } catch (IOException iOException) {
                    return new SimpleTexture.TextureImage(iOException);
                }
            }
        }
    }
}
