package com.luneruniverse.minecraft.mod.nbteditor.screens.configurable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.InputOverlay;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.StringInput;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ConfigList extends ConfigGroupingVertical<Integer, ConfigList> {
	
	private static class ConfigListEntry implements ConfigPath {
		
		public enum ListContextMenuAction {
			MOVE("nbteditor.configurable.list.move"),
			DUPLICATE("nbteditor.configurable.list.duplicate", "nbteditor.configurable.list.duplicate.any_amount"),
			REMOVE("nbteditor.configurable.list.remove");
			
			private final Component msg;
			private final Component tooltip;
			private ListContextMenuAction(String msg, String tooltip) {
				this.msg = Component.translatableEscape(msg);
				this.tooltip = tooltip == null ? null : Component.translatableEscape(tooltip);
			}
			private ListContextMenuAction(String msg) {
				this(msg, null);
			}
		}
		
		private static final int LIST_CONTEXT_MENU_HEIGHT = (MainUtil.client.font.lineHeight + 2) * 3 + 2;
		
		private final ConfigList parent;
		private final ConfigPath value;
		private final boolean named;
		private final boolean indexed;
		private int index;
		private Component indexText;
		private int indexTextOffset;
		
		private boolean contextMenuOpen;
		private int contextMenuX;
		private int contextMenuY;
		
		public ConfigListEntry(ConfigList parent, ConfigPath value, int index, boolean indexed) {
			this.parent = parent;
			this.value = value;
			this.indexed = indexed;
			this.named = value instanceof ConfigPathNamed;
			setIndex(index);
			
			if (named)
				((ConfigPathNamed) value).setNamePrefix(indexText);
		}
		
		public void setIndex(int index) {
			this.index = index;
			if (indexed) {
				this.indexText = Component.literal("(#" + (index + 1) + ") ");
				this.indexTextOffset = named ? 0 : MainUtil.client.font.width(this.indexText);
				if (named)
					((ConfigPathNamed) value).setNamePrefix(this.indexText);
			} else {
				this.indexText = Component.nullToEmpty("");
				this.indexTextOffset = 0;
			}
		}
		
		public void duplicate(int numCopies) {
			for (int dupe = 0; dupe < numCopies; dupe++) {
				for (int i = parent.paths.size() - 2; i > index; i--) {
					ConfigListEntry entry = parent.getListEntry(i);
					entry.setIndex(i + 1);
					parent.setListEntry(i + 1, entry);
				}
				ConfigListEntry clone = this.clone(false);
				clone.setIndex(index + 1);
				parent.setListEntry(index + 1, clone);
			}
			parent.onChanged.forEach(listener -> listener.onValueChanged(null));
		}
		
		@Override
		public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			if (named)
				value.extractRenderState(context, mouseX, mouseY, delta);
			else {
				MVDrawableHelper.drawTextWithShadow(context, MainUtil.client.font, indexText, 0, (getSpacingHeight() - MainUtil.client.font.lineHeight) / 2, -1);
				context.pose().pushMatrix();
				context.pose().translate((float) (indexTextOffset), (float) (0.0));
				value.extractRenderState(context, mouseX - indexTextOffset, mouseY, delta);
				context.pose().popMatrix();
			}
		}
		
		public void renderContextMenu(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			if (!contextMenuOpen)
				return;
			
			context.pose().pushMatrix();
			context.pose().translate((float) (0.0), (float) (0.0));
			
			MVDrawableHelper.fill(context, contextMenuX - 1, contextMenuY - 1, contextMenuX + 51, contextMenuY + LIST_CONTEXT_MENU_HEIGHT + 1, -1);
			MVDrawableHelper.fill(context, contextMenuX, contextMenuY, contextMenuX + 50, contextMenuY + LIST_CONTEXT_MENU_HEIGHT, 0xFF000000);
			boolean xHover = mouseX > contextMenuX && mouseX < contextMenuX + 50; // Prevent the first option from being hovered before moving the mouse
			int y = contextMenuY;
			for (ListContextMenuAction action : ListContextMenuAction.values()) {
				int color = -1;
				if (xHover && mouseY >= y && mouseY <= y + MainUtil.client.font.lineHeight) {
					color = 0xFF257789;
					if (action.tooltip != null && !ConfigScreen.isKeybindsHidden())
						new MVTooltip(action.tooltip).render(context, mouseX, mouseY);
				}
				Component msg = action.msg;
				if (action == ListContextMenuAction.REMOVE)
					msg = msg.copy().withStyle(color == -1 ? ChatFormatting.RED : ChatFormatting.GOLD);
				MVDrawableHelper.drawCenteredTextWithShadow(context, MainUtil.client.font, msg, contextMenuX + 25, y + 2, color);
				y += MainUtil.client.font.lineHeight + 2;
			}
			
			context.pose().popMatrix();
		}
		
		@Override
		public boolean isValueValid() {
			return value.isValueValid();
		}
		@Override
		public ConfigListEntry addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener) {
			value.addValueListener(listener);
			return this;
		}
		
		@Override
		public int getSpacingWidth() {
			return indexTextOffset + value.getSpacingWidth();
		}
		
		@Override
		public int getSpacingHeight() {
			return Math.max(20, value.getSpacingHeight());
		}
		
		@Override
		public int getRenderWidth() {
			return indexTextOffset + value.getRenderWidth();
		}
		
		@Override
		public int getRenderHeight() {
			return Math.max(getSpacingHeight(), value.getRenderHeight());
		}
		
		@Override
		public ConfigListEntry clone(boolean defaults) {
			return new ConfigListEntry(parent, value.clone(defaults), index, indexed);
		}
		
		
		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
			if (contextMenuOpen) {
				if (mouseX >= contextMenuX && mouseX <= contextMenuX + 50 && mouseY >= contextMenuY && mouseY <= contextMenuY + LIST_CONTEXT_MENU_HEIGHT) {
					if (mouseX > contextMenuX && mouseX < contextMenuX + 50) {
						int y = contextMenuY;
						for (ListContextMenuAction action : ListContextMenuAction.values()) {
							if (mouseY >= y && mouseY <= y + MainUtil.client.font.lineHeight) {
								switch (action) {
									case MOVE -> {
										InputOverlay.show(
												Component.translatableEscape("nbteditor.configurable.list.move"),
												StringInput.builder()
														.withDefault(index + 1 + "")
														.withPlaceholder(
																Component.translatableEscape("nbteditor.configurable.list.move.index"))
														.withValidator(
																MainUtil.intPredicate(() -> 1, () -> parent.paths.size() - 1, false))
														.build(),
												str -> {
													int target = Integer.parseInt(str) - 1;
													if (target == index)
														return;
													int dir = (index < target ? 1 : -1);
													for (int i = index; dir == 1 ? i < target : i > target; i += dir) {
														ConfigListEntry entry = parent.getListEntry(i + dir);
														entry.setIndex(i);
														parent.setListEntry(i, entry);
													}
													setIndex(target);
													parent.setListEntry(target, this);
													parent.onChanged.forEach(listener -> listener.onValueChanged(null));
												});
									}
									case DUPLICATE -> {
										if (Keys.hasShiftDown()) {
											InputOverlay.show(
													Component.translatableEscape("nbteditor.configurable.list.duplicate"),
													StringInput.builder()
															.withPlaceholder(
																	Component.translatableEscape("nbteditor.configurable.list.duplicate.amount"))
															.withValidator(
																	MainUtil.intPredicate(1, Integer.MAX_VALUE, false))
															.build(),
													numCopies -> duplicate(Integer.parseInt(numCopies)));
										} else
											duplicate(1);
									}
									case REMOVE -> {
										for (int i = index; i < parent.paths.size() - 2; i++) {
											ConfigListEntry entry = parent.getListEntry(i + 1);
											entry.setIndex(i);
											parent.setListEntry(i, entry);
										}
										parent.paths.remove(parent.paths.size() - 2);
										parent.onChanged.forEach(listener -> listener.onValueChanged(null));
									}
								}
								contextMenuOpen = false;
								break;
							}
							y += MainUtil.client.font.lineHeight + 2;
						}
					}
					return true;
				} else
					contextMenuOpen = false;
			}
			
			int height = getSpacingHeight();
			if (mouseX >= -PADDING * 2 && mouseX <= -PADDING && mouseY >= 0 && mouseY <= height) { // MouseButtonEvent on the bar
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					if (mouseY <= PADDING) { // Move up
						if (index > 0) {
							ConfigListEntry above = parent.getListEntry(index - 1);
							above.setIndex(index);
							parent.setListEntry(index, above);
							
							setIndex(index - 1);
							parent.setListEntry(index, this);
							parent.onChanged.forEach(listener -> listener.onValueChanged(null));
						}
						return true;
					} else if (mouseY >= height - PADDING) { // Move down
						if (index < parent.paths.size() - 2) { // Account for '+' button
							ConfigListEntry below = parent.getListEntry(index + 1);
							below.setIndex(index);
							parent.setListEntry(index, below);
							
							setIndex(index + 1);
							parent.setListEntry(index, this);
							parent.onChanged.forEach(listener -> listener.onValueChanged(null));
						}
						return true;
					} else {
						contextMenuOpen = true;
						contextMenuX = (int) mouseX;
						contextMenuY = (int) mouseY;
						return true;
					}
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					contextMenuOpen = true;
					contextMenuX = (int) mouseX;
					contextMenuY = (int) mouseY;
					return true;
				}
			}
			
			return value.mouseClicked(new MouseButtonEvent(mouseX - indexTextOffset, mouseY, click.buttonInfo()), doubled);
		}
		@Override
		public boolean mouseReleased(MouseButtonEvent click) {
			double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
			return value.mouseReleased(new MouseButtonEvent(mouseX - indexTextOffset, mouseY, click.buttonInfo()));
		}
		@Override
		public void mouseMoved(double mouseX, double mouseY) {
			value.mouseMoved(mouseX - indexTextOffset, mouseY);
		}
		@Override
		public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
			double mouseX = click.x(); double mouseY = click.y(); int button = click.button();
			return value.mouseDragged(new MouseButtonEvent(mouseX - indexTextOffset, mouseY, click.buttonInfo()), deltaX, deltaY);
		}
		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
			return value.mouseScrolled(mouseX - indexTextOffset, mouseY, xAmount, yAmount);
		}
		
		@Override
		public boolean keyPressed(KeyEvent input) {
			return value.keyPressed(input);
		}
		@Override
		public boolean keyReleased(KeyEvent input) {
			return value.keyReleased(input);
		}
		@Override
		public boolean charTyped(CharacterEvent input) {
			return value.charTyped(input);
		}
		
		@Override
		public void tick() {
			value.tick();
		}
		
	}
	
	private final boolean indexed;
	
	public ConfigList(Component name, boolean indexed, ConfigPath defaultEntry) {
		super(name, name2 -> new ConfigList(name2, indexed, defaultEntry));
		this.indexed = indexed;
		
		super.setSorter((a, b) -> a - b);
		super.setConfigurable(-1, new ConfigButton(20, Component.nullToEmpty("+"), btn -> {
			addConfigurable(defaultEntry.clone(true));
			onChanged.forEach(listener -> listener.onValueChanged(null));
		}));
	}
	public ConfigList(boolean indexed, ConfigPath defaultEntry) {
		this(null, indexed, defaultEntry);
	}
	public ConfigList(ConfigPath defaultEntry) {
		this(null, false, defaultEntry);
	}
	
	@Override
	public ConfigList setConfigurable(Integer key, ConfigPath path) {
		if (key == null)
			throw new IllegalArgumentException("The key cannot be null");
		if (key < 0 || key >= paths.size())
			throw new ArrayIndexOutOfBoundsException(key);
		
		super.setConfigurable(key, new ConfigListEntry(this, path, paths.size() - 1, indexed));
		path.setParent(this);
		return this;
	}
	@Override
	public ConfigPath getConfigurable(Integer key) {
		ConfigPath output = super.getConfigurable(key);
		if (output == null)
			return null;
		return ((ConfigListEntry) output).value;
	}
	@Override
	public Map<Integer, ConfigPath> getConfigurables() {
		return paths.entrySet().stream().filter(entry -> entry.getKey() >= 0).collect(Collectors.toMap(
				Map.Entry::getKey, entry -> ((ConfigListEntry) entry.getValue()).value, (a, b) -> a, LinkedHashMap::new));
	}
	@Override
	public ConfigList sort(Comparator<Integer> sorter) {
		Map<Integer, ConfigPath> indexes = new TreeMap<>(sorter);
		for (int i = 0; i < paths.size() - 1; i++)
			indexes.put(i, paths.get(i));
		int i = 0;
		for (ConfigPath path : indexes.values()) {
			((ConfigListEntry) path).setIndex(i);
			paths.put(i, path);
			i++;
		}
		return this;
	}
	@Override
	public ConfigList setSorter(Comparator<Integer> sorter) {
		throw new UnsupportedOperationException("Lists are always sorted by their index! Use sort instead.");
	}
	
	public ConfigList addConfigurable(ConfigPath path) {
		return setConfigurable(paths.size() - 1, path);
	}
	
	private void setListEntry(int key, ConfigListEntry entry) {
		paths.put(key, entry);
	}
	private ConfigListEntry getListEntry(int key) {
		return (ConfigListEntry) paths.get(key);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		
		if (isValueValid()) {
			int yOffset = getNameHeight();
			for (ConfigPath path : paths.values()) {
				int height = path.getSpacingHeight();
				if (path instanceof ConfigListEntry) {
					MVDrawableHelper.fill(context, 0, yOffset, PADDING, yOffset + height, 0xFF000000);
					MVDrawableHelper.fill(context, 0, yOffset + PADDING, PADDING, yOffset + height - PADDING, 0xFF257789);
					
					context.pose().pushMatrix();
					context.pose().translate((float) (-PADDING / 2), (float) (-(yOffset + height / 2)));
					context.pose().scale((float) (2), (float) (2));
					context.pose().translate((float) (PADDING / 2 - 0.5), (float) (yOffset + height / 2));
					MVDrawableHelper.drawTextWithShadow(context, MainUtil.client.font, Component.nullToEmpty("⋮"), 0,
							-MainUtil.client.font.lineHeight / 2, -1);
					context.pose().popMatrix();
				}
				yOffset += height + PADDING;
			}
		}
		
		int yOffset = getNameHeight();
		for (ConfigPath path : paths.values()) {
			if (path instanceof ConfigListEntry entry) {
				context.pose().pushMatrix();
				context.pose().translate((float) (PADDING * 2), (float) (yOffset));
				entry.renderContextMenu(context, mouseX - PADDING * 2, mouseY - yOffset, delta);
				context.pose().popMatrix();
			}
			yOffset += path.getSpacingHeight() + PADDING;
		}
	}
	
}
