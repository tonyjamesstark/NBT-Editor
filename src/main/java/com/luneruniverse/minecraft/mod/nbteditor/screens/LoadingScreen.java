package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.util.Drawing;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVScreen;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;

public class LoadingScreen extends MVScreen {
	
	private static final int MAX_FREEZE_TIME = 200;
	private static final int MIN_LOADING_TIME = 200; // Prevent flicker
	
	public static <T> void show(CompletableFuture<T> future, Runnable onLoading, BiConsumer<Boolean, T> onFinish, BiConsumer<Boolean, Throwable> onException) {
		try {
			onFinish.accept(false, future.get(MAX_FREEZE_TIME, TimeUnit.MILLISECONDS));
			return;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			onException.accept(false, e.getCause());
			return;
		} catch (CancellationException e) {
			onException.accept(false, e);
			return;
		} catch (TimeoutException e) {
			// Expected
		}
		
		onLoading.run();
		Minecraft.getInstance().setScreenAndShow(new LoadingScreen(future, value -> onFinish.accept(true, value), e -> onException.accept(true, e)));
	}
	public static <T> void show(CompletableFuture<T> future, Runnable onLoading, BiConsumer<Boolean, T> onFinish) {
		show(future, onLoading, onFinish, (loaded, e) -> NBTEditor.LOGGER.error("Error processing something", e));
	}
	public static <T> void show(CompletableFuture<T> future, Consumer<T> onFinish) {
		show(future, () -> {}, (loaded, value) -> onFinish.accept(value));
	}
	
	private final CompletableFuture<?> future;
	private final Consumer<Object> onFinish;
	private final Consumer<Throwable> onException;
	private final long startTime;
	
	@SuppressWarnings("unchecked")
	private <T> LoadingScreen(CompletableFuture<T> future, Consumer<T> onFinish, Consumer<Throwable> onException) {
		super(Component.nullToEmpty("Loading"));
		
		this.future = future;
		this.onFinish = result -> onFinish.accept((T) result);
		this.onException = onException;
		this.startTime = System.currentTimeMillis();
	}
	
	@Override
	protected void init() {
		addRenderableWidget(Buttons.of(width / 2 - 75, height / 2, 150, 20, Component.translatableEscape("nbteditor.hide"), btn -> onClose()));
	}
	
	@Override
	public void tick() {
		super.tick();
		
		if (future.isDone() && System.currentTimeMillis() - startTime >= MIN_LOADING_TIME) {
			try {
				onFinish.accept(future.get());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				onException.accept(e.getCause());
			} catch (CancellationException e) {
				onException.accept(e);
			}
		}
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Drawing.renderBackground(this, context);
		super.extractRenderState(context, mouseX, mouseY, delta);
		Drawing.renderLogo(context);
		
		Drawing.drawCenteredTextWithShadow(context, font, Component.translatableEscape("nbteditor.loading"),
				width / 2, height / 2 - font.lineHeight / 2 - 10, -1);
	}
	
}
