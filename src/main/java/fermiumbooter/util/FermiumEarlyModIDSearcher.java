package fermiumbooter.util;

import com.google.gson.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.relauncher.libraries.Artifact;
import net.minecraftforge.fml.relauncher.libraries.LibraryManager;
import net.minecraftforge.fml.relauncher.libraries.Repository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Janky simplified implementation of FML's ModDiscoverer/ModCandidate/ContainerType/JarDiscoverer
 * Searches for both mcmod.info and @Mod annotations, since some mods only have one or the other
 * Some also have neither or set it improperly and give me a headache so they have manual compatibility
 * Could it be better? Yes. Am I tired and just want to add tiny dumb feature? Yes.
 * Who even reads these
 * Maybe one day I'll improve it
 */
public abstract class FermiumEarlyModIDSearcher {
	
	private static final Logger LOGGER = LogManager.getLogger("FermiumEarlyModIDSearcher");
	
	private static final Pattern classFilePattern = Pattern.compile("[^\\s\\$]+(\\$[^\\s]+)?\\.class$");
	
	private static Set<String> earlyModIDs = null;
	
	public static boolean isModPresent(String modID) {
		if(earlyModIDs == null) earlyModIDs = searchEarlyModIDs();
		if(modID == null || modID.isEmpty()) return false;
		return earlyModIDs.contains(modID);
	}
	
	//Not the most efficient implementation possible, but only takes around 2-3 seconds or so total even in relatively large packs
	private static Set<String> searchEarlyModIDs() {
		LOGGER.log(Level.INFO, "FermiumEarlyModIDSearcher beginning early ModID search.");
		
		Set<String> modIDs = new HashSet<>(Arrays.asList("minecraft", "mcp", "FML", "forge"));
		
		File mcDir = new File(".");
		List<Artifact> maven_canidates = LibraryManager.flattenLists(mcDir);
		List<File> file_canidates = LibraryManager.gatherLegacyCanidates(mcDir);
		for(Artifact artifact : maven_canidates) {
			artifact = Repository.resolveAll(artifact);
			if(artifact != null) {
				File target = artifact.getFile();
				if(!file_canidates.contains(target)) {
					file_canidates.add(target);
				}
			}
		}
		
		for(File modFile : file_canidates) {
			searchFile(modFile, modIDs);
		}
		
		LOGGER.log(Level.INFO, "FermiumEarlyModIDSearcher finished early ModID search, found {} ModIDs.", modIDs.size());
		
		return modIDs;
	}
	
	private static void searchFile(File modFile, Set<String> modIDs) {
		try(JarFile jar = new JarFile(modFile)) {
			//mcmod.info search
			ZipEntry modInfo = jar.getEntry("mcmod.info");
			if(modInfo != null) {
				try(InputStream inputStream = jar.getInputStream(modInfo)) {
					InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
					JsonElement rootElement = new JsonParser().parse(reader);
					//Manually search instead of properly parsing to class to avoid classloading issues
					searchModInfoRecursive(rootElement, modIDs);
				}
				catch(Exception ignored) { }
				//It'd be faster to break early if modIDs are found from the mcmod.info
				//Unfortunately some mods are dumb and set their modid's in the file incorrectly
			}
			
			//mod annotation search
			Type modType = Type.getType(Mod.class);
			for(ZipEntry ze : Collections.list(jar.entries())) {
				//Skip irrelevant paths/files that waste time
				if(ze.getName().contains("__MACOSX")) continue;
				if(ze.getName().contains("module-info")) continue;
				if(ze.getName().contains("org/spongepowered")) continue;
				if(ze.getName().contains("it/unimi")) continue;
				if(ze.getName().contains("kotlin")) continue;
				
				//Manual error avoidance/compat, crucify me
				//TODO: config defined overrides?
				if(ze.getName().contains("net/jan/moddirector")) {
					modIDs.add("moddirector");
					return;
				}
				if(ze.getName().contains("git/jbredwards/jsonpaintings")) {
					modIDs.add("jsonpaintings");
					return;
				}
				if(ze.getName().contains("net/optifine")) {
					modIDs.add("optifine");
					return;
				}
				
				if(classFilePattern.matcher(ze.getName()).matches()) {
					//Could use a custom parsing method only for annotations but im lazy, and it doesn't take much time regardless
					//Would rather emulate FML as close as possible to avoid inconsistency rather than save a second or so total
					ASMModParser modParser;
					try {
						try(InputStream inputStream = jar.getInputStream(ze)) {
							modParser = new ASMModParser(inputStream);
						}
					}
					catch(Exception ex) {
						LOGGER.log(Level.INFO, "FermiumEarlyModIDSearcher failed to search jar " + jar.getName() + ", likely fine to ignore.");
						break;
					}
					
					modParser.validate();
					String modid = null;
					for(ModAnnotation ann : modParser.getAnnotations()) {
						if(modType.equals(ann.getASMType())) {
							modid = (String)ann.getValues().get("modid");
							break;
						}
					}
					if(modid != null && !modid.isEmpty()) {
						//Much faster to break early, but some mods are nested
						modIDs.add(modid);
					}
				}
			}
		}
		catch(Exception ignored) { }
	}
	
	private static void searchModInfoRecursive(JsonElement element, Set<String> modIDs) {
		if(element instanceof JsonObject) {
			if(((JsonObject)element).has("modid")) {
				modIDs.add(((JsonObject)element).get("modid").getAsString().toLowerCase());
			}
			else {
				for(Map.Entry<String, JsonElement> entry : ((JsonObject)element).entrySet()) {
					searchModInfoRecursive(entry.getValue(), modIDs);
				}
			}
		}
		else if(element instanceof JsonArray) {
			for(JsonElement elem : ((JsonArray)element)) {
				searchModInfoRecursive(elem, modIDs);
			}
		}
	}
}