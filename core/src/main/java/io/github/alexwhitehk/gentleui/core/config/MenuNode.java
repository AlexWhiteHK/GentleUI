package io.github.alexwhitehk.gentleui.core.config;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of a Liquid edge menu. A node either runs {@link #action} or, when it has children, opens the next
 * ring. {@link #label} is a translation key or literal text.
 */
public final class MenuNode {
	public String id = "";
	public String label = "";
	public String icon = "dot";
	public String action = "";
	public List<MenuNode> children = new ArrayList<>();
	public boolean hidden;

	public MenuNode() {
	}

	public static MenuNode leaf(String id, String label, String icon, String action) {
		MenuNode n = new MenuNode();
		n.id = id;
		n.label = label;
		n.icon = icon;
		n.action = action;
		return n;
	}

	public static MenuNode branch(String id, String label, String icon, MenuNode... kids) {
		MenuNode n = leaf(id, label, icon, "");
		for (MenuNode k : kids) {
			n.children.add(k);
		}
		return n;
	}

	public boolean isBranch() {
		return children != null && !children.isEmpty();
	}

	public List<MenuNode> visibleChildren() {
		List<MenuNode> out = new ArrayList<>();
		if (children != null) {
			for (MenuNode c : children) {
				if (!c.hidden) {
					out.add(c);
				}
			}
		}
		return out;
	}

	public MenuNode copy() {
		MenuNode n = leaf(id, label, icon, action);
		n.hidden = hidden;
		if (children != null) {
			for (MenuNode c : children) {
				n.children.add(c.copy());
			}
		}
		return n;
	}
}
