package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screen routing lives in {@link GuiMixin}; only the forced single-frame show remains on Minecraft. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** The screen gets one forced frame before the game blocks, so it must not start invisible. */
	@Inject(method = "setScreenAndShow", at = @At("HEAD"))
	private void gentleui$showNow(Screen screen, CallbackInfo ci) {
		SkinContext.showNextImmediately();
	}
}
