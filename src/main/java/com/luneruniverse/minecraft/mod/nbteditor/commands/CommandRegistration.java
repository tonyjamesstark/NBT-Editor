package com.luneruniverse.minecraft.mod.nbteditor.commands;

import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandRegistrationCallback;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.item.ItemArgument;

/**
 * Registers the mod's commands, and builds the argument types that cannot be built without the
 * registries.
 *
 * <p>These belong together because of when they happen. An item, block-state or text argument
 * needs a {@link CommandBuildContext}, and the only place the game hands one over is the
 * registration callback. {@link #register} captures it there so the argument factories below can
 * be called from a command's own declaration, which is where they read naturally.
 *
 * <p>Calling an argument factory before registration is a programming error and throws.
 */
public class CommandRegistration {
	
	private static volatile CommandBuildContext buildContext;
	
	public static void register(Consumer<CommandDispatcher<FabricClientCommandSource>> callback) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
					buildContext = (CommandBuildContext) access;
					callback.accept(dispatcher);
				});
	}
	
	private static CommandBuildContext buildContext() {
		CommandBuildContext context = buildContext;
		if (context == null)
			throw new IllegalStateException("Command arguments are only available during registration");
		return context;
	}
	
	public static ItemArgument itemArg() {
		return ItemArgument.item(buildContext());
	}
	
	public static BlockStateArgument blockStateArg() {
		return BlockStateArgument.block(buildContext());
	}
	
	public static ComponentArgument textArg() {
		return ComponentArgument.textComponent(buildContext());
	}
	
}
