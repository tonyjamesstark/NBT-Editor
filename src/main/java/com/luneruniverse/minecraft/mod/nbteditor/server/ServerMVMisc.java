package com.luneruniverse.minecraft.mod.nbteditor.server;

import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Reflection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.network.packet.Packet;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.world.World;

public class ServerMVMisc {
	
	private static final Supplier<Reflection.MethodInvoker> EntityTrackingListener_sendPacket =
			Reflection.getOptionalMethod(() -> Reflection.getClass("net.minecraft.class_5629"), () -> "method_14364", () -> MethodType.methodType(void.class, Packet.class));
	public static void sendS2CPacket(ServerPlayerEntity player, Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}
	
	public static boolean isInstanceOfVehicleInventory(NamedScreenHandlerFactory factory) {
		return (factory instanceof VehicleInventory);
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
	
	private static final Supplier<Reflection.MethodInvoker> EntityType_create =
			Reflection.getOptionalMethod(EntityType.class, "method_5883", MethodType.methodType(Entity.class, World.class));
	public static Entity createEntity(EntityType<?> entityType, World world) {
		return entityType.create(world, SpawnReason.COMMAND);
	}
	
	public static boolean hasPermissionLevel(PlayerEntity player, int level) {
		return player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(level)));
	}
	
	private static final Supplier<Reflection.MethodInvoker> Property_getValues =
			Reflection.getOptionalMethod(Property.class, "method_11898", MethodType.methodType(Collection.class));
	public static <T extends Comparable<T>> Collection<T> getValues(Property<T> property) {
		return property.getValues();
	}
	
}
