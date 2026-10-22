package io.github.alexwhitehk.gentleui.fabric.mixin;

import net.minecraft.client.gui.components.AbstractScrollArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractScrollArea.class)
public interface AbstractScrollAreaAccessor {
	@Invoker("scrollBarX")
	int gentleui$scrollBarX();
}
