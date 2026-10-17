package io.ula.blui.client.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
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
    private static final IntSupplier BRAND_BACKGROUND = () -> Minecraft.getInstance().options.darkMojangStudiosBackground().get()
            ? LOGO_BACKGROUND_COLOR_DARK
            : LOGO_BACKGROUND_COLOR;
    private static final int LOGO_SCALE = 240;
    private static final float LOGO_QUARTER_FLOAT = 60.0F;
    private static final int LOGO_QUARTER = 60;
    private static final int LOGO_HALF = 120;
    private static final float LOGO_OVERLAP = 0.0625F;
    private static final float SMOOTHING = 0.95F;
    public static final long FADE_OUT_TIME = 1000L;
    public static final long FADE_IN_TIME = 500L;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;
    private long onStart = -1L;

    public CustomLoadingOverlay(Minecraft minecraft, ReloadInstance reloadInstance, Consumer<Optional<Throwable>> consumer, boolean bl) {
        super(minecraft,reloadInstance,consumer,bl);
        this.minecraft = minecraft;
        this.reload = reloadInstance;
        this.onFinish = consumer;
        this.fadeIn = bl;
        this.onStart = Util.getMillis();
        registerTextures(minecraft.getTextureManager());
    }

    public static void registerTextures(TextureManager textureManager) {
        textureManager.registerAndLoad(MOJANG_STUDIOS_LOGO_LOCATION, new CustomLoadingOverlay.LogoTexture(MOJANG_STUDIOS_LOGO_LOCATION));
        textureManager.registerAndLoad(MINECRAFT_LOGO_LOCATION,new CustomLoadingOverlay.LogoTexture(MINECRAFT_LOGO_LOCATION));
    }

    private static int replaceAlpha(int i, int j) {
        return i & 16777215 | j << 24;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        int width = guiGraphics.guiWidth();//k
        int height = guiGraphics.guiHeight();//l

        long m = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = m;
        }

        float g = this.fadeOutStart > -1L ? (float)(m - this.fadeOutStart) / 1000.0F : -1.0F;
        float h = this.fadeInStart > -1L ? (float)(m - this.fadeInStart) / 500.0F : -1.0F;
        float o;
        if (g >= 1.0F) {
            if (this.minecraft.screen != null) {
                this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, 0, 0, f);
            } else {
                this.minecraft.gui.renderDeferredSubtitles();
            }

            int n = Mth.ceil((1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F)) * 255.0F);
            guiGraphics.nextStratum();
            guiGraphics.fill(0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = 1.0F - Mth.clamp(g - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && h < 1.0F) {
                this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, i, j, f);
            } else {
                this.minecraft.gui.renderDeferredSubtitles();
            }

            int n = Mth.ceil(Mth.clamp(h, 0.15, 1.0) * 255.0);
            guiGraphics.nextStratum();
            guiGraphics.fill(0, 0, width, height, replaceAlpha(BRAND_BACKGROUND.getAsInt(), n));
            o = Mth.clamp(h, 0.0F, 1.0F);
        } else {
            int n = BRAND_BACKGROUND.getAsInt();
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.minecraft.getMainRenderTarget().getColorTexture(), n);
            o = 1.0F;
        }


        int progressBar_MaxY = guiGraphics.guiHeight();//进度条位置（高度）
        float reloadProgress = this.reload.getActualProgress();

        int logo_width = (int)Math.ceil(width * 0.2);
        int logo_height = (int)Math.ceil(logo_width*0.25);
        int logo_MinX = (int)(width * 0.99) - logo_width;
        int logo_MinY = (int)(progressBar_MaxY * 0.99) - logo_height;
        int s = ARGB.white(o);
        int textColor = this.fadeOutStart > -1L ?ARGB.color(Math.round(1.0F - Mth.clamp(g, 0.0F, 1.0F) * 255.0F),255,255,255) : ARGB.color(255,255,255);
        guiGraphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX, logo_MinY, 0.0F, 0.0F, logo_width/2, logo_height, 120, 60, 120, 120, s);
        guiGraphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, logo_MinX + logo_width/2 , logo_MinY, 0.0F, 60.0F, logo_width/2, logo_height, 120, 60, 120, 120, s);
        guiGraphics.drawString(minecraft.font,Component.translatable("betterlaunchui.loading.present"),logo_MinX - 70,logo_MinY,textColor);

        int mcLogo_width = (int)(width*0.6);
        int mcLogo_height = (int)(mcLogo_width*0.25);
        int mcLogo_MinX = (int)(width * 0.02);
        int mcLogo_MinY = (int)(height * 0.5 - mcLogo_height * 0.5);
        guiGraphics.blit(RenderPipelines.MOJANG_LOGO,MINECRAFT_LOGO_LOCATION,mcLogo_MinX,mcLogo_MinY,0,0,mcLogo_width,mcLogo_height,1024,512,1024,512);
        guiGraphics.drawString(
                minecraft.font,
                Component.translatable("betterlaunchui.loading.greeting",minecraft.getGameProfile().name()),
                mcLogo_MinX+mcLogo_width + 20,
                mcLogo_MinY + mcLogo_height -30,
                textColor
        );

        guiGraphics.drawString(
                minecraft.font,
                String.format("Minecraft %s", SharedConstants.getCurrentVersion().name()),
                0,
                0,
                textColor
        );

        if(!Minecraft.getInstance().options.darkMojangStudiosBackground().get()){
            guiGraphics.drawString(
                    minecraft.font,
                    "建议开启黑白徽标以获得最佳体验！",
                    0,
                    10,
                    textColor
            );
        }



        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + reloadProgress * 0.050000012F, 0.0F, 1.0F);
        if (g < 1.0F) {

            guiGraphics.drawCenteredString(
                    this.minecraft.font,
                    Component.translatable("betterlaunchui.loading.progress",(int)(this.currentProgress * 100)),
                    (int)(width * 0.1),
                    progressBar_MaxY - 20,
                    textColor
            );
            this.drawProgressBar(guiGraphics, 0, progressBar_MaxY-3, width, progressBar_MaxY, 1.0F - Mth.clamp(g, 0.0F, 1.0F));


        }

        if (g >= 2.0F) {
            this.minecraft.setOverlay(null);
        }
    }

    @Override
    public void tick() {
        if (this.fadeOutStart == -1L && this.reload.isDone() && this.isReadyToFadeOut()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable var2) {
                this.onFinish.accept(Optional.of(var2));
            }

            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.screen != null) {
                Window window = this.minecraft.getWindow();
                this.minecraft.screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }

    private boolean isReadyToFadeOut() {
        return (!this.fadeIn && Util.getMillis() - this.onStart >= 5000L) || this.fadeInStart > -1L && Util.getMillis() - this.fadeInStart >= 1000L;
    }

    private void drawProgressBar(GuiGraphics guiGraphics, int frontX, int topY, int endX, int bottomY, float f) {
        int progress = Mth.ceil((endX - frontX - 2) * this.currentProgress);
        int n = Math.round(f * 255.0F);
        int color = ARGB.color(n, 255, 255, 255);
        guiGraphics.fill(frontX, topY, frontX + progress, bottomY, color);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Environment(EnvType.CLIENT)
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
