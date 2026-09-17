package com.luneruniverse.minecraft.mod.nbteditor.commands.factories;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.commands.ClientCommand;
import com.luneruniverse.minecraft.mod.nbteditor.containers.ContainerIOs;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVComponentType;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.manager.NBTManagers;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.BlockReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.NBTReferenceFilter;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ContainerItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.nbtreferences.itemreferences.ItemReference;
import com.luneruniverse.minecraft.mod.nbteditor.screens.factories.BookScreen;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.WrittenBookTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.StyleUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import com.luneruniverse.minecraft.mod.nbteditor.util.PlayerItems;

public class BookCommand extends ClientCommand {
	
	public static final NBTReferenceFilter BOOK_FILTER = NBTReferenceFilter.create(
			ref -> ref.getItem().getItem() == Items.WRITTEN_BOOK,
			ref -> {
				if (ref.getBlock() != Blocks.LECTERN)
					return false;
				LocalNBT nbt = ref.getLocalNBT();
				if (!ContainerIOs.isSupported(nbt))
					return false;
				ItemStack[] contents = ContainerIOs.read(nbt);
				return contents.length == 1 && contents[0].getItem() == Items.WRITTEN_BOOK;
			},
			null,
			Component.translatableEscape("nbteditor.no_ref.book"),
			Component.translatableEscape("nbteditor.no_hand.no_item.book"));
	
	public static boolean convertBookToWritable(ItemReference ref) {
		ItemStack item = ref.getItem().transmuteCopy(Items.WRITABLE_BOOK, 1);
		boolean formatted = false;
		List<Component> pages = WrittenBookTagReferences.PAGES.get(item);
		List<String> convertedPages = new ArrayList<>();
		for (Component page : pages) {
			if (!formatted && TextUtil.isTextFormatted(page, StyleUtil.BOOK_STYLE))
				formatted = true;
			convertedPages.add(page.getString());
		}
		ItemTagReferences.WRITABLE_BOOK_PAGES.set(item, convertedPages);
		item.remove(MVComponentType.WRITTEN_BOOK_CONTENT);
		if (formatted) {
			Minecraft.getInstance().player.sendSystemMessage(Component.translatableEscape("nbteditor.book.convert.formatting_saved"));
			PlayerItems.get(item, true);
		} else
			ref.saveItem(item, Component.translatableEscape("nbteditor.book.convert.success"));
		return !formatted;
	}
	
	@Override
	public String getName() {
		return "book";
	}
	
	@Override
	public String getExtremeAlias() {
		return "b";
	}
	
	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		builder.then(literal("make_writable").executes(context -> {
			getReference(BookCommand::convertBookToWritable);
			return Command.SINGLE_SUCCESS;
		})).then(literal("new").executes(context -> {
			ItemReference ref = ItemReference.getHeldAir();
			ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
			WrittenBookTagReferences.TITLE.set(book, "");
			WrittenBookTagReferences.AUTHOR.set(book, "");
			WrittenBookTagReferences.GENERATION.set(book, 0);
			WrittenBookTagReferences.PAGES.set(book, new ArrayList<>());
			ref.saveItem(book);
			Minecraft.getInstance().setScreenAndShow(new BookScreen(ref));
			return Command.SINGLE_SUCCESS;
		})).executes(context -> {
			getReference(ref -> Minecraft.getInstance().setScreenAndShow(new BookScreen(ref)));
			return Command.SINGLE_SUCCESS;
		});
	}
	
	private void getReference(Consumer<ItemReference> consumer) {
		NBTReference.getReference(BOOK_FILTER, false, ref -> {
			if (ref instanceof ItemReference itemRef)
				consumer.accept(itemRef);
			else if (ref instanceof BlockReference blockRef)
				consumer.accept(new ContainerItemReference<>(blockRef, 0));
		});
	}
	
}
