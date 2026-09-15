package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;


import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponentPatch;
import java.util.Map;

public class MVMisc {
	
	
	
	public static Optional<InputStream> getResource(Identifier id) throws IOException {
		try {
			return MainUtil.client.getResourceManager().getResource(id).map(resource -> {
						try {
							return resource.open();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		} catch (UncheckedIOException e) {
			if (e.getMessage() != null) {
				IOException checkedE = new IOException(e.getMessage(), e.getCause());
				checkedE.setStackTrace(e.getStackTrace());
				throw checkedE;
			}
			throw e.getCause();
		}
	}
	
	public static Object registryAccess;
	public static ItemArgument getItemStackArg() {
		return ItemArgument.item((CommandBuildContext) registryAccess);
	}
	public static BlockStateArgument getBlockStateArg() {
		return BlockStateArgument.block((CommandBuildContext) registryAccess);
	}
	public static ComponentArgument getTextArg() {
		return ComponentArgument.textComponent((CommandBuildContext) registryAccess);
	}
	
	public static void registerCommands(Consumer<CommandDispatcher<FabricClientCommandSource>> callback) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
					registryAccess = access;
					callback.accept(dispatcher);
				});
	}
	
	public static Button newButton(int x, int y, int width, int height, Component message, Button.OnPress onPress, MVTooltip tooltip) {
		Tooltip newTooltip = (tooltip == null ? null : tooltip.toNewTooltip());
		return Button.builder(message, onPress).bounds(x, y, width, height).tooltip(newTooltip).build();
	}
	public static Button newButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
		return newButton(x, y, width, height, message, onPress, null);
	}
	
	public static Button newTexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier img, Button.OnPress onPress, MVTooltip tooltip) {
		Button output = new MVTexturedButtonWidget_1_20_2(
						x, y, width, height, 0, 0, hoveredVOffset, img, width, height + hoveredVOffset, onPress);
		if (tooltip != null) {
			output.setTooltip(tooltip.toNewTooltip());
		}
		return output;
	}
	public static Button newTexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier img, Button.OnPress onPress) {
		return newTexturedButton(x, y, width, height, hoveredVOffset, img, onPress, null);
	}
	
	public static boolean isValidChar(char c) {
		return c != '§' && c >= ' ' && c != 127;
	}
	public static String stripInvalidChars(String str, boolean allowLinebreaks) {
		StringBuilder output = new StringBuilder();
		for (char c : str.toCharArray()) {
			if (isValidChar(c)) {
				output.append(c);
			} else if (allowLinebreaks && c == '\n') {
				output.append(c);
			}
		}
		return output.toString();
	}
	
	public static String getContent(Component text) {
		StringBuilder output = new StringBuilder();
		text.getContents().visit(str -> {
			output.append(str);
			return Optional.empty();
		});
		return output.toString();
	}
	
	public static CompoundTag readNbt(InputStream stream) throws IOException {
		return NbtIo.read(new DataInputStream(stream), NbtAccounter.unlimitedHeap());
	}
	public static CompoundTag readCompressedNbt(InputStream stream) throws IOException {
		return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
	}
	public static void writeNbt(CompoundTag nbt, OutputStream stream) throws IOException {
		NbtIo.write(nbt, new DataOutputStream(stream));
	}
	public static void writeCompressedNbt(CompoundTag nbt, OutputStream stream) throws IOException {
		NbtIo.writeCompressed(nbt, stream);
	}
	public static CompoundTag readNbt(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return readNbt(stream);
		}
	}
	public static CompoundTag readCompressedNbt(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return readCompressedNbt(stream);
		}
	}
	public static void writeNbt(CompoundTag nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			writeNbt(nbt, stream);
		}
	}
	public static void writeCompressedNbt(CompoundTag nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			writeCompressedNbt(nbt, stream);
		}
	}
	
	public static MobEffectInstance newStatusEffectInstance(MobEffect effect, int duration) {
		return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration);
	}
	public static MobEffectInstance newStatusEffectInstance(MobEffect effect, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
		return new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, amplifier, ambient, showParticles, showIcon);
	}
	
	public static void showToast(Component title, Component description) {
		MainUtil.client.gui.toastManager().addToast(new SystemToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, title, description));
	}
	
	public static CommandSourceStack getCommandSource(Entity entity) {
		return new CommandSourceStack(
						CommandSource.NULL, entity.position(), entity.getRotationVector(), null, PermissionSet.NO_PERMISSIONS,
						entity.getName().getString(), entity.getDisplayName(), null, entity);
	}
	
	// From Minecraft#addBlockEntityNbt (1.21.3)
	// Edited to remove x, y, & z
	@SuppressWarnings("deprecation")
	public static void addBlockEntityNbtWithoutXYZ(ItemStack item, BlockEntity entity) {
		// writeComponentlessData omits x/y/z, so the position strip this used to do by hand is gone.
		TagValueOutput view = NbtViews.newWriteView();
		entity.saveCustomOnly(view);
		BlockEntity.addEntityType(view, entity.getType());
		entity.removeComponentsFromTag(view);
		BlockItem.setBlockEntityData(item, entity.getType(), view);
		item.applyComponents(entity.collectComponents());
	}
	
	public static int scaleRgb(int argb, double scale) {
		Color color = new Color(argb, true);
		int r = (int) (color.getRed() * scale);
		int g = (int) (color.getGreen() * scale);
		int b = (int) (color.getBlue() * scale);
		return new Color(r, g, b, color.getAlpha()).getRGB();
	}
	
	/**
	 * 26.2 folded a prototype lookup into {@link DataComponentPatch#get}, which
	 * loses the distinction the mod needs: absent from the patch (null) versus
	 * explicitly removed by it ({@link Optional#empty()}).
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optional<? extends T> getPatched(DataComponentPatch patch, DataComponentType<? extends T> type) {
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
			if (entry.getKey() == type)
				return (Optional<? extends T>) entry.getValue();
		}
		return null;
	}
	
	public static Tag parseNbt(StringReader snbt) throws CommandSyntaxException {
		return TagParser.create(NbtOps.INSTANCE).parseFully(snbt);
	}
	public static Tag parseNbt(String snbt) throws CommandSyntaxException {
		return parseNbt(new StringReader(snbt));
	}
	
	public static boolean isSimpleName(String name) {
		return (!name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false") &&
						StringTagVisitor.UNQUOTED_KEY_MATCH.matcher(name).matches());
	}
	
	public static Object withEnchantments(Object component, Object2IntOpenHashMap<Holder<Enchantment>> enchantments) {
		return new ItemEnchantments(enchantments);
	}
	
	public static void setPreviousCursorStack(AbstractContainerMenu handler, ItemStack item) {
		handler.remoteCarried.force(item);
	}
	
	public static Item getBoatItem(EntityType<?> entityType, CompoundTag nbt) {
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof BoatItem boat && entityType == boat.entityType)
				return item;
		}
		throw new IllegalStateException("Unknown boat entity type: " + EntityType.getKey(entityType));
	}
	
	
	// 1.21.9 moved the modifier queries off Screen and onto the input record. These
	// callers ask outside an event, which is what Screen's statics polled for.
	private static boolean isEitherPressed(int left, int right) {
		Window window = MainUtil.client.getWindow();
		return InputConstants.isKeyDown(window, left) || InputConstants.isKeyDown(window, right);
	}
	public static boolean hasShiftDown() {
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
	public static boolean hasControlDown() {
		if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY)
			return isEitherPressed(GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER);
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
	public static boolean hasAltDown() {
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT);
	}
	private static boolean isShortcut(int keyCode, int shortcutKey) {
		return keyCode == shortcutKey && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}
	public static boolean isSelectAll(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_A);
	}
	public static boolean isCopy(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_C);
	}
	public static boolean isPaste(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_V);
	}
	public static boolean isCut(int keyCode) {
		return isShortcut(keyCode, GLFW.GLFW_KEY_X);
	}
	

}
