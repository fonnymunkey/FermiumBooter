package fermiumbooter.handler;

import fermiumbooter.config.FermiumBooterConfig;
import fermiumbooter.util.FermiumMixinConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public abstract class CompatibilityWarningHandler {
	
	private static String warningMessage = null;
	
	@SubscribeEvent
	public static void render(TickEvent.RenderTickEvent event) {
		if(FermiumBooterConfig.suppressMixinCompatibilityWarningsRender) return;
		if(event.phase != TickEvent.Phase.END) return;
		
		int warningCount = FermiumMixinConfigHandler.getWarningCount();
		if(warningCount <= 0) return;
		
		if(warningMessage == null) warningMessage = String.format("FermiumBooter found %d possible mixin compat errors, check your log", warningCount);
		
		Minecraft minecraft = Minecraft.getMinecraft();
		FontRenderer fontRenderer = minecraft.fontRenderer;
		int width = fontRenderer.getStringWidth(warningMessage);
		int font = fontRenderer.FONT_HEIGHT;
		
		GlStateManager.pushMatrix();
		Gui.drawRect(font, font, font + width + 4, font * 2 + 2, Integer.MIN_VALUE);
		fontRenderer.drawString(warningMessage, font + 2, font + 2, 16711680);
		GlStateManager.popMatrix();
	}
}