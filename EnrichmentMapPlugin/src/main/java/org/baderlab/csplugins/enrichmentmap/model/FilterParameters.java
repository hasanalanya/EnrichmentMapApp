package org.baderlab.csplugins.enrichmentmap.model;

/**
 * Parameters used for specifying filters and cutoffs for post-analysis.
 */
public class FilterParameters {
	
	private final FilterType type;
	private final double value;
	

	public FilterParameters(FilterType type, double value) {
		this.type = type;
		this.value = value;
	}
	
	public FilterParameters(FilterType type) {
		this(type, type.defaultValue);
	}
	
	public FilterType getType() {
		return type;
	}

	public double getValue() {
		return value;
	}
}