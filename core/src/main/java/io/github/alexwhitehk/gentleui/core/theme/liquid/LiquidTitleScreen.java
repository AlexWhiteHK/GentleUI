package io.github.alexwhitehk.gentleui.core.theme.liquid;

import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.config.LiquidConfig;
import io.github.alexwhitehk.gentleui.core.config.MenuNode;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.render.Paint;
import io.github.alexwhitehk.gentleui.core.scene.TitleScreenBase;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Liquid main menu. The idle lake carries no text besides hints and the version; menus are pulled out of
 * its four shores and bloom as concentric rings of bubbles, one ring per level of the menu tree.
 */
public final class LiquidTitleScreen extends TitleScreenBase {
	enum Edge {
		TOP(LiquidConfig.TOP, 0f, 1f),
		BOTTOM(LiquidConfig.BOTTOM, 0f, -1f),
		LEFT(LiquidConfig.LEFT, 1f, 0f),
		RIGHT(LiquidConfig.RIGHT, -1f, 0f);

		final String id;
		/** Inward normal: the direction a menu travels when pulled out of this shore. */
		final float nx;
		final float ny;

		Edge(String id, float nx, float ny) {
			this.id = id;
			this.nx = nx;
			this.ny = ny;
		}

		float angle() {
			return MathUtil.atan2(ny, nx);
		}
	}

	private static final float[] RING_RADIUS = {0f, 66f, 120f, 168f, 212f};
	private static final float[] BUBBLE_RADIUS = {23f, 18.5f, 15.5f, 13.5f, 12f};

	private final LiquidTheme theme;
	private final Lake lake;
	private final Paint paint = new Paint();
	private final Map<Edge, Handle> handles = new EnumMap<>(Edge.class);

	private Tongue drag;
	private final List<Tongue> tongues = new ArrayList<>();
	private float pressX;
	private float pressY;
	private boolean pressMoved;
	private Edge pressEdge;

	private Edge openEdge;
	private Bubble root;
	private final List<Bubble> bubbles = new ArrayList<>();
	private final List<Bubble> openPath = new ArrayList<>();
	private final Spring[] ringGuides = new Spring[RING_RADIUS.length];
	private final Spring layoutScale = new Spring(1f, 140f, 1f);
	private Bubble hovered;
	private int keyboardIndex = -1;

	private Bubble launching;
	private float launchTimer;

	private float idle;
	private float menuOpenTime;
	private final Spring hintAlpha = new Spring(0f, 40f, 1f);
	private final Spring menuHintAlpha = new Spring(0f, 60f, 1f);

	public LiquidTitleScreen(LiquidTheme theme) {
		super(theme);
		this.theme = theme;
		this.lake = theme.lake();
		for (int i = 0; i < ringGuides.length; i++) {
			ringGuides[i] = new Spring(0f, 90f, 0.9f);
		}
	}

	private LiquidConfig cfg() {
		return ConfigStore.config().liquid;
	}

	@Override
	protected void onOpen() {
		super.onOpen();
		rebuildHandles();
	}

	@Override
	protected void onResume() {
		// Coming back from a menu opened here: let the open rings bloom again instead of sitting still.
		launching = null;
		for (Bubble b : bubbles) {
			if (!b.dying) {
				b.alpha.target(1f);
				b.scale.snap(b.scale.value() * 0.7f).target(1f);
			}
		}
		if (root != null) {
			lake.ripple(root.x, root.y, 0.6f);
		}
	}

	@Override
	protected void layout() {
		super.layout();
		rebuildHandles();
	}

	private void rebuildHandles() {
		Map<String, MenuNode> menus = cfg().menus;
		for (Edge e : Edge.values()) {
			MenuNode node = menus.get(e.id);
			if (node == null || node.hidden) {
				handles.remove(e);
				continue;
			}
			Handle h = handles.computeIfAbsent(e, k -> new Handle());
			h.edge = e;
			h.node = node;
			h.x = e == Edge.LEFT ? 0f : (e == Edge.RIGHT ? width : width * 0.5f);
			h.y = e == Edge.TOP ? 0f : (e == Edge.BOTTOM ? height : height * 0.5f);
		}
	}

	// ----------------------------------------------------------------------------------------------------------
	// Frame
	// ----------------------------------------------------------------------------------------------------------

	@Override
	protected void renderScene(Canvas c, float dt) {
		float adt = Motion.scale(dt);
		updateMenu(dt, adt);
		for (Bubble b : bubbles) {
			if (b.alpha.value() > 0.05f) {
				lake.obstacle(b.x, b.y, b.radiusNow(), b.alpha.value());
			}
		}
		if (drag != null) {
			lake.obstacle(drag.hx.value(), drag.hy.value(), drag.headRadius(), 0.8f);
		}
		drawBackground(c, dt, 0f);

		updateHints(dt);
		drawRingGuides(c);
		drawThreads(c);
		for (Tongue t : tongues) {
			drawTongue(c, t);
		}
		if (drag != null) {
			drawTongue(c, drag);
		}
		for (Bubble b : bubbles) {
			if (b != root) {
				drawBubble(c, b);
			}
		}
		if (root != null) {
			drawBubble(c, root);
		}
		drawLabels(c);
		drawHandles(c, dt);
		drawHints(c);
	}

	private void updateHints(float dt) {
		boolean idleNow = openEdge == null && drag == null && tongues.isEmpty();
		idle = idleNow ? idle + dt : 0f;
		hintAlpha.target(idleNow && idle > 1.6f && cfg().idleHint ? 1f : 0f).update(dt);
		menuOpenTime = openEdge != null ? menuOpenTime + dt : 0f;
		menuHintAlpha.target(openEdge != null && launching == null && menuOpenTime < 5f ? 1f : 0f).update(dt);
	}

	// ----------------------------------------------------------------------------------------------------------
	// Menu state
	// ----------------------------------------------------------------------------------------------------------

	private float ringRadius(int ring) {
		return RING_RADIUS[Math.min(ring, RING_RADIUS.length - 1)] * layoutScale.value();
	}

	private float bubbleRadius(int ring) {
		return BUBBLE_RADIUS[Math.min(ring, BUBBLE_RADIUS.length - 1)] * layoutScale.value();
	}

	private int outermostRing() {
		return openPath.size();
	}

	private void openMenu(Edge edge, float fromX, float fromY) {
		Handle h = handles.get(edge);
		if (h == null) {
			return;
		}
		closeMenuImmediate();
		openEdge = edge;
		root = new Bubble(h.node, null, 0, 0f);
		root.rootX.snap(fromX);
		root.rootY.snap(fromY);
		root.x = fromX;
		root.y = fromY;
		root.scale.snap(0.55f).target(1f);
		root.alpha.snap(1f).target(1f);
		root.label.target(1f);
		bubbles.add(root);
		openPath.add(root);
		openRing(root);
		lake.ripple(fromX, fromY, 1f * cfg().rippleStrength);
		host.playSound(UiSound.OPEN);
	}

	/** Opens the ring of {@code parent}'s children. */
	private void openRing(Bubble parent) {
		List<MenuNode> kids = parent.node.visibleChildren();
		int ring = parent.ring + 1;
		int n = kids.size();
		if (n == 0) {
			return;
		}
		float center;
		float step;
		float rr = RING_RADIUS[Math.min(ring, RING_RADIUS.length - 1)];
		float br = BUBBLE_RADIUS[Math.min(ring, BUBBLE_RADIUS.length - 1)];
		if (ring == 1) {
			center = openEdge.angle();
			step = n <= 4 ? (float) Math.toRadians(56) : MathUtil.TAU / n;
		} else {
			center = parent.angle;
			step = Math.max((float) Math.toRadians(24), (2f * br + 16f) / rr);
			if (step * n > MathUtil.TAU) {
				step = MathUtil.TAU / n;
			}
		}
		boolean full = ring == 1 && n > 4;
		for (int i = 0; i < n; i++) {
			float a = full ? center + i * step : center + (i - (n - 1) * 0.5f) * step;
			Bubble b = new Bubble(kids.get(i), parent, ring, a);
			b.delay = i * 0.045f;
			b.pendingStart = true;
			b.dist.snap(ring == 1 ? 0f : RING_RADIUS[ring - 1]);
			b.spin.snap(ring == 1 ? -0.55f : MathUtil.wrapAngle(parent.angle - a));
			b.scale.snap(0f);
			b.alpha.snap(0f);
			b.available = kids.get(i).isBranch() || host.available(kids.get(i).action);
			bubbles.add(b);
		}
		ringGuides[ring].target(1f);
		keyboardIndex = -1;
	}

	/** Collapses every ring deeper than {@code ring}. */
	private void collapseFrom(int ring) {
		for (Bubble b : bubbles) {
			if (b.ring > ring && !b.dying) {
				b.die();
			}
		}
		while (openPath.size() > ring + 1) {
			openPath.remove(openPath.size() - 1);
		}
		for (int k = ring + 1; k < ringGuides.length; k++) {
			ringGuides[k].target(0f);
		}
		keyboardIndex = -1;
	}

	private void closeMenu() {
		if (root == null) {
			return;
		}
		for (Bubble b : bubbles) {
			b.die();
		}
		// The root drains back into its shore.
		Handle h = handles.get(openEdge);
		if (h != null) {
			root.rootX.target(h.x - openEdge.nx * 30f);
			root.rootY.target(h.y - openEdge.ny * 30f);
		}
		for (Spring s : ringGuides) {
			s.target(0f);
		}
		openPath.clear();
		root = null;
		openEdge = null;
		keyboardIndex = -1;
		host.playSound(UiSound.CLOSE);
	}

	private void closeMenuImmediate() {
		bubbles.clear();
		openPath.clear();
		root = null;
		openEdge = null;
		for (Spring s : ringGuides) {
			s.snap(0f);
		}
	}

	private void select(Bubble b) {
		if (launching != null) {
			return;
		}
		if (!b.available) {
			b.shake = 1f;
			host.playSound(UiSound.CLOSE);
			return;
		}
		if (b == root) {
			closeMenu();
			return;
		}
		if (openPath.contains(b)) {
			// Clicking the open branch folds its ring back.
			collapseFrom(b.ring);
			host.playSound(UiSound.CLOSE);
			return;
		}
		if (b.node.isBranch()) {
			collapseFrom(b.ring - 1);
			openPath.add(b);
			openRing(b);
			lake.ripple(b.x, b.y, 0.7f * cfg().rippleStrength);
			host.playSound(UiSound.OPEN);
			return;
		}
		launch(b);
	}

	private void launch(Bubble b) {
		launching = b;
		launchTimer = 0f;
		b.scale.target(1.3f);
		for (Bubble o : bubbles) {
			if (o != b) {
				o.alpha.target(0.25f);
			}
		}
		lake.ripple(b.x, b.y, 1.2f * cfg().rippleStrength);
		host.playSound(UiSound.CLICK);
	}

	private void updateMenu(float dt, float adt) {
		// Fit the composition: scale it down on small windows, then keep it centered on screen.
		float maxR = ringRadius(Math.max(1, outermostRing())) / Math.max(0.01f, layoutScale.value());
		float need = (maxR + BUBBLE_RADIUS[Math.min(outermostRing(), BUBBLE_RADIUS.length - 1)] + 22f) * 2f;
		float fit = Math.min(1f, Math.min((width - 16f) / need, (height - 16f) / need));
		layoutScale.target(MathUtil.clamp(fit, 0.55f, 1f)).update(adt);

		if (root != null) {
			float[] box = compositionBounds();
			float cx = width * 0.5f - (box[0] + box[2]) * 0.5f;
			float cy = height * 0.5f - (box[1] + box[3]) * 0.5f;
			cx = MathUtil.clamp(cx, -box[0] + 10f, width - box[2] - 10f);
			cy = MathUtil.clamp(cy, -box[1] + 10f, height - box[3] - 18f);
			root.rootX.target(cx);
			root.rootY.target(cy);
		}
		hovered = null;
		Bubble best = null;
		float bestD = Float.MAX_VALUE;
		boolean pointerFree = drag == null && launching == null;
		for (Bubble b : bubbles) {
			b.update(dt, adt);
			if (pointerFree && !b.dying && b.alpha.value() > 0.3f) {
				float d = MathUtil.dist(mouseX, mouseY, b.x, b.y);
				if (d < b.radiusNow() + 3f && d < bestD) {
					best = b;
					bestD = d;
				}
			}
		}
		// Rings are concentric around the root, so children follow it.
		for (Bubble b : bubbles) {
			if (b.parent != null) {
				Bubble anchor = rootOf(b);
				float a = b.angle + b.spin.value();
				float d = b.dist.value() * layoutScale.value();
				b.x = anchor.x + MathUtil.cos(a) * d + b.shakeOffset();
				b.y = anchor.y + MathUtil.sin(a) * d;
			}
		}
		if (keyboardIndex >= 0) {
			List<Bubble> ring = ringBubbles(outermostRing());
			if (keyboardIndex < ring.size()) {
				best = ring.get(keyboardIndex);
			}
		}
		if (best != hovered && best != null && best.hover.target() < 0.5f) {
			lake.ripple(best.x, best.y, 0.25f * cfg().rippleStrength);
		}
		hovered = best;
		for (Bubble b : bubbles) {
			boolean inPath = openPath.contains(b);
			b.hover.target(b == hovered ? 1f : 0f);
			boolean deeperOpen = b.ring < outermostRing() && b.ring > 0;
			b.dim.target(deeperOpen && !inPath ? 1f : 0f);
			b.selected.target(inPath && b != root ? 1f : 0f);
			float labelOn = b.ring == outermostRing() || b == hovered ? 1f : 0f;
			if (b == root) {
				// A full ring leaves no room under the root; its icon speaks for itself until hovered.
				labelOn = b == hovered || ringBubbles(1).size() <= 4 ? 1f : 0f;
			}
			if (b.dying || launching != null && b != launching) {
				labelOn = 0f;
			}
			b.label.target(labelOn);
		}
		bubbles.removeIf(b -> b.dying && b.alpha.value() < 0.02f && b.scale.value() < 0.05f);
		for (int k = 0; k < ringGuides.length; k++) {
			ringGuides[k].update(adt);
		}
		if (launching != null) {
			launchTimer += dt;
			if (launchTimer > (Motion.reduced() ? 0.08f : 0.3f)) {
				Bubble b = launching;
				launching = null;
				for (Bubble o : bubbles) {
					if (!o.dying) {
						o.alpha.target(1f);
					}
				}
				b.scale.target(1f);
				host.run(b.node.action);
			}
		}
		// Tongues.
		if (drag != null) {
			drag.update(adt, mouseX, mouseY, pullThreshold(drag.edge));
		}
		for (int i = tongues.size() - 1; i >= 0; i--) {
			Tongue t = tongues.get(i);
			t.update(adt, mouseX, mouseY, pullThreshold(t.edge));
			if (t.finished()) {
				tongues.remove(i);
			}
		}
	}

	private Bubble rootOf(Bubble b) {
		Bubble r = b;
		while (r.parent != null) {
			r = r.parent;
		}
		return r;
	}

	private List<Bubble> ringBubbles(int ring) {
		List<Bubble> out = new ArrayList<>();
		for (Bubble b : bubbles) {
			if (b.ring == ring && !b.dying) {
				out.add(b);
			}
		}
		return out;
	}

	/** Bounds of the open menu relative to the root center: {minX, minY, maxX, maxY}. */
	private float[] compositionBounds() {
		float minX = -BUBBLE_RADIUS[0];
		float minY = -BUBBLE_RADIUS[0];
		float maxX = BUBBLE_RADIUS[0];
		float maxY = BUBBLE_RADIUS[0] + 14f;
		float s = layoutScale.value();
		for (Bubble b : bubbles) {
			if (b.dying || b.parent == null) {
				continue;
			}
			float d = RING_RADIUS[Math.min(b.ring, RING_RADIUS.length - 1)];
			float r = BUBBLE_RADIUS[Math.min(b.ring, BUBBLE_RADIUS.length - 1)] + 12f;
			float x = MathUtil.cos(b.angle) * d;
			float y = MathUtil.sin(b.angle) * d;
			minX = Math.min(minX, x - r - (MathUtil.cos(b.angle) < -0.38f ? 40f : 0f));
			maxX = Math.max(maxX, x + r + (MathUtil.cos(b.angle) > 0.38f ? 40f : 0f));
			minY = Math.min(minY, y - r);
			maxY = Math.max(maxY, y + r);
		}
		return new float[] {minX * s, minY * s, maxX * s, maxY * s};
	}

	private float pullThreshold(Edge e) {
		float dim = e == Edge.TOP || e == Edge.BOTTOM ? height : width;
		return MathUtil.clamp(dim * 0.16f, 40f, 64f);
	}

	// ----------------------------------------------------------------------------------------------------------
	// Input
	// ----------------------------------------------------------------------------------------------------------

	private Edge edgeAt(float x, float y) {
		float zone = 26f;
		Edge best = null;
		float bestD = Float.MAX_VALUE;
		for (Handle h : handles.values()) {
			float d;
			switch (h.edge) {
				case TOP -> d = y;
				case BOTTOM -> d = height - y;
				case LEFT -> d = x;
				default -> d = width - x;
			}
			float near = MathUtil.dist(x, y, h.x, h.y);
			if ((d < zone || near < 46f) && d < bestD) {
				best = h.edge;
				bestD = d;
			}
		}
		return best;
	}

	@Override
	public boolean mouseDown(float x, float y, int button, int mods) {
		if (super.mouseDown(x, y, button, mods)) {
			return true;
		}
		keyboardIndex = -1;
		if (launching != null) {
			return true;
		}
		if (button == Keys.MOUSE_RIGHT) {
			back();
			return true;
		}
		pressX = x;
		pressY = y;
		pressMoved = false;
		pressEdge = null;
		if (hovered != null) {
			return true;
		}
		Edge e = edgeAt(x, y);
		if (e != null && (openEdge != e)) {
			pressEdge = e;
			Handle h = handles.get(e);
			float bx = e == Edge.TOP || e == Edge.BOTTOM ? MathUtil.clamp(x, width * 0.15f, width * 0.85f) : h.x;
			float by = e == Edge.LEFT || e == Edge.RIGHT ? MathUtil.clamp(y, height * 0.15f, height * 0.85f) : h.y;
			drag = new Tongue(e, h.node, bx - e.nx * 6f, by - e.ny * 6f);
			drag.hx.snap(bx + e.nx * 4f);
			drag.hy.snap(by + e.ny * 4f);
			return true;
		}
		theme.pointerPressed(x, y);
		return true;
	}

	@Override
	public boolean mouseDragged(float x, float y, int button, float dx, float dy) {
		if (MathUtil.dist(x, y, pressX, pressY) > 4f) {
			pressMoved = true;
		}
		return drag != null;
	}

	@Override
	public boolean mouseUp(float x, float y, int button, int mods) {
		if (super.mouseUp(x, y, button, mods)) {
			return true;
		}
		if (button != Keys.MOUSE_LEFT) {
			return false;
		}
		if (drag != null) {
			Tongue t = drag;
			drag = null;
			float threshold = pullThreshold(t.edge);
			boolean flick = t.pull > threshold * 0.35f && t.pullVelocity > 260f;
			if (t.pull >= threshold || flick || !pressMoved) {
				// Released far enough (or simply clicked): the drop detaches and becomes the menu.
				float hx = pressMoved ? t.hx.value() : t.bx + t.edge.nx * 40f;
				float hy = pressMoved ? t.hy.value() : t.by + t.edge.ny * 40f;
				openMenu(t.edge, hx, hy);
				t.mode = Tongue.DETACH;
				t.attached = root;
			} else {
				t.mode = Tongue.RETRACT;
				lake.ripple(t.bx + t.edge.nx * 10f, t.by + t.edge.ny * 10f, 0.4f);
			}
			t.time = 0f;
			tongues.add(t);
			return true;
		}
		if (hovered != null && !pressMoved) {
			select(hovered);
			return true;
		}
		if (!pressMoved && openEdge != null && hovered == null) {
			closeMenu();
			return true;
		}
		return false;
	}

	private void back() {
		if (openEdge == null) {
			return;
		}
		if (outermostRing() > 1) {
			collapseFrom(outermostRing() - 2);
			host.playSound(UiSound.CLOSE);
		} else {
			closeMenu();
		}
	}

	@Override
	public boolean keyDown(int key, int scancode, int mods) {
		if (super.keyDown(key, scancode, mods)) {
			return true;
		}
		if (launching != null) {
			return true;
		}
		if (key == Keys.ESCAPE || key == Keys.BACKSPACE) {
			back();
			return true;
		}
		if (openEdge == null) {
			Edge e = switch (key) {
				case Keys.UP, Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> Edge.BOTTOM;
				case Keys.DOWN -> Edge.TOP;
				case Keys.LEFT -> Edge.RIGHT;
				case Keys.RIGHT -> Edge.LEFT;
				default -> null;
			};
			if (e != null && handles.containsKey(e)) {
				Handle h = handles.get(e);
				openMenu(e, h.x + e.nx * 40f, h.y + e.ny * 40f);
				keyboardIndex = 0;
				return true;
			}
			return false;
		}
		List<Bubble> ring = ringBubbles(outermostRing());
		if (ring.isEmpty()) {
			return false;
		}
		switch (key) {
			case Keys.LEFT, Keys.UP -> {
				keyboardIndex = keyboardIndex < 0 ? 0 : Math.floorMod(keyboardIndex - 1, ring.size());
				return true;
			}
			case Keys.RIGHT, Keys.DOWN, Keys.TAB -> {
				keyboardIndex = keyboardIndex < 0 ? 0 : Math.floorMod(keyboardIndex + 1, ring.size());
				return true;
			}
			case Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> {
				if (keyboardIndex >= 0 && keyboardIndex < ring.size()) {
					Bubble b = ring.get(keyboardIndex);
					boolean branch = b.node.isBranch();
					select(b);
					if (branch) {
						keyboardIndex = 0;
					}
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	@Override
	public boolean closeOnEscape() {
		return false;
	}

	// ----------------------------------------------------------------------------------------------------------
	// Drawing
	// ----------------------------------------------------------------------------------------------------------

	private String label(MenuNode n) {
		String text = host.hasTranslation(n.label) ? host.tr(n.label) : n.label;
		// Vanilla button labels end with an ellipsis meaning "opens a screen"; bubbles do not need it.
		if (text.endsWith("...")) {
			text = text.substring(0, text.length() - 3);
		} else if (text.endsWith("\u2026")) {
			text = text.substring(0, text.length() - 1);
		}
		return text.strip();
	}

	private void drawRingGuides(Canvas c) {
		Bubble anchor = root;
		if (anchor == null) {
			return;
		}
		LiquidPalette p = lake.palette();
		for (int k = 1; k < ringGuides.length; k++) {
			float g = ringGuides[k].value();
			if (g < 0.01f) {
				continue;
			}
			float r = ringRadius(k) * (0.6f + 0.4f * MathUtil.clamp01(g));
			c.strokeCircle(anchor.x, anchor.y, r, c.pixel() * 1.1f, Argb.withAlpha(p.rim, (p.light ? 0.16f : 0.07f) * MathUtil.clamp01(g)));
		}
	}

	private void drawThreads(Canvas c) {
		LiquidPalette p = lake.palette();
		for (Bubble b : bubbles) {
			if (b.parent == null) {
				continue;
			}
			float a = b.alpha.value() * (1f - 0.6f * b.dim.value());
			if (a < 0.02f) {
				continue;
			}
			Bubble from = b.parent;
			float strength = 0.10f + 0.22f * b.selected.value() + 0.12f * b.hover.value();
			int color = Argb.lerp(Argb.withAlpha(p.rim, (p.light ? 1.6f : 1f) * strength * a), Argb.withAlpha(p.accent, 0.5f * a), b.selected.value());
			thread(c, from.x, from.y, from.radiusNow(), b.x, b.y, b.radiusNow(), color);
		}
	}

	private final float[] threadLeft = new float[2 * 18];
	private final float[] threadRight = new float[2 * 18];

	/** A thin liquid filament between two bubbles, pinched in the middle by surface tension. */
	private void thread(Canvas c, float x0, float y0, float r0, float x1, float y1, float r1, int color) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float len = MathUtil.length(dx, dy);
		if (len < r0 + r1 + 2f) {
			return;
		}
		float ux = dx / len;
		float uy = dy / len;
		float nxv = -uy;
		float nyv = ux;
		float sx = x0 + ux * r0 * 0.8f;
		float sy = y0 + uy * r0 * 0.8f;
		float ex = x1 - ux * r1 * 0.8f;
		float ey = y1 - uy * r1 * 0.8f;
		int n = 18;
		for (int i = 0; i < n; i++) {
			float t = (float) i / (n - 1);
			float px = MathUtil.lerp(sx, ex, t);
			float py = MathUtil.lerp(sy, ey, t);
			float pinch = 1f - MathUtil.sin(t * MathUtil.PI);
			float w = 0.35f + 1.9f * pinch * pinch;
			threadLeft[i * 2] = px + nxv * w;
			threadLeft[i * 2 + 1] = py + nyv * w;
			threadRight[i * 2] = px - nxv * w;
			threadRight[i * 2 + 1] = py - nyv * w;
		}
		c.fillStrip(threadLeft, threadRight, n, null, color);
	}

	private void drawBubble(Canvas c, Bubble b) {
		float a = b.alpha.value() * (1f - 0.55f * b.dim.value()) * (b.available ? 1f : 0.55f);
		float r = b.radiusNow() * (1f - 0.12f * b.dim.value()) * (1f + 0.1f * b.hover.value());
		if (a < 0.01f || r < 0.5f) {
			return;
		}
		LiquidPalette p = lake.palette();
		float x = b.x;
		float y = b.y;
		c.pushAlpha(a);
		float hov = b.hover.value();
		float sel = b.selected.value();
		if (hov > 0.01f || sel > 0.01f) {
			c.glow(x, y, r * 2.6f, Argb.withAlpha(p.accent, 0.2f * hov + 0.12f * sel));
		}
		if (p.light) {
			c.glow(x, y + r * 0.35f, r * 1.7f, Argb.withAlpha(p.shadow, 0.16f));
		}
		// Wobbly droplet outline: deformation follows speed, so bubbles jiggle as they travel and calm down.
		int segs = Math.max(28, Math.min(72, (int) (r * 2.2f / c.pixel() / 6f) + 28));
		float wob = MathUtil.clamp(b.speed / 900f, 0f, 0.07f) + 0.006f + 0.01f * hov;
		float[] pts = b.outline(segs, x, y, r, wob, time());
		int core = Argb.withAlpha(p.fill, p.light ? 0.78f : 0.13f + 0.06f * hov);
		int edge = Argb.withAlpha(p.light ? 0xFFFFFFFF : p.fill, p.light ? 0.5f : 0.035f);
		if (b == root) {
			core = Argb.withAlpha(p.accent, p.light ? 0.85f : 0.26f + 0.08f * hov);
			edge = Argb.withAlpha(p.accent, p.light ? 0.55f : 0.08f);
		}
		paint.setRadial(x - r * 0.3f, y - r * 0.35f, r * 1.25f, core, edge);
		c.fillPolygon(pts, segs, paint);
		int rim = Argb.lerp(Argb.withAlpha(p.rim, (p.light ? 0.9f : 0.42f) + 0.35f * hov), Argb.withAlpha(p.accent, 0.95f), sel);
		c.strokePolyline(pts, segs, true, c.pixel() * 1.4f, rim, Canvas.Cap.BUTT);
		// Specular highlight on the upper left and a faint reflection on the lower right.
		c.strokeArc(x, y, r * 0.72f, (float) Math.toRadians(195), (float) Math.toRadians(245), c.pixel() * 1.6f,
			Argb.withAlpha(0xFFFFFFFF, p.light ? 0.9f : 0.55f), Canvas.Cap.ROUND);
		c.strokeArc(x, y, r * 0.8f, (float) Math.toRadians(20), (float) Math.toRadians(55), c.pixel() * 1.2f,
			Argb.withAlpha(0xFFFFFFFF, p.light ? 0.5f : 0.16f), Canvas.Cap.ROUND);
		int iconColor = b == root && p.light ? 0xFFFFFFFF : Argb.withAlpha(p.text, 0.92f);
		Icon.byId(b.node.icon).draw(c, x, y, r * 0.92f, iconColor, 2f);
		c.popAlpha();
	}

	private void drawLabels(Canvas c) {
		if (!cfg().labels) {
			return;
		}
		LiquidPalette p = lake.palette();
		float fh = c.fontHeight();
		for (Bubble b : bubbles) {
			float a = b.label.value() * b.alpha.value() * (1f - 0.6f * b.dim.value());
			if (a < 0.02f) {
				continue;
			}
			String text = label(b.node);
			if (!b.available && b == hovered) {
				String reason = host.unavailableReason(b.node.action);
				if (reason != null && !reason.isEmpty()) {
					text = reason;
				}
			}
			float r = b.radiusNow() * (1f + 0.1f * b.hover.value());
			int color = Argb.withAlpha(Argb.lerp(p.textMuted, p.text, Math.max(b.hover.value(), b == root ? 1f : 0f)), a);
			float lx;
			float ly;
			Align align;
			if (b == root) {
				lx = b.x;
				ly = b.y + r + 6f;
				align = Align.CENTER;
				if (outermostRing() >= 1 && hasChildBelow()) {
					// A child sits right below the root: put the root label above instead.
					ly = b.y - r - 6f - fh;
				}
			} else {
				Bubble anchor = rootOf(b);
				float ang = MathUtil.atan2(b.y - anchor.y, b.x - anchor.x);
				float ca = MathUtil.cos(ang);
				float sa = MathUtil.sin(ang);
				if (ca > 0.42f) {
					lx = b.x + r + 6f;
					ly = b.y - fh * 0.5f;
					align = Align.LEFT;
				} else if (ca < -0.42f) {
					lx = b.x - r - 6f;
					ly = b.y - fh * 0.5f;
					align = Align.RIGHT;
				} else {
					lx = b.x;
					ly = sa < 0f ? b.y - r - 5f - fh : b.y + r + 5f;
					align = Align.CENTER;
				}
			}
			float lift = (1f - b.label.value()) * 3f;
			c.text(text, lx, ly + lift, color, 1f, align, false);
		}
	}

	private boolean hasChildBelow() {
		for (Bubble b : bubbles) {
			if (b.ring == 1 && !b.dying && MathUtil.sin(b.angle) > 0.9f) {
				return true;
			}
		}
		return false;
	}

	private void drawHandles(Canvas c, float dt) {
		LiquidPalette p = lake.palette();
		for (Handle h : handles.values()) {
			Edge e = h.edge;
			boolean busy = openEdge == e || drag != null && drag.edge == e;
			float near = 1f - MathUtil.clamp01((MathUtil.dist(mouseX, mouseY, h.x, h.y) - 30f) / 90f);
			boolean zone = edgeAt(mouseX, mouseY) == e;
			float approach = openEdge != null || launching != null ? 0f : near * 0.6f;
			h.hover.target(busy ? 0f : Math.max(approach, zone ? 1f : 0f)).update(dt);
			h.visible.target(busy ? 0f : 1f).update(dt);
			float vis = h.visible.value();
			if (vis < 0.01f) {
				continue;
			}
			float hov = h.hover.value();
			float breathe = 0.5f + 0.5f * MathUtil.sin(time() * 1.4f + e.ordinal() * 1.7f);
			float a = vis * (0.30f + 0.10f * breathe + 0.55f * hov);
			float len = 22f + 12f * hov;
			float tx = -e.ny;
			float ty = e.nx;
			float cx = h.x + e.nx * 7f;
			float cy = h.y + e.ny * 7f;
			c.line(cx - tx * len * 0.5f, cy - ty * len * 0.5f, cx + tx * len * 0.5f, cy + ty * len * 0.5f, 2.4f,
				Argb.withAlpha(p.rim, a), Canvas.Cap.ROUND);
			if (hov > 0.01f) {
				c.glow(cx, cy, 40f, Argb.withAlpha(p.accent, 0.18f * hov * vis));
			}
			float ix = h.x + e.nx * 19f;
			float iy = h.y + e.ny * 19f;
			Icon.byId(h.node.icon).draw(c, ix, iy, 10f + 2f * hov, Argb.withAlpha(p.text, vis * (0.32f + 0.6f * hov)), 2.2f);
			if (hov > 0.02f) {
				String text = label(h.node);
				float la = vis * hov;
				int color = Argb.withAlpha(p.text, 0.85f * la);
				float shift = (1f - hov) * 4f;
				switch (e) {
					case BOTTOM -> c.text(text, ix, iy - 18f - shift, color, 1f, Align.CENTER, false);
					case TOP -> c.text(text, ix, iy + 10f + shift, color, 1f, Align.CENTER, false);
					case LEFT -> c.text(text, ix + 11f + shift, iy - c.fontHeight() * 0.5f, color, 1f, Align.LEFT, false);
					case RIGHT -> c.text(text, ix - 11f - shift, iy - c.fontHeight() * 0.5f, color, 1f, Align.RIGHT, false);
				}
			}
		}
	}

	private void drawHints(Canvas c) {
		LiquidPalette p = lake.palette();
		float ha = hintAlpha.value();
		if (ha > 0.01f) {
			float pulse = 0.75f + 0.25f * MathUtil.sin(time() * 1.2f);
			c.textTracked(host.tr("gentleui.hint.drag"), width * 0.5f, height - 52f, Argb.withAlpha(p.textMuted, 0.75f * ha * pulse), 1f, 0.6f, Align.CENTER);
		}
		float ma = menuHintAlpha.value();
		if (ma > 0.01f) {
			c.text(host.tr("gentleui.hint.menu"), 8f, 8f, Argb.withAlpha(p.textMuted, 0.6f * ma), 1f, Align.LEFT, false);
		}
	}

	private final float[] stripLeft = new float[2 * 48];
	private final float[] stripRight = new float[2 * 48];
	private final int[] stripColors = new int[48];
	private final float[] outline = new float[2 * 48];

	private void drawTongue(Canvas c, Tongue t) {
		LiquidPalette p = lake.palette();
		float bx = t.bx;
		float by = t.by;
		float hx = t.hx.value();
		float hy = t.hy.value();
		float thin = t.widthScale();
		if (thin <= 0.01f) {
			return;
		}
		if (t.mode == Tongue.DETACH && t.attached != null) {
			hx = t.attached.x;
			hy = t.attached.y;
		}
		float th = pullThreshold(t.edge);
		float k = MathUtil.smoothstep(0f, th * 1.4f, t.pull);
		float baseW = MathUtil.lerp(26f, 15f, k) * thin;
		float neckW = MathUtil.lerp(12f, 3.2f, k) * t.neckScale();
		float headR = t.mode == Tongue.DETACH ? 0f : t.headRadius();
		// Centerline: a quadratic curve leaving the shore along its normal and bending toward the head.
		float len = MathUtil.dist(bx, by, hx, hy);
		float cxp = bx + t.edge.nx * len * 0.55f;
		float cyp = by + t.edge.ny * len * 0.55f;
		int body = 32;
		int cap = headR > 0.5f ? 12 : 0;
		int n = body + cap;
		float lastTx = 0f;
		float lastTy = 0f;
		for (int i = 0; i < body; i++) {
			float s = (float) i / (body - 1);
			float u = 1f - s;
			float px = u * u * bx + 2f * u * s * cxp + s * s * hx;
			float py = u * u * by + 2f * u * s * cyp + s * s * hy;
			float tx = 2f * u * (cxp - bx) + 2f * s * (hx - cxp);
			float ty = 2f * u * (cyp - by) + 2f * s * (hy - cyp);
			float tl = MathUtil.length(tx, ty);
			if (tl > 1e-4f) {
				tx /= tl;
				ty /= tl;
			} else {
				tx = t.edge.nx;
				ty = t.edge.ny;
			}
			lastTx = tx;
			lastTy = ty;
			float neck = MathUtil.lerp(baseW, neckW, MathUtil.smoothstep(0f, 0.75f, s));
			float dh = MathUtil.dist(px, py, hx, hy);
			float head = headR > dh ? MathUtil.sqrt(headR * headR - dh * dh) : 0f;
			float w = smoothMax(neck, head, 6f);
			if (t.mode == Tongue.DETACH) {
				w *= 1f - MathUtil.smoothstep(0.55f, 1f, s);
			}
			setStrip(i, px, py, -ty, tx, w, Argb.lerp(Argb.withAlpha(p.accent, p.light ? 0.55f : 0.30f), Argb.withAlpha(p.fill, p.light ? 0.75f : 0.16f), s));
		}
		for (int j = 1; j <= cap; j++) {
			float e = headR * j / cap;
			float px = hx + lastTx * e;
			float py = hy + lastTy * e;
			float w = MathUtil.sqrt(Math.max(0f, headR * headR - e * e));
			setStrip(body + j - 1, px, py, -lastTy, lastTx, w, Argb.withAlpha(p.fill, p.light ? 0.75f : 0.16f));
		}
		c.pushAlpha(t.alpha());
		c.fillStrip(stripLeft, stripRight, n, stripColors, 0);
		int rim = Argb.withAlpha(p.rim, p.light ? 0.85f : 0.45f);
		for (int i = 0; i < n; i++) {
			outline[i * 2] = stripLeft[i * 2];
			outline[i * 2 + 1] = stripLeft[i * 2 + 1];
		}
		c.strokePolyline(outline, n, false, c.pixel() * 1.3f, rim, Canvas.Cap.BUTT);
		for (int i = 0; i < n; i++) {
			outline[i * 2] = stripRight[i * 2];
			outline[i * 2 + 1] = stripRight[i * 2 + 1];
		}
		c.strokePolyline(outline, n, false, c.pixel() * 1.3f, rim, Canvas.Cap.BUTT);
		if (headR > 4f && t.mode != Tongue.DETACH) {
			float ready = MathUtil.smoothstep(th * 0.6f, th, t.pull);
			c.strokeArc(hx, hy, headR * 0.7f, (float) Math.toRadians(195), (float) Math.toRadians(245), c.pixel() * 1.6f,
				Argb.withAlpha(0xFFFFFFFF, p.light ? 0.9f : 0.5f), Canvas.Cap.ROUND);
			Icon.byId(t.node.icon).draw(c, hx, hy, headR * 0.9f, Argb.withAlpha(p.text, 0.4f + 0.55f * ready), 2f);
			if (ready > 0.02f) {
				c.glow(hx, hy, headR * 2.5f, Argb.withAlpha(p.accent, 0.22f * ready));
				String text = label(t.node);
				c.text(text, hx, hy - headR - 6f - c.fontHeight() + (t.edge == Edge.TOP ? headR * 2f + 12f + c.fontHeight() : 0f),
					Argb.withAlpha(p.text, 0.9f * ready), 1f, Align.CENTER, false);
			}
		}
		c.popAlpha();
	}

	private void setStrip(int i, float px, float py, float nx, float ny, float w, int color) {
		stripLeft[i * 2] = px + nx * w;
		stripLeft[i * 2 + 1] = py + ny * w;
		stripRight[i * 2] = px - nx * w;
		stripRight[i * 2 + 1] = py - ny * w;
		stripColors[i] = color;
	}

	private static float smoothMax(float a, float b, float k) {
		float h = Math.max(k - Math.abs(a - b), 0f) / k;
		return Math.max(a, b) + h * h * k * 0.25f;
	}

	// ----------------------------------------------------------------------------------------------------------
	// Model
	// ----------------------------------------------------------------------------------------------------------

	private static final class Handle {
		Edge edge;
		MenuNode node;
		float x;
		float y;
		final Spring hover = new Spring(0f, 200f, 1f);
		final Spring visible = new Spring(1f, 120f, 1f);
	}

	private final class Bubble {
		final MenuNode node;
		final Bubble parent;
		final int ring;
		final float angle;
		final Spring rootX = new Spring(0f, 150f, 0.82f);
		final Spring rootY = new Spring(0f, 150f, 0.82f);
		final Spring dist = new Spring(0f, 150f, 0.72f);
		final Spring spin = new Spring(0f, 110f, 0.8f);
		final Spring scale = new Spring(0f, 210f, 0.6f);
		final Spring alpha = new Spring(0f, 120f, 1f);
		final Spring hover = new Spring(0f, 320f, 0.9f);
		final Spring dim = new Spring(0f, 140f, 1f);
		final Spring selected = new Spring(0f, 160f, 1f);
		final Spring label = new Spring(0f, 110f, 1f);
		final float phase;
		float delay;
		boolean pendingStart;
		float x;
		float y;
		float lastX;
		float lastY;
		float speed;
		float shake;
		boolean dying;
		boolean available = true;
		private float[] pts = new float[0];

		Bubble(MenuNode node, Bubble parent, int ring, float angle) {
			this.node = node;
			this.parent = parent;
			this.ring = ring;
			this.angle = angle;
			this.phase = MathUtil.hash01(node.id.hashCode()) * MathUtil.TAU;
		}

		float radiusNow() {
			return bubbleRadius(ring) * Math.max(0f, scale.value());
		}

		void die() {
			dying = true;
			alpha.target(0f);
			scale.target(0f);
			label.target(0f);
			if (parent != null) {
				dist.target(ring == 1 ? 0f : RING_RADIUS[ring - 1]);
				spin.target(ring == 1 ? -0.4f : MathUtil.wrapAngle(parent.angle - angle));
			}
		}

		float shakeOffset() {
			return shake <= 0f ? 0f : MathUtil.sin(shake * 40f) * 4f * shake;
		}

		void update(float dt, float adt) {
			if (pendingStart) {
				delay -= adt;
				if (delay <= 0f) {
					pendingStart = false;
				}
				if (!pendingStart && !dying) {
					dist.target(RING_RADIUS[Math.min(ring, RING_RADIUS.length - 1)]);
					spin.target(0f);
					scale.target(1f);
					alpha.target(1f);
				}
			}
			shake = Math.max(0f, shake - dt * 2.2f);
			rootX.update(adt);
			rootY.update(adt);
			dist.update(adt);
			spin.update(adt);
			scale.update(adt);
			alpha.update(adt);
			hover.update(dt);
			dim.update(adt);
			selected.update(adt);
			label.update(adt);
			if (parent == null) {
				x = rootX.value() + shakeOffset();
				y = rootY.value();
			}
			float v = dt > 0f ? MathUtil.dist(x, y, lastX, lastY) / dt : 0f;
			speed = MathUtil.approach(speed, v, 0.08f, dt);
			lastX = x;
			lastY = y;
		}

		float[] outline(int segs, float cx, float cy, float r, float wob, float t) {
			if (pts.length < segs * 2) {
				pts = new float[segs * 2];
			}
			for (int i = 0; i < segs; i++) {
				float a = MathUtil.TAU * i / segs;
				float k = 1f + wob * MathUtil.sin(3f * a + t * 2.3f + phase) + wob * 0.6f * MathUtil.sin(5f * a - t * 1.7f + phase * 1.3f);
				pts[i * 2] = cx + MathUtil.cos(a) * r * k;
				pts[i * 2 + 1] = cy + MathUtil.sin(a) * r * k;
			}
			return pts;
		}
	}

	private final class Tongue {
		static final int DRAG = 0;
		static final int RETRACT = 1;
		static final int DETACH = 2;

		final Edge edge;
		final MenuNode node;
		final float bx;
		final float by;
		final Spring hx = new Spring(0f, 260f, 0.7f);
		final Spring hy = new Spring(0f, 260f, 0.7f);
		int mode = DRAG;
		float time;
		float pull;
		float pullVelocity;
		Bubble attached;

		Tongue(Edge edge, MenuNode node, float bx, float by) {
			this.edge = edge;
			this.node = node;
			this.bx = bx;
			this.by = by;
		}

		float headRadius() {
			float th = pullThreshold(edge);
			return MathUtil.lerp(7f, 19f, MathUtil.smoothstep(0f, th * 1.2f, pull)) * widthScale();
		}

		float widthScale() {
			return switch (mode) {
				case RETRACT -> 1f - MathUtil.smoothstep(0.1f, 0.45f, time);
				case DETACH -> 1f - MathUtil.smoothstep(0f, 0.42f, time);
				default -> 1f;
			};
		}

		float neckScale() {
			return mode == DETACH ? 1f - MathUtil.smoothstep(0f, 0.18f, time) : widthScale();
		}

		float alpha() {
			return mode == DRAG ? 1f : widthScale();
		}

		boolean finished() {
			return mode != DRAG && time > 0.5f;
		}

		void update(float dt, float mx, float my, float threshold) {
			time += dt;
			if (mode == DRAG) {
				float rx = mx - bx;
				float ry = my - by;
				float d = Math.max(0f, rx * edge.nx + ry * edge.ny);
				float lateral = rx * -edge.ny + ry * edge.nx;
				float soft = threshold * 1.6f;
				if (d > soft) {
					d = soft + (d - soft) * 0.3f;
				}
				lateral *= 0.6f;
				hx.target(bx + edge.nx * d + -edge.ny * lateral);
				hy.target(by + edge.ny * d + edge.nx * lateral);
			} else if (mode == RETRACT) {
				hx.target(bx);
				hy.target(by);
			}
			hx.update(dt);
			hy.update(dt);
			float before = pull;
			pull = Math.max(0f, (hx.value() - bx) * edge.nx + (hy.value() - by) * edge.ny);
			if (dt > 0f) {
				pullVelocity = MathUtil.approach(pullVelocity, (pull - before) / dt, 0.03f, dt);
			}
		}
	}
}
