package io.github.alexwhitehk.gentleui.core.render;

public enum Align {
	LEFT(0f),
	CENTER(0.5f),
	RIGHT(1f);

	public final float factor;

	Align(float factor) {
		this.factor = factor;
	}
}
