package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.ScreenRouter;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen gentleui$route(Screen screen) {
		return ScreenRouter.route(screen);
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void gentleui$opened(Screen ignored, CallbackInfo ci) {
		SkinContext.screenOpened(this.screen);
	}
}
