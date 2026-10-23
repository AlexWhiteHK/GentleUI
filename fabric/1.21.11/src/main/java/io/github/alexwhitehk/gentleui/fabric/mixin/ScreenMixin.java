package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Themed background and entrance transition for every restyled screen. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Shadow
	protected Minecraft minecraft;

	@Shadow
	protected abstract void renderBlurredBackground(GuiGraphics graphics);

	private static final String RENDER = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"))
	private void gentleui$begin(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		SkinContext.beginScreen((Screen) (Object) this, graphics);
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.BEFORE))
	private void gentleui$beforeContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (SkinContext.active()) {
			SkinContext.beginContent();
			graphics.pose().pushMatrix();
			graphics.pose().translate(0f, SkinContext.slide());
		}
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER))
	private void gentleui$afterContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (SkinContext.active()) {
			graphics.pose().popMatrix();
			SkinContext.endContent();
		}
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("TAIL"))
	private void gentleui$end(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		SkinContext.endScreen();
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void gentleui$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!SkinContext.active()) {
			return;
		}
		if (this.minecraft.level != null) {
			this.renderBlurredBackground(graphics);
		}
		SkinContext.drawBackground(graphics, (Screen) (Object) this);
		this.minecraft.gui.renderDeferredSubtitles();
		ci.cancel();
	}

	/**
	 * Status screens (reading world data, loading resources) override the background method and draw the
	 * panorama themselves; the panorama is where their background starts, so the theme takes over here. Calls
	 * from inside a screen's own content, like the classic title screen's, keep the panorama.
	 */
	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void gentleui$panorama(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
		if (SkinContext.active() && !SkinContext.inContent()) {
			SkinContext.drawBackground(graphics, (Screen) (Object) this);
			ci.cancel();
		}
	}

	/** Blur only belongs over a running world; over the theme background it would smear it. */
	@Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
	private void gentleui$blur(GuiGraphics graphics, CallbackInfo ci) {
		if (SkinContext.active() && this.minecraft.level == null) {
			ci.cancel();
		}
	}
}
