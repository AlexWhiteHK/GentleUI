package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The themed tab already marks the selection; vanilla's white underline would sit on top of it. */
@Mixin(MenuTabBar.MenuTabButton.class)
public abstract class TabButtonMixin {
	@Inject(method = "renderFocusUnderline", at = @At("HEAD"), cancellable = true)
	private void gentleui$underline(GuiGraphicsExtractor graphics, Font font, int color, CallbackInfo ci) {
		if (SkinContext.active()) {
			ci.cancel();
		}
	}
}
