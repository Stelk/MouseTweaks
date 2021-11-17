package yalter.mousetweaks;

import java.util.List;
import net.minecraft.screen.slot.Slot;

public interface IGuiScreenHandler {
	boolean isMouseTweaksDisabled();

	boolean isWheelTweakDisabled();

	List<Slot> getSlots();

	Slot getSlotUnderMouse(double mouseX, double mouseY);

	boolean disableRMBDraggingFunctionality();

	void clickSlot(Slot slot, MouseButton mouseButton, boolean shiftPressed);

	boolean isCraftingOutput(Slot slot);

	boolean isIgnored(Slot slot);
}
