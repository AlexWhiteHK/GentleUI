package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.ScreenRouter;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Single routing point: every screen change passes through here so GentleUI can substitute its own screens. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public Screen screen;

	@Shadow
	public ClientLevel level;

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen gentleui$route(Screen screen) {
		if (screen == null && this.level == null) {
			// Vanilla would create a title screen further down; create it here so it is routed as well.
			screen = new TitleScreen();
		}
		return ScreenRouter.route(screen);
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void gentleui$opened(Screen ignored, CallbackInfo ci) {
		SkinContext.screenOpened(this.screen);
	}
}
