package io.github.alexwhitehk.gentleui.core.config;

/** A user-placed element on the title screen, created and arranged in the layout editor. */
public final class CustomElement {
	public static final String TEXT = "text";
	public static final String IMAGE = "image";
	public static final String CLOCK = "clock";
	public static final String BUTTON = "button";
	public static final String SHAPE = "shape";

	public String id = "";
	public String type = TEXT;
	/** One of top_left, top, top_right, left, center, right, bottom_left, bottom, bottom_right. */
	public String anchor = "center";
	/** Offset of the element's center from the anchor point, in GUI units. */
	public float x;
	public float y;
	public float w = 80f;
	public float h = 20f;
	public float scale = 1f;
	public float opacity = 1f;
	public float rotation;
	public String text = "";
	/** For images: "file:name.png" inside config/gentleui/images, or a resource id. */
	public String image = "";
	public String icon = "";
	public String action = "";
	public String color = "";
	public float radius = 8f;

	public CustomElement copy() {
		CustomElement e = new CustomElement();
		e.id = id;
		e.type = type;
		e.anchor = anchor;
		e.x = x;
		e.y = y;
		e.w = w;
		e.h = h;
		e.scale = scale;
		e.opacity = opacity;
		e.rotation = rotation;
		e.text = text;
		e.image = image;
		e.icon = icon;
		e.action = action;
		e.color = color;
		e.radius = radius;
		return e;
	}
}
