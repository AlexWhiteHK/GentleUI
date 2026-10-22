package io.github.alexwhitehk.gentleui.core.anim;

/**
 * Global motion preferences. Every animated value reads its time scale from here so that users can slow down,
 * speed up or reduce motion everywhere at once.
 */
public final class Motion {
	private static float speed = 1f;
	private static boolean reduced;

	private Motion() {
	}

	public static void configure(float speedMultiplier, boolean reduceMotion) {
		speed = Math.max(0.1f, speedMultiplier);
		reduced = reduceMotion;
	}

	/** Scales a real frame delta into animation time. */
	public static float scale(float dt) {
		return reduced ? dt * 4f : dt * speed;
	}

	public static boolean reduced() {
		return reduced;
	}

	public static float speed() {
		return speed;
	}
}
