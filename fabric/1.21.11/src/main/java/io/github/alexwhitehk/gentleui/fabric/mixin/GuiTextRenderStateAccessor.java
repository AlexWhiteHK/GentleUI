package io.github.alexwhitehk.gentleui.fabric.mixin;

import net.minecraft.client.gui.render.state.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiTextRenderState.class)
public interface GuiTextRenderStateAccessor {
	@Accessor("includeEmpty")
	boolean gentleui$includeEmpty();
}
