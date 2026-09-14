package tsp.headdb.ported.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

public class InventoryClickEvent {
	private final Slot slot;
	private final int slotId;
	private final int button;
	private final ClickType actionType;
	private final ClickTypeMod clickType;
	
	public InventoryClickEvent(Slot slot, int slotId, int button, ClickType actionType, ClickTypeMod clickType) {
		this.slot = slot;
		this.slotId = slotId;
		this.button = button;
		this.actionType = actionType;
		this.clickType = clickType;
	}
	
	public Slot getSlot() {
		return slot;
	}
	public int getSlotId() {
		return slotId;
	}
	public int getButton() {
		return button;
	}
	public ClickType getActionType() {
		return actionType;
	}
	public ClickTypeMod getClickType() {
		return clickType;
	}
}
