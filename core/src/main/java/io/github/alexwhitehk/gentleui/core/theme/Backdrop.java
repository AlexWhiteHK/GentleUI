package io.github.alexwhitehk.gentleui.core.theme;

/** Per-frame inputs for drawing a theme's full-screen background. */
public final class Backdrop {
	public float width;
	public float height;
	public float mouseX;
	public float mouseY;
	public float time;
	public float dt;
	/** A world is running behind the menu: draw a translucent veil instead of an opaque scene. */
	public boolean inWorld;
	/** 0 = full showcase intensity (title screen), 1 = calm backdrop behind dense content. */
	public float calm;
	/** Whether the pointer may influence the background this frame. */
	public boolean pointerActive = true;
}
