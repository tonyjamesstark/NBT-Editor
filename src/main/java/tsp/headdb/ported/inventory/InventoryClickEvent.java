package tsp.headdb.ported.inventory;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

public class InventoryClickEvent {
	private final Slot slot;
	private final int slotId;
	private final int button;
	private final ContainerInput actionType;
	private final ContainerInputMod clickType;
	
	public InventoryClickEvent(Slot slot, int slotId, int button, ContainerInput actionType, ContainerInputMod clickType) {
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
	public ContainerInput getActionType() {
		return actionType;
	}
	public ContainerInputMod getContainerInput() {
		return clickType;
	}
}
