package fermiumbooter;

import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Arrays;
import java.util.List;

@Mod(modid = FermiumBooter.MODID, version = FermiumBooter.VERSION, name = FermiumBooter.NAME)
public class FermiumBooter
{
    public static final String MODID = "fermiumbooter";
    public static final String VERSION = "1.1.1";
    public static final String NAME = "FermiumBooter";

    private static final List<String> pedoModIds= Arrays.asList("mixinbooter","loliasm","naughthirium","flare","gregtech",
            "gtclassic","groovyscript","configanytime");
	@Instance(MODID)
	public static FermiumBooter instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        for(String pedoModId:pedoModIds)
        {
            if(Loader.isModLoaded(pedoModId))
                throw new ReportedException(new CrashReport(String.format("Pedophilic mod detected:%s",pedoModId),new IllegalArgumentException()));
        }
    }
}