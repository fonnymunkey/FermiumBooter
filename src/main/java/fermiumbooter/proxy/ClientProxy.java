package fermiumbooter.proxy;

import fermiumbooter.handler.CompatibilityWarningHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void registerSubscribers() {
		MinecraftForge.EVENT_BUS.register(CompatibilityWarningHandler.class);
	}
}