package io.github.jimbozoomer.jugcraft.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.Nullable;

/**
 * The scrolling body of {@link ComposerScreen}: its widgets, each placed at an offset below the top of the content and
 * drawn only inside the body's own rectangle. The mouse wheel and the scroll bar move it; a widget reached with Tab or
 * the arrow keys is scrolled into view; a click outside the rectangle reaches nothing in it. Unlike vanilla's own
 * scrollable layout it keeps its place when the screen lays its widgets out again (as the composer does after each
 * edit and answer).
 */
final class ComposerScroll extends AbstractContainerWidget {
	private final Minecraft minecraft;
	private final Consumer<@Nullable GuiEventListener> focused;
	private final List<AbstractWidget> children = new ArrayList<>();
	private final List<Integer> offsets = new ArrayList<>();
	private int content;

	ComposerScroll(Minecraft minecraft, int x, int y, int width, int height, Consumer<@Nullable GuiEventListener> focused) {
		super(x, y, width, height, CommonComponents.EMPTY, AbstractScrollArea.defaultSettings(10));
		this.minecraft = minecraft;
		this.focused = focused;
	}

	/** Adds a widget {@code offset} pixels below the top of the content (its x is its own). */
	<T extends AbstractWidget> T add(T widget, int offset) {
		children.add(widget);
		offsets.add(offset);
		content = Math.max(content, offset + widget.getHeight());
		return widget;
	}

	/** Places the children once all are added, scrolled to {@code amount} (kept within range). */
	void arrange(double amount) {
		refreshScrollAmount();
		setScrollAmount(amount);
	}

	private void place() {
		// The scroll area may set its amount while it is being made, before these lists exist.
		if (children == null || offsets == null) {
			return;
		}
		for (int i = 0; i < children.size(); i++) {
			children.get(i).setY(getY() + offsets.get(i) - (int) scrollAmount());
		}
	}

	@Override
	protected int contentHeight() {
		return content;
	}

	@Override
	public void setScrollAmount(double amount) {
		super.setScrollAmount(amount);
		place();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		boolean over = isMouseOver(mouseX, mouseY);
		graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
		for (AbstractWidget child : children) {
			if (child.getY() + child.getHeight() < getY() || child.getY() > getY() + height) {
				continue;
			}
			// Outside the body nothing in it is under the mouse (no hover, no tooltip).
			child.extractRenderState(graphics, over ? mouseX : -1, over ? mouseY : -1, delta);
		}
		graphics.disableScissor();
		extractScrollbar(graphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return isMouseOver(event.x(), event.y()) && super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}

	@Override
	public ScreenRectangle getBorderForArrowNavigation(ScreenDirection opposite) {
		GuiEventListener current = getFocused();
		return current != null ? current.getBorderForArrowNavigation(opposite)
				: new ScreenRectangle(getX(), getY(), width, contentHeight()).getBorder(opposite);
	}

	@Override
	public void setFocused(@Nullable GuiEventListener next) {
		super.setFocused(next);
		if (next != null && minecraft.getLastInputType().isKeyboard()) {
			ScreenRectangle area = getRectangle();
			ScreenRectangle shown = next.getRectangle();
			int above = shown.top() - area.top();
			int below = shown.bottom() - area.bottom();
			if (above < 0) {
				setScrollAmount(scrollAmount() + above - scrollRate());
			} else if (below > 0) {
				setScrollAmount(scrollAmount() + below + scrollRate());
			}
		}
		focused.accept(next);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public Collection<? extends NarratableEntry> getNarratables() {
		return children;
	}
}
