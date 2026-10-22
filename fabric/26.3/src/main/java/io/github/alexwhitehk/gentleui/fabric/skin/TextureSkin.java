package io.github.alexwhitehk.gentleui.fabric.skin;

import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Replaces the textured menu chrome (separators, list wells, dirt-free menu backgrounds) with themed shapes. */
public final class TextureSkin {
	private static final Rect RECT = new Rect();

	private TextureSkin() {
	}

	public static boolean paint(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		if (!"minecraft".equals(id.getNamespace())) {
			return false;
		}
		String path = id.getPath();
		Skin skin = SkinContext.theme().skin();
		switch (path) {
			case "textures/gui/header_separator.png", "textures/gui/inworld_header_separator.png",
				"textures/gui/footer_separator.png", "textures/gui/inworld_footer_separator.png" -> {
				McCanvas c = McCanvas.begin(g);
				c.pushAlpha(SkinContext.contentAlpha());
				skin.separator(c, x, y + h * 0.5f, w);
				c.popAlpha();
				c.end();
				return true;
			}
			case "textures/gui/menu_list_background.png", "textures/gui/inworld_menu_list_background.png" -> {
				RECT.set(x + 1f, y + 1f, w - 2f, h - 2f);
				McCanvas c = McCanvas.begin(g);
				c.pushAlpha(SkinContext.contentAlpha());
				skin.panel(c, RECT, PanelStyle.LIST);
				c.popAlpha();
				c.end();
				return true;
			}
			case "textures/gui/menu_background.png", "textures/gui/inworld_menu_background.png" -> {
				// The themed background already covers the screen; partial uses (behind the selected tab) are dropped too.
				return true;
			}
			case "textures/gui/tab_header_background.png" -> {
				return true;
			}
			default -> {
				return false;
			}
		}
	}
}
