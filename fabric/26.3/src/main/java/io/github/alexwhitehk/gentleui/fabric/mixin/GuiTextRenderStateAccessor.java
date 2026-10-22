package io.github.alexwhitehk.gentleui.fabric.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiTextRenderState.class)
public interface GuiTextRenderStateAccessor {
	@Accessor("font")
	Font gentleui$font();

	@Accessor("text")
	FormattedCharSequence gentleui$text();

	@Accessor("x")
	int gentleui$x();

	@Accessor("y")
	int gentleui$y();

	@Accessor("color")
	int gentleui$color();

	@Accessor("backgroundColor")
	int gentleui$backgroundColor();

	@Accessor("dropShadow")
	boolean gentleui$dropShadow();

	@Accessor("includeEmpty")
	boolean gentleui$includeEmpty();
}
