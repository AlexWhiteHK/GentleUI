package io.github.alexwhitehk.gentleui.fabric.skin;

import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import net.minecraft.client.gui.GuiGraphics;

/** Themed selection and hover rows for vanilla selection lists. */
public final class ListSkin {
	private static final Rect RECT = new Rect();

	private ListSkin() {
	}

	public static void selection(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
		RECT.set(x, y, w, h);
		McCanvas c = McCanvas.begin(g);
		c.pushAlpha(SkinContext.contentAlpha());
		SkinContext.theme().skin().selection(c, RECT, 1f, focused);
		c.popAlpha();
		c.end();
	}

	public static void well(GuiGraphics g, int x, int y, int w, int h) {
		RECT.set(x, y + 1f, w, h - 2f);
		McCanvas c = McCanvas.begin(g);
		c.pushAlpha(SkinContext.contentAlpha());
		SkinContext.theme().skin().panel(c, RECT, io.github.alexwhitehk.gentleui.core.theme.PanelStyle.LIST);
		c.popAlpha();
		c.end();
	}

	/** A message box, such as the black text panel of status screens. */
	public static void card(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		RECT.set(x, y, w, h);
		McCanvas c = McCanvas.begin(g);
		c.pushAlpha(SkinContext.contentAlpha() * alpha);
		SkinContext.theme().skin().panel(c, RECT, io.github.alexwhitehk.gentleui.core.theme.PanelStyle.CARD);
		c.popAlpha();
		c.end();
	}

	public static void hover(GuiGraphics g, int x, int y, int w, int h, boolean hovered) {
		float strength = SpriteSkin.rowHover(x, y, w, h, hovered);
		if (strength < 0.01f) {
			return;
		}
		RECT.set(x, y, w, h);
		McCanvas c = McCanvas.begin(g);
		c.pushAlpha(SkinContext.contentAlpha());
		SkinContext.theme().skin().selection(c, RECT, strength * 0.55f, false);
		c.popAlpha();
		c.end();
	}
}
