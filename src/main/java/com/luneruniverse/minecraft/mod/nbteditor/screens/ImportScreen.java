package com.luneruniverse.minecraft.mod.nbteditor.screens;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.NBTEditor;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalBlock;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalEntity;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalItem;
import com.luneruniverse.minecraft.mod.nbteditor.localnbt.LocalNBT;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVDrawableHelper;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.MVTooltip;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.ScreenTexts;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.Version;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.nbt.MVNbtCompoundParent;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.ImageToLoreWidget;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.ImportPosWidget;
import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.NamedTextFieldWidget;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.ItemTagReferences;
import com.luneruniverse.minecraft.mod.nbteditor.util.MainUtil;
import com.luneruniverse.minecraft.mod.nbteditor.util.TextUtil;

import com.luneruniverse.minecraft.mod.nbteditor.screens.widgets.Buttons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ImportScreen extends OverlaySupportingScreen {
	
	public static void importFiles(List<Path> paths, Optional<Integer> defaultDataVersion) {
		List<Consumer<BlockPos>> posConsumers = new ArrayList<>();
		
		for (Path path : paths) {
			File file = path.toFile();
			if (!file.isFile())
				continue;
			
			if (file.getName().endsWith(".nbt")) {
				try (FileInputStream in = new FileInputStream(file)) {
					CompoundTag nbt = MainUtil.readNBT(in);
					if (defaultDataVersion.isEmpty() && !nbt.nbte$contains("DataVersion", MVNbtCompoundParent.NUMBER_TYPE))
						MainUtil.client.player.sendSystemMessage(TextUtil.parseTranslatableFormatted("nbteditor.nbt.import.data_version.unknown", file.getName()));
					if (nbt.nbte$getIntOrDefault("DataVersion") > Version.getDataVersion())
						MainUtil.client.player.sendSystemMessage(TextInst.translatable("nbteditor.nbt.import.data_version.new", file.getName()));
					LocalNBT.deserialize(nbt, defaultDataVersion.orElse(Version.getDataVersion())).ifPresent(localNBT -> {
						if (localNBT instanceof LocalItem item)
							item.receive();
						else if (localNBT instanceof LocalBlock block)
							posConsumers.add(pos -> block.place(pos));
						else if (localNBT instanceof LocalEntity entity)
							posConsumers.add(pos -> entity.summon(MainUtil.client.level.dimension(), Vec3.atCenterOf(pos)));
					});
				} catch (Exception e) {
					NBTEditor.LOGGER.error("Error while importing a .nbt file", e);
					MainUtil.client.player.sendSystemMessage(TextInst.literal(e.getClass().getName() + ": " + e.getMessage()).withStyle(ChatFormatting.RED));
				}
				continue;
			}
		}
		
		if (!posConsumers.isEmpty()) {
			ImportPosWidget.openImportPos(MainUtil.client.player.blockPosition(),
					pos -> posConsumers.forEach(consumer -> consumer.accept(pos)));
			return;
		}
		
		ImageToLoreWidget.openImportFiles(paths, (file, imgLore) -> {
			String name = file.getName();
			int nameDot = name.lastIndexOf('.');
			if (nameDot != -1)
				name = name.substring(0, nameDot);
			
			ItemStack painting = new ItemStack(Items.PAINTING);
			painting.nbte$setCustomName(TextInst.literal(name).withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GOLD)));
			ItemTagReferences.LORE.set(painting, imgLore);
			MainUtil.getWithMessage(painting);
		}, () -> {});
	}
	
	private final List<Component> msg;
	private NamedTextFieldWidget dataVersion;
	
	public ImportScreen() {
		super(TextInst.of("Import"));
		msg = TextUtil.getLongTranslatableTextLines("nbteditor.nbt.import.desc");
	}
	
	@Override
	protected void init() {
		super.init();
		dataVersion = addRenderableWidget(
				new NamedTextFieldWidget(16, 64 + font.lineHeight * msg.size() + 16, 100, 16, dataVersion)
				.name(TextInst.translatable("nbteditor.nbt.import.data_version"))
				.tooltip(new MVTooltip("nbteditor.nbt.import.data_version.desc")));
		addRenderableWidget(Buttons.of(this.width - 116, this.height - 36, 100, 20, ScreenTexts.DONE, btn -> onClose()));
	}
	
	@Override
	protected void renderMain(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		dataVersion.setValid(dataVersion.getValue().isEmpty() ||
				Version.getDataVersion(dataVersion.getValue()).filter(value -> value <= Version.getDataVersion()).isPresent());
		
		MVDrawableHelper.renderBackground(this, context);
		super.renderMain(context, mouseX, mouseY, delta);
		for (int i = 0; i < msg.size(); i++)
			MVDrawableHelper.drawText(context, font, msg.get(i), 16, 64 + font.lineHeight * i, -1, true);
		MainUtil.renderLogo(context);
	}
	
	@Override
	public void onFilesDrop(List<Path> paths) {
		importFiles(paths, Version.getDataVersion(dataVersion.getValue()).filter(value -> value <= Version.getDataVersion()));
	}
	
}
