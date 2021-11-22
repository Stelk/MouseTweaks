package yalter.mousetweaks;

public class Constants {
	public static final String MOD_ID = "mousetweaks";

	static final String CONFIG_RMB_TWEAK = "RMBTweak";
	static final String CONFIG_LMB_TWEAK_WITH_ITEM = "LMBTweakWithItem";
	static final String CONFIG_LMB_TWEAK_WITHOUT_ITEM = "LMBTweakWithoutItem";
	static final String CONFIG_WHEEL_TWEAK = "WheelTweak";
	static final String CONFIG_WHEEL_SEARCH_ORDER = "WheelSearchOrder";
	static final String CONFIG_WHEEL_SCROLL_DIRECTION = "WheelScrollDirection";
	static final String CONFIG_DEBUG = "Debug";
	static final String CONFIG_SCROLL_ITEM_SCALING = "ScrollItemScaling";

	// Names for reflection.
	public static final ObfuscatedName IGNOREMOUSEUP_NAME
			= new ObfuscatedName("cancelNextRelease", "f_97719_", "field_2798", "Q");
	public static final ObfuscatedName DRAGSPLITTING_NAME
			= new ObfuscatedName("cursorDragging", "f_97738_", "field_2794", "y");
	public static final ObfuscatedName DRAGSPLITTINGBUTTON_NAME
			= new ObfuscatedName("heldButtonCode", "f_97718_", "field_2778", "P");
	public static final ObfuscatedName GETSELECTEDSLOT_NAME
			= new ObfuscatedName("getSlotAt", "m_97744_", "method_2386", "a");
	static final ObfuscatedName HANDLEMOUSECLICK_NAME
			= new ObfuscatedName("onMouseClick", "m_6597_", "method_2383", "a");
}
