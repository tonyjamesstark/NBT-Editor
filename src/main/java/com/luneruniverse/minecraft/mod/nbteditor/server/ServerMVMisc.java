package com.luneruniverse.minecraft.mod.nbteditor.server;

import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;

import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
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
	
	@SuppressWarnings("unchecked")
	public static <T> T packetCodecDecode(Object codec, Object buf) {
		return (T) ((StreamDecoder<Object, T>) codec).decode(buf);
	}
	@SuppressWarnings("unchecked")
	public static void packetCodecEncode(Object codec, Object buf, Object value) {
		((StreamEncoder<Object, Object>) codec).encode(buf, value);
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
