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
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Vector2ic;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.toast.SystemToast;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent.StewEffect;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

public class MVMisc {
	
	private static final Supplier<Class<?>> SequencedSet = Reflection.getOptionalClass("java.util.SequencedSet");
	
	
	public static Optional<InputStream> getResource(Identifier id) throws IOException {
		try {
			return MainUtil.client.getResourceManager().getResource(id).map(resource -> {
						try {
							return resource.getInputStream();
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
	public static ItemStackArgumentType getItemStackArg() {
		return ItemStackArgumentType.itemStack((CommandRegistryAccess) registryAccess);
	}
	public static BlockStateArgumentType getBlockStateArg() {
		return BlockStateArgumentType.blockState((CommandRegistryAccess) registryAccess);
	}
	public static TextArgumentType getTextArg() {
		return TextArgumentType.text((CommandRegistryAccess) registryAccess);
	}
	
	public static void registerCommands(Consumer<CommandDispatcher<FabricClientCommandSource>> callback) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
					registryAccess = access;
					callback.accept(dispatcher);
				});
	}
	
	public static ButtonWidget newButton(int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress, MVTooltip tooltip) {
		Tooltip newTooltip = (tooltip == null ? null : tooltip.toNewTooltip());
		return ButtonWidget.builder(message, onPress).dimensions(x, y, width, height).tooltip(newTooltip).build();
	}
	public static ButtonWidget newButton(int x, int y, int width, int height, Text message, ButtonWidget.PressAction onPress) {
		return newButton(x, y, width, height, message, onPress, null);
	}
	
	public static ButtonWidget newTexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier img, ButtonWidget.PressAction onPress, MVTooltip tooltip) {
		ButtonWidget output = new MVTexturedButtonWidget_1_20_2(
						x, y, width, height, 0, 0, hoveredVOffset, img, width, height + hoveredVOffset, onPress);
		if (tooltip != null) {
			output.setTooltip(tooltip.toNewTooltip());
		}
		return output;
	}
	public static ButtonWidget newTexturedButton(int x, int y, int width, int height, int hoveredVOffset, Identifier img, ButtonWidget.PressAction onPress) {
		return newTexturedButton(x, y, width, height, hoveredVOffset, img, onPress, null);
	}
	
	public static boolean isCreativeInventoryTabSelected() {
		if (MainUtil.client.currentScreen instanceof CreativeInventoryScreen screen) {
			return screen.isInventoryTabSelected();
		}
		return false;
	}
	
	public static void setKeyboardRepeatEvents(boolean repeatEvents) {
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
	
	public static String getContent(Text text) {
		StringBuilder output = new StringBuilder();
		text.getContent().visit(str -> {
			output.append(str);
			return Optional.empty();
		});
		return output.toString();
	}
	
	public static Vector2ic getPosition(Object positioner, Screen screen, int x, int y, int width, int height) {
		return ((TooltipPositioner) positioner).getPosition(
						MainUtil.client.getWindow().getScaledWidth(), MainUtil.client.getWindow().getScaledHeight(), x, y, width, height);
	}
	
	private static final Supplier<Class<?>> SuspiciousStewItem = Reflection.getOptionalClass("net.minecraft.class_1830");
	public static void addEffectToStew(ItemStack item, StatusEffect effect, int duration) {
		item.apply(MVComponentType.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffectsComponent(List.of()), effects -> effects.with(new StewEffect(Registries.STATUS_EFFECT.getEntry(effect), duration)));
	}
	
	public static void sendC2SPacket(Packet<?> packet) {
		MainUtil.client.getNetworkHandler().sendPacket(packet);
	}
	
	public static NbtCompound readNbt(InputStream stream) throws IOException {
		return NbtIo.readCompound(new DataInputStream(stream), NbtSizeTracker.ofUnlimitedBytes());
	}
	public static NbtCompound readCompressedNbt(InputStream stream) throws IOException {
		return NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
	}
	public static void writeNbt(NbtCompound nbt, OutputStream stream) throws IOException {
		NbtIo.write(nbt, new DataOutputStream(stream));
	}
	public static void writeCompressedNbt(NbtCompound nbt, OutputStream stream) throws IOException {
		NbtIo.writeCompressed(nbt, stream);
	}
	public static NbtCompound readNbt(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return readNbt(stream);
		}
	}
	public static NbtCompound readCompressedNbt(File file) throws IOException {
		try (FileInputStream stream = new FileInputStream(file)) {
			return readCompressedNbt(stream);
		}
	}
	public static void writeNbt(NbtCompound nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			writeNbt(nbt, stream);
		}
	}
	public static void writeCompressedNbt(NbtCompound nbt, File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file)) {
			writeCompressedNbt(nbt, stream);
		}
	}
	
	public static void setCursor(TextFieldWidget textField, int cursor) {
		textField.setCursor(cursor, false);
	}
	
	
	public static EntityType<?> getEntityType(ItemStack item) {
		SpawnEggItem spawnEggItem = (SpawnEggItem) item.getItem();
		return spawnEggItem.getEntityType(item);
	}
	
	public static StatusEffectInstance newStatusEffectInstance(StatusEffect effect, int duration) {
		return new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(effect), duration);
	}
	public static StatusEffectInstance newStatusEffectInstance(StatusEffect effect, int duration, int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
		return new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(effect), duration, amplifier, ambient, showParticles, showIcon);
	}
	
	public static StatusEffect getEffectType(StatusEffectInstance effect) {
		return effect.getEffectType().value();
	}
	
	public static BookScreen.Contents getBookContents(List<Text> pages) {
		if (NBTManagers.COMPONENTS_EXIST)
			return new BookScreen.Contents(pages);
		
		return (BookScreen.Contents) Proxy.newProxyInstance(MVMisc.class.getClassLoader(),
				new Class<?>[] {BookScreen.Contents.class}, (obj, method, args) -> {
			if (method.getName().equals("method_17560")) // getPageCount
				return pages.size();
			if (method.getName().equals("method_17561")) // getPageUnchecked
				return (StringVisitable) pages.get((int) args[0]);
			
			if (method.getName().equals("method_17563")) { // default getPage
				int index = (int) args[0];
				return (index >= 0 && index < pages.size() ? pages.get(index) : StringVisitable.EMPTY);
			}
			
			throw new IllegalArgumentException("Unknown method: " + method);
		});
	}
	
	public static boolean isWrittenBookContents(BookScreen.Contents contents) {
		return (MixinLink.WRITTEN_BOOK_CONTENTS.getIfPresent(contents) != null);
	}
	
	private static final Supplier<Class<?>> SystemToast$Type = Reflection.getOptionalClass("net.minecraft.class_370$class_371");
	private static final Object SystemToast$Type_PACK_LOAD_FAILURE =
			null;
	public static void showToast(Text title, Text description) {
		MainUtil.client.getToastManager().add(new SystemToast(SystemToast.Type.PACK_LOAD_FAILURE, title, description));
	}
	
	public static void setInitialFocus(Screen screen, Element element, Consumer<Element> superCall) {
		superCall.accept(element);
		screen.setFocused(element);
	}
	
	
	public static VertexConsumer startVertex(VertexConsumer vertexConsumer, double x, double y, double z) {
		return vertexConsumer.vertex((float) x, (float) y, (float) z);
	}
	
	public static float getTickDelta() {
		return MainUtil.client.getRenderTickCounter().getTickProgress(true);
	}
	
	public static EquipmentSlot getEquipmentSlot(EquipmentSlot.Type type, int entityId) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == type && slot.getEntitySlotId() == entityId)
				return slot;
		}
		throw new IllegalArgumentException("Unknown equipment slot: type=" + type + ", entityId=" + entityId);
	}
	
	public static void onRegistriesLoad(Runnable callback) {
		DynamicRegistryManagerHolder.onDefaultManagerLoad(callback);
	}
	
	
	public static <T> T withDefaultRegistryManager(Supplier<T> callback) {
		if (NBTManagers.COMPONENTS_EXIST)
			return DynamicRegistryManagerHolder.withDefaultManager(callback);
		return callback.get();
	}
	public static void withDefaultRegistryManager(Runnable callback) {
		if (NBTManagers.COMPONENTS_EXIST)
			DynamicRegistryManagerHolder.withDefaultManager(callback);
		else
			callback.run();
	}
	
	public static int getTooltipComponentHeight(TooltipComponent line) {
		return line.getHeight(MainUtil.client.textRenderer);
	}
	
	public static ServerCommandSource getCommandSource(Entity entity) {
		return new ServerCommandSource(
						CommandOutput.DUMMY, entity.getEntityPos(), entity.getRotationClient(), null, PermissionPredicate.NONE,
						entity.getName().getString(), entity.getDisplayName(), null, entity);
	}
	
	public static Profiler getProfiler() {
		return Profilers.get();
	}
	
	public static PotionContentsComponent newPotionContentsComponent(Optional<RegistryEntry<Potion>> potion, Optional<Integer> customColor, List<StatusEffectInstance> customEffects) {
		return new PotionContentsComponent(potion, customColor, customEffects, Optional.empty());
	}
	
	
	// From MinecraftClient#addBlockEntityNbt (1.21.3)
	// Edited to remove x, y, & z
	@SuppressWarnings("deprecation")
	public static void addBlockEntityNbtWithoutXYZ(ItemStack item, BlockEntity entity) {
		// writeComponentlessData omits x/y/z, so the position strip this used to do by hand is gone.
		NbtWriteView view = NbtViews.newWriteView();
		entity.writeComponentlessData(view);
		BlockEntity.writeId(view, entity.getType());
		entity.removeFromCopiedStackData(view);
		BlockItem.setBlockEntityData(item, entity.getType(), view);
		item.applyComponentsFrom(entity.createComponentMap());
	}
	
	public static int scaleRgb(int argb, double scale) {
		Color color = new Color(argb, true);
		int r = (int) (color.getRed() * scale);
		int g = (int) (color.getGreen() * scale);
		int b = (int) (color.getBlue() * scale);
		return new Color(r, g, b, color.getAlpha()).getRGB();
	}
	
	public static CreativeInventoryScreen newCreativeInventoryScreen(ClientPlayerEntity player) {
		return new CreativeInventoryScreen(
						player, player.networkHandler.getEnabledFeatures(), MainUtil.client.options.getOperatorItemsTab().getValue());
	}
	
	public static Text getName(Item item) {
		return item.getName();
	}
	
	public static boolean isSignItem(Item item) {
		if (item instanceof SignItem)
			return true;
		return false;
	}
	
	public static <T> Optional<T> result(DataResult<T> result) {
		return result.result();
	}
	
	public static String value(NbtString str) {
		return str.value();
	}
	
	public static Object newTooltipDisplayComponent(boolean hideTooltip, LinkedHashSet<ComponentType<?>> hiddenComponents) {
		return Reflection.newInstance(TooltipDisplayComponent.class, new Class<?>[] {boolean.class, SequencedSet.get()}, hideTooltip, hiddenComponents);
	}
	
	private static final Supplier<Reflection.MethodInvoker> TooltipDisplayComponent_hiddenComponents =
			Reflection.getOptionalMethod(() -> TooltipDisplayComponent.class, () -> "comp_3601", () -> MethodType.methodType(SequencedSet.get()));
	public static Set<ComponentType<?>> hiddenComponents(Object tooltipDisplayComponent) {
		return TooltipDisplayComponent_hiddenComponents.get().invoke(tooltipDisplayComponent);
	}
	
	@SuppressWarnings("unchecked")
	public static void setArmor(EquipmentSlot slot, ItemStack item) {
		MainUtil.client.player.equipStack(slot, item);
	}
	
	public static NbtElement parseNbt(StringReader snbt) throws CommandSyntaxException {
		return StringNbtReader.fromOps(NbtOps.INSTANCE).read(snbt);
	}
	public static NbtElement parseNbt(String snbt) throws CommandSyntaxException {
		return parseNbt(new StringReader(snbt));
	}
	
	public static boolean isSimpleName(String name) {
		return (!name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false") &&
						StringNbtWriter.QUOTATION_UNNECESSARY_PATTERN.matcher(name).matches());
	}
	
	public static Object withEnchantments(Object component, Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments) {
		return new ItemEnchantmentsComponent(enchantments);
	}
	
	public static Object withAttributes(Object component, List<AttributeModifiersComponent.Entry> list) {
		return new AttributeModifiersComponent(list);
	}
	
	public static boolean hasCreativeInventory() {
		return MainUtil.client.player.isInCreativeMode();
	}
	
	public static void setPreviousCursorStack(ScreenHandler handler, ItemStack item) {
		handler.trackedCursorSlot.setReceivedStack(item);
	}
	
	public static SlotActionType getActionType(ClickSlotC2SPacket packet) {
		return packet.actionType();
	}
	
	public static int getButton(ClickSlotC2SPacket packet) {
		return (int) packet.button();
	}
	
	public static int getSlot(ClickSlotC2SPacket packet) {
		return (int) packet.slot();
	}
	
	public static List<ItemStack> getContents(InventoryS2CPacket packet) {
		return packet.contents();
	}
	
	public static int getSyncId(InventoryS2CPacket packet) {
		return packet.syncId();
	}
	
	private static final Supplier<Class<?>> BoatEntity$Type = Reflection.getOptionalClass("net.minecraft.class_1690$class_1692");
	private static final Supplier<Reflection.MethodInvoker> BoatEntity$Type_getType =
			Reflection.getOptionalMethod(BoatEntity$Type, () -> "method_7561", () -> MethodType.methodType(BoatEntity$Type.get(), String.class));
	public static Item getBoatItem(EntityType<?> entityType, NbtCompound nbt) {
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof BoatItem boat && entityType == boat.boatEntityType)
				return item;
		}
		throw new IllegalStateException("Unknown boat entity type: " + EntityType.getId(entityType));
	}
	
	
	// 1.21.9 moved the modifier queries off Screen and onto the input record. These
	// callers ask outside an event, which is what Screen's statics polled for.
	private static boolean isEitherPressed(int left, int right) {
		Window window = MainUtil.client.getWindow();
		return InputUtil.isKeyPressed(window, left) || InputUtil.isKeyPressed(window, right);
	}
	public static boolean hasShiftDown() {
		return isEitherPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
	public static boolean hasControlDown() {
		if (SystemKeycodes.IS_MAC_OS)
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
