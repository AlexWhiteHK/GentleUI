package io.github.alexwhitehk.gentleui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.CustomElement;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.config.LiquidConfig;
import io.github.alexwhitehk.gentleui.core.config.MenuNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
	@TempDir
	Path dir;

	@Test
	void roundTrip() {
		ConfigStore store = ConfigStore.init(dir);
		store.update(c -> {
			c.theme = "glass";
			c.animationSpeed = 1.5f;
			CustomElement e = new CustomElement();
			e.id = "t1";
			e.text = "Hello";
			c.layout("glass").custom.add(e);
			c.layout("glass").override("dock").dx = 12f;
		});
		ConfigStore reloaded = ConfigStore.init(dir);
		GentleConfig c = ConfigStore.config();
		assertEquals("glass", c.theme);
		assertEquals(1.5f, c.animationSpeed);
		assertEquals("Hello", c.layout("glass").custom.get(0).text);
		assertEquals(12f, c.layout("glass").overrideOrNull("dock").dx);
		assertNotNull(reloaded);
	}

	@Test
	void sanitizeClampsAndRepairs() {
		GentleConfig c = new GentleConfig();
		c.animationSpeed = 99f;
		c.detail = Float.NaN;
		c.liquid = null;
		c.theme = " ";
		c.sanitize();
		assertEquals(3f, c.animationSpeed);
		assertEquals(0.25f, c.detail);
		assertNotNull(c.liquid.menus);
		assertEquals("liquid", c.theme);
	}

	@Test
	void brokenFileFallsBackToDefaultsAndKeepsBackup() throws Exception {
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("gentleui.json"), "{ this is not json", StandardCharsets.UTF_8);
		ConfigStore.init(dir);
		assertEquals("liquid", ConfigStore.config().theme);
		assertTrue(Files.exists(dir.resolve("gentleui.json.broken")));
	}

	@Test
	void defaultMenusCoverEveryEdgeAndHaveActions() {
		for (String edge : new String[] {LiquidConfig.TOP, LiquidConfig.BOTTOM, LiquidConfig.LEFT, LiquidConfig.RIGHT}) {
			MenuNode root = LiquidConfig.defaultMenus().get(edge);
			assertNotNull(root, edge);
			assertTrue(root.isBranch());
			assertLeavesHaveActions(root);
		}
	}

	private static void assertLeavesHaveActions(MenuNode n) {
		for (MenuNode child : n.children) {
			if (child.isBranch()) {
				assertLeavesHaveActions(child);
			} else {
				assertFalse(child.action.isEmpty(), child.id);
			}
		}
	}
}
