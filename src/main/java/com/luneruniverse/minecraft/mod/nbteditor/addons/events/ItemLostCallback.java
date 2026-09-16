package com.luneruniverse.minecraft.mod.nbteditor.addons.events;

import com.luneruniverse.minecraft.mod.nbteditor.multiversion.ActionResult;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;

/**
 * Callback for when an item is detected to be lost<br>
 * Called before the lost item is set and the message is sent to the player
 */
public interface ItemLostCallback {
	Event<ItemLostCallback> EVENT = EventFactory.createArrayBacked(ItemLostCallback.class, listeners -> lostItem -> {
		for (ItemLostCallback listener : listeners) {
			ActionResult result = listener.onItemLost(lostItem);
			if (result != ActionResult.PASS)
				return result;
		}
		return ActionResult.PASS;
	});
	
	/**
	 * @param lostItem The item that was lost
	 * @return What should be done next
	 */
	ActionResult onItemLost(ItemStack lostItem);
}
