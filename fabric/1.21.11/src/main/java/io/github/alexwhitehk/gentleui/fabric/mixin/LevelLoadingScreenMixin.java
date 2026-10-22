package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.core.scene.LoadingView;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Themed world loading: the theme's background and progress indicator instead of the chunk map. */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
	@Shadow
	private LevelLoadTracker loadTracker;

	@Shadow
	private float smoothedProgress;

	@Shadow
	private LevelLoadingScreen.Reason reason;

	protected LevelLoadingScreenMixin(Component title) {
		super(title);
	}

	private boolean gentleui$themed() {
		return SkinContext.active() && this.reason == LevelLoadingScreen.Reason.OTHER;
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void gentleui$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (gentleui$themed()) {
			SkinContext.drawBackground(graphics, this, false);
			ci.cancel();
		}
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void gentleui$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!gentleui$themed()) {
			return;
		}
		McCanvas c = McCanvas.begin(graphics);
		float progress = this.loadTracker.hasProgress() ? this.smoothedProgress : -1f;
		LoadingView.draw(c, SkinContext.theme(), this.width, this.height, Component.translatable("multiplayer.downloadingTerrain").getString(), progress, SkinContext.time());
		c.end();
		ci.cancel();
	}
}
