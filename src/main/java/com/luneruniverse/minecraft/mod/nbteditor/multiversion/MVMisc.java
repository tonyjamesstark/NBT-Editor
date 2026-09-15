package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;


import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.NbtViews;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;
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
	
	// 1.21.9 moved the modifier queries off Screen and onto the input record. These
	// callers ask outside an event, which is what Screen's statics polled for.
}
