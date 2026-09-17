package com.luneruniverse.minecraft.mod.nbteditor.multiversion;

import net.minecraft.resources.Identifier;
import net.minecraft.IdentifierException;

public class IdentifierInst {
	
	public static Identifier of(String id) throws IdentifierException {
		return Identifier.parse(id);
	}
	public static Identifier of(String namespace, String path) throws IdentifierException {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}
	
	public static boolean isValid(String id) {
		try {
			of(id);
			return true;
		} catch (IdentifierException e) {
			return false;
		}
	}
	public static boolean isValid(String namespace, String path) {
		try {
			of(namespace, path);
			return true;
		} catch (IdentifierException e) {
			return false;
		}
	}
	
}
