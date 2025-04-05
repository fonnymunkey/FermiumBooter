package fermiumbooter.annotations;

import java.lang.annotation.*;

/**
 * Utility annotation system for dynamically registering and enabling/disabling mixin configs based on Forge @Config fields
 * This is specifically designed for configs using the Forge @Config annotation system
 * This was primarily made for my own use and sanity but provided for others to use, please report any issues to the Github
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MixinConfig {
	
	/**
	 * Annotates a class instance field that contains Forge @Config fields that should additionally be parsed
	 * Ex. Sub-config-class contained within your general Forge @Config class
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface SubInstance {}
	
	/**
	 * Annotates a boolean Forge @Config field that should toggle if a mixin should attempt to be loaded early or not
	 * Early loading is for mixins targeting Vanilla/Core-mod classes
	 * name: Name of the mixin configuration file (Json) to be loaded
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface EarlyMixin {
		String name();
	}
	
	/**
	 * Annotates a boolean Forge @Config field that should toggle if a mixin should attempt to be loaded late or not
	 * Late loading is for mixins targeting mod classes that are loaded too late for normal mixins to target
	 * name: Name of the mixin configuration file (Json) to be loaded
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface LateMixin {
		String name();
	}
	
	/**
	 * Provides additional modID-based compatibility handling for fields annotated by EarlyMixin or LateMixin
	 * If checks fail a specific warning will be logged and a general warning will display on screen
	 * modid: ModID of the target mod
	 * desired: If the target mod is desired, true is treated as a dependency, false is treated as an incompatibility
	 * disableMixin: If the check fails, true will disable the annotated mixin(s), false will only log a warning
	 * reason: Reasoning to print to the log to give additional context to the relation if the check fails
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Repeatable(CompatHandlingContainer.class)
	@interface CompatHandling {
		String modid();
		boolean desired() default true;
		boolean disableMixin() default true;
		String reason() default "Undefined";
	}
	
	/**
	 * Internal use only
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@interface CompatHandlingContainer {
		CompatHandling[] value();
	}
}