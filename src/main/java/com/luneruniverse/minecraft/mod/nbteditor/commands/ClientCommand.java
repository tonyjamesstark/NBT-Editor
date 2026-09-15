package com.luneruniverse.minecraft.mod.nbteditor.commands;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.screens.ConfigScreen;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

public abstract class ClientCommand {
	
	/**
	 * A command source that runs as the given entity, at its position and facing.
	 *
	 * <p>Used to evaluate a vanilla command argument against an entity the editor is holding
	 * rather than one in the world, so it carries no permissions and no server.
	 */
	protected static CommandSourceStack getCommandSource(Entity entity) {
		return new CommandSourceStack(
				CommandSource.NULL, entity.position(), entity.getRotationVector(), null, PermissionSet.NO_PERMISSIONS,
				entity.getName().getString(), entity.getDisplayName(), null, entity);
	}

	
	public static <T> T getDefaultArg(CommandContext<FabricClientCommandSource> context, String name, T defaultValue, Class<T> type) {
		try {
			return context.getArgument(name, type);
		} catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}
	
	
	public ClientCommand() {
		
	}
	
	public void registerAll(Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> commandHandler, String path) {
		LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(getName());
		register(builder, path);
		commandHandler.accept(builder);
		
		Set<String> aliases = new HashSet<>();
		for (ConfigScreen.Alias alias : ConfigScreen.getAliases()) {
			if (alias.original().equals(path))
				aliases.add(alias.alias());
		}
		for (String alias : aliases) {
			builder = literal(alias);
			register(builder, path);
			commandHandler.accept(builder);
		}
	}
	
	public abstract String getName();
	public abstract String getExtremeAlias();
	public abstract void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path);
	public ClientCommand getShortcut(List<String> path, int index) {
		if (path.size() == index)
			return this;
		return null;
	}
	
}
