package yalter.mousetweaks;

import org.lwjgl.glfw.GLFW;
import yalter.mousetweaks.api.IMTModGuiContainer3;
import yalter.mousetweaks.api.IMTModGuiContainer3Ex;
import yalter.mousetweaks.handlers.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class Main {
	public static Config config;

	private static MinecraftClient mc;

	private static IGuiScreenHandler handler = null;
	private static boolean disableWheelForThisContainer = false;
	private static Slot oldSelectedSlot = null;
	private static double accumulatedScrollDelta = 0;
	private static boolean canDoLMBDrag = false;
	private static boolean canDoRMBDrag = false;

	private static boolean initialized = false;

	public static void initialize() {
		Logger.Log("Main.initialize()");

		if (initialized)
			return;

		mc = MinecraftClient.getInstance();

		config = new Config(mc.runDirectory.getAbsolutePath() + File.separator + "config" + File.separator + "MouseTweaks.cfg");
		config.read();

		Reflection.reflectGuiContainer();

		Logger.Log("Initialized.");
		initialized = true;
	}

	/**
	 * Call on opening or closing a GuiScreen.
	 * @param newScreen The newly opened screen, or null on closing.
	 */
	public static void onGuiOpen(Screen newScreen) {
	    // Reset the state.
		handler = null;
		oldSelectedSlot = null;
		accumulatedScrollDelta = 0;
		canDoLMBDrag = false;
		canDoRMBDrag = false;

		if (newScreen != null) {
			Logger.DebugLog("You have just opened a " + newScreen.getClass().getSimpleName() + ".");

			config.read();

            handler = findHandler(newScreen);
            if (handler == null) {
                Logger.DebugLog("No valid handler found; Mouse Tweaks is disabled.");
            } else {
                boolean disableForThisContainer = handler.isMouseTweaksDisabled();
                disableWheelForThisContainer = handler.isWheelTweakDisabled();

                Logger.DebugLog("Handler: "
                        + handler.getClass().getSimpleName()
                        + "; Mouse Tweaks is "
                        + (disableForThisContainer ? "disabled" : "enabled")
                        + "; wheel tweak is "
                        + (disableWheelForThisContainer ? "disabled" : "enabled")
                        + ".");

				if (disableForThisContainer)
					handler = null;
            }
		}
	}

	/**
	 * Call when a mouse button is clicked.
	 * @param x Mouse X.
	 * @param y Mouse Y.
	 * @param button The button that was clicked.
	 * @return True if the event was handled and should be cancelled.
	 */
	public static boolean onMouseClicked(double x, double y, MouseButton button) {
	    if (handler == null)
	    	return false;

		// Store the currently selected slot.
		Slot selectedSlot = handler.getSlotUnderMouse(x, y);
		oldSelectedSlot = selectedSlot;

		// Stack that the player is currently "holding" on the mouse cursor.
		ItemStack stackOnMouse = mc.player.currentScreenHandler.getCursorStack();

		if (button == MouseButton.LEFT) {
			// If the stack on mouse isn't empty, the vanilla LMB dragging mechanic is going to start. We don't want to
			// interfere with that.
			if (stackOnMouse.isEmpty())
				canDoLMBDrag = true;
		} else if (button == MouseButton.RIGHT) {
			// We're only interested in the RMB tweak when there's something on the mouse.
			if (stackOnMouse.isEmpty())
				return false;

			// Check if the RMB tweak is enabled right away, as otherwise the vanilla RMB dragging mechanic should
			// occur.
			if (!config.rmbTweak)
				return false;

			// Let players right click bundles. Cancel the event so vanilla RMB dragging doesn't start.
			// FIXME: a better solution would be to still start the RMB drag and then forward through the release event,
			// so that you can still start a Mouse Tweaks RMB drag from a bundle, as you can with the vanilla mechanic.
			// FIXME: an even better solution would be to *not* cancel any events so Mouse Tweaks doesn't break any
			// mod items similar to bundles too.
			if (selectedSlot != null && selectedSlot.getStack().getItem() instanceof BundleItem)
				return true;

			// Set the flag, right-click an item right away, and cancel the event so the vanilla RMB dragging doesn't
			// happen.
			canDoRMBDrag = true;

			if (selectedSlot != null)
				rmbTweakNewSlot(selectedSlot, stackOnMouse);

			return true;
		}

		return false;
	}

	/**
	 * Call to handle a new selected slot for the RMB tweak.
	 *
	 * This method assumes a number of checks have already been done, such as that RMB tweak is enabled, or that the
	 * selected slot isn't null, or the stack on mouse isn't empty.
	 * @param selectedSlot The new selected slot.
	 * @param stackOnMouse The stack currently "held" on the mouse cursor.
	 */
	private static void rmbTweakNewSlot(Slot selectedSlot, ItemStack stackOnMouse) {
		assert selectedSlot != null;
		assert !stackOnMouse.isEmpty();

		// Don't act on ignored slots.
		if (handler.isIgnored(selectedSlot))
			return;

		// Can't put items into crafting output slots.
		if (handler.isCraftingOutput(selectedSlot))
			return;

		// If the stacks are incompatible, we can't right click.
		ItemStack selectedSlotStack = selectedSlot.getStack();
		if (!areStacksCompatible(selectedSlotStack, stackOnMouse))
			return;

		// Return if we cannot put any more items into the slot.
		if (selectedSlotStack.getCount() == selectedSlotStack.getMaxCount())
			return;

		handler.clickSlot(selectedSlot, MouseButton.RIGHT, false);
	}

	/**
	 * Call when a mouse button is released.
	 * @param x Mouse X.
	 * @param y Mouse Y.
	 * @param button The button that was released.
	 * @return True if the event was handled and should be cancelled.
	 */
	public static boolean onMouseReleased(double x, double y, MouseButton button) {
		if (handler == null)
			return false;

		// Reset the flags.
		if (button == MouseButton.LEFT)
			canDoLMBDrag = false;
		else if (button == MouseButton.RIGHT) {
			if (canDoRMBDrag) {
				canDoRMBDrag = false;

				// Cancel the release event to prevent an extra item from being inserted into the selected slot.
				return true;
			}
		}

		return false;
	}

	/**
	 * Call when the mouse is dragged.
	 * @param x New mouse X.
	 * @param y New mouse Y.
	 * @param button Currently active button.
	 * @return True if the event was handled and should be cancelled.
	 */
	public static boolean onMouseDrag(double x, double y, MouseButton button) {
		if (handler == null)
			return false;

		Slot selectedSlot = handler.getSlotUnderMouse(x, y);

		// If the mouse hasn't moved to a new slot, we don't need to do anything.
		if (selectedSlot == oldSelectedSlot)
			return false;

		oldSelectedSlot = selectedSlot;

		// If no slot was selected, we don't need to do anything.
		if (selectedSlot == null)
			return false;

		// If the selected slot is ignored, we don't need to do anything.
		if (handler.isIgnored(selectedSlot))
			return false;

		// Stack that the player is currently "holding" on the mouse cursor.
		ItemStack stackOnMouse = mc.player.currentScreenHandler.getCursorStack();

		// At this point the mouse has just entered a new, non-ignored slot.

		if (button == MouseButton.LEFT) {
			if (!canDoLMBDrag)
				return false;

			// LMB tweaks don't do anything for empty slots.
			ItemStack selectedSlotStack = selectedSlot.getStack();
			if (selectedSlotStack.isEmpty())
				return false;

			boolean shiftIsDown = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)
					|| InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);

			if (stackOnMouse.isEmpty()) {
				// Shift-LMB drag without item.
				if (!config.lmbTweakWithoutItem || !shiftIsDown)
					return false;

				// Shift-left click the newly selected slot.
				handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
			} else {
				// (Shift-)LMB drag with item.
				if (!config.lmbTweakWithItem)
					return false;

				// Here we only click on slots with the same item.
				if (!areStacksCompatible(selectedSlotStack, stackOnMouse))
					return false;

				if (shiftIsDown) {
					// If shift is down, just shift-click on the slot, and the items get moved to the other inventory.
					handler.clickSlot(selectedSlot, MouseButton.LEFT, true);
				} else {
					// If shift is not down, we need to merge the item stack on the mouse with the one in the slot.
					// However, if the slot stack contains more items than can still fit on mouse, clicking on it will
					// result in filling the slot with the maximum item count and leaving only the remaining items on
					// the mouse, without any way to get the items back onto the mouse, which is not what we want for
					// the LMB tweak.
					if (stackOnMouse.getCount() + selectedSlotStack.getCount() > stackOnMouse.getMaxCount())
						return false;

					// We need to click on the slot so that our item stack gets merged with it, and then click again to
					// return the stack to the mouse. However, if the slot is crafting output, then the item is added to
					// the mouse stack on the first click and we don't need to click the second time.
					handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

					if (!handler.isCraftingOutput(selectedSlot))
						handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
				}
			}
		} else if (button == MouseButton.RIGHT) {
			if (!canDoRMBDrag)
				return false;

			// RMB tweak doesn't do anything when the stack on mouse is empty.
			if (stackOnMouse.isEmpty())
				return false;

			// We don't check config.rmbTweak here because it's already checked when setting canDoRMBDrag.

			rmbTweakNewSlot(selectedSlot, stackOnMouse);
		}

		return false;
	}

	/**
	 * Call when a scroll is registered.
	 * @param x Mouse X.
	 * @param y Mouse Y.
	 * @param scrollDelta The scroll delta.
	 * @return True if the event was handled and should be cancelled.
	 */
	public static boolean onMouseScrolled(double x, double y, double scrollDelta) {
	    // Check if the wheel tweak is disabled.
		if (handler == null || disableWheelForThisContainer || !config.wheelTweak)
			return false;

		// Check if the scroll is above a non-ignored slot.
		Slot selectedSlot = handler.getSlotUnderMouse(x, y);
		if (selectedSlot == null || handler.isIgnored(selectedSlot))
			return false;

		// If we're above a valid slot, always handle the scroll, even if no items are moved. This is to prevent
		// surprising behavior when e.g. the player scrolls items out of a slot, the items run out and suddenly
		// scrolling starts doing something else (being handled by another mod).

		// Perform scroll delta scaling and accumulation the same way it's done in MouseHelper.scrollCallback().
		double scaledDelta = config.scrollItemScaling.scale(scrollDelta);
		if (accumulatedScrollDelta != 0 && Math.signum(scaledDelta) != Math.signum(accumulatedScrollDelta))
			accumulatedScrollDelta = 0;

		accumulatedScrollDelta += scaledDelta;
		int delta = (int)accumulatedScrollDelta; // delta is the number of items to move, sign controls the direction.
		accumulatedScrollDelta -= (double)delta;

		// Return if we didn't accumulate any scrolls.
		if (delta == 0)
			return true;

		List<Slot> slots = handler.getSlots();

		// Split delta into the number of items to move and the direction.
		int numItemsToMove = Math.abs(delta);
		boolean pushItems = (delta < 0);
		if (config.wheelScrollDirection.isPositionAware() && otherInventoryIsAbove(selectedSlot, slots)) {
			pushItems = !pushItems;
		}
		if (config.wheelScrollDirection.isInverted()) {
			pushItems = !pushItems;
		}

		// Return if the selected slot is empty.
		ItemStack selectedSlotStack = selectedSlot.getStack();
		if (selectedSlotStack.isEmpty())
			return true;

		// Stack that the player is currently "holding" on the mouse cursor.
		ItemStack stackOnMouse = mc.player.currentScreenHandler.getCursorStack();

		// Scrolling over a crafting output slot requires special handling as those slots behave differently.
		if (handler.isCraftingOutput(selectedSlot)) {
			// Pulling from a crafting output slot only works if the stack is compatible to that on the mouse.
			if (!areStacksCompatible(selectedSlotStack, stackOnMouse))
				return true;

			if (stackOnMouse.isEmpty()) {
				// Can't pull into the crafting output slot.
			    if (!pushItems)
			    	return true;

			    while (numItemsToMove-- > 0) {
			        // Crafting outputs batches of items, we need to be able to distribute the whole batch at once.
			    	List<Slot> targetSlots = findPushSlots(slots, selectedSlot, selectedSlotStack.getCount(), true);

			    	// If we can't distribute the batch, do nothing.
			    	if (targetSlots == null)
			    		break;

			    	// Grab the item batch from the crafting output slot.
			    	handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

			    	// Distribute the items.
					for (int i = 0; i < targetSlots.size(); i++) {
						Slot slot = targetSlots.get(i);

						if (i == targetSlots.size() - 1) {
							// If this is the last slot, just left-click it to put the remaining items in.
							handler.clickSlot(slot, MouseButton.LEFT, false);
						} else {
							// Otherwise right click the needed number of times.
                            int clickTimes = slot.getStack().getMaxCount() - slot.getStack().getCount();
							while (clickTimes-- > 0)
								handler.clickSlot(slot, MouseButton.RIGHT, false);
						}
					}
				}
			} else {
				// Retrieve items from the slot by left-clicking it.
				while (numItemsToMove-- > 0)
					handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
			}

			return true;
		}

		// It's not possible to interact with the slot cleanly if the player is holding a compatible stack.
		if (!stackOnMouse.isEmpty() && areStacksCompatible(selectedSlotStack, stackOnMouse))
			return true;

        if (pushItems) {
			// If the stack on mouse isn't empty, it should be possible to put it into the selected slot.
			if (!stackOnMouse.isEmpty() && !selectedSlot.canInsert(stackOnMouse))
				return true;

        	// Clip the number of items to move by the available item count.
            numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.getCount());

            // Distribute them.
            List<Slot> targetSlots = findPushSlots(slots, selectedSlot, numItemsToMove, false);
            // Always non-null because mustDistributeAll is false.
			assert targetSlots != null;

			// If no target slots were found, don't pick up the items in first place.
			if (targetSlots.isEmpty())
				return true;

            // Click the selected slot to pick the items up.
			handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

			// Click the target slots.
			for (Slot slot : targetSlots) {
				// Right click the needed number of times.
				//
				// Can't do the last slot left click optimization, because we usually want to move less items (1) than
				// the whole available stack.
				int clickTimes = slot.getStack().getMaxCount() - slot.getStack().getCount();
				clickTimes = Math.min(clickTimes, numItemsToMove);
				numItemsToMove -= clickTimes;

				while (clickTimes-- > 0)
					handler.clickSlot(slot, MouseButton.RIGHT, false);
			}

			// Click the selected slot again to
			// 1) put down potentially left over items;
			// 2) pick up whatever we had on mouse originally.
			handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

			return true;
        }

        // Handle pulling items.
        // Clip the number of items to move by the maximum item count that would fit in the slot.
        int maxItemsToMove = selectedSlotStack.getMaxCount() - selectedSlotStack.getCount();
		numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
			// Find a slot to pull from.
			Slot targetSlot = findPullSlot(slots, selectedSlot);
			if (targetSlot == null)
				break;

			int numItemsInTargetSlot = targetSlot.getStack().getCount();

			if (handler.isCraftingOutput(targetSlot)) {
				// When pulling from the crafting output slot, one mouse wheel tick equals one batch of crafted items.
				//
				// Also, for safety, we'll pull only one batch at a time (in order to not trigger any unwanted behavior
				// when crafting runs out of ingredients causing the output to possibly change to a different one).

				// If the selected slot does not have enough room for the whole crafting output batch, break.
				if (maxItemsToMove < numItemsInTargetSlot)
					break;

				maxItemsToMove -= numItemsInTargetSlot;
				// maxItemsToMove is always the highest possible number of items to move, because each tick we move
				// either one or more items.
				numItemsToMove = Math.min(numItemsToMove - 1, maxItemsToMove);

				// If the stack on mouse isn't empty, it should be possible to put it into the selected slot.
				if (!stackOnMouse.isEmpty() && !selectedSlot.canInsert(stackOnMouse))
					break;

				// Click the selected slot to put down the possibly non-empty stack on mouse and pick up the items.
                handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

				// Click the crafting output slot to get the crafting output batch.
				handler.clickSlot(targetSlot, MouseButton.LEFT, false);

				// Click the selected slot again to put the items back.
				handler.clickSlot(selectedSlot, MouseButton.LEFT, false);

				continue;
			}

			// Compute the number of items we want to move from that slot.
			int numItemsToMoveFromTargetSlot = Math.min(numItemsToMove, numItemsInTargetSlot);
			maxItemsToMove -= numItemsToMoveFromTargetSlot;
			numItemsToMove -= numItemsToMoveFromTargetSlot;

			// If the stack on mouse isn't empty, it should be possible to put it into the target slot.
			if (!stackOnMouse.isEmpty() && !targetSlot.canInsert(stackOnMouse))
				break;

			// Click the target slot to pick up the items and put the items on mouse there.
			handler.clickSlot(targetSlot, MouseButton.LEFT, false);

			if (numItemsToMoveFromTargetSlot == numItemsInTargetSlot) {
				// If we want to move all items from the target slot, just left click the selected slot.
				handler.clickSlot(selectedSlot, MouseButton.LEFT, false);
			} else {
				// Otherwise right click the required number of times.
				for (int i = 0; i < numItemsToMoveFromTargetSlot; i++)
					handler.clickSlot(selectedSlot, MouseButton.RIGHT, false);
			}

			// Click the target slot again to put the left-over items back and pick up the stack that was on mouse
			// originally.
			handler.clickSlot(targetSlot, MouseButton.LEFT, false);
		}

		return true;
	}

	// Returns true if the other inventory is above the selected slot inventory.
	//
	// This is used for the inventory position aware scroll direction. To prevent any surprises, this should have the
	// same logic for what constitutes the "other" inventory as findWheelApplicableSlot().
	private static boolean otherInventoryIsAbove(Slot selectedSlot, List<Slot> slots) {
		boolean selectedIsInPlayerInventory = selectedSlot.inventory == mc.player.getInventory();
		for (Slot slot : slots) {
			if ((slot.inventory == mc.player.getInventory()) != selectedIsInPlayerInventory
			    && slot.y < selectedSlot.y) {
				return true;
			}
		}
		return false;
	}

	// Finds the appropriate handler to use with this GuiScreen. Returns null if no handler was found.
	private static IGuiScreenHandler findHandler(Screen currentScreen) {
		if (currentScreen instanceof IMTModGuiContainer3Ex) {
			return new IMTModGuiContainer3ExHandler((IMTModGuiContainer3Ex) currentScreen);
		} else if (currentScreen instanceof IMTModGuiContainer3) {
			return new IMTModGuiContainer3Handler((IMTModGuiContainer3) currentScreen);
		} else if (currentScreen instanceof CreativeInventoryScreen) {
			return new GuiContainerCreativeHandler((CreativeInventoryScreen) currentScreen);
		} else if (currentScreen instanceof HandledScreen) {
			return new GuiContainerHandler((HandledScreen) currentScreen);
		}

		return null;
	}

	// Returns true if we can put items from one stack into another.
	// This is different from ItemStack.areItemsEqual() because here empty stacks are compatible with anything.
	private static boolean areStacksCompatible(ItemStack a, ItemStack b) {
		return a.isEmpty() || b.isEmpty() || (a.isItemEqualIgnoreDamage(b) && ItemStack.areNbtEqual(a, b));
	}

	/**
	 * Finds a slot to pull items from.
	 * @param slots Slots to search.
	 * @param selectedSlot Slot where items will be pulled into.
	 * @return The applicable target slot, if any.
	 */
	private static Slot findPullSlot(List<Slot> slots, Slot selectedSlot) {
		int startIndex, endIndex, direction;
		if (config.wheelSearchOrder == WheelSearchOrder.FIRST_TO_LAST) {
			startIndex = 0;
			endIndex = slots.size();
			direction = 1;
		} else {
			startIndex = slots.size() - 1;
			endIndex = -1;
			direction = -1;
		}

		ItemStack selectedSlotStack = selectedSlot.getStack();
		boolean findInPlayerInventory = (selectedSlot.inventory != mc.player.getInventory());

		for (int i = startIndex; i != endIndex; i += direction) {
			Slot slot = slots.get(i);

			// Skip ignored slots.
			if (handler.isIgnored(slot))
				continue;

			boolean slotInPlayerInventory = (slot.inventory == mc.player.getInventory());
			if (findInPlayerInventory != slotInPlayerInventory)
				continue;

			ItemStack stack = slot.getStack();

			// Can't pull from an empty stack.
			if (stack.isEmpty())
				continue;

			// Can't pull from an incompatible stack.
			if (!areStacksCompatible(selectedSlotStack, stack))
				continue;

			return slot;
		}

		return null;
	}

	/**
	 * Finds target slots to push items into.
	 * @param slots Slots to search.
	 * @param selectedSlot Slot where items will be pushed from.
	 * @param itemCount Number of items to distribute.
	 * @param mustDistributeAll If true, return null if only part of the items can be distributed.
	 * @return List of target slots among which all items can be distributed, in order of priority.
	 */
	private static List<Slot> findPushSlots(List<Slot> slots, Slot selectedSlot, int itemCount, boolean mustDistributeAll) {
		ItemStack selectedSlotStack = selectedSlot.getStack();
		boolean findInPlayerInventory = (selectedSlot.inventory != mc.player.getInventory());

		List<Slot> rv = new ArrayList<>();
		// Applicable empty slots, they can be used once applicable non-empty slots run out.
		List<Slot> goodEmptySlots = new ArrayList<>();

		for (int i = 0; i != slots.size() && itemCount > 0; i++) {
			Slot slot = slots.get(i);

			// Skip ignored slots.
			if (handler.isIgnored(slot))
				continue;

			boolean slotInPlayerInventory = (slot.inventory == mc.player.getInventory());
			if (findInPlayerInventory != slotInPlayerInventory)
				continue;

			// Skip crafting output slots.
			if (handler.isCraftingOutput(slot))
				continue;

			ItemStack stack = slot.getStack();

			if (stack.isEmpty()) {
			    // Empty slots need to be able to accept the target item.
				if (slot.canInsert(selectedSlotStack)) {
                    goodEmptySlots.add(slot);
				}
			} else {
			    // Non-empty slots should have a compatible stack, not maxed out.
				if (areStacksCompatible(selectedSlotStack, stack) && stack.getCount() < stack.getMaxCount()) {
					rv.add(slot);
					itemCount -= Math.min(itemCount, stack.getMaxCount() - stack.getCount());
				}
			}
		}

		// If some items still weren't distributed, use the empty slots.
		for (int i = 0; i != goodEmptySlots.size() && itemCount > 0; i++) {
		    Slot slot = goodEmptySlots.get(i);
		    rv.add(slot);
		    itemCount -= Math.min(itemCount, slot.getStack().getMaxCount() - slot.getStack().getCount());
		}

		// If we were unable to distribute all items as requested, return null.
		if (mustDistributeAll && itemCount > 0)
			return null;

		return rv;
	}
}
