/**
 **                       EnrichmentMap Cytoscape Plugin
 **
 ** Copyright (c) 2008-2009 Bader Lab, Donnelly Centre for Cellular and Biomolecular 
 ** Research, University of Toronto
 **
 ** Contact: http://www.baderlab.org
 **
 ** Code written by: Ruth Isserlin
 ** Authors: Daniele Merico, Ruth Isserlin, Oliver Stueker, Gary D. Bader
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** University of Toronto
 ** has no obligations to provide maintenance, support, updates, 
 ** enhancements or modifications.  In no event shall the
 ** University of Toronto
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** University of Toronto
 ** has been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **
 **/

// $Id$
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
// $HeadURL$

package org.baderlab.csplugins.enrichmentmap.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.baderlab.csplugins.enrichmentmap.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.actions.SliderBarActionListener;
import org.cytoscape.application.CyApplicationManager;

//import prefuse.data.query.NumberRangeModel;

/**
 * Slider bar panel - it a panel contained within legend panel
 */
@SuppressWarnings("serial")
public class SliderBarPanel extends JPanel {

	private CyApplicationManager applicationManager;
	private EnrichmentMapManager emManager;

	//height of panel
	private final int DIM_HEIGHT = 72;
	//width of panel
	private final int DIM_WIDTH = 150;

	//min and max values for the slider
	private int min;
	private int max;
	//private NumberRangeModel rangeModel;

	//flag to indicate very small number
	private boolean smallNumber = false;

	//precision that the slider can be adjusted to
	private double precision = 1000.0;
	private int dec_precision = (int) Math.log10(precision);

	private JLabel label;
	private String sliderLabel;

	private boolean edgesOnly;
	private int initial_value;

	
	
	/**
	 * Class constructor
	 *
	 * @param min - slider mininmum value
	 * @param max - slider maximum value
	 * @param sliderLabel
	 * @param params - enrichment map parameters for current map
	 * @param attrib1 - attribute for dataset 1 that the slider bar is specific
	 *            to (i.e. p-value or q-value)
	 * @param attrib2 - attribute for dataset 2 that the slider bar is specific
	 *            to (i.e. p-value or q-value)
	 * @param desired_width
	 */
	public SliderBarPanel(double min, double max, String sliderLabel, EnrichmentMapParameters params, 
			String attrib1, String attrib2, 
			int desired_width, boolean edgesOnly, double initial_value,
			CyApplicationManager applicationManager, EnrichmentMapManager emManager) {

		this.applicationManager = applicationManager;
		this.emManager = emManager;
		this.setPreferredSize(new Dimension(DIM_WIDTH, DIM_HEIGHT));
		this.setLayout(new BorderLayout(0, 0));
		this.setOpaque(false);

		if((min <= 1) && (max <= 1)) {

			//if the max is a very small number then use the precision to filter the results 
			if(max <= 0.0001) {
				DecimalFormat df = new DecimalFormat("#.##############################");
				String text = df.format(max);
				int integerPlaces = text.indexOf('.');
				int decimalPlaces = text.length() - integerPlaces - 1;
				this.precision = decimalPlaces;
				this.min = (int) (min * Math.pow(10, (this.precision + this.dec_precision)));
				this.max = (int) (max * Math.pow(10, (this.precision + this.dec_precision)));

				this.initial_value = (int) (initial_value * Math.pow(10, (this.precision + this.dec_precision)));
				this.smallNumber = true;
			} else {
				this.min = (int) (min * precision);
				this.max = (int) (max * precision);
				this.initial_value = (int) (initial_value * precision);
			}
		} else {
			this.min = (int) min;
			this.max = (int) max;
			this.initial_value = (int) initial_value;
		}
		this.sliderLabel = sliderLabel;

		label = new JLabel(sliderLabel);

		Dimension currentsize = label.getPreferredSize();
		currentsize.height = DIM_HEIGHT / 12;
		label.setPreferredSize(currentsize);

		this.edgesOnly = edgesOnly;

		initPanel(params, attrib1, attrib2, desired_width);
	}

	/**
	 * Initialize panel based on enrichment map parameters and desired
	 * attributes
	 *
	 * @param params - enrichment map parameters for current map
	 * @param attrib1 - attribute for dataset 1 that the slider bar is specific
	 *            to (i.e. p-value or q-value)
	 * @param attrib2 - attribute for dataset 2 that the slider bar is specific
	 *            to (i.e. p-value or q-value)
	 * @param desired_width
	 */
	public void initPanel(EnrichmentMapParameters params, String attrib1, String attrib2, int desired_width) {

		JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, initial_value);

		slider.addChangeListener(new SliderBarActionListener(this, attrib1, attrib2, edgesOnly, applicationManager, emManager));

		slider.setMajorTickSpacing((max - min) / 5);
		slider.setPaintTicks(true);

		//Create the label table
		Hashtable labelTable = new Hashtable();
		if(smallNumber) {
			labelTable.put(new Integer(min),
					new JLabel("" + (int) this.min / Math.pow(10, dec_precision) + "E-" + (int) this.precision));
			labelTable.put(new Integer(max),
					new JLabel("" + (int) this.max / Math.pow(10, dec_precision) + "E-" + (int) this.precision));
		} else {
			labelTable.put(new Integer(min), new JLabel("" + min / precision));
			labelTable.put(new Integer(max), new JLabel("" + max / precision));
		}
		slider.setLabelTable(labelTable);

		slider.setPaintLabels(true);

		Dimension currentsize = slider.getPreferredSize();
		currentsize.width = desired_width;
		currentsize.height = (DIM_HEIGHT / 12) * 11;
		slider.setPreferredSize(currentsize);

		this.setLayout(new GridLayout(2, 1));

		this.add(label, BorderLayout.NORTH);

		this.add(slider, BorderLayout.SOUTH);

		this.revalidate();
	}

	//Getters and Setters

	public void setLabel(int current_value) {
		if(smallNumber)
			label.setText("<html>" + sliderLabel + // "P-value Cutoff" or "Q-value Cutoff"
					" &#8594; " + // HTML entity right-arrow ( &rarr; )
					"<font size=\"-2\"> "
					+ (current_value / Math.pow(10, (dec_precision + precision)) + " </font></html>" // dec_precision is the number of decimals for given precision
					)) // the current P/Q-value cutoff
			;
		else
			label.setText(String.format(
					"<html>" + sliderLabel + // "P-value Cutoff" or "Q-value Cutoff"
							" &#8594; " + // HTML entity right-arrow ( &rarr; )
							"<font size=\"-2\"> %." + dec_precision + "f </font></html>", // dec_precision is the number of decimals for given precision
					(current_value / precision) // the current P/Q-value cutoff
			));

		this.revalidate();
	}

	public double getPrecision() {
		if(smallNumber)
			return Math.pow(10, this.precision + this.dec_precision);
		else
			return precision;
	}

	//Methods are currently not used.  If they become useful in the future need to add case for smallNumbers case

	/*
	 * public double getMin() { return min/precision; }
	 * 
	 * public void setMin(double min) { this.min = (int)(min * precision); }
	 * 
	 * public double getMax() { return max/precision; }
	 * 
	 * public void setMax(double max) { this.max = (int) (max*precision); }
	 * 
	 * public NumberRangeModel getRangeModel() { return rangeModel; }
	 * 
	 * public void setRangeModel(NumberRangeModel rangeModel) { this.rangeModel
	 * = rangeModel; }
	 */
}
