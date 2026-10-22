package io.github.alexwhitehk.gentleui.fabric.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.github.alexwhitehk.gentleui.core.theme.TextColors;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import io.github.alexwhitehk.gentleui.fabric.skin.SpriteSkin;
import io.github.alexwhitehk.gentleui.fabric.skin.TextureSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sprite and texture choke points used by the vanilla skin. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
	@Inject(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"), cancellable = true)
	private void gentleui$sprite(RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, int color, CallbackInfo ci) {
		if (SkinContext.active() && SpriteSkin.paint((GuiGraphics) (Object) this, id, x, y, w, h, color)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int gentleui$fadeSprite(int color) {
		float a = SkinContext.contentAlpha();
		return a >= 1f ? color : TextColors.fade(color, a);
	}

	@Inject(method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V", at = @At("HEAD"), cancellable = true)
	private void gentleui$texture(RenderPipeline pipeline, Identifier id, int x, int y, float u, float v, int w, int h, int uw, int vh, int texW, int texH, int color, CallbackInfo ci) {
		if (SkinContext.active() && TextureSkin.paint((GuiGraphics) (Object) this, id, x, y, w, h)) {
			ci.cancel();
		}
	}
}
