package io.github.alexwhitehk.gentleui.fabric.screen;

import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import io.github.alexwhitehk.gentleui.fabric.InputMap;
import io.github.alexwhitehk.gentleui.fabric.McHost;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Native screen hosting a GentleUI {@link UiScreen}. All drawing and input is forwarded to the core. */
public class GentleScreen extends Screen {
	private final UiScreen ui;
	private final @Nullable Screen parent;
	private boolean shownBefore;

	public GentleScreen(UiScreen ui, @Nullable Screen parent) {
		super(Component.literal(ui.bindHost(McHost.get()).title()));
		this.ui = ui;
		this.parent = parent;
	}

	public UiScreen ui() {
		return ui;
	}

	public @Nullable Screen parent() {
		return parent;
	}

	@Override
	protected void init() {
		ui.attach(McHost.get(), this.width, this.height);
	}

	@Override
	public void added() {
		if (shownBefore) {
			ui.resumed();
		}
		shownBefore = true;
	}

	@Override
	public void removed() {
		ui.removed();
	}

	@Override
	public void tick() {
		ui.tick();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (this.minecraft != null && this.minecraft.level != null) {
			// Keep the game world softly blurred behind in-game GentleUI screens.
			this.extractBlurredBackground(graphics);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		McCanvas canvas = McCanvas.begin(graphics);
		double scale = this.minecraft.getWindow().getGuiScale();
		// Use the precise pointer position so motion stays smooth at high GUI scales.
		float mx = (float) (this.minecraft.mouseHandler.xpos() * this.width / this.minecraft.getWindow().getScreenWidth());
		float my = (float) (this.minecraft.mouseHandler.ypos() * this.height / this.minecraft.getWindow().getScreenHeight());
		if (Double.isNaN(scale)) {
			mx = mouseX;
			my = mouseY;
		}
		ui.frame(canvas, mx, my);
		canvas.end();
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void mouseMoved(double x, double y) {
		ui.mouseMoved((float) x, (float) y);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return ui.mouseDown((float) event.x(), (float) event.y(), InputMap.button(event.button()), InputMap.mods(event.modifiers()));
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return ui.mouseUp((float) event.x(), (float) event.y(), InputMap.button(event.button()), InputMap.mods(event.modifiers()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return ui.mouseDragged((float) event.x(), (float) event.y(), InputMap.button(event.button()), (float) dx, (float) dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		return ui.mouseScrolled((float) x, (float) y, (float) horizontal, (float) vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return ui.keyDown(InputMap.key(event.key()), event.key(), InputMap.mods(event.modifiers()));
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		return ui.keyUp(InputMap.key(event.key()), event.key(), InputMap.mods(event.modifiers()));
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return ui.charTyped(event.codepoint(), 0);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return ui.closeOnEscape();
	}

	@Override
	public void onClose() {
		ui.onClose();
	}

	/** Returns to the screen this one was opened from. */
	public void closeToParent() {
		this.minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return ui.pausesGame();
	}
}
