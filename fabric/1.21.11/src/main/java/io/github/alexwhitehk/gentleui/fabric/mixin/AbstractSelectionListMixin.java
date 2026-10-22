package io.github.alexwhitehk.gentleui.fabric.mixin;

import io.github.alexwhitehk.gentleui.fabric.skin.ListSkin;
import io.github.alexwhitehk.gentleui.fabric.skin.SkinContext;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rounded, animated row highlights instead of the vanilla outline box. */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin<E extends AbstractSelectionList.Entry<E>> {
	@Shadow
	private E hovered;

	@Shadow
	public abstract E getSelected();

	@Shadow
	protected abstract boolean entriesCanBeSelected();

	@Shadow
	public abstract int getRowLeft();

	@Shadow
	public abstract int getRowRight();

	@Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
	private void gentleui$background(GuiGraphics graphics, CallbackInfo ci) {
		if (!SkinContext.active()) {
			return;
		}
		AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
		int scrollRight = ((AbstractScrollAreaAccessor) self).gentleui$scrollBarX() + 6;
		int left = Math.max(self.getX(), getRowLeft() - 8);
		int right = Math.min(self.getRight(), Math.max(getRowRight() + 8, scrollRight + 4));
		ListSkin.well(graphics, left, self.getY(), right - left, self.getHeight());
		ci.cancel();
	}

	@Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
	private void gentleui$separators(GuiGraphics graphics, CallbackInfo ci) {
		if (SkinContext.active()) {
			// The rounded well already frames the list.
			ci.cancel();
		}
	}

	@Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
	private void gentleui$selection(GuiGraphics graphics, E entry, int color, CallbackInfo ci) {
		if (!SkinContext.active()) {
			return;
		}
		ListSkin.selection(graphics, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight(), color == -1);
		ci.cancel();
	}

	@Inject(method = "renderItem", at = @At("HEAD"))
	private void gentleui$hover(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, E entry, CallbackInfo ci) {
		if (!SkinContext.active() || !entriesCanBeSelected() || Objects.equals(getSelected(), entry)) {
			return;
		}
		ListSkin.hover(graphics, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight(), Objects.equals(hovered, entry));
	}
}
