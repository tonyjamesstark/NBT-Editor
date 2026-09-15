package com.luneruniverse.minecraft.mod.nbteditor.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import net.minecraft.resources.Identifier;

/** Reads a file bundled in the mod's own resources. */
public class ModResources {
	
	/**
	 * Opens a bundled resource, or empty if no pack supplies it.
	 *
	 * <p>The resource manager opens lazily inside a {@link Optional#map}, so a failure surfaces
	 * as an {@link UncheckedIOException} rather than the {@link IOException} the caller is
	 * already handling. This unwraps it back, preserving the original stack trace.
	 */
	public static Optional<InputStream> open(Identifier id) throws IOException {
		try {
			return MainUtil.client.getResourceManager().getResource(id).map(resource -> {
						try {
							return resource.open();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
		} catch (UncheckedIOException e) {
			if (e.getMessage() != null) {
				IOException checkedE = new IOException(e.getMessage(), e.getCause());
				checkedE.setStackTrace(e.getStackTrace());
				throw checkedE;
			}
			throw e.getCause();
		}
	}
	
}
