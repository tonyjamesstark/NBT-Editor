package com.luneruniverse.minecraft.mod.nbteditor.commands.factories;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.util.UUID;

import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

public class RandomUUIDCommand extends ClientCommand {
	
	@Override
	public String getName() {
		return "randomuuid";
	}
	
	@Override
	public String getExtremeAlias() {
		return "ru";
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		Command<FabricClientCommandSource> add = context -> {
			ItemReference ref = ItemReference.getHeldItem();
			ItemStack item = ref.getItem();
			CompoundTag nbt = ItemTagReferences.CUSTOM_DATA.get(item);
			UUID uuid = UUID.randomUUID();
			nbt.nbte$putUuid("UUID", uuid);
			ItemTagReferences.CUSTOM_DATA.set(item, nbt);
			ref.saveItem(item, Component.translatableEscape("nbteditor.random_uuid.added",
					Component.literal(uuid.toString()).withStyle(ChatFormatting.GOLD)));
			return Command.SINGLE_SUCCESS;
		};
		Command<FabricClientCommandSource> remove = context -> {
			ItemReference ref = ItemReference.getHeldItem();
			ItemStack item = ref.getItem();
			CompoundTag nbt = ItemTagReferences.CUSTOM_DATA.get(item);
			if (!nbt.nbte$containsUuid("UUID")) {
				Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.random_uuid.already_removed"));
				return Command.SINGLE_SUCCESS;
			}
			nbt.remove("UUID");
			ItemTagReferences.CUSTOM_DATA.set(item, nbt);
			ref.saveItem(item, Component.translatableEscape("nbteditor.random_uuid.removed"));
			return Command.SINGLE_SUCCESS;
		};
		
		builder
				.then(literal("add").executes(add))
				.then(literal("remove").executes(remove))
				.executes(add);
	}
	
}
