package com.luneruniverse.minecraft.mod.nbteditor.commands.get;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.argument;

import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.luneruniverse.minecraft.mod.nbteditor.commands.CommandRegistration;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.item.ItemStack;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

public class GetItemCommand extends ClientCommand {
	
	@Override
	public String getName() {
		return "item";
	}
	
	@Override
	public String getExtremeAlias() {
		return "i";
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		Command<FabricClientCommandSource> getItem = context -> {
			int count = getDefaultArg(context, "count", 1, Integer.class);
			ItemStack item = context.getArgument("item", ItemInput.class).createItemStack(count);
			PlayerItems.getWithMessage(item);
			return Command.SINGLE_SUCCESS;
		};
		
		builder.then(argument("item", CommandRegistration.itemArg())
				.then(argument("count", IntegerArgumentType.integer(1)).executes(getItem)).executes(getItem));
	}
	
}
