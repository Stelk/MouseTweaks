package yalter.mousetweaks.handlers;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.screen.slot.Slot;

public class GuiContainerCreativeHandler extends GuiContainerHandler {
	public GuiContainerCreativeHandler(CreativeInventoryScreen guiContainerCreative) {
		super(guiContainerCreative);
	}

	@Override
	public boolean isIgnored(Slot slot) {
		return (super.isIgnored(slot) || slot.inventory != mc.player.getInventory());
	}
}
