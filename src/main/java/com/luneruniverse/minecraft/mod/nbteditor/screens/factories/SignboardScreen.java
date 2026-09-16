package com.luneruniverse.minecraft.mod.nbteditor.screens.factories;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.BlockReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.LocalEditorScreen;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.ButtonDropdownWidget;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.FormattedTextFieldWidget;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.SignSideTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.StyleUtil;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import com.luneruniverse.minecraft.mod.nbteditor.util.Keys;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

public class SignboardScreen<L extends LocalNBT> extends LocalEditorScreen<L> {
	
	// Double sided & waxable
	private static boolean NEW_FEATURES = true;
	
	private static int getRenderedColor(DyeColor dye) {
		if (dye == DyeColor.BLACK)
			return 0xFFF0EBCC;
		return StyleUtil.scaleRgb(dye.getTextColor(), 0.4);
	}
	
	private final Identifier texture;
	private boolean back;
	private FormattedTextFieldWidget lines;
	
	public SignboardScreen(NBTReference<L> ref) {
		super(TextInst.of("Signboard"), ref);
		
		String woodType;
		boolean hanging;
		if (NEW_FEATURES) {
			Block block = null;
			if (ref instanceof ItemReference itemRef)
				block = ((SignItem) itemRef.getItem().getItem()).getBlock();
			else if (ref instanceof BlockReference blockRef)
				block = blockRef.getBlock();
			woodType = SignBlock.getWoodType(block).name();
			hanging = block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock;
		} else {
			String id = ref.getId().getPath();
			woodType = id.replaceAll("(_wall)?(_hanging)?_sign$", "");
			hanging = id.matches("^[a-z_]+_hanging_sign$");
		}
		String textureName;
		if (hanging) {
			textureName = switch (woodType) {
				case "crimson" -> "stripped_crimson_stem";
				case "warped" -> "stripped_warped_stem";
				case "bamboo" -> "bamboo_planks";
				default -> "stripped_" + woodType + "_log";
			};
		} else
			textureName = woodType + "_planks";
		texture = Identifier.fromNamespaceAndPath("minecraft", "textures/block/" + textureName + ".png");
		
		if (localNBT instanceof LocalItem localItem) {
			CompoundTag nbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(localItem.getEditableItem());
			nbt.putString("id",
					localItem.getItemType() instanceof HangingSignItem ? "minecraft:hanging_sign" : "minecraft:sign");
			ItemTagReferences.BLOCK_ENTITY_DATA.set(localItem.getEditableItem(), nbt);
		}
	}
	
	private CompoundTag getSideNbt() {
		CompoundTag nbt;
		if (localNBT instanceof LocalItem localItem)
			nbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(localItem.getEditableItem());
		else {
			nbt = localNBT.getNBT();
			if (nbt == null)
				return new CompoundTag();
		}
		
		if (NEW_FEATURES)
			return nbt.nbte$getCompoundOrDefault(back ? "back_text" : "front_text");
		return nbt;
	}
	private void setSideNbt(CompoundTag sideNbt) {
		if (!NEW_FEATURES) {
			if (localNBT instanceof LocalItem localItem)
				ItemTagReferences.BLOCK_ENTITY_DATA.set(localItem.getEditableItem(), sideNbt);
			else
				localNBT.setNBT(sideNbt);
			return;
		}
		
		if (localNBT instanceof LocalItem localItem) {
			CompoundTag nbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(localItem.getEditableItem());
			nbt.put(back ? "back_text" : "front_text", sideNbt);
			ItemTagReferences.BLOCK_ENTITY_DATA.set(localItem.getEditableItem(), nbt);
		} else {
			CompoundTag nbt = localNBT.getNBT();
			nbt.put(back ? "back_text" : "front_text", sideNbt);
			localNBT.setNBT(nbt);
		}
	}
	private void modifySideNbt(Consumer<CompoundTag> modifier) {
		CompoundTag sideNbt = getSideNbt();
		modifier.accept(sideNbt);
		setSideNbt(sideNbt);
	}
	
	private void setWaxed(boolean waxed) {
		if (!NEW_FEATURES)
			throw new IllegalStateException("Incorrect version!");
		
		if (localNBT instanceof LocalItem localItem) {
			CompoundTag nbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(localItem.getEditableItem());
			nbt.putBoolean("is_waxed", waxed);
			ItemTagReferences.BLOCK_ENTITY_DATA.set(localItem.getEditableItem(), nbt);
		} else {
			CompoundTag nbt = localNBT.getNBT();
			nbt.putBoolean("is_waxed", waxed);
			localNBT.setNBT(nbt);
		}
		checkSave();
	}
	private boolean isWaxed() {
		CompoundTag nbt;
		if (localNBT instanceof LocalItem localItem)
			nbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(localItem.getEditableItem());
		else
			nbt = localNBT.getNBT();
		return nbt != null && nbt.nbte$getBooleanOrDefault("is_waxed");
	}
	
	private void setGlowing(boolean glowing) {
		modifySideNbt(nbt -> SignSideTagReferences.GLOWING.set(nbt, glowing));
		checkSave();
	}
	private boolean isGlowing() {
		return SignSideTagReferences.GLOWING.get(getSideNbt());
	}
	
	private void setColor(DyeColor color) {
		modifySideNbt(nbt -> SignSideTagReferences.COLOR.set(nbt, color.getSerializedName()));
		checkSave();
	}
	private DyeColor getColor() {
		return DyeColor.byName(SignSideTagReferences.COLOR.get(getSideNbt()), DyeColor.BLACK);
	}
	
	private void setLines(List<Component> lines) {
		modifySideNbt(nbt -> SignSideTagReferences.TEXT.set(nbt, lines.stream()
				.map(this::fixClickEvent).map(line -> NEW_FEATURES ? fixEditable(line) : line).toList()));
		checkSave();
	}
	private List<Component> getLines() {
		List<Component> output = SignSideTagReferences.TEXT.get(getSideNbt());
		while (output.size() < 4)
			output.add(TextInst.of(""));
		return output;
	}
	
	private Component fixClickEvent(Component line) { // https://bugs.mojang.com/browse/MC-62833
		ClickEvent event = getClickEvent(line);
		if (event == null)
			return line;
		return TextInst.copy(line).withStyle(style -> style.withClickEvent(event));
	}
	private ClickEvent getClickEvent(Component text) {
		ClickEvent event = text.getStyle().getClickEvent();
		if (event != null)
			return event;
		for (Component child : text.getSiblings()) {
			event = getClickEvent(child);
			if (event != null)
				return event;
		}
		return null;
	}
	
	private Component fixEditable(Component line) { // {"extra":[{...}]} makes the sign uneditable
		if (StyleUtil.identical(line.getStyle(), Style.EMPTY) && line.getSiblings().size() == 1 &&
				line.getSiblings().get(0).getSiblings().isEmpty()) {
			return line.getSiblings().get(0);
		}
		return line;
	}
	
	@Override
	protected void initEditor() {
		if (NEW_FEATURES) {
			addRenderableWidget(Buttons.of(16, 64, 100, 20,
					TextInst.translatable("nbteditor.signboard.side." + (back ? "back" : "front")), btn -> {
				back = !back;
				clearWidgets();
				init();
			}));
			addRenderableWidget(Buttons.of(16 + 104, 64, 100, 20,
					TextInst.translatable("nbteditor.signboard.wax." + (isWaxed() ? "enabled" : "disabled")), btn -> {
				boolean prevWaxed = isWaxed();
				setWaxed(!prevWaxed);
				btn.setMessage(TextInst.translatable("nbteditor.signboard.wax." + (prevWaxed ? "disabled" : "enabled")));
			}));
		}
		
		int glowingBtnX = 16 + (NEW_FEATURES ? 104 * 2 : 0);
		int glowingBtnY = 64;
		AtomicReference<Button> glowingBtn = new AtomicReference<>();
		
		ButtonDropdownWidget colors = addWidget(new ButtonDropdownWidget(glowingBtnX, glowingBtnY + 20, 20, 20, null, 20, 20) {
			@Override
			public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
				context.pose().pushMatrix();
				context.pose().translate((float) (0.0), (float) (0.0));
				super.extractRenderState(context, mouseX, mouseY, delta);
				context.pose().popMatrix();
			}
		});
		for (DyeColor color : DyeColor.values()) {
			colors.addButton(TextInst.literal("⬛").withStyle(style -> style.withColor(getRenderedColor(color))), btn -> {
				setColor(color);
				colors.setOpen(false);
				glowingBtn.get().setMessage(TextInst.translatable("nbteditor.signboard.glowing.enabled")
						.withStyle(style -> style.withColor(getRenderedColor(getColor()))));
			}, new MVTooltip(TextInst.of(color.getSerializedName())));
		}
		colors.build();
		
		glowingBtn.set(addRenderableWidget(Buttons.of(glowingBtnX, glowingBtnY, 100, 20,
				TextInst.translatable("nbteditor.signboard.glowing." + (isGlowing() ? "enabled" : "disabled"))
				.withStyle(style -> style.withColor(getRenderedColor(getColor()))), btn -> {
			boolean prevGlowing = isGlowing();
			if (prevGlowing && Keys.hasShiftDown()) {
				colors.setOpen(true);
				return;
			}
			setGlowing(!prevGlowing);
			btn.setMessage(TextInst.translatable("nbteditor.signboard.glowing." + (prevGlowing ? "disabled" : "enabled"))
					.withStyle(style -> style.withColor(getRenderedColor(getColor()))));
			if (!prevGlowing)
				colors.setOpen(true);
		}, new MVTooltip("nbteditor.signboard.glowing.desc"))));
		
		lines = addRenderableWidget(FormattedTextFieldWidget.create(lines, 16, 64 + 24, width - 32, height - 80 - 24,
				getLines(), Style.EMPTY.withColor(ChatFormatting.BLACK), this::setLines));
		lines.setMaxLines(4);
		lines.setBackgroundColor(0);
		lines.setShadow(false);
		
		addRenderableOnly(colors); // Render on top of FormattedTextFieldWidget highlights
	}
	
	@Override
	protected void preRenderEditor(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		MVDrawableHelper.drawTexture(context, texture, 16, 64 + 24 * 2, 0, 0, width - 32, height - 80 - 24 * 2);
	}
	
}
