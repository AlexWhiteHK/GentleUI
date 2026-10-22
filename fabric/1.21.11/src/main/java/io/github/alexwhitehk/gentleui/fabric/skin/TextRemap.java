package io.github.alexwhitehk.gentleui.fabric.skin;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.TextColors;
import io.github.alexwhitehk.gentleui.fabric.mixin.GuiTextRenderStateAccessor;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

/** Adapts every piece of text drawn by a restyled screen: theme-aware colors, no drop shadow, entrance fade. */
public final class TextRemap {
	private TextRemap() {
	}

	public static GuiTextRenderState adapt(GuiTextRenderState s) {
		if (!SkinContext.active() || !ConfigStore.config().adaptText) {
			return s;
		}
		Palette p = SkinContext.theme().palette();
		float fade = SkinContext.contentAlpha();
		int color = p.light ? TextColors.remap(s.color, p) : s.color;
		color = TextColors.fade(color, fade);
		FormattedCharSequence text = p.light ? recolor(s.text, p) : s.text;
		if (color == s.color && !s.dropShadow && text == s.text) {
			return s;
		}
		boolean includeEmpty = ((GuiTextRenderStateAccessor) (Object) s).gentleui$includeEmpty();
		return new GuiTextRenderState(s.font, text, s.pose, s.x, s.y, color, s.backgroundColor, false, includeEmpty, s.scissor);
	}

	/** Re-maps explicit style colors (formatting codes, colored components) inside the text. */
	private static FormattedCharSequence recolor(FormattedCharSequence original, Palette p) {
		return sink -> original.accept((index, style, codepoint) -> sink.accept(index, adaptStyle(style, p), codepoint));
	}

	private static Style adaptStyle(Style style, Palette p) {
		TextColor c = style.getColor();
		if (c == null) {
			return style;
		}
		int rgb = c.getValue();
		int mapped = TextColors.remapOpaque(rgb, p);
		return mapped == rgb ? style : style.withColor(mapped);
	}
}
