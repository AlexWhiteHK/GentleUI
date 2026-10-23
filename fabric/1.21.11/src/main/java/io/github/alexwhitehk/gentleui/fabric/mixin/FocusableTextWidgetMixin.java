package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.ListSkin;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.FocusableTextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The black, outlined text box used by status and notice screens becomes a themed card. */
@Mixin(FocusableTextWidget.class)
public abstract class FocusableTextWidgetMixin {
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void gentleui$fill(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
		if (SkinContext.active()) {
			// Vanilla insets the fill by one pixel on the left to sit inside its outline.
			ListSkin.card(graphics, x0 - 1, y0, x1 - x0 + 1, y1 - y0, (color >>> 24) / 255f);
		} else {
			graphics.fill(x0, y0, x1, y1, color);
		}
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderOutline(IIIII)V"))
	private void gentleui$outline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
		// Keep the outline as the keyboard focus ring only; the card has its own edge.
		if (!SkinContext.active() || ((FocusableTextWidget) (Object) this).isFocused()) {
			graphics.renderOutline(x, y, w, h, color);
		}
	}
}
