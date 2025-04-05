package fermiumbooter.config;

import fermiumbooter.FermiumBooter;
import net.minecraftforge.common.config.Config;

@Config(modid = FermiumBooter.MODID)
public class FermiumBooterConfig {
	
	@Config.Comment("Prevents the mixin compatibility warning text from rendering on screen" + "\n" +
			"Errors and warnings will still be printed to the log")
	@Config.Name("Suppress Mixin Compatibility Warnings Render")
	public static boolean suppressMixinCompatibilityWarningsRender = false;
	
	@Config.Comment("Disables config based mixin compatibility checks" + "\n" +
			"Warning: this may cause undefined behavior in mods, you should not enable this if not absolutely required" + "\n" +
			"Do not report issues to any mods if you have this enabled unless you want to be laughed at")
	@Config.Name("Override Mixin Config Compatibility Checks")
	@Config.RequiresMcRestart
	public static boolean overrideMixinCompatibilityChecks = false;
}