package fermiumbooter;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

@IFMLLoadingPlugin.Name("FermiumBooter")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE-10)//Sorting index? Nah, alphabetical order based on jar name, whatever lol
public class FermiumPlugin implements IFMLLoadingPlugin {

	public static final Logger LOGGER = LogManager.getLogger("FermiumBooter");

	public FermiumPlugin() {
		MixinBootstrap.init();
		Mixins.addConfiguration("mixins.fermiumbooter.init.json");
	}

	@Override
	public String[] getASMTransformerClass()
	{
		return new String[0];
	}
	
	@Override
	public String getModContainerClass()
	{
		return null;
	}
	
	@Override
	public String getSetupClass()
	{
		return null;
	}

	/**
	 * Handle actually parsing and adding the early configurations here, as it gets called after all other plugins are initialized
	 * SortingIndex is either a sneaky liar, or drunk
	 */
	@Override
	public void injectData(Map<String, Object> data) {
		for(Map.Entry<String, Supplier<Boolean>> entry : FermiumRegistryAPI.getEarlyMixins().entrySet()) {
			//Check for removals
			if(FermiumRegistryAPI.getRejectMixins().contains(entry.getKey())) {
				LOGGER.warn("FermiumBooter received removal of \"" + entry.getKey() + "\" for early mixin application, rejecting.");
				continue;
			}
			//Check for enabled
			Boolean enabled = entry.getValue().get();
			if(enabled == null) {
				LOGGER.warn("FermiumBooter received null value for supplier from \"" + entry.getKey() + "\" for early mixin application, ignoring.");
				continue;
			}
			//Add configuration
			if(enabled) {
				LOGGER.info("FermiumBooter adding \"" + entry.getKey() + "\" for early mixin application.");
				Mixins.addConfiguration(entry.getKey());
			}
		}
	}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}