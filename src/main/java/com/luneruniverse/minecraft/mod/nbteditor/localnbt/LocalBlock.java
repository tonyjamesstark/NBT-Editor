package com.luneruniverse.minecraft.mod.nbteditor.localnbt;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVRegistry;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTextEvents;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.BlockReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.BlockStateProperties;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

public class LocalBlock implements LocalNBT {
	
	public static LocalBlock deserialize(CompoundTag nbt, int defaultDataVersion) {
		Tag dataVersion = nbt.get("DataVersion");
		
		String id = MainUtil.updateDynamic(References.BLOCK_NAME,
				StringTag.valueOf(nbt.nbte$getStringOrDefault("id")), dataVersion, defaultDataVersion).value();
		Block block = MVRegistry.BLOCK.get(Identifier.parse(id));
		
		BlockStateProperties state = new BlockStateProperties(block.defaultBlockState());
		state.setValues(MainUtil.updateDynamic(References.BLOCK_STATE,
				nbt.nbte$getCompoundOrDefault("state"), dataVersion, defaultDataVersion));
		
		CompoundTag tag = null;
		if (nbt.nbte$contains("tag", Tag.TAG_COMPOUND)) {
			tag = nbt.nbte$getCompoundOrDefault("tag");
			tag.putString("id", nbt.nbte$getStringOrDefault("id"));
			tag = MainUtil.updateDynamic(References.BLOCK_ENTITY, tag, dataVersion, defaultDataVersion);
			tag.remove("id");
		}
		
		return new LocalBlock(block, state, tag);
	}
	
	private Block block;
	private BlockStateProperties state;
	private CompoundTag nbt;
	
	private BlockEntity cachedBlockEntity;
	private BlockStateProperties cachedState;
	private CompoundTag cachedNbt;
	
	public LocalBlock(Block block, BlockStateProperties state, CompoundTag nbt) {
		this.block = block;
		this.state = state;
		this.nbt = nbt;
	}
	
	private BlockEntity getCachedBlockEntity() {
		if (!(block instanceof EntityBlock entityProvider))
			return null;
		
		if (cachedBlockEntity != null && cachedBlockEntity.getBlockState().getBlock() == block &&
				cachedState.equals(state) && Objects.equals(cachedNbt, nbt)) {
			return cachedBlockEntity;
		}
		
		cachedBlockEntity = entityProvider.newBlockEntity(new BlockPos(0, 1000, 0), state.applyTo(block.defaultBlockState()));
		cachedBlockEntity.setLevel(MainUtil.client.level);
		if (nbt != null)
			NBTManagers.BLOCK_ENTITY.setNbt(cachedBlockEntity, nbt);
		
		cachedState = state.copy();
		cachedNbt = nbt.copy();
		
		return cachedBlockEntity;
	}
	
	public boolean isBlockEntity() {
		return block instanceof EntityBlock;
	}
	
	@Override
	public boolean isEmpty(Identifier id) {
		return MVRegistry.BLOCK.get(id) == Blocks.AIR;
	}
	
	@Override
	public Component getName() {
		return MainUtil.getNbtNameSafely(nbt, "CustomName", () -> Component.nullToEmpty(getDefaultName()));
	}
	@Override
	public void setName(Component name) {
		if (name == null)
			getOrCreateNBT().remove("CustomName");
		else
			getOrCreateNBT().put("CustomName", TextUtil.toMinecraft(name));
	}
	@Override
	public String getDefaultName() {
		return ((Component) block.getName()).getString();
	}
	
	@Override
	public Identifier getId() {
		return MVRegistry.BLOCK.getId(block);
	}
	@Override
	public void setId(Identifier id) {
		this.block = MVRegistry.BLOCK.get(id);
		this.state = this.state.mapTo(block.defaultBlockState());
	}
	@Override
	public Set<Identifier> getIdOptions() {
		return MVRegistry.BLOCK.getIds();
	}
	
	public Block getBlock() {
		return block;
	}
	public void setBlock(Block block) {
		this.block = block;
	}
	
	public BlockStateProperties getState() {
		return state;
	}
	public void setState(BlockStateProperties state) {
		this.state = state;
	}
	
	@Override
	public CompoundTag getNBT() {
		return nbt;
	}
	@Override
	public void setNBT(CompoundTag nbt) {
		this.nbt = nbt;
	}
	
	private ItemStack cachedItem;
	private BlockStateProperties cachedItemState;
	private CompoundTag cachedItemNbt;
	
	@Override
	public void renderIcon(GuiGraphicsExtractor context, int x, int y, float tickDelta) {
		// The GUI lost its world-space rendering path in 1.21.9. Drawing the block's
		// item form keeps block entity detail (shulker colour, banner patterns) without
		// a hand-rolled render pipeline.
		// ponytail: a block with no BlockItem draws nothing; the editor still works.
		if (cachedItem == null || !Objects.equals(cachedItemState, state) || !Objects.equals(cachedItemNbt, nbt)) {
			cachedItem = toItem(false).orElse(ItemStack.EMPTY);
			cachedItemState = state.copy();
			cachedItemNbt = nbt == null ? null : nbt.copy();
		}
		context.item(cachedItem, x, y);
	}
	
	@Override
	public Optional<ItemStack> toItem(boolean cleanup) {
		for (Item item : MVRegistry.ITEM) {
			if (item instanceof BlockItem blockItem && blockItem.getBlock() == block) {
				ItemStack output = new ItemStack(blockItem);
				if (nbt != null && block instanceof EntityBlock provider) {
					BlockEntity entity = provider.newBlockEntity(new BlockPos(0, 1000, 0), state.applyTo(block.defaultBlockState()));
					entity.setLevel(MainUtil.client.level);
					NBTManagers.BLOCK_ENTITY.setNbt(entity, nbt);
					addBlockEntityNbtWithoutXYZ(output, entity);
				}
				ItemTagReferences.BLOCK_STATE.set(output, state.getValuesMap());
				return Optional.of(output);
			}
		}
		return Optional.empty();
	}
	@Override
	public CompoundTag serialize() {
		CompoundTag output = new CompoundTag();
		output.putString("id", getId().toString());
		output.put("state", state.getValues());
		if (nbt != null && (!nbt.isEmpty() || isBlockEntity()))
			output.put("tag", nbt);
		output.putString("type", "block");
		return output;
	}
	@Override
	public Component toHoverableText() {
		MutableComponent tooltip = Component.translatableEscape("gui.entity_tooltip.type", block.getName());
		if (!state.getProperties().isEmpty())
			tooltip.append("\n" + state);
		Component customName = MainUtil.getNbtNameSafely(nbt, "CustomName", () -> null);
		if (customName != null)
			tooltip = Component.literal("").append(customName).append("\n").append(tooltip);
		final Component finalTooltip = tooltip;
		return Component.translatableEscape("chat.square_brackets", getName()).withStyle(
				style -> style.withHoverEvent(MVTextEvents.HoverAction.SHOW_TEXT.newEvent(finalTooltip)));
	}
	
	public BlockReference place(BlockPos pos) {
		BlockReference ref = BlockReference.getBlockWithoutNBT(pos);
		ref.saveLocalNBT(this, Component.translatableEscape("nbteditor.get.block").append(toHoverableText()));
		return ref;
	}
	
	@Override
	public LocalBlock copy() {
		return new LocalBlock(block, state.copy(), nbt == null ? null : nbt.copy());
	}
	
	@Override
	public boolean equals(Object nbt) {
		if (nbt instanceof LocalBlock block)
			return this.block == block.block && this.state.equals(block.state) && Objects.equals(this.nbt, block.nbt);
		return false;
	}
	
	
	/**
	 * Copies a block entity's data onto an item, the way {@code Minecraft#addBlockEntityNbt} does,
	 * minus the position.
	 *
	 * <p>A block entity carries the x/y/z it was created at. The editor creates one at a throwaway
	 * position purely to hold the data, so writing that position onto the item would bake a lie
	 * into it. {@code writeComponentlessData} already omits x/y/z, so this is now just the vanilla
	 * sequence; the hand-rolled position strip it used to need is gone.
	 */
	@SuppressWarnings("deprecation")
	private static void addBlockEntityNbtWithoutXYZ(ItemStack item, BlockEntity entity) {
		TagValueOutput view = NbtViews.newWriteView();
		entity.saveCustomOnly(view);
		BlockEntity.addEntityType(view, entity.getType());
		entity.removeComponentsFromTag(view);
		BlockItem.setBlockEntityData(item, entity.getType(), view);
		item.applyComponents(entity.collectComponents());
	}
	
}
