package io.github.alexwhitehk.gentleui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.scene.elements.Anchor;
import io.github.alexwhitehk.gentleui.core.theme.material.Shapes;
import io.github.alexwhitehk.gentleui.core.ui.WidgetRoot;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiscTest {
	@Test
	void shapeOutlinesStayNearTheirRadius() {
		float[] pts = new float[2 * 96];
		for (Shapes.Kind k : Shapes.Kind.values()) {
			Shapes.outline(k, 0f, 0f, 10f, 0.3f, 96, pts);
			for (int i = 0; i < 96; i++) {
				float d = MathUtil.length(pts[i * 2], pts[i * 2 + 1]);
				assertTrue(d > 1f && d <= 10.5f, k + " point at " + d);
			}
		}
	}

	@Test
	void wrapBreaksWordsAndCjk() {
		RecordingCanvas c = new RecordingCanvas();
		List<String> lines = WidgetRoot.wrap(c, "one two three four", 60f);
		for (String l : lines) {
			assertTrue(c.textWidth(l) <= 60f, l);
		}
		assertEquals("one two three four", String.join(" ", lines));
		List<String> cjk = WidgetRoot.wrap(c, "从任意边缘拖出菜单", 24f);
		assertTrue(cjk.size() >= 2);
		assertEquals("从任意边缘拖出菜单", String.join("", cjk));
	}

	@Test
	void actionIdsSplitArguments() {
		assertEquals("connect", Actions.id("connect:play.example.net:25565"));
		assertEquals("play.example.net:25565", Actions.argument("connect:play.example.net:25565"));
		assertEquals("quit", Actions.id("quit"));
		assertEquals("", Actions.argument("quit"));
	}

	@Test
	void nearestAnchorUsesThirds() {
		assertEquals(Anchor.TOP_LEFT, Anchor.nearest(10, 10, 300, 300));
		assertEquals(Anchor.CENTER, Anchor.nearest(150, 150, 300, 300));
		assertEquals(Anchor.BOTTOM_RIGHT, Anchor.nearest(290, 290, 300, 300));
		assertEquals(Anchor.TOP, Anchor.byId("top"));
		assertEquals(Anchor.CENTER, Anchor.byId("nonsense"));
	}
}
