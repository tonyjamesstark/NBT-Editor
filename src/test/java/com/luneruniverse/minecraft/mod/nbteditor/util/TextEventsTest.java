package com.luneruniverse.minecraft.mod.nbteditor.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Style;

/**
 * Pins the lifetime of a run-click handler. The registry behind
 * {@link TextEvents#withRunClickEvent} used to be a strong map that nothing ever removed from, so
 * every handler, and every screen one had captured, survived until the game closed.
 */
class TextEventsTest {

	/** Attaches a handler, then drops every reference to the text carrying its id. */
	private static String attachAndForget(Runnable onClick) {
		Style style = TextEvents.withRunClickEvent(Style.EMPTY, onClick);
		return new String(TextEvents.ClickAction.OPEN_FILE.getStringifiedValue(style.getClickEvent()));
	}

	@Test
	void aClickOnTheAttachedIdRunsTheHandler() {
		AtomicBoolean ran = new AtomicBoolean();
		Style style = TextEvents.withRunClickEvent(Style.EMPTY, () -> ran.set(true));
		String id = TextEvents.ClickAction.OPEN_FILE.getStringifiedValue(style.getClickEvent());

		assertTrue(TextEvents.tryRunClickEvent(id), "the id the style carries is registered");
		assertTrue(ran.get(), "the handler ran");
	}

	@Test
	void anUnknownIdIsDeclinedRatherThanSwallowed() {
		assertFalse(TextEvents.tryRunClickEvent("/home/someone/a-real-file.txt"));
	}

	@Test
	void aHandlerIsReleasedOnceItsTextIsGone() throws InterruptedException {
		AtomicBoolean ran = new AtomicBoolean();
		String id = attachAndForget(() -> ran.set(true));

		for (int attempt = 0; attempt < 50; attempt++) {
			System.gc();
			if (!TextEvents.tryRunClickEvent(id)) {
				assertFalse(ran.get(), "a released handler is not run");
				return;
			}
			Thread.sleep(10);
		}
		throw new AssertionError("the handler outlived the text carrying its id");
	}

}
