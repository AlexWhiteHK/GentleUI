package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.TextRemap;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** All GUI text passes through here, which lets restyled screens adapt text without per-screen code. */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
	@ModifyVariable(method = "submitText", at = @At("HEAD"), argsOnly = true)
	private GuiTextRenderState gentleui$adaptText(GuiTextRenderState state) {
		return TextRemap.adapt(state);
	}
}
