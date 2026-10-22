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
		GuiTextRenderStateAccessor a = (GuiTextRenderStateAccessor) (Object) s;
		Palette p = SkinContext.theme().palette();
		float fade = SkinContext.contentAlpha();
		int original = a.gentleui$color();
		int color = p.light ? TextColors.remap(original, p) : original;
		color = TextColors.fade(color, fade);
		FormattedCharSequence text = p.light ? recolor(a.gentleui$text(), p) : a.gentleui$text();
		if (color == original && !a.gentleui$dropShadow() && text == a.gentleui$text()) {
			return s;
		}
		return new GuiTextRenderState(a.gentleui$font(), text, s.pose, a.gentleui$x(), a.gentleui$y(), color, a.gentleui$backgroundColor(), false,
			a.gentleui$includeEmpty(), s.scissor);
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
