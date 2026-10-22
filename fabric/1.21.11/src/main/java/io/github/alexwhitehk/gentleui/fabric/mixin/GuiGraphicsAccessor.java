package io.github.alexwhitehk.gentleui.fabric.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {
	@Accessor("mouseX")
	int gentleui$mouseX();

	@Accessor("mouseY")
	int gentleui$mouseY();
}
