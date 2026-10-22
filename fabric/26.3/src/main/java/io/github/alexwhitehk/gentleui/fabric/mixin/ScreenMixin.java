package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
	protected abstract void extractBlurredBackground(GuiGraphicsExtractor graphics);

	private static final String RENDER = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V";

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
	private void gentleui$begin(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		SkinContext.beginScreen((Screen) (Object) this, graphics);
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.BEFORE))
	private void gentleui$beforeContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (SkinContext.active()) {
			SkinContext.beginContent();
			graphics.pose().pushMatrix();
			graphics.pose().translate(0f, SkinContext.slide());
		}
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER))
	private void gentleui$afterContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (SkinContext.active()) {
			graphics.pose().popMatrix();
			SkinContext.endContent();
		}
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void gentleui$end(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		SkinContext.endScreen();
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void gentleui$background(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!SkinContext.active()) {
			return;
		}
		if (this.minecraft.level != null) {
			this.extractBlurredBackground(graphics);
		}
		SkinContext.drawBackground(graphics, (Screen) (Object) this);
		this.minecraft.gui.hud.extractDeferredSubtitles();
		ci.cancel();
	}
}
