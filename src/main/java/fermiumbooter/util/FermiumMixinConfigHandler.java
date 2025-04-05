package fermiumbooter.util;

import com.google.common.base.Strings;
import fermiumbooter.FermiumRegistryAPI;
import fermiumbooter.annotations.MixinConfig;
import net.minecraftforge.common.config.Config;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FermiumMixinConfigHandler {
	
	private static final Logger LOGGER = LogManager.getLogger("FermiumMixinConfigHandler");
	
	public static <T> void registerForgeConfigClass(Class<T> configClass, @Nullable T configClassInstance) {
		if(configClass == null) {
			LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler provided null config class.");
			return;
		}
		
		LOGGER.log(Level.INFO, "FermiumMixinConfigHandler registering MixinConfig class {}.", configClass.getCanonicalName());
		
		//Initial provided class should have the Config annotation, sub instances do not necessarily need it
		if(!configClass.isAnnotationPresent(Config.class)) {
			LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler provided config class not annotated as a Forge config {}.", configClass.getCanonicalName());
			return;
		}
		
		Config configAnno = configClass.getAnnotation(Config.class);
		String modConfigName = configAnno.name();
		if(Strings.isNullOrEmpty(modConfigName)) modConfigName = configAnno.modid();
		if(Strings.isNullOrEmpty(modConfigName)) {
			LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler provided config class missing name or modid from Forge config annotation {}.", configClass.getCanonicalName());
			return;
		}
		
		parseForgeConfigClass(modConfigName, configClass, configClassInstance);
	}
	
	private static void parseForgeConfigClass(String modConfigName, Class<?> configClass, @Nullable Object configClassInstance) {
		if(configClass == null) return;
		try {
			for(Field configField : configClass.getDeclaredFields()) {
				if(!Modifier.isPublic(configField.getModifiers())) continue;
				if(!configField.isAnnotationPresent(Config.Name.class)) continue;
				
				if(configField.isAnnotationPresent(MixinConfig.SubInstance.class)) {
					//Recursive parse nested config classes
					//Initial parent config instance can be null (static declared fields) but nested configs shouldn't be
					parseForgeConfigClass(modConfigName, configField.getType(), configField.get(configClassInstance));
				}
				else if(configField.isAnnotationPresent(MixinConfig.EarlyMixin.class) || configField.isAnnotationPresent(MixinConfig.LateMixin.class)) {
					String configFieldName = configField.getAnnotation(Config.Name.class).value();
					if(Strings.isNullOrEmpty(configFieldName)) {
						LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler config field annotated as a mixin toggle missing name in {} from {}.", configClass.getCanonicalName(), modConfigName);
						continue;
					}
					if(!configField.getType().equals(boolean.class)) {
						LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler non-boolean config field annotated as a mixin toggle {} from {}.", configFieldName, modConfigName);
						continue;
					}
					//Parse mixin config field for mixin toggles
					parseMixinConfigField(modConfigName, configFieldName, configField, configField.getBoolean(configClassInstance));
				}
			}
		}
		catch(Exception ex) {
			LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler failed to parse provided config class {} from {}.", configClass.getCanonicalName(), modConfigName);
		}
	}
	
	private static void parseMixinConfigField(String modConfigName, String configFieldName, Field configField, boolean defaultValue) {
		try {
			String earlyMixinConfig = null;
			if(configField.isAnnotationPresent(MixinConfig.EarlyMixin.class)) {
				MixinConfig.EarlyMixin earlyAnno = configField.getAnnotation(MixinConfig.EarlyMixin.class);
				if(earlyAnno != null) earlyMixinConfig = earlyAnno.name();
				if(Strings.isNullOrEmpty(earlyMixinConfig)) {
					LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler config field {} from {} annotated as early mixin toggle has invalid mixin config name.", configFieldName, modConfigName);
					earlyMixinConfig = null;
				}
			}
			
			String lateMixinConfig = null;
			if(configField.isAnnotationPresent(MixinConfig.LateMixin.class)) {
				MixinConfig.LateMixin lateAnno = configField.getAnnotation(MixinConfig.LateMixin.class);
				if(lateAnno != null) lateMixinConfig = lateAnno.name();
				if(Strings.isNullOrEmpty(lateMixinConfig)) {
					LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler config field {} from {} annotated as late mixin toggle has invalid mixin config name.", configFieldName, modConfigName);
					lateMixinConfig = null;
				}
			}
			
			//No reason to go further if the mixin annotations are invalid
			if(earlyMixinConfig == null && lateMixinConfig == null) return;
			
			boolean shouldApply = getRawBooleanConfigValue(modConfigName, configFieldName, defaultValue);
			//Don't bother checking compats if the config is off
			if(!shouldApply) return;
			
			if(!skipCompatHandlingChecks()) {
				MixinConfig.CompatHandling[] compatArray = configField.getAnnotationsByType(MixinConfig.CompatHandling.class);
				for(MixinConfig.CompatHandling compat : compatArray) {
					String modid = compat.modid();
					boolean desired = compat.desired();
					boolean disableMixin = compat.disableMixin();
					String reason = compat.reason();
					
					if(Strings.isNullOrEmpty(modid)) {
						LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler config field {} from {} annotated with compat handling has invalid target modid.", configFieldName, modConfigName);
						continue;
					}
					if(Strings.isNullOrEmpty(reason)) {
						LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler config field {} from {} annotated with compat handling has invalid reason.", configFieldName, modConfigName);
						continue;
					}
					
					//Loader.isModLoaded works for suppliers during late application, but not early application
					//Use replacement method and check early now instead of with enqueued suppliers
					if(desired != FermiumRegistryAPI.isModPresent(modid)) {
						warningCount++;
						if(disableMixin) {
							shouldApply = false;
							LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler annotated mixin config {} from {} disabled as incompatible {} {}: {}.", configFieldName, modConfigName, (desired ? "without" : "with"), modid, reason);
						}
						else {
							LOGGER.log(Level.WARN, "FermiumMixinConfigHandler annotated mixin config {} from {} may have issues {} {}: {}.", configFieldName, modConfigName, (desired ? "without" : "with"), modid, reason);
						}
					}
				}
			}
			
			if(shouldApply) {
				LOGGER.log(Level.INFO, "FermiumMixinConfigHandler enqueueing mixin(s) parsed from annotated mixin config {} from {}.", configFieldName, modConfigName);
				//Don't need to provide suppliers as enqueue will just be skipped in the first place instead
				if(earlyMixinConfig != null) FermiumRegistryAPI.enqueueMixin(false, earlyMixinConfig);
				if(lateMixinConfig != null) FermiumRegistryAPI.enqueueMixin(true, lateMixinConfig);
			}
		}
		catch(Exception ex) {
			LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler failed to parse provided config field {} from {}.", configFieldName, modConfigName);
		}
	}
	
	private static final Map<String, String> modConfigMap = new HashMap<>();
	
	//Janky but works, has worked, will continue to work (hopefully), too lazy to change
	private static boolean getRawBooleanConfigValue(String modConfigName, String configFieldName, boolean defaultValue) {
		if(!modConfigMap.containsKey(modConfigName)) {
			//Read config file
			File configFile = new File("config", modConfigName + ".cfg");
			String rawConfigString = null;
			if(configFile.exists() && configFile.isFile()) {
				try(Stream<String> stream = Files.lines(configFile.toPath())) {
					//Only collect boolean config options
					rawConfigString = stream.filter(s -> s.trim().startsWith("B:")).collect(Collectors.joining());
				}
				catch(Exception ex) {
					LOGGER.log(Level.ERROR, "FermiumMixinConfigHandler failed to read config {}.cfg: {}.", modConfigName, ex);
					rawConfigString = null;
				}
			}
			if(rawConfigString == null) LOGGER.log(Level.WARN, "FermiumMixinConfigHandler config {}.cfg missing or failed to read, using default values.", modConfigName);
			modConfigMap.put(modConfigName, rawConfigString);
		}
		
		String modConfigString = modConfigMap.get(modConfigName);
		//File doesn't exist or failed to read, assume all default values
		if(modConfigString == null) return defaultValue;
		if(modConfigString.contains("B:\"" + configFieldName + "\"=")) {
			return modConfigString.contains("B:\"" + configFieldName + "\"=true");
		}
		//Option likely new and not written to file yet, return default
		return defaultValue;
	}
	
	private static Boolean skipCompatHandlingChecks = null;
	
	private static boolean skipCompatHandlingChecks() {
		if(skipCompatHandlingChecks == null) {
			skipCompatHandlingChecks = getRawBooleanConfigValue("fermiumbooter", "Override Mixin Config Compatibility Checks", false);
			if(skipCompatHandlingChecks) {
				LOGGER.log(Level.WARN, "FermiumMixinConfigHandler detected Override Mixin Config Compatibility Checks as enabled, good luck, don't report issues.");
			}
		}
		return skipCompatHandlingChecks;
	}
	
	public static void clearConfigCache() {
		modConfigMap.clear();
	}
	
	private static int warningCount = 0;
	
	public static int getWarningCount() {
		return warningCount;
	}
}