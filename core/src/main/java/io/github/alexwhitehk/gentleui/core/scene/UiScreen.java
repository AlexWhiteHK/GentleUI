package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.anim.Clock;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.render.Canvas;

/**
 * A full screen owned by GentleUI. The platform wraps it in a native screen and forwards rendering and input;
 * coordinates are GUI-scaled units with the origin at the top-left corner.
 */
public abstract class UiScreen {
	protected Host host;
	protected float width;
	protected float height;
	protected float mouseX = -1000f;
	protected float mouseY = -1000f;
	protected final Clock clock = new Clock();
	private boolean opened;

	/** Called by the platform when the screen is shown and whenever the window is resized. */
	public final void attach(Host host, float width, float height) {
		this.host = host;
		this.width = width;
		this.height = height;
		if (!opened) {
			opened = true;
			onOpen();
		}
		layout();
	}

	/** Platform notification that the screen became current again after a screen opened from it closed. */
	public final void resumed() {
		if (opened) {
			onResume();
		}
	}

	/** The screen is shown again, e.g. after a child screen opened from it was closed. */
	protected void onResume() {
	}

	public Host host() {
		return host;
	}

	/** Gives the screen its host before it is first shown (titles and labels may need translations). */
	public final UiScreen bindHost(Host host) {
		this.host = host;
		return this;
	}

	public float width() {
		return width;
	}

	public float height() {
		return height;
	}

	/** First time the screen is shown. */
	protected void onOpen() {
	}

	/** Recompute positions after a resize. */
	protected void layout() {
	}

	/** Renders one frame. */
	public final void frame(Canvas canvas, float mouseX, float mouseY) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		float dt = clock.tick();
		render(canvas, dt);
	}

	protected abstract void render(Canvas canvas, float dt);

	public boolean mouseMoved(float x, float y) {
		return false;
	}

	public boolean mouseDown(float x, float y, int button, int mods) {
		return false;
	}

	public boolean mouseUp(float x, float y, int button, int mods) {
		return false;
	}

	public boolean mouseDragged(float x, float y, int button, float dx, float dy) {
		return false;
	}

	public boolean mouseScrolled(float x, float y, float dx, float dy) {
		return false;
	}

	public boolean keyDown(int key, int scancode, int mods) {
		if (key == Keys.ESCAPE && closeOnEscape()) {
			onClose();
			return true;
		}
		return false;
	}

	public boolean keyUp(int key, int scancode, int mods) {
		return false;
	}

	public boolean charTyped(int codepoint, int mods) {
		return false;
	}

	/** 20 Hz game tick. */
	public void tick() {
	}

	/** The screen was replaced or closed. */
	public void removed() {
	}

	public boolean closeOnEscape() {
		return true;
	}

	public void onClose() {
		host.close();
	}

	public boolean pausesGame() {
		return true;
	}

	public String title() {
		return "GentleUI";
	}
}
