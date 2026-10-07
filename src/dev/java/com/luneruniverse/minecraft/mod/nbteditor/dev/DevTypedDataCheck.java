package com.luneruniverse.minecraft.mod.nbteditor.dev;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIO;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.HandItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.SignboardScreen;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

/**
 * Exercises everything that reads or writes the <code>entity_data</code> and
 * <code>block_entity_data</code> item components, which 26.2 stores as {@code TypedEntityData}.
 * The mod read them as {@code CustomData}, and a server-given spawn egg crashed the client when
 * hovered in the creative inventory tab (2026-10-06).
 *
 * <p>Runs before {@link DevScreenSweep}, which waits on {@link #isDone()}. The dev server ops the
 * dev player, so the items under test come from the server the way a real server sends them, and
 * the hover goes through the game's own frame loop with the cursor over the slot. The nbteditor
 * tooltip only reads container data with the creative inventory tab open, so every tooltip here
 * is built with that tab on screen.
 */
public class DevTypedDataCheck {

	private static final String EGG_GIVE = "give @s minecraft:zombie_spawn_egg[minecraft:entity_data="
			+ "{id:\"minecraft:zombie\",Health:5f,CustomName:\"Probe\"}]";
	private static final String LECTERN_GIVE = "give @s minecraft:lectern[minecraft:block_entity_data="
			+ "{id:\"minecraft:lectern\",Book:{id:\"minecraft:writable_book\",count:1}}]";

	private static DevTypedDataCheck instance;

	public static void start() {
		instance = new DevTypedDataCheck();
	}
	public static boolean isDone() {
		return instance == null || instance.step >= instance.steps.size();
	}
	public static void tick(Minecraft client) {
		if (instance != null && !isDone())
			instance.run(client);
	}

	private record Step(String name, int waitTicks, Supplier<Boolean> action) {}

	private final List<Step> steps = new ArrayList<>();
	private int step;
	private int waited;
	private Minecraft client;
	private int itemThrows;

	private DevTypedDataCheck() {
		steps.add(new Step("op commands", 0, () -> {
			sendCommand("gamemode creative");
			sendCommand("clear");
			sendCommand(EGG_GIVE);
			sendCommand(LECTERN_GIVE);
			sendCommand("give @s minecraft:pig_spawn_egg");
			return true;
		}));
		steps.add(new Step("server items arrive", 200, () -> findInHotbar(Items.LECTERN) >= 0
				&& findInHotbar(Items.ZOMBIE_SPAWN_EGG) >= 0 && findInHotbar(Items.PIG_SPAWN_EGG) >= 0
				&& client.gameMode.getPlayerMode() == GameType.CREATIVE));
		steps.add(new Step("open creative inventory tab", 0, this::openCreativeInventory));
		steps.add(new Step("creative inventory tab open", 40, () ->
				client.gui.screen() instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen()));
		steps.add(new Step("server items", 0, this::checkServerItems));
		steps.add(new Step("tag reference", 0, this::checkTagReference));
		steps.add(new Step("every item", 0, this::checkEveryItem));
		steps.add(new Step("LocalEntity items", 0, this::checkLocalEntityItems));
		steps.add(new Step("raw NBT", 0, this::checkRawNbt));
		steps.add(new Step("hover zombie egg", 0, () -> hover(Items.ZOMBIE_SPAWN_EGG)));
		steps.add(new Step("zombie egg hovered", 40, () -> hovered(Items.ZOMBIE_SPAWN_EGG)));
		steps.add(new Step("screenshot zombie egg", 0, () -> screenshot("typed-hover-zombie-egg.png")));
		steps.add(new Step("hover lectern", 10, () -> hover(Items.LECTERN)));
		steps.add(new Step("lectern hovered", 40, () -> hovered(Items.LECTERN)));
		steps.add(new Step("screenshot lectern", 0, () -> screenshot("typed-hover-lectern.png")));
		steps.add(new Step("hover pig egg", 10, () -> hover(Items.PIG_SPAWN_EGG)));
		steps.add(new Step("pig egg hovered", 40, () -> hovered(Items.PIG_SPAWN_EGG)));
		steps.add(new Step("close creative", 10, () -> {
			client.setScreenAndShow(null);
			return true;
		}));
		steps.add(new Step("signboard", 0, () -> {
			checkSignboard(Items.OAK_SIGN, BlockEntityTypes.SIGN);
			checkSignboard(Items.OAK_HANGING_SIGN, BlockEntityTypes.HANGING_SIGN);
			return true;
		}));
		steps.add(new Step("export cmdblock", 0, this::exportCmdBlock));
		steps.add(new Step("cmdblock arrives", 100, () -> findInHotbar(Items.COMMAND_BLOCK) >= 0));
		steps.add(new Step("check cmdblock", 0, () -> {
			checkCmdBlock();
			sendCommand("clear");
			sendCommand("gamemode survival");
			return true;
		}));
		steps.add(new Step("back to survival", 100, () -> client.gameMode.getPlayerMode() == GameType.SURVIVAL));
	}

	private void run(Minecraft client) {
		if (client.player == null || client.level == null)
			return;
		this.client = client;
		Step current = steps.get(step);
		boolean ok;
		try {
			ok = current.action().get();
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("SWEEP fail typed data step '" + current.name() + "' threw", e);
			ok = true;
		}
		if (ok) {
			step++;
			waited = 0;
			if (isDone())
				NBTEditor.LOGGER.info("SWEEP check typed data steps all ran");
		} else if (++waited > current.waitTicks()) {
			NBTEditor.LOGGER.error("SWEEP fail typed data step '{}' never happened in {} ticks",
					current.name(), current.waitTicks());
			step++;
			waited = 0;
		}
	}

	private void sendCommand(String command) {
		client.player.connection.sendCommand(command);
	}

	private int findInHotbar(Item item) {
		for (int slot = 0; slot < 9; slot++) {
			if (client.player.getInventory().getItem(slot).is(item))
				return slot;
		}
		return -1;
	}

	private void fail(String message, Object... args) {
		NBTEditor.LOGGER.error("SWEEP fail typed data: " + message, args);
	}
	private void pass(String message, Object... args) {
		NBTEditor.LOGGER.info("SWEEP check typed data: " + message, args);
	}

	// ---- Encoding ----

	/** Saves and sends the stack the way the game does; a component of the wrong class throws here. */
	private String roundTrip(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		RegistryAccess access = client.level.registryAccess();
		var ops = access.createSerializationContext(NbtOps.INSTANCE);
		Tag saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
		ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
		if (!ItemStack.matches(stack, loaded))
			return "save/load changed it: " + saved;
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
		ItemStack.STREAM_CODEC.encode(buf, stack);
		ItemStack received = ItemStack.STREAM_CODEC.decode(buf);
		if (!ItemStack.matches(stack, received))
			return "network send changed it";
		return null;
	}

	/** Null when the raw components are absent or the right class, else what is wrong. */
	private static String componentClasses(ItemStack stack) {
		Object entity = stack.get(DataComponents.ENTITY_DATA);
		Object blockEntity = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (entity != null && !(entity instanceof TypedEntityData<?>))
			return "entity_data is " + entity.getClass().getSimpleName();
		if (blockEntity != null && !(blockEntity instanceof TypedEntityData<?>))
			return "block_entity_data is " + blockEntity.getClass().getSimpleName();
		return null;
	}

	private List<net.minecraft.network.chat.Component> tooltip(ItemStack stack) {
		return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.ADVANCED);
	}

	// ---- Server items ----

	private boolean checkServerItems() {
		ItemStack egg = client.player.getInventory().getItem(findInHotbar(Items.ZOMBIE_SPAWN_EGG));
		TypedEntityData<EntityType<?>> eggData = egg.get(DataComponents.ENTITY_DATA);
		CompoundTag eggNbt = ItemTagReferences.ENTITY_DATA.get(egg);
		if (eggData != null && eggData.type() == EntityTypes.ZOMBIE
				&& "minecraft:zombie".equals(eggNbt.getStringOr("id", ""))
				&& eggNbt.getFloatOr("Health", 0) == 5 && "Probe".equals(eggNbt.getStringOr("CustomName", "")))
			pass("server-given zombie egg reads {}", eggNbt);
		else
			fail("server-given zombie egg holds {} and reads {}", eggData, eggNbt);

		ItemStack lectern = client.player.getInventory().getItem(findInHotbar(Items.LECTERN));
		CompoundTag lecternNbt = ItemTagReferences.BLOCK_ENTITY_DATA.get(lectern);
		ItemStack[] book = ContainerIOs.get(lectern).read(lectern);
		if ("minecraft:lectern".equals(lecternNbt.getStringOr("id", "")) && book.length > 0
				&& book[0] != null && book[0].is(Items.WRITABLE_BOOK))
			pass("server-given lectern reads {} and holds its book", lecternNbt);
		else
			fail("server-given lectern reads {}", lecternNbt);

		for (Item item : List.of(Items.ZOMBIE_SPAWN_EGG, Items.LECTERN, Items.PIG_SPAWN_EGG)) {
			ItemStack stack = client.player.getInventory().getItem(findInHotbar(item));
			List<net.minecraft.network.chat.Component> lines = tooltip(stack);
			boolean editHint = lines.stream().anyMatch(line -> line.getString().contains("Ctrl")
					|| line.getContents().toString().contains("nbteditor.keybind"));
			if (editHint)
				pass("server-given {} tooltip has the nbteditor keybind lines ({} lines)", item, lines.size());
			else
				fail("server-given {} tooltip lacks the nbteditor keybind lines, so the container read never ran: {}",
						item, lines);
		}
		return true;
	}

	// ---- Tag reference semantics ----

	private boolean checkTagReference() {
		List<String> wrong = new ArrayList<>();

		ItemStack stone = new ItemStack(Items.STONE);
		if (!ItemTagReferences.ENTITY_DATA.get(stone).isEmpty() || !ItemTagReferences.BLOCK_ENTITY_DATA.get(stone).isEmpty())
			wrong.add("an item without the components reads non-empty");
		ItemTagReferences.ENTITY_DATA.set(stone, tag("Health", 3));
		if (stone.has(DataComponents.ENTITY_DATA))
			wrong.add("a tag with no id and no type to keep was written onto stone");

		ItemStack egg = zombieEgg();
		CompoundTag read = ItemTagReferences.ENTITY_DATA.get(egg);
		read.putFloat("Health", 99);
		read.putString("id", "minecraft:creeper");
		if (ItemTagReferences.ENTITY_DATA.get(egg).getFloatOr("Health", 0) != 5
				|| egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.ZOMBIE)
			wrong.add("mutating the tag read changed the item");

		ItemTagReferences.ENTITY_DATA.set(egg, tag("Health", 7));
		if (egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.ZOMBIE
				|| ItemTagReferences.ENTITY_DATA.get(egg).getFloatOr("Health", 0) != 7)
			wrong.add("a write without an id lost the type or the data: " + egg.get(DataComponents.ENTITY_DATA));

		CompoundTag unknown = tag("Health", 8);
		unknown.putString("id", "minecraft:not_an_entity");
		ItemTagReferences.ENTITY_DATA.set(egg, unknown);
		if (egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.ZOMBIE)
			wrong.add("a write with an unknown id lost the type");

		CompoundTag badId = tag("Health", 8);
		badId.putString("id", "Not A Valid Identifier!");
		ItemTagReferences.ENTITY_DATA.set(egg, badId);
		if (egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.ZOMBIE)
			wrong.add("a write with an unparsable id lost the type");

		CompoundTag numericId = tag("Health", 8);
		numericId.putInt("id", 4);
		ItemTagReferences.ENTITY_DATA.set(egg, numericId);
		if (egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.ZOMBIE)
			wrong.add("a write with a numeric id lost the type");

		CompoundTag skeleton = tag("Health", 9);
		skeleton.putString("id", "minecraft:skeleton");
		ItemTagReferences.ENTITY_DATA.set(egg, skeleton);
		if (egg.get(DataComponents.ENTITY_DATA).type() != EntityTypes.SKELETON
				|| SpawnEggItem.getType(egg) != EntityTypes.SKELETON
				|| egg.get(DataComponents.ENTITY_DATA).copyTagWithoutId().contains("id"))
			wrong.add("a write with a new id did not change the type cleanly: " + egg.get(DataComponents.ENTITY_DATA));

		ItemTagReferences.ENTITY_DATA.set(egg, null);
		if (egg.has(DataComponents.ENTITY_DATA))
			wrong.add("writing null left the component");

		ItemStack lectern = new ItemStack(Items.LECTERN);
		CompoundTag lecternTag = new CompoundTag();
		lecternTag.putString("id", "minecraft:lectern");
		lecternTag.putInt("Page", 2);
		ItemTagReferences.BLOCK_ENTITY_DATA.set(lectern, lecternTag);
		if (lectern.get(DataComponents.BLOCK_ENTITY_DATA) == null
				|| lectern.get(DataComponents.BLOCK_ENTITY_DATA).type() != BlockEntityTypes.LECTERN
				|| ItemTagReferences.BLOCK_ENTITY_DATA.get(lectern).getIntOr("Page", 0) != 2)
			wrong.add("a block entity write did not land typed: " + lectern.get(DataComponents.BLOCK_ENTITY_DATA));
		String encoded = roundTrip(lectern);
		if (encoded != null)
			wrong.add("lectern " + encoded);

		if (wrong.isEmpty())
			pass("tag reference semantics hold");
		else
			wrong.forEach(message -> fail("tag reference: {}", message));
		return true;
	}

	private static CompoundTag tag(String key, float value) {
		CompoundTag tag = new CompoundTag();
		tag.putFloat(key, value);
		return tag;
	}

	private static ItemStack zombieEgg() {
		ItemStack egg = new ItemStack(Items.ZOMBIE_SPAWN_EGG);
		CompoundTag tag = tag("Health", 5);
		egg.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.ZOMBIE, tag));
		return egg;
	}

	// ---- Every item ----

	private boolean checkEveryItem() {
		int items = 0;
		int containers = 0;
		int defaultEntity = 0;
		int defaultBlockEntity = 0;
		int failures = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			if (item == Items.AIR)
				continue;
			items++;
			ItemStack bare = new ItemStack(item);
			if (bare.has(DataComponents.ENTITY_DATA))
				defaultEntity++;
			if (bare.has(DataComponents.BLOCK_ENTITY_DATA))
				defaultBlockEntity++;
			if (ContainerIOs.get(bare) != null)
				containers++;
			failures += checkItem("bare", bare);
			failures += checkItem("with data", withGameData(bare));
		}
		pass("{} items, {} with a container io, {} with default entity_data, {} with default block_entity_data",
				items, containers, defaultEntity, defaultBlockEntity);
		if (failures == 0)
			pass("every item, bare and carrying game-built data, survives tooltip, container io and encoding");
		else
			fail("{} of {} item variants failed", failures, items * 2);
		return true;
	}

	/** The components a server would send: typed, with a probe key the edit must keep. */
	private static ItemStack withGameData(ItemStack bare) {
		ItemStack stack = bare.copy();
		TypedEntityData<EntityType<?>> entity = stack.get(DataComponents.ENTITY_DATA);
		EntityType<?> entityType = entity != null ? entity.type()
				: stack.getItem() instanceof SpawnEggItem ? SpawnEggItem.getType(stack) : EntityTypes.ZOMBIE;
		CompoundTag entityTag = entity != null ? entity.copyTagWithoutId() : new CompoundTag();
		entityTag.putString("nbte_probe", "kept");
		stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, entityTag));

		Optional<BlockEntityType<?>> blockEntityType = Optional.empty();
		TypedEntityData<BlockEntityType<?>> blockEntity = stack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockEntity != null)
			blockEntityType = Optional.of(blockEntity.type());
		else if (stack.getItem() instanceof BlockItem blockItem)
			blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.stream()
					.filter(type -> type.isValid(blockItem.getBlock().defaultBlockState())).findFirst()
					.map(type -> (BlockEntityType<?>) type);
		blockEntityType.ifPresent(type -> {
			CompoundTag blockTag = blockEntity != null ? blockEntity.copyTagWithoutId() : new CompoundTag();
			blockTag.putString("nbte_probe", "kept");
			stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, blockTag));
		});
		return stack;
	}

	private int checkItem(String variant, ItemStack stack) {
		String name = BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + variant;
		try {
			Object entityTypeBefore = typeOf(stack.get(DataComponents.ENTITY_DATA));
			Object blockEntityTypeBefore = typeOf(stack.get(DataComponents.BLOCK_ENTITY_DATA));
			EntityType<?> eggTypeBefore = stack.getItem() instanceof SpawnEggItem ? SpawnEggItem.getType(stack) : null;

			tooltip(stack);
			ItemTagReferences.ENTITY_DATA.get(stack);
			ItemTagReferences.BLOCK_ENTITY_DATA.get(stack);
			boolean supported = ContainerIOs.isSupported(stack);
			ContainerIO<ItemStack> io = ContainerIOs.get(stack);
			if (io != null && supported) {
				ItemStack[] read = io.read(stack);
				ItemStack[] contents = new ItemStack[27];
				for (int slot = 0; slot < contents.length; slot++)
					contents[slot] = slot < read.length && read[slot] != null ? read[slot] : ItemStack.EMPTY;
				int target = -1;
				for (int slot = 0; slot < io.getMaxSlots(stack); slot++) {
					if (contents[slot].isEmpty()) {
						target = slot;
						break;
					}
				}
				if (target >= 0)
					contents[target] = new ItemStack(Items.DIAMOND);
				io.write(stack, contents);
				ItemStack[] back = io.read(stack);
				boolean found = target < 0;
				for (ItemStack got : back)
					found |= got != null && got.is(Items.DIAMOND);
				if (!found) {
					fail("{} wrote a diamond into slot {} and read back {}", name, target, List.of(back));
					return 1;
				}
				tooltip(stack);
			}

			String wrongClass = componentClasses(stack);
			if (wrongClass != null) {
				fail("{} {}", name, wrongClass);
				return 1;
			}
			Object entityTypeAfter = typeOf(stack.get(DataComponents.ENTITY_DATA));
			Object blockEntityTypeAfter = typeOf(stack.get(DataComponents.BLOCK_ENTITY_DATA));
			if (entityTypeBefore != null && entityTypeBefore != entityTypeAfter
					|| blockEntityTypeBefore != null && blockEntityTypeBefore != blockEntityTypeAfter) {
				fail("{} changed type {} / {} to {} / {}", name, entityTypeBefore, blockEntityTypeBefore,
						entityTypeAfter, blockEntityTypeAfter);
				return 1;
			}
			if (eggTypeBefore != null && SpawnEggItem.getType(stack) != eggTypeBefore) {
				fail("{} spawns {} after the edit, not {}", name, SpawnEggItem.getType(stack), eggTypeBefore);
				return 1;
			}
			for (CompoundTag probed : List.of(ItemTagReferences.ENTITY_DATA.get(stack), ItemTagReferences.BLOCK_ENTITY_DATA.get(stack))) {
				if (variant.equals("with data") && probed.contains("id") && !"kept".equals(probed.getStringOr("nbte_probe", ""))) {
					fail("{} lost data the server sent: {}", name, probed);
					return 1;
				}
			}
			String encoded = roundTrip(stack);
			if (encoded != null) {
				fail("{} {}", name, encoded);
				return 1;
			}
			return 0;
		} catch (Throwable e) {
			if (itemThrows++ == 0)
				NBTEditor.LOGGER.error("SWEEP fail typed data: " + name + " threw", e);
			else
				fail("{} threw {}", name, e);
			return 1;
		}
	}

	private static Object typeOf(TypedEntityData<?> data) {
		return data == null ? null : data.type();
	}

	// ---- LocalEntity ----

	private boolean checkLocalEntityItems() {
		List<EntityType<?>> types = List.of(EntityTypes.ZOMBIE, EntityTypes.PIG, EntityTypes.VILLAGER,
				EntityTypes.ARMOR_STAND, EntityTypes.ITEM_FRAME, EntityTypes.GLOW_ITEM_FRAME, EntityTypes.PAINTING,
				EntityTypes.MINECART, EntityTypes.CHEST_MINECART, EntityTypes.HOPPER_MINECART,
				EntityTypes.FURNACE_MINECART, EntityTypes.TNT_MINECART, EntityTypes.COMMAND_BLOCK_MINECART,
				EntityTypes.OAK_BOAT, EntityTypes.OAK_CHEST_BOAT);
		int good = 0;
		for (EntityType<?> type : types) {
			String name = EntityType.getKey(type).toString();
			try {
				CompoundTag nbt = new CompoundTag();
				nbt.putString("nbte_probe", "kept");
				Optional<ItemStack> item = new LocalEntity(type, nbt).toItem(true);
				if (item.isEmpty()) {
					fail("LocalEntity {} made no item", name);
					continue;
				}
				ItemStack stack = item.get();
				TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
				String encoded = roundTrip(stack);
				if (componentClasses(stack) != null || data == null || data.type() != type
						|| !"kept".equals(data.copyTagWithoutId().getStringOr("nbte_probe", "")) || encoded != null)
					fail("LocalEntity {} made {} holding {} ({})", name, stack, data, encoded);
				else
					good++;
			} catch (Throwable e) {
				NBTEditor.LOGGER.error("SWEEP fail typed data: LocalEntity " + name + " threw", e);
			}
		}
		if (good == types.size())
			pass("LocalEntity.toItem writes typed entity data for all {} entity types", good);
		return true;
	}

	// ---- Raw NBT view ----

	private boolean checkRawNbt() {
		ItemStack lectern = new ItemStack(Items.LECTERN);
		CompoundTag lecternTag = new CompoundTag();
		lecternTag.putString("id", "minecraft:lectern");
		lecternTag.putInt("Page", 2);
		ItemTagReferences.BLOCK_ENTITY_DATA.set(lectern, lecternTag);
		int good = 0;
		List<ItemStack> stacks = List.of(zombieEgg(), lectern, withGameData(new ItemStack(Items.ARMOR_STAND)),
				withGameData(new ItemStack(Items.OAK_SIGN)));
		for (ItemStack stack : stacks) {
			try {
				CompoundTag nbt = stack.nbte$getNbt();
				ItemStack copy = new ItemStack(stack.getItem());
				copy.nbte$setNbt(nbt.copy());
				String encoded = roundTrip(copy);
				if (!ItemStack.matches(stack, copy) || componentClasses(copy) != null || encoded != null)
					fail("raw NBT round trip of {} via {} gave {} ({})", stack, nbt, copy, encoded);
				else
					good++;
			} catch (Throwable e) {
				NBTEditor.LOGGER.error("SWEEP fail typed data: raw NBT of " + stack + " threw", e);
			}
		}
		pass("raw NBT view of a zombie egg reads {}", zombieEgg().nbte$getNbt());
		if (good == stacks.size())
			pass("raw NBT view round trips all {} items", good);
		return true;
	}

	// ---- Creative inventory hover ----

	private boolean openCreativeInventory() {
		CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(client.player,
				client.player.connection.enabledFeatures(), false);
		client.setScreenAndShow(screen);
		CreativeModeTab inventory = BuiltInRegistries.CREATIVE_MODE_TAB.stream()
				.filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY).findFirst().orElseThrow();
		try {
			Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
			selectTab.setAccessible(true);
			selectTab.invoke(screen, inventory);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		return true;
	}

	private Slot slotHolding(Item item) {
		if (!(client.gui.screen() instanceof CreativeModeInventoryScreen screen))
			return null;
		for (Slot slot : screen.getMenu().slots) {
			if (slot.container == client.player.getInventory() && slot.getItem().is(item))
				return slot;
		}
		return null;
	}

	private boolean hover(Item item) {
		Slot slot = slotHolding(item);
		if (slot == null) {
			fail("no creative inventory slot holds {}", item);
			return true;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
		int scale = client.getWindow().getGuiScale();
		double x = (field(screen, "leftPos", Integer.class) + slot.x + 8) * scale;
		double y = (field(screen, "topPos", Integer.class) + slot.y + 8) * scale;
		long window = client.getWindow().handle();
		GLFW.glfwSetCursorPos(window, x, y);
		// GLFW on X11 warps the pointer without calling the cursor callback, so feed the move to
		// the handler that callback calls.
		try {
			Method onMove = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
			onMove.setAccessible(true);
			onMove.invoke(client.mouseHandler, window, x, y);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		pass("moved the cursor to {} at ({}, {}) in window pixels", item, x, y);
		return true;
	}

	private boolean hovered(Item item) {
		if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen))
			return false;
		Slot hovered = field(screen, "hoveredSlot", Slot.class);
		if (hovered == null || !hovered.getItem().is(item))
			return false;
		pass("the creative inventory tab rendered frames with the cursor on {} (mouse at {}, {})",
				item, client.mouseHandler.xpos(), client.mouseHandler.ypos());
		return true;
	}

	private boolean screenshot(String name) {
		Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1,
				message -> pass("screenshot {}: {}", name, message.getString()));
		return true;
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object owner, String name, Class<T> type) {
		Class<?> clazz = owner.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(name);
				field.setAccessible(true);
				return (T) field.get(owner);
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalStateException("no field " + name + " on " + owner.getClass());
	}

	// ---- Signboard and export ----

	private void checkSignboard(Item sign, BlockEntityType<?> expected) {
		String name = BuiltInRegistries.ITEM.getKey(sign).toString();
		try {
			client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(sign));
			SignboardScreen<?> screen = new SignboardScreen<>(new HandItemReference(InteractionHand.MAIN_HAND));
			LocalItem localItem = (LocalItem) field(screen, "localNBT", Object.class);
			Method setSideNbt = SignboardScreen.class.getDeclaredMethod("setSideNbt", CompoundTag.class);
			setSideNbt.setAccessible(true);
			CompoundTag side = new CompoundTag();
			side.putBoolean("has_glowing_text", true);
			setSideNbt.invoke(screen, side);
			ItemStack edited = localItem.getEditableItem();
			TypedEntityData<BlockEntityType<?>> data = edited.get(DataComponents.BLOCK_ENTITY_DATA);
			String encoded = roundTrip(edited);
			if (data == null || data.type() != expected
					|| !data.copyTagWithoutId().getCompoundOrEmpty("front_text").getBooleanOr("has_glowing_text", false)
					|| encoded != null)
				fail("signboard on {} wrote {} ({})", name, data, encoded);
			else
				pass("signboard on {} writes typed {} data with the edited side", name, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(expected));
		} catch (Throwable e) {
			NBTEditor.LOGGER.error("SWEEP fail typed data: signboard on " + name + " threw", e);
		} finally {
			client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
	}

	private boolean exportCmdBlock() {
		for (int slot = 0; slot < 9; slot++)
			client.player.getInventory().setItem(slot, ItemStack.EMPTY);
		client.player.setItemInHand(InteractionHand.MAIN_HAND, zombieEgg());
		try {
			ClientCommandManager.getActiveDispatcher().execute("nbteditor export cmdblock",
					(FabricClientCommandSource) client.player.connection.getSuggestionsProvider());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return true;
	}

	private void checkCmdBlock() {
		ItemStack cmdBlock = client.player.getInventory().getItem(findInHotbar(Items.COMMAND_BLOCK));
		TypedEntityData<BlockEntityType<?>> data = cmdBlock.get(DataComponents.BLOCK_ENTITY_DATA);
		String command = data == null ? "" : data.copyTagWithoutId().getStringOr("Command", "");
		String encoded = roundTrip(cmdBlock);
		if (data != null && data.type() == BlockEntityTypes.COMMAND_BLOCK && command.contains("zombie_spawn_egg")
				&& encoded == null)
			pass("export cmdblock gives a typed command block running {}", command);
		else
			fail("export cmdblock gave {} holding {} ({})", cmdBlock, data, encoded);
	}

}
