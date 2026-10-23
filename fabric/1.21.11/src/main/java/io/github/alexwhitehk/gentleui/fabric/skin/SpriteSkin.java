package io.github.alexwhitehk.gentleui.fabric.skin;

import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * Replaces vanilla widget sprites with themed vector drawings. Widgets are recognised by the sprite they ask for,
 * so this also covers modded screens built from vanilla widgets. Hover and focus are animated per on-screen
 * rectangle, since sprite calls carry no widget identity.
 */
public final class SpriteSkin {
	private enum Kind {
		BUTTON,
		SLIDER,
		SLIDER_HANDLE,
		TEXT_FIELD,
		CHECKBOX,
		TAB,
		SCROLLER,
		SCROLLER_BG,
		POPUP,
		PANEL,
		TOOLTIP,
		TOOLTIP_FRAME,
		LOCK,
		LIST_ROW
	}

	private record Match(Kind kind, boolean highlighted, boolean disabled, boolean selected) {
	}

	private static final Map<String, Match> SPRITES = new HashMap<>();

	static {
		put("widget/button", Kind.BUTTON, false, false, false);
		put("widget/button_highlighted", Kind.BUTTON, true, false, false);
		put("widget/button_disabled", Kind.BUTTON, false, true, false);
		put("widget/slider", Kind.SLIDER, false, false, false);
		put("widget/slider_highlighted", Kind.SLIDER, true, false, false);
		put("widget/slider_handle", Kind.SLIDER_HANDLE, false, false, false);
		put("widget/slider_handle_highlighted", Kind.SLIDER_HANDLE, true, false, false);
		put("widget/text_field", Kind.TEXT_FIELD, false, false, false);
		put("widget/text_field_highlighted", Kind.TEXT_FIELD, true, false, false);
		put("widget/checkbox", Kind.CHECKBOX, false, false, false);
		put("widget/checkbox_highlighted", Kind.CHECKBOX, true, false, false);
		put("widget/checkbox_selected", Kind.CHECKBOX, false, false, true);
		put("widget/checkbox_selected_highlighted", Kind.CHECKBOX, true, false, true);
		put("widget/tab", Kind.TAB, false, false, false);
		put("widget/tab_highlighted", Kind.TAB, true, false, false);
		put("widget/tab_selected", Kind.TAB, false, false, true);
		put("widget/tab_selected_highlighted", Kind.TAB, true, false, true);
		put("widget/scroller", Kind.SCROLLER, false, false, false);
		put("widget/scroller_background", Kind.SCROLLER_BG, false, false, false);
		// The difficulty lock is a single sprite with the padlock baked in; "selected" means locked.
		put("widget/locked_button", Kind.LOCK, false, false, true);
		put("widget/locked_button_highlighted", Kind.LOCK, true, false, true);
		put("widget/locked_button_disabled", Kind.LOCK, false, true, true);
		put("widget/unlocked_button", Kind.LOCK, false, false, false);
		put("widget/unlocked_button_highlighted", Kind.LOCK, true, false, false);
		put("widget/unlocked_button_disabled", Kind.LOCK, false, true, false);
		put("popup/background", Kind.POPUP, false, false, false);
		put("social_interactions/background", Kind.PANEL, false, false, false);
		put("tooltip/background", Kind.TOOLTIP, false, false, false);
		put("tooltip/frame", Kind.TOOLTIP_FRAME, false, false, false);
	}

	private static void put(String path, Kind kind, boolean highlighted, boolean disabled, boolean selected) {
		SPRITES.put("minecraft:" + path, new Match(kind, highlighted, disabled, selected));
	}

	private static final class Anim {
		float hover;
		float focus;
		float active;
		long seen;
	}

	private static final Map<Long, Anim> ANIMS = new HashMap<>();
	private static final WidgetState STATE = new WidgetState();
	private static final Rect RECT = new Rect();
	private static final Rect THUMB = new Rect();
	private static final Rect pendingTrack = new Rect();
	private static boolean trackPending;
	private static final WidgetState trackState = new WidgetState();
	private static final Rect scrollerTrack = new Rect();
	private static long scrollerFrame = -1L;
	private static long lastGc;

	private SpriteSkin() {
	}

	private static Anim anim(Kind kind, int x, int y, int w, int h, float hoverTarget, float focusTarget, float activeTarget) {
		long key = ((long) kind.ordinal() << 58) ^ ((long) (x & 0x7FFF) << 43) ^ ((long) (y & 0x7FFF) << 28) ^ ((long) (w & 0x3FFF) << 14) ^ (h & 0x3FFF);
		long frame = SkinContext.frame();
		Anim a = ANIMS.get(key);
		if (a == null) {
			// New rectangles (scrolling lists, freshly opened screens) start at their target to avoid flicker.
			a = new Anim();
			a.hover = hoverTarget;
			a.focus = focusTarget;
			a.active = activeTarget;
			ANIMS.put(key, a);
		} else if (a.seen != frame) {
			float dt = SkinContext.dt();
			a.hover = MathUtil.approach(a.hover, hoverTarget, hoverTarget > a.hover ? 0.035f : 0.07f, dt);
			a.focus = MathUtil.approach(a.focus, focusTarget, 0.05f, dt);
			a.active = MathUtil.approach(a.active, activeTarget, 0.045f, dt);
		}
		a.seen = frame;
		if (frame - lastGc > 120) {
			lastGc = frame;
			for (Iterator<Anim> it = ANIMS.values().iterator(); it.hasNext(); ) {
				if (frame - it.next().seen > 60) {
					it.remove();
				}
			}
		}
		return a;
	}

	/** Animated hover value for an arbitrary list row rectangle. */
	public static float rowHover(int x, int y, int w, int h, boolean hovered) {
		return anim(Kind.LIST_ROW, x, y, w, h, hovered ? 1f : 0f, 0f, 0f).hover;
	}

	/** Called for every sprite blit while a restyled screen draws; returns true when the sprite was replaced. */
	public static boolean paint(GuiGraphics g, Identifier id, int x, int y, int w, int h, int color) {
		Match m = SPRITES.get(id.toString());
		if (m == null) {
			flushTrack(g);
			return false;
		}
		if (m.kind == Kind.TOOLTIP_FRAME) {
			return true;
		}
		float alpha = ((color >>> 24) & 0xFF) / 255f;
		if (color == -1) {
			alpha = 1f;
		}
		alpha *= SkinContext.contentAlpha();
		float mx = SkinContext.graphicsMouseX(g);
		float my = SkinContext.graphicsMouseY(g);
		boolean pointer = mx >= x && my >= y && mx < x + w && my < y + h;
		Skin skin = SkinContext.theme().skin();
		RECT.set(x, y, w, h);
		if (m.kind != Kind.SLIDER_HANDLE) {
			flushTrack(g);
		}
		switch (m.kind) {
			case BUTTON -> {
				Anim a = anim(m.kind, x, y, w, h, !m.disabled && (pointer || m.highlighted) ? 1f : 0f, m.highlighted && !pointer ? 1f : 0f, 0f);
				STATE.set(a.hover, 0f, a.focus, 0f, !m.disabled);
				draw(g, alpha, c -> skin.button(c, RECT, ButtonStyle.TONAL, STATE));
			}
			case SLIDER -> {
				Anim a = anim(m.kind, x, y, w, h, pointer ? 1f : 0f, m.highlighted ? 1f : 0f, 0f);
				pendingTrack.set(x, y, w, h);
				trackState.set(a.hover, 0f, a.focus, 0f, true);
				trackPending = true;
				pendingAlpha = alpha;
			}
			case SLIDER_HANDLE -> {
				float value = -1f;
				if (trackPending && pendingTrack.w > w) {
					value = MathUtil.clamp01((x - pendingTrack.x) / (pendingTrack.w - w));
				}
				Rect track = trackPending ? pendingTrack : RECT;
				trackPending = false;
				float v = value;
				float a = pendingAlpha;
				draw(g, a, c -> skin.sliderContainer(c, track, v, trackState));
			}
			case TEXT_FIELD -> {
				Anim a = anim(m.kind, x, y, w, h, pointer ? 1f : 0f, 0f, m.highlighted ? 1f : 0f);
				STATE.set(a.hover, 0f, 0f, a.active, true);
				draw(g, alpha, c -> skin.textField(c, RECT, STATE));
			}
			case CHECKBOX -> {
				Anim a = anim(m.kind, x, y, w, h, pointer ? 1f : 0f, m.highlighted ? 1f : 0f, m.selected ? 1f : 0f);
				STATE.set(a.hover, 0f, a.focus, a.active, true);
				draw(g, alpha, c -> skin.checkbox(c, RECT, STATE));
			}
			case TAB -> {
				Anim a = anim(m.kind, x, y, w, h, pointer || m.highlighted ? 1f : 0f, 0f, m.selected ? 1f : 0f);
				STATE.set(a.hover, 0f, 0f, a.active, true);
				draw(g, alpha, c -> skin.tab(c, RECT, STATE));
			}
			case SCROLLER_BG -> {
				scrollerTrack.set(x, y, w, h);
				scrollerFrame = SkinContext.frame();
			}
			case SCROLLER -> {
				Anim a = anim(m.kind, x, y, w, 0, pointer ? 1f : 0f, 0f, 0f);
				STATE.set(a.hover, 0f, 0f, 0f, true);
				THUMB.set(x + w * 0.5f - 1.5f, y + 1f, 3f, h - 2f);
				Rect track = scrollerFrame == SkinContext.frame() ? scrollerTrack : RECT;
				draw(g, alpha, c -> skin.scrollbar(c, track, THUMB, STATE));
			}
			case LOCK -> {
				Anim a = anim(m.kind, x, y, w, h, !m.disabled && (pointer || m.highlighted) ? 1f : 0f, m.highlighted && !pointer ? 1f : 0f, 0f);
				STATE.set(a.hover, 0f, a.focus, 0f, !m.disabled);
				Icon icon = m.selected ? Icon.LOCK : Icon.UNLOCK;
				draw(g, alpha, c -> {
					skin.button(c, RECT, ButtonStyle.TONAL, STATE);
					icon.draw(c, RECT.cx(), RECT.cy(), Math.min(w, h) * 0.55f, skin.iconColor(ButtonStyle.TONAL, STATE), 2.2f);
				});
			}
			case POPUP -> draw(g, alpha, c -> skin.panel(c, RECT, PanelStyle.POPOVER));
			case PANEL -> draw(g, alpha, c -> skin.panel(c, RECT, PanelStyle.CARD));
			case TOOLTIP -> draw(g, 1f, c -> skin.tooltip(c, RECT));
			default -> {
				return false;
			}
		}
		return true;
	}

	private static float pendingAlpha = 1f;

	/** A slider track without a handle (should not happen with vanilla sliders) is still drawn. */
	private static void flushTrack(GuiGraphics g) {
		if (!trackPending) {
			return;
		}
		trackPending = false;
		Skin skin = SkinContext.theme().skin();
		draw(g, pendingAlpha, c -> skin.sliderContainer(c, pendingTrack, -1f, trackState));
	}

	private interface Painter {
		void paint(McCanvas c);
	}

	private static void draw(GuiGraphics g, float alpha, Painter p) {
		McCanvas c = McCanvas.begin(g);
		c.pushAlpha(alpha);
		p.paint(c);
		c.popAlpha();
		c.end();
	}
}
