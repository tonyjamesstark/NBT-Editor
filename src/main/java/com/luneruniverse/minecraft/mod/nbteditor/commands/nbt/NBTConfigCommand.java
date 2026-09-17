package com.luneruniverse.minecraft.mod.nbteditor.commands.nbt;

import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;

public class NBTConfigCommand extends ClientCommand {
	
	@Override
	public String getName() {
		return "config";
	}
	
	@Override
	public String getExtremeAlias() {
		return "c";
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		builder.executes(context -> {
			Minecraft.getInstance().setScreenAndShow(new ConfigScreen(null));
			return Command.SINGLE_SUCCESS;
		});
	}
	
}
