package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.EntityReference;
import com.luneruniverse.minecraft.mod.nbteditor.packets.SummonEntityC2SPacket;
import com.luneruniverse.minecraft.mod.nbteditor.packets.ViewEntityS2CPacket;
import com.luneruniverse.minecraft.mod.nbteditor.server.ServerMVMisc;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class LocalEntity implements LocalNBT {
	
	public static LocalEntity deserialize(CompoundTag nbt, int defaultDataVersion) {
		CompoundTag tag = nbt.nbte$getCompoundOrDefault("tag");
		tag.putString("id", nbt.nbte$getStringOrDefault("id"));
		tag = MainUtil.updateDynamic(References.ENTITY, tag, nbt.get("DataVersion"), defaultDataVersion);
		String id = tag.nbte$getStringOrDefault("id");
		tag.remove("id");
		return new LocalEntity(MVRegistry.ENTITY_TYPE.get(Identifier.parse(id)), tag);
	}
	
	private EntityType<?> entityType;
	private CompoundTag nbt;
	
	private Entity cachedEntity;
	private CompoundTag cachedNbt;
	
	public LocalEntity(EntityType<?> entityType, CompoundTag nbt) {
		this.entityType = entityType;
		this.nbt = nbt;
	}
	
	private Entity getCachedEntity() {
		if (cachedEntity != null && cachedEntity.getType() == entityType && Objects.equals(cachedNbt, nbt))
			return cachedEntity;
		
		cachedEntity = ServerMVMisc.createEntity(entityType, MainUtil.client.level);
		NBTManagers.ENTITY.setNbt(cachedEntity, nbt);
		
		cachedNbt = nbt.copy();
		
		return cachedEntity;
	}
	
	@Override
	public boolean isEmpty(Identifier id) {
		return false;
	}
	
	@Override
	public Component getName() {
		return MainUtil.getNbtNameSafely(nbt, "CustomName", () -> TextInst.of(getDefaultName()));
	}
	@Override
	public void setName(Component name) {
		if (name == null)
			getOrCreateNBT().remove("CustomName");
		else
			getOrCreateNBT().put("CustomName", TextInst.toMinecraft(name));
	}
	@Override
	public String getDefaultName() {
		return entityType.getDescription().getString();
	}
	
	@Override
	public Identifier getId() {
		return MVRegistry.ENTITY_TYPE.getId(entityType);
	}
	@Override
	public void setId(Identifier id) {
		this.entityType = MVRegistry.ENTITY_TYPE.get(id);
	}
	@Override
	public Set<Identifier> getIdOptions() {
		return MVRegistry.ENTITY_TYPE.getIds();
	}
	
	public EntityType<?> getEntityType() {
		return entityType;
	}
	public void setEntityType(EntityType<?> entityType) {
		this.entityType = entityType;
	}
	
	@Override
	public CompoundTag getNBT() {
		return nbt;
	}
	@Override
	public void setNBT(CompoundTag nbt) {
		this.nbt = nbt;
	}
	@Override
	public CompoundTag getOrCreateNBT() {
		return nbt;
	}
	
	@Override
	public void renderIcon(GuiGraphicsExtractor context, int x, int y, float tickDelta) {
		// 1.21.9 turned GUI entity rendering into a queued render state; there is no
		// longer a matrix stack to push the entity onto.
		Entity entity = getCachedEntity();
		EntityRenderState state = MainUtil.client.getEntityRenderDispatcher()
				.getRenderer(entity).createRenderState(entity, tickDelta);
		state.lightCoords = 0xF000F0;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		
		float scale = 16 / Math.max(Math.max(state.boundingBoxWidth, state.boundingBoxHeight), 1.0E-4F);
		context.entity(state, scale, new Vector3f(0, state.boundingBoxHeight / 2, 0),
				new Quaternionf().rotateZ((float) Math.PI), LocalNBT.iconSpin(),
				x, y, x + 16, y + 16);
	}
	
	@Override
	public Optional<ItemStack> toItem(boolean cleanup) {
		ItemStack output = null;
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof SpawnEggItem spawnEggItem && spawnEggItem.getType(new ItemStack(spawnEggItem)) == entityType)
				output = new ItemStack(spawnEggItem);
		}
		if (output == null) {
			if (entityType == EntityTypes.ARMOR_STAND)
				output = new ItemStack(Items.ARMOR_STAND);
			else if (entityType == EntityTypes.ITEM_FRAME)
				output = new ItemStack(Items.ITEM_FRAME);
			else if (entityType == EntityTypes.GLOW_ITEM_FRAME)
				output = new ItemStack(Items.GLOW_ITEM_FRAME);
			else if (entityType == EntityTypes.PAINTING)
				output = new ItemStack(Items.PAINTING);
			else if (entityType == EntityTypes.COMMAND_BLOCK_MINECART)
				output = new ItemStack(Items.COMMAND_BLOCK_MINECART);
			else if (entityType == EntityTypes.FURNACE_MINECART)
				output = new ItemStack(Items.FURNACE_MINECART);
			else if (entityType == EntityTypes.MINECART)
				output = new ItemStack(Items.MINECART);
			else if (entityType == EntityTypes.CHEST_MINECART)
				output = new ItemStack(Items.CHEST_MINECART);
			else if (entityType == EntityTypes.HOPPER_MINECART)
				output = new ItemStack(Items.HOPPER_MINECART);
			else if (entityType == EntityTypes.TNT_MINECART)
				output = new ItemStack(Items.TNT_MINECART);
			else if (getCachedEntity() instanceof AbstractBoat)
				output = new ItemStack(AccessWidenedApi.getBoatItem(entityType));
			else
				output = new ItemStack(Items.PIG_SPAWN_EGG);
		}
		
		CompoundTag nbt = this.nbt.copy();
		nbt.putString("id", getId().toString());
		
		if (cleanup) {
			nbt.remove("Passengers"); // Passengers don't work on spawn eggs
			nbt.remove("UUID");
			nbt.remove("Pos");
			if (entityType == EntityTypes.ITEM_FRAME || entityType == EntityTypes.GLOW_ITEM_FRAME ||
					entityType == EntityTypes.PAINTING) {
				nbt.remove("Rotation");
				nbt.remove("block_pos");
				if (entityType == EntityTypes.PAINTING)
					nbt.remove("facing");
				else
					nbt.remove("Facing");
			}
		}
		
		ItemTagReferences.ENTITY_DATA.set(output, nbt);
		
		return Optional.of(output);
	}
	@Override
	public CompoundTag serialize() {
		CompoundTag output = new CompoundTag();
		output.putString("id", getId().toString());
		output.put("tag", nbt);
		output.putString("type", "entity");
		return output;
	}
	@Override
	public Component toHoverableText() {
		UUID uuid = nbt.nbte$getUuid("UUID").orElseGet(() -> new UUID(0, 0));
		return TextInst.bracketed(getName()).withStyle(
				style -> style.withHoverEvent(MVTextEvents.HoverAction.SHOW_ENTITY.newEvent(new HoverEvent.EntityTooltipInfo(
						entityType, uuid, MainUtil.getNbtNameSafely(nbt, "CustomName", () -> null)))));
	}
	
	public CompletableFuture<Optional<EntityReference>> summon(ResourceKey<Level> world, Vec3 pos) {
		return NBTEditorClient.SERVER_CONN
				.sendRequest(requestId -> new SummonEntityC2SPacket(requestId, world, pos, getId(), nbt.copy()), ViewEntityS2CPacket.class)
				.thenApply(optional -> optional.filter(ViewEntityS2CPacket::foundEntity)
						.map(packet -> {
							EntityReference ref = new EntityReference(packet.getWorld(), packet.getUUID(),
									MVRegistry.ENTITY_TYPE.get(packet.getId()), packet.getNbt());
							MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.get.entity")
									.append(ref.getLocalNBT().toHoverableText()));
							return ref;
						}));
	}
	
	@Override
	public LocalEntity copy() {
		return new LocalEntity(entityType, nbt.copy());
	}
	
	@Override
	public boolean equals(Object nbt) {
		if (nbt instanceof LocalEntity entity)
			return this.entityType == entity.entityType && this.nbt.equals(entity.nbt);
		return false;
	}
	
}
