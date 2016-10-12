package fr.cs.ikats.util.configuration;

public class IkatsConfiguration<T extends Enum<T> & ConfigProperties> extends AbstractIkatsConfiguration<T> {

	public IkatsConfiguration(Class<T> configPropertiesClazz) {
		super.init(configPropertiesClazz);
	}

}
