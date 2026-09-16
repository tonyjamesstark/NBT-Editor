package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.resources.Identifier;
import com.luneruniverse.minecraft.mod.nbteditor.util.ModResources;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class Version {
	
	private static String releaseTarget;
	public static String getReleaseTarget() {
		if (releaseTarget == null)
			readVersionJson();
		return releaseTarget;
	}
	
	private static Integer dataVersion;
	public static int getDataVersion() {
		if (dataVersion == null)
			readVersionJson();
		return dataVersion;
	}
	
	private static void readVersionJson() {
		try (InputStream in = Version.class.getResourceAsStream("/version.json");
				InputStreamReader reader = new InputStreamReader(in);) {
			JsonObject data = new Gson().fromJson(reader, JsonObject.class);
			
			if (data.has("release_target"))
				releaseTarget = data.get("release_target").getAsString();
			else {
				String id = data.get("id").getAsString();
				releaseTarget = id.split("\\+|-")[0];
			}
			
			dataVersion = data.get("world_version").getAsInt();
		} catch (IOException e) {
			throw new UncheckedIOException("Error trying to parse version.json", e);
		}
	}
	
	private static Map<String, Integer> dataVersions;
	public static Optional<Integer> getDataVersion(String version) {
		try {
			return Optional.of(Integer.parseInt(version));
		} catch (NumberFormatException e) {}
		
		if (dataVersions == null)
			readDataVersionsJson();
		return Optional.ofNullable(dataVersions.get(version));
	}
	
	private static Map<Integer, String> mcVersions;
	public static Optional<String> getMCVersion(int dataVersion) {
		if (mcVersions == null)
			readDataVersionsJson();
		return Optional.ofNullable(mcVersions.get(dataVersion));
	}
	
	private static void readDataVersionsJson() {
		try (InputStream in = ModResources.open(Identifier.fromNamespaceAndPath("nbteditor", "data_versions.json")).orElseThrow()) {
			dataVersions = new Gson().fromJson(new InputStreamReader(in), new TypeToken<Map<String, Integer>>() {}.getType());
			mcVersions = dataVersions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse data_versions.json", e);
		}
	}

}
