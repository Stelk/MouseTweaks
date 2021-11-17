package yalter.mousetweaks.fabric.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yalter.mousetweaks.Main;
import yalter.mousetweaks.MouseButton;

@Mixin(Mouse.class)
public abstract class MixinMouseHandler {
    @Shadow
    private int activeButton;

    @Dynamic("Lambda that calls screen.mouseDragged()")
    @Inject(method = "method_1602(Lnet/minecraft/client/gui/screen/Screen;DDDD)V", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(Screen screen, double x, double y, double dx, double dy, CallbackInfo ci) {
        MouseButton button = MouseButton.fromEventButton(this.activeButton);
        if (button != null) {
            if (Main.onMouseDrag(x, y, button)) {
                ci.cancel();
            }
        }
    }
}
//@Inject(method = "lambda$onMove$11(Lnet/minecraft/client/gui/screens/Screen;DDDD)V", at = @At("HEAD"), cancellable = true)
