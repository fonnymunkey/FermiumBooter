package fermiumbooter;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

@IFMLLoadingPlugin.Name("FermiumBooter")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(990)
public class FermiumPlugin implements IFMLLoadingPlugin {

	public static final Logger LOGGER = LogManager.getLogger("FermiumBooter");

	public FermiumPlugin() {
		MixinBootstrap.init();
		MixinExtrasBootstrap.init();
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
	 */
	@Override
	public void injectData(Map<String, Object> data) {
		for(Map.Entry<String, List<Supplier<Boolean>>> entry : FermiumRegistryAPI.getEarlyMixins().entrySet()) {
			//Check for removals
			if(FermiumRegistryAPI.getRejectMixins().contains(entry.getKey())) {
				LOGGER.warn("FermiumBooter received removal of \"" + entry.getKey() + "\" for early mixin application, rejecting.");
				continue;
			}
			//Check for enabled
			Boolean enabled = null;
			for(Supplier<Boolean> supplier : entry.getValue()) {
				if(Boolean.TRUE.equals(enabled)) continue;//Short circuit OR
				Boolean supplied = supplier.get();
				if(supplied == null) LOGGER.warn("FermiumBooter received null value for individual supplier from \"" + entry.getKey() + "\" for early mixin application.");
				else enabled = supplied;
			}
			if(enabled == null) {
				LOGGER.warn("FermiumBooter received null value for suppliers from \"" + entry.getKey() + "\" for early mixin application, ignoring.");
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