package com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.hideflags;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.general.TagReference;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.HideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.TooltipDisplayComponentHideFlag;
import com.luneruniverse.minecraft.mod.nbteditor.tagreferences.specific.data.hideflags.TooltipHideFlag;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemStack;

public class HideFlagsTooltipDisplayComponentTagReference implements TagReference<Map<HideFlag, Boolean>, ItemStack> {
	
	@Override
	public Map<HideFlag, Boolean> get(ItemStack object) {
		TooltipDisplay display = object.get(DataComponents.TOOLTIP_DISPLAY);
		// HideFlagsScreen lays the toggles out in iteration order, and FLAGS is ordered on purpose.
		Map<HideFlag, Boolean> output = new LinkedHashMap<>();
		
		output.put(TooltipHideFlag.INSTANCE, display.hideTooltip());
		
		for (Map.Entry<DataComponentType<?>, HideFlag> component : TooltipDisplayComponentHideFlag.FLAGS.entrySet())
			output.put(component.getValue(), display.hiddenComponents().contains(component.getKey()));
		
		return output;
	}
	
	@Override
	public void set(ItemStack object, Map<HideFlag, Boolean> value) {
		if (value.isEmpty())
			return;
		
		TooltipDisplay display = object.get(DataComponents.TOOLTIP_DISPLAY);
		boolean hideTooltip = display.hideTooltip();
		LinkedHashSet<DataComponentType<?>> hiddenComponents = new LinkedHashSet<>(display.hiddenComponents());
		
		for (Map.Entry<HideFlag, Boolean> flag : value.entrySet()) {
			if (flag.getKey() == TooltipHideFlag.INSTANCE) {
				hideTooltip = flag.getValue();
				continue;
			}
			
			DataComponentType<?> component = ((TooltipDisplayComponentHideFlag) flag.getKey()).getComponent();
			if (flag.getValue())
				hiddenComponents.add(component);
			else
				hiddenComponents.remove(component);
		}
		
		object.set(DataComponents.TOOLTIP_DISPLAY,
				new TooltipDisplay(hideTooltip, hiddenComponents));
	}
	
}
