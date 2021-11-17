package yalter.mousetweaks.handlers;

import yalter.mousetweaks.IGuiScreenHandler;
import yalter.mousetweaks.MouseButton;
import yalter.mousetweaks.Reflection;
import yalter.mousetweaks.api.IMTModGuiContainer3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

public class IMTModGuiContainer3Handler implements IGuiScreenHandler {
	private MinecraftClient mc;
	private IMTModGuiContainer3 modGuiContainer;
	private Method handleMouseClick;

	public IMTModGuiContainer3Handler(IMTModGuiContainer3 modGuiContainer) {
		this.mc = MinecraftClient.getInstance();
		this.modGuiContainer = modGuiContainer;
		this.handleMouseClick = Reflection.getHMCMethod(modGuiContainer);
	}

	@Override
	public boolean isMouseTweaksDisabled() {
		return modGuiContainer.MT_isMouseTweaksDisabled();
	}

	@Override
	public boolean isWheelTweakDisabled() {
		return modGuiContainer.MT_isWheelTweakDisabled();
	}

	@Override
	public List<Slot> getSlots() {
		return modGuiContainer.MT_getContainer().slots;
	}

	@Override
	public Slot getSlotUnderMouse(double mouseX, double mouseY) {
		return modGuiContainer.MT_getSlotUnderMouse(mouseX, mouseY);
	}

	@Override
	public boolean disableRMBDraggingFunctionality() {
		return modGuiContainer.MT_disableRMBDraggingFunctionality();
	}

	@Override
	public void clickSlot(Slot slot, MouseButton mouseButton, boolean shiftPressed) {
		if (handleMouseClick != null) {
			try {
				handleMouseClick.invoke(modGuiContainer,
				                        slot,
				                        slot.id,
				                        mouseButton.getValue(),
				                        shiftPressed ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
			} catch (InvocationTargetException e) {
				CrashReport crashreport = CrashReport.create(e,
						                                  "handleMouseClick() threw an exception when "
				                                                   + "called from MouseTweaks.");
				throw new CrashException(crashreport);
			} catch (IllegalAccessException e) {
				CrashReport crashreport = CrashReport.create(e,
				                                                   "Calling handleMouseClick() from MouseTweaks.");
				throw new CrashException(crashreport);
			}
		} else {
			mc.interactionManager.clickSlot(modGuiContainer.MT_getContainer().syncId,
			                                      slot.id,
			                                      mouseButton.getValue(),
			                                      shiftPressed ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP,
			                                      mc.player);
		}
	}

	@Override
	public boolean isCraftingOutput(Slot slot) {
		return modGuiContainer.MT_isCraftingOutput(slot);
	}

	@Override
	public boolean isIgnored(Slot slot) {
		return modGuiContainer.MT_isIgnored(slot);
	}
}
