package fermiumbooter;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;

@Mod(modid = FermiumBooter.MODID, version = FermiumBooter.VERSION, name = FermiumBooter.NAME)
public class FermiumBooter
{
    public static final String MODID = "fermiumbooter";
    public static final String VERSION = "1.1.1";
    public static final String NAME = "FermiumBooter";
	
	@Instance(MODID)
	public static FermiumBooter instance;
}