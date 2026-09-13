package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.awt.Color;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.joml.Vector2ic;

import com.luneruniverse.minecraft.mod.nbteditor.misc.MixinLink;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
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
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.BlockRenderView;

public class MVMisc {
	
	private static final Supplier<Class<?>> SequencedSet = Reflection.getOptionalClass("java.util.SequencedSet");
	
	
	private static final Supplier<Reflection.MethodInvoker> ResourceFactory_getResource =
			Reflection.getOptionalMethod(ResourceFactory.class, "method_14486", MethodType.methodType(Resource.class, Identifier.class));
	private static final Supplier<Reflection.MethodInvoker> Resource_getInputStream =
			Reflection.getOptionalMethod(Resource.class, "method_14482", MethodType.methodType(InputStream.class));
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
	private static final Supplier<Reflection.MethodInvoker> ItemStackArgumentType_itemStack =
			Reflection.getOptionalMethod(ItemStackArgumentType.class, "method_9776", MethodType.methodType(ItemStackArgumentType.class));
	public static ItemStackArgumentType getItemStackArg() {
		return ItemStackArgumentType.itemStack((CommandRegistryAccess) registryAccess);
	}
	private static final Supplier<Reflection.MethodInvoker> BlockStateArgumentType_blockState =
			Reflection.getOptionalMethod(BlockStateArgumentType.class, "method_9653", MethodType.methodType(BlockStateArgumentType.class));
	public static BlockStateArgumentType getBlockStateArg() {
		return BlockStateArgumentType.blockState((CommandRegistryAccess) registryAccess);
	}
	private static final Supplier<Reflection.MethodInvoker> TextArgumentType_text =
			Reflection.getOptionalMethod(TextArgumentType.class, "method_9281", MethodType.methodType(TextArgumentType.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> CreativeInventoryScreen_getSelectedTab =
			Reflection.getOptionalMethod(CreativeInventoryScreen.class, "method_2469", MethodType.methodType(int.class));
	private static final Supplier<Reflection.FieldReference> ItemGroup_INVENTORY =
			Reflection.getOptionalField(ItemGroup.class, "field_7918", "Lnet/minecraft/class_1761;");
	private static final Supplier<Reflection.MethodInvoker> ItemGroup_getIndex =
			Reflection.getOptionalMethod(ItemGroup.class, "method_7741", MethodType.methodType(int.class));
	public static boolean isCreativeInventoryTabSelected() {
		if (MainUtil.client.currentScreen instanceof CreativeInventoryScreen screen) {
			return screen.isInventoryTabSelected();
		}
		return false;
	}
	
	private static final Supplier<Reflection.MethodInvoker> Keyboard_setRepeatEvents =
			Reflection.getOptionalMethod(Keyboard.class, "method_1462", MethodType.methodType(void.class, boolean.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> Text_asString =
			Reflection.getOptionalMethod(Text.class, "method_10851", MethodType.methodType(String.class));
	public static String getContent(Text text) {
		StringBuilder output = new StringBuilder();
		text.getContent().visit(str -> {
			output.append(str);
			return Optional.empty();
		});
		return output.toString();
	}
	
	private static final Supplier<Reflection.MethodInvoker> TooltipPositioner_getPosition =
			Reflection.getOptionalMethod(() -> TooltipPositioner.class, () -> "method_47944", () ->
			MethodType.methodType(Vector2ic.class, Screen.class, int.class, int.class, int.class, int.class));
	public static Vector2ic getPosition(Object positioner, Screen screen, int x, int y, int width, int height) {
		return ((TooltipPositioner) positioner).getPosition(
						MainUtil.client.getWindow().getScaledWidth(), MainUtil.client.getWindow().getScaledHeight(), x, y, width, height);
	}
	
	private static final Supplier<Class<?>> SuspiciousStewItem = Reflection.getOptionalClass("net.minecraft.class_1830");
	private static final Supplier<Reflection.MethodInvoker> SuspiciousStewItem_addEffectsToStew =
			Reflection.getOptionalMethod(SuspiciousStewItem, () -> "method_53209", () -> MethodType.methodType(void.class, ItemStack.class, List.class));
	private static final Supplier<Reflection.MethodInvoker> SuspiciousStewItem_addEffectToStew =
			Reflection.getOptionalMethod(SuspiciousStewItem, () -> "method_8021", () -> MethodType.methodType(void.class, ItemStack.class, StatusEffect.class, int.class));
	public static void addEffectToStew(ItemStack item, StatusEffect effect, int duration) {
		item.apply(MVComponentType.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffectsComponent(List.of()), effects -> effects.with(new StewEffect(Registries.STATUS_EFFECT.getEntry(effect), duration)));
	}
	
	private static final Supplier<Reflection.MethodInvoker> ClientPlayNetworkHandler_sendPacket =
			Reflection.getOptionalMethod(ClientPlayNetworkHandler.class, "method_2883", MethodType.methodType(void.class, Packet.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> TextFieldWidget_setCursor =
			Reflection.getOptionalMethod(TextFieldWidget.class, "method_1883", MethodType.methodType(void.class, int.class));
	public static void setCursor(TextFieldWidget textField, int cursor) {
		textField.setCursor(cursor, false);
	}
	
	
	private static final Supplier<Reflection.MethodInvoker> SpawnEggItem_getEntityType_NbtCompound =
			Reflection.getOptionalMethod(SpawnEggItem.class, "method_8015", MethodType.methodType(EntityType.class, NbtCompound.class));
	private static final Supplier<Reflection.MethodInvoker> SpawnEggItem_getEntityType_ItemStack =
			Reflection.getOptionalMethod(SpawnEggItem.class, "method_8015", MethodType.methodType(EntityType.class, ItemStack.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> StatusEffectInstance_getEffectType =
			Reflection.getOptionalMethod(StatusEffectInstance.class, "method_5579", MethodType.methodType(StatusEffect.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> ParentElement_setInitialFocus =
			Reflection.getOptionalMethod(ParentElement.class, "method_20085", MethodType.methodType(void.class, Element.class));
	public static void setInitialFocus(Screen screen, Element element, Consumer<Element> superCall) {
		superCall.accept(element);
		screen.setFocused(element);
	}
	
	private static final Supplier<Reflection.MethodInvoker> VertexConsumer_next =
			Reflection.getOptionalMethod(VertexConsumer.class, "method_1344", MethodType.methodType(void.class));
	
	private static final Supplier<Reflection.MethodInvoker> VertexConsumer_vertex =
			Reflection.getOptionalMethod(VertexConsumer.class, "method_22912", MethodType.methodType(VertexConsumer.class, double.class, double.class, double.class));
	public static VertexConsumer startVertex(VertexConsumer vertexConsumer, double x, double y, double z) {
		return vertexConsumer.vertex((float) x, (float) y, (float) z);
	}
	
	private static final Supplier<Reflection.MethodInvoker> MinecraftClient_getTickDelta =
			Reflection.getOptionalMethod(MinecraftClient.class, "method_1488", MethodType.methodType(float.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> VertexConsumer_light =
			Reflection.getOptionalMethod(VertexConsumer.class, "method_22916", MethodType.methodType(VertexConsumer.class, int.class));
	
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
	
	private static final Supplier<Reflection.MethodInvoker> TooltipComponent_getHeight =
			Reflection.getOptionalMethod(TooltipComponent.class, "method_32661", MethodType.methodType(int.class));
	public static int getTooltipComponentHeight(TooltipComponent line) {
		return line.getHeight(MainUtil.client.textRenderer);
	}
	
	private static final Supplier<Reflection.MethodInvoker> Entity_getCommandSource =
			Reflection.getOptionalMethod(Entity.class, "method_5671", MethodType.methodType(ServerCommandSource.class));
	public static ServerCommandSource getCommandSource(Entity entity) {
		return new ServerCommandSource(
						CommandOutput.DUMMY, entity.getEntityPos(), entity.getRotationClient(), null, PermissionPredicate.NONE,
						entity.getName().getString(), entity.getDisplayName(), null, entity);
	}
	
	private static final Supplier<Reflection.MethodInvoker> MinecraftClient_getProfiler =
			Reflection.getOptionalMethod(MinecraftClient.class, "method_16011", MethodType.methodType(Profiler.class));
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
	
	private static final Supplier<Reflection.MethodInvoker> Item_getName =
			Reflection.getOptionalMethod(Item.class, "method_7848", MethodType.methodType(Text.class));
	public static Text getName(Item item) {
		return item.getName();
	}
	
	public static boolean isSignItem(Item item) {
		if (item instanceof SignItem)
			return true;
		return false;
	}
	
	private static final Supplier<Reflection.MethodInvoker> DataResult_result =
			Reflection.getOptionalMethod(DataResult.class, "result", MethodType.methodType(Optional.class));
	public static <T> Optional<T> result(DataResult<T> result) {
		return result.result();
	}
	
	private static final Supplier<Reflection.MethodInvoker> NbtElement_asString =
			Reflection.getOptionalMethod(NbtElement.class, "method_10714", MethodType.methodType(String.class));
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
	
	private static final Supplier<Reflection.FieldReference> PlayerInventory_armor =
			Reflection.getOptionalField(PlayerInventory.class, "field_7548", "Lnet/minecraft/class_2371;");
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
	
	private static final Supplier<Reflection.FieldReference> StringNbtWriter_SIMPLE_NAME =
			Reflection.getOptionalField(StringNbtWriter.class, "field_27829", "Ljava/util/regex/Pattern;");
	public static boolean isSimpleName(String name) {
		return (!name.equalsIgnoreCase("true") && !name.equalsIgnoreCase("false") &&
						StringNbtWriter.QUOTATION_UNNECESSARY_PATTERN.matcher(name).matches());
	}
	
	private static final Supplier<Reflection.FieldReference> ItemEnchantmentsComponent_showInTooltip =
			Reflection.getOptionalField(ItemEnchantmentsComponent.class, "field_49390", "Z");
	public static Object withEnchantments(Object component, Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantments) {
		return new ItemEnchantmentsComponent(enchantments);
	}
	
	private static final Supplier<Reflection.MethodInvoker> AttributeModifiersComponent_showInTooltip =
			Reflection.getOptionalMethod(AttributeModifiersComponent.class, "comp_2394", MethodType.methodType(boolean.class));
	public static Object withAttributes(Object component, List<AttributeModifiersComponent.Entry> list) {
		return new AttributeModifiersComponent(list);
	}
	
	private static final Supplier<Reflection.MethodInvoker> ClientPlayerInteractionManager_hasCreativeInventory =
			Reflection.getOptionalMethod(ClientPlayerInteractionManager.class, "method_2914", MethodType.methodType(boolean.class));
	public static boolean hasCreativeInventory() {
		return MainUtil.client.player.isInCreativeMode();
	}
	
	private static final Supplier<Reflection.MethodInvoker> ScreenHandler_setPreviousCursorStack =
			Reflection.getOptionalMethod(ScreenHandler.class, "method_34250", MethodType.methodType(void.class, ItemStack.class));
	public static void setPreviousCursorStack(ScreenHandler handler, ItemStack item) {
		handler.trackedCursorSlot.setReceivedStack(item);
	}
	
	private static final Supplier<Reflection.MethodInvoker> ClickSlotC2SPacket_getActionType =
			Reflection.getOptionalMethod(ClickSlotC2SPacket.class, "method_12195", MethodType.methodType(SlotActionType.class));
	public static SlotActionType getActionType(ClickSlotC2SPacket packet) {
		return packet.actionType();
	}
	
	private static final Supplier<Reflection.MethodInvoker> ClickSlotC2SPacket_getButton =
			Reflection.getOptionalMethod(ClickSlotC2SPacket.class, "method_12193", MethodType.methodType(int.class));
	public static int getButton(ClickSlotC2SPacket packet) {
		return (int) packet.button();
	}
	
	private static final Supplier<Reflection.MethodInvoker> ClickSlotC2SPacket_getSlot =
			Reflection.getOptionalMethod(ClickSlotC2SPacket.class, "method_12192", MethodType.methodType(int.class));
	public static int getSlot(ClickSlotC2SPacket packet) {
		return (int) packet.slot();
	}
	
	private static final Supplier<Reflection.MethodInvoker> InventoryS2CPacket_getContents =
			Reflection.getOptionalMethod(InventoryS2CPacket.class, "method_11441", MethodType.methodType(List.class));
	public static List<ItemStack> getContents(InventoryS2CPacket packet) {
		return packet.contents();
	}
	
	private static final Supplier<Reflection.MethodInvoker> InventoryS2CPacket_getSyncId =
			Reflection.getOptionalMethod(InventoryS2CPacket.class, "method_11440", MethodType.methodType(int.class));
	public static int getSyncId(InventoryS2CPacket packet) {
		return packet.syncId();
	}
	
	private static final Supplier<Class<?>> BoatEntity$Type = Reflection.getOptionalClass("net.minecraft.class_1690$class_1692");
	private static final Supplier<Reflection.MethodInvoker> BoatEntity$Type_getType =
			Reflection.getOptionalMethod(BoatEntity$Type, () -> "method_7561", () -> MethodType.methodType(BoatEntity$Type.get(), String.class));
	private static final Supplier<Reflection.FieldReference> BoatItem_type =
			Reflection.getOptionalField(BoatItem.class, "field_7902", "Lnet/minecraft/class_1690$class_1692;");
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
