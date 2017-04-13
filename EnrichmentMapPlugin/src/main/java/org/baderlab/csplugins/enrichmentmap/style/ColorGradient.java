package org.baderlab.csplugins.enrichmentmap.style;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder.Colors;

public enum ColorGradient {
	
	// 3-color ColorBrewer schemes (color-blind safe and print friendly):
	// Diverging
	// -- same as NODE_FILL_COLOR Continuous Mapping:
	RD_BU("RdBu", Colors.MAX_PHENOTYPE_2, Colors.OVER_COLOR, Colors.MAX_PHENOTYPE_1),
//		BR_BG("BrBG", new Color(90,180,172), null, new Color(216,179,101)),
//		PI_YG("PiYG", new Color(161,215,106), null, new Color(233,163,201)),
//		PR_GN("PRGn", new Color(127,191,123), null, new Color(175,141,195)),
//		PU_OR("PuOr", new Color(153,142,195), null, new Color(241,163,64)),
	// Sequential - Multi-hue
	YL_GN("YlGn", new Color(49,163,84), new Color(173,221,142), new Color(247,252,185)),
	YL_GN_B("YlGnB", new Color(44,127,184), new Color(127,205,187), new Color(237,248,177)),
	YL_OR_BR("YlOrBr", new Color(217,95,14), new Color(254,196,79), new Color(255,247,188)),
	YL_OR_RD("YlOrRd", new Color(240,59,32), new Color(254,178,76), new Color(255,237,160)),
	;

	private String label;
	private final Color up, zero, down;
	
	private static Map<String, ColorGradient>cMap;

	ColorGradient(final String label, final Color down, final Color zero, final Color up) {
		this.label = label;
		this.up = up;
		this.down = down;
		this.zero = zero;
		addGradient(this);
	}

	public String getLabel() {
		return label;
	}
	
	public List<Color> getColors() {
		final List<Color> retColors = new ArrayList<>();
		retColors.add(up);
		if (zero != null) retColors.add(zero);
		retColors.add(down);

		return retColors;
	}

	public static boolean contains(final String name) {
		return name != null && cMap.containsKey(normalize(name));
	}
	
	public static ColorGradient getGradient(final String name) {
		return cMap.get(normalize(name));
	}
	
	public static List<Color> getColors(String name) {
		name = normalize(name);
		
		if (name != null && cMap.containsKey(name))
			return cMap.get(name).getColors();
		
		return Collections.emptyList();
	}
	
	private void addGradient(final ColorGradient cg) {
		if (cMap == null) cMap = new HashMap<>();
		cMap.put(normalize(cg.name()), cg);
	}
	
	private static String normalize(final String name) {
		return name != null ? name.toUpperCase().replaceAll("[-_]", "") : null;
	}
}
