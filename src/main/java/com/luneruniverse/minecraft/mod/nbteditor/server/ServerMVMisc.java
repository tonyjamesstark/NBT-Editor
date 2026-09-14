package com.luneruniverse.minecraft.mod.nbteditor.server;

import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.Level;

public class ServerMVMisc {
	
	public static void sendS2CPacket(ServerPlayer player, Packet<?> packet) {
		player.connection.send(packet);
	}
	
	public static boolean isInstanceOfVehicleInventory(MenuProvider factory) {
		return (factory instanceof ContainerEntity);
	}
	
	private static final Supplier<Reflection.MethodInvoker> PacketDecoder_decode =
			Reflection.getOptionalMethod(() -> Reflection.getClass("net.minecraft.class_9141"), () -> "decode", () -> MethodType.methodType(Object.class, Object.class));
	@SuppressWarnings("unchecked")
	public static <T> T packetCodecDecode(Object codec, Object buf) {
		return (T) PacketDecoder_decode.get().invoke(codec, buf);
	}
	private static final Supplier<Reflection.MethodInvoker> PacketEncoder_encode =
			Reflection.getOptionalMethod(() -> Reflection.getClass("net.minecraft.class_9142"), () -> "encode", () -> MethodType.methodType(void.class, Object.class, Object.class));
	public static void packetCodecEncode(Object codec, Object buf, Object value) {
		PacketEncoder_encode.get().invoke(codec, buf, value);
	}
	
	public static Entity createEntity(EntityType<?> entityType, Level world) {
		return entityType.create(world, EntitySpawnReason.COMMAND);
	}
	
	public static boolean hasPermissionLevel(Player player, int level) {
		return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
	}
	
	public static <T extends Comparable<T>> Collection<T> getValues(Property<T> property) {
		return property.getPossibleValues();
	}
	
}
