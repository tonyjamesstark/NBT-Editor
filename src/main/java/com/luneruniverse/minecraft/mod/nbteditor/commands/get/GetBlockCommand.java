package com.luneruniverse.minecraft.mod.nbteditor.commands.get;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.argument;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditorClient;
import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.util.AccessWidenedApi;
import com.luneruniverse.minecraft.mod.nbteditor.util.BlockStateProperties;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.luneruniverse.minecraft.mod.nbteditor.commands.CommandRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

public class GetBlockCommand extends ClientCommand {
	
	@Override
	public String getName() {
		return "block";
	}
	
	@Override
	public String getExtremeAlias() {
		return "b";
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		Command<FabricClientCommandSource> getBlock = context -> {
			Coordinates posArg = getDefaultArg(context, "pos", null, Coordinates.class);
			BlockPos pos = (posArg == null ? null : posArg.getBlockPos(getCommandSource(context.getSource().getPlayer())));
			if (pos != null && !Minecraft.getInstance().level.isInWorldBounds(pos))
				throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
			BlockInput blockArg = context.getArgument("block", BlockInput.class);
			CompoundTag nbt = AccessWidenedApi.getBlockArgumentNbt(blockArg);
			if (nbt == null)
				nbt = new CompoundTag();
			LocalBlock block = new LocalBlock(blockArg.getState().getBlock(), new BlockStateProperties(blockArg.getState()), nbt);
			
			if (pos == null) {
				block.toItem(false).ifPresentOrElse(PlayerItems::getWithMessage,
						() -> Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.nbt.export.item.error")));
			} else if (NBTEditorClient.SERVER_CONN.isEditingExpanded())
				block.place(pos);
			else
				Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.requires_server"));
			
			return Command.SINGLE_SUCCESS;
		};
		
		builder.then(argument("block", CommandRegistration.blockStateArg()).executes(getBlock))
				.then(argument("pos", BlockPosArgument.blockPos()).then(argument("block", CommandRegistration.blockStateArg()).executes(getBlock)));
	}
	
}
