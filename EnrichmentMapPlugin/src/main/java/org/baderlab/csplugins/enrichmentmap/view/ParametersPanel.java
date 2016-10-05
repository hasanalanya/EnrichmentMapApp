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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.baderlab.csplugins.enrichmentmap.ApplicationModule.Edges;
import org.baderlab.csplugins.enrichmentmap.ApplicationModule.Nodes;
import org.baderlab.csplugins.enrichmentmap.heatmap.HeatMapPanel;
import org.baderlab.csplugins.enrichmentmap.heatmap.HeatMapParameters;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.style.EnrichmentMapVisualStyle;
import org.baderlab.csplugins.enrichmentmap.task.CreatePublicationVisualStyleTaskFactory;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.swing.DialogTaskManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Right hand information Panel containing files uploaded, legends and
 * p-value,q-value sliders.
 */
@Singleton
public class ParametersPanel extends JPanel implements CytoPanelComponent, NetworkAboutToBeDestroyedListener {

	private static final long serialVersionUID = 2230165793903119571L;

	public static int summaryPanelWidth = 150;
	public static int summaryPanelHeight = 1000;
	
	@Inject private OpenBrowser browser;
	@Inject private CyApplicationManager cyApplicationManager;
	@Inject private DialogTaskManager taskManager;
	@Inject private EnrichmentMapManager emManager;
	
	@Inject private @Nodes HeatMapPanel nodesOverlapPanel;
	@Inject private @Edges HeatMapPanel edgesOverlapPanel;
	
	@Inject private Provider<CreatePublicationVisualStyleTaskFactory> visualStyleTaskFactoryProvider;
	
	private Map<Long, SliderBarPanel> pvalueSliderPanels = new HashMap<>();
	private Map<Long, SliderBarPanel> qvalueSliderPanels = new HashMap<>();
	private Map<Long, SliderBarPanel> similaritySliderPanels = new HashMap<>();
	
	private JCheckBox heatmapAutofocusCheckbox;
	
	private EnrichmentMap map;

	


	/**
	 * Update parameters panel based on given enrichment map parameters
	 *
	 * @param params - enrichment map parameters to update panel according to
	 */
	public void updatePanel(EnrichmentMap map) {
		this.map = map;
		EnrichmentMapParameters params = map.getParams();

		this.removeAll();
		this.revalidate();
		this.setLayout(new java.awt.BorderLayout());

		JPanel main = new JPanel(new BorderLayout(0, 3));

		JPanel legends = createLegend(params, map);
		// legends.setBorder(BorderFactory.createEtchedBorder());
		main.add(legends, BorderLayout.NORTH);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		// centerPanel.setBorder(BorderFactory.createEtchedBorder());
		centerPanel.setAlignmentX(LEFT_ALIGNMENT);

		if (params.getMethod().equalsIgnoreCase(EnrichmentMapParameters.method_GSEA)) {
			final String reportFileDataset1 = resolveGseaReportFilePath(params, 1);
			final String reportFileDataset2 = resolveGseaReportFilePath(params, 2);

			if (!(reportFileDataset1 == null)) {
				JButton openReport1Button = new JButton("Open GSEA report Dataset 1");
				openReport1Button.setAlignmentX(LEFT_ALIGNMENT);
				openReport1Button.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						browser.openURL("file://" + reportFileDataset1);
					}
				});
				// Disable button if we can't read the file:
				if (!new File(reportFileDataset1).canRead()) {
					openReport1Button.setEnabled(false);
					openReport1Button.setToolTipText("Report file not found: " + reportFileDataset1);
				}
				centerPanel.add(openReport1Button);

			}
			if (!(reportFileDataset2 == null)) {
				JButton openReport2Button = new JButton("Open GSEA-report Dataset 2");
				openReport2Button.setAlignmentX(LEFT_ALIGNMENT);
				openReport2Button.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						browser.openURL("file://" + reportFileDataset2);
					}
				});
				// Disable button if we can't read the file:
				if (!(new File(reportFileDataset2)).canRead()) {
					openReport2Button.setEnabled(false);
					openReport2Button.setToolTipText("Report file not found: " + reportFileDataset2);
				}
				centerPanel.add(openReport2Button);
			}
		}
		JTextPane runInfo;
		// information about the current analysis
		runInfo = new JTextPane();
		runInfo.setEditable(false);
		runInfo.setContentType("text/html");
		runInfo.setText(getRunInfo(params));
		// runInfo.setPreferredSize(new
		// Dimension(summaryPanelWidth,summaryPanelHeight/2));

		// put Parameters into Collapsible Panel
		CollapsiblePanel runInfoPanel = new CollapsiblePanel("current Parameters");
		runInfoPanel.setCollapsed(true);
		runInfoPanel.getContentPane().add(runInfo);

		centerPanel.add(runInfoPanel);
		main.add(centerPanel, BorderLayout.CENTER);

		CollapsiblePanel preferences = new CollapsiblePanel("advanced Preferences");
		preferences.setCollapsed(true);
		JPanel prefsPanel = new JPanel();
		prefsPanel.setLayout(new BoxLayout(prefsPanel, BoxLayout.Y_AXIS));

		JButton togglePublicationButton = new JButton("Toggle Publication-Ready");
		togglePublicationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				taskManager.execute(visualStyleTaskFactoryProvider.get().createTaskIterator());
			}
		});
		prefsPanel.add(togglePublicationButton);
		
		// Begin of Code to toggle "Disable Heatmap autofocus"
		heatmapAutofocusCheckbox = new JCheckBox(new AbstractAction("Heatmap autofocus") {
			private static final long serialVersionUID = 6964856044019118837L;

			public void actionPerformed(ActionEvent e) {
				// Do this in the GUI Event Dispatch thread...
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						// toggle state of overrideHeatmapRevalidation
						if (map.getParams().isDisableHeatmapAutofocus()) {
							map.getParams().setDisableHeatmapAutofocus(false);
						} else {
							map.getParams().setDisableHeatmapAutofocus(true);
						}
						heatmapAutofocusCheckbox.setSelected(!map.getParams().isDisableHeatmapAutofocus());
					}
				});
			}
		});
		heatmapAutofocusCheckbox.setSelected(!params.isDisableHeatmapAutofocus());
		prefsPanel.add(heatmapAutofocusCheckbox);

		// add a radio button to set default sort method for the heat map
		ButtonGroup sorting_methods = new ButtonGroup();
		JPanel sortingPanel = new JPanel();
		sortingPanel.setLayout(new BoxLayout(sortingPanel, BoxLayout.Y_AXIS));

		JRadioButton hc = new JRadioButton(HeatMapParameters.sort_hierarchical_cluster);
		hc.setActionCommand(HeatMapParameters.sort_hierarchical_cluster);
		hc.setSelected(false);

		JRadioButton nosort = new JRadioButton(HeatMapParameters.sort_none);
		nosort.setActionCommand(HeatMapParameters.sort_none);
		nosort.setSelected(false);

		JRadioButton ranks = new JRadioButton(HeatMapParameters.sort_rank);
		ranks.setActionCommand(HeatMapParameters.sort_rank);
		ranks.setSelected(false);

		JRadioButton columns = new JRadioButton(HeatMapParameters.sort_column);
		columns.setActionCommand(HeatMapParameters.sort_column);
		columns.setSelected(false);

		if (params.getDefaultSortMethod().equalsIgnoreCase(HeatMapParameters.sort_hierarchical_cluster))
			hc.setSelected(true);
		if (params.getDefaultSortMethod().equalsIgnoreCase(HeatMapParameters.sort_none))
			nosort.setSelected(true);
		if (params.getDefaultSortMethod().equalsIgnoreCase(HeatMapParameters.sort_rank))
			ranks.setSelected(true);
		if (params.getDefaultSortMethod().equalsIgnoreCase(HeatMapParameters.sort_column))
			columns.setSelected(true);

		hc.addActionListener(e -> map.getParams().setDefaultSortMethod(HeatMapParameters.sort_hierarchical_cluster));
		sorting_methods.add(hc);
		nosort.addActionListener(e -> map.getParams().setDefaultSortMethod(HeatMapParameters.sort_none));
		sorting_methods.add(nosort);
		ranks.addActionListener(e -> map.getParams().setDefaultSortMethod(HeatMapParameters.sort_rank));
		sorting_methods.add(ranks);
		columns.addActionListener(e -> map.getParams().setDefaultSortMethod(HeatMapParameters.sort_column));
		sorting_methods.add(columns);

		sortingPanel.add(new JLabel("Default Sorting Order:"));
		sortingPanel.add(hc);
		sortingPanel.add(ranks);
		sortingPanel.add(columns);
		sortingPanel.add(nosort);

		preferences.getContentPane().add(sortingPanel, BorderLayout.CENTER);
		preferences.getContentPane().add(prefsPanel, BorderLayout.NORTH);

		// add a radio button to set default distance metric for the
		// hierarchical cluster
		ButtonGroup distance_metric = new ButtonGroup();
		JPanel dm_Panel = new JPanel();
		dm_Panel.setLayout(new BoxLayout(dm_Panel, BoxLayout.Y_AXIS));

		JRadioButton pearson = new JRadioButton(HeatMapParameters.pearson_correlation);
		pearson.setActionCommand(HeatMapParameters.pearson_correlation);
		pearson.setSelected(false);

		JRadioButton cosine = new JRadioButton(HeatMapParameters.cosine);
		cosine.setActionCommand(HeatMapParameters.cosine);
		cosine.setSelected(false);

		JRadioButton euclidean = new JRadioButton(HeatMapParameters.euclidean);
		euclidean.setActionCommand(HeatMapParameters.euclidean);
		euclidean.setSelected(false);

		if (params.getDefaultDistanceMetric().equalsIgnoreCase(HeatMapParameters.pearson_correlation))
			pearson.setSelected(true);
		if (params.getDefaultDistanceMetric().equalsIgnoreCase(HeatMapParameters.cosine))
			cosine.setSelected(true);
		if (params.getDefaultDistanceMetric().equalsIgnoreCase(HeatMapParameters.euclidean))
			euclidean.setSelected(true);

		pearson.addActionListener(e-> {
			edgesOverlapPanel.updatePanel(map);
			nodesOverlapPanel.updatePanel(map);
		});
		distance_metric.add(pearson);
		cosine.addActionListener(e-> {
			edgesOverlapPanel.updatePanel(map);
			nodesOverlapPanel.updatePanel(map);
		});
		distance_metric.add(cosine);
		euclidean.addActionListener(e-> {
			edgesOverlapPanel.updatePanel(map);
			nodesOverlapPanel.updatePanel(map);
		});
		distance_metric.add(euclidean);

		dm_Panel.add(new JLabel("Default Distance Metric:"));
		dm_Panel.add(pearson);
		dm_Panel.add(cosine);
		dm_Panel.add(euclidean);

		preferences.getContentPane().add(dm_Panel, BorderLayout.SOUTH);
		main.add(preferences, BorderLayout.SOUTH);

		JScrollPane jScrollPane = new javax.swing.JScrollPane(main);

		// jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(jScrollPane);
		this.revalidate();

	}

	/**
	 * Get the files and parameters corresponding to the current enrichment map
	 *
	 * @param params
	 *            - enrichment map parameters to get the info from
	 * @return html string of all the current files and parameters of the
	 *         enrichment map
	 */
	private String getRunInfo(EnrichmentMapParameters params) {

		// MKTODO OMG! this needs to use a StringBuilder!
		String runInfoText = "<html>";

		// runInfoText = runInfoText + " <h1>Parameters:</h1>";

		runInfoText = runInfoText + "<b>P-value Cut-off:</b>" + params.getPvalue() + "<br>";
		runInfoText = runInfoText + "<b>FDR Q-value Cut-off:</b>" + params.getQvalue() + "<br>";

		if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_JACCARD)) {
			runInfoText = runInfoText + "<b>Jaccard Cut-off:</b>" + params.getSimilarityCutOff() + "<br>";
			runInfoText = runInfoText + "<b>Test used:</b>  Jaccard Index<br>";
		} else if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_OVERLAP)) {
			runInfoText = runInfoText + "<b>Overlap Cut-off:</b>" + params.getSimilarityCutOff() + "<br>";
			runInfoText = runInfoText + "<b>Test used:</b>  Overlap Index<br>";
		} else if (params.getSimilarityMetric().equalsIgnoreCase(EnrichmentMapParameters.SM_COMBINED)) {
			runInfoText = runInfoText + "<b>Jaccard Overlap Combined Cut-off:</b>" + params.getSimilarityCutOff() + "<br>";
			runInfoText = runInfoText + "<b>Test used:</b>  Jaccard Overlap Combined Index (k constant = " + params.getCombinedConstant() + ")<br>";
		}
		runInfoText = runInfoText + "<font size=-1><b>Genesets File:</b>"
				+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET1).getGMTFileName()) + "<br>";
		runInfoText = runInfoText + "<b>Dataset 1 Data Files:</b> "
				+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET1).getEnrichmentFileName1()) + ",<br>"
				+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET1).getEnrichmentFileName2()) + "<br>";
		if (params.isTwoDatasets()) {
			runInfoText = runInfoText + "<b>Dataset 2 Data Files:</b> "
					+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET2).getEnrichmentFileName1()) + ",<br>"
					+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET2).getEnrichmentFileName2()) + "<br>";
		}
		runInfoText = runInfoText + "<b>Data file:</b>" + shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET1).getExpressionFileName()) + "<br>";
		// TODO:fix second dataset viewing.
		/*
		 * if(params.isData2() &&
		 * params.getEM().getExpression(EnrichmentMap.DATASET2) != null){
		 * runInfoText = runInfoText + "<b>Data file 2:</b>" +
		 * shortenPathname(params.getExpressionFileName2()) + "<br>"; }
		 */
		if ((map.getParams().getFiles().containsKey(EnrichmentMap.DATASET1)
				&& map.getParams().getFiles().get(EnrichmentMap.DATASET1).getGseaHtmlReportFile() != null)) {
			runInfoText = runInfoText + "<b>GSEA Report 1:</b>"
					+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET1).getGseaHtmlReportFile()) + "<br>";
		}
		if ((map.getParams().getFiles().containsKey(EnrichmentMap.DATASET2)
				&& map.getParams().getFiles().get(EnrichmentMap.DATASET2).getGseaHtmlReportFile() != null)) {
			runInfoText = runInfoText + "<b>GSEA Report 2:</b>"
					+ shortenPathname(map.getParams().getFiles().get(EnrichmentMap.DATASET2).getGseaHtmlReportFile()) + "<br>";
		}

		runInfoText = runInfoText + "</font></html>";
		return runInfoText;
	}

	/**
	 * Create the legend - contains the enrichment score colour mapper and
	 * diagram where the colours are
	 *
	 * @param params
	 *            - enrichment map parameters of current map
	 * @return panel with legend
	 */
	private JPanel createLegend(EnrichmentMapParameters params, EnrichmentMap map) {

		JPanel legends = new JPanel();
		setPreferredSize(new Dimension(summaryPanelWidth, summaryPanelHeight / 2));

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		legends.setLayout(gridbag);

		c.weighty = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.NONE;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(5, 30, 5, 2);
		c.gridwidth = GridBagConstraints.REMAINDER;

		// first row - circle
		c.gridx = 0;
		c.gridy = 0;

		// represent the node color as an png/gif instead of using java to
		// generate the representation
		URL nodeIconURL = this.getClass().getResource("node_color_small.png");
		if (nodeIconURL != null) {
			ImageIcon nodeIcon;
			nodeIcon = new ImageIcon(nodeIconURL);
			JLabel nodeColorLabel = new JLabel(nodeIcon);
			gridbag.setConstraints(nodeColorLabel, c);
			legends.add(nodeColorLabel);
		}

		LegendPanel node_legend = new LegendPanel(EnrichmentMapVisualStyle.max_phenotype1, EnrichmentMapVisualStyle.max_phenotype2,
				map.getDataset(EnrichmentMap.DATASET1).getEnrichments().getPhenotype1(),
				map.getDataset(EnrichmentMap.DATASET1).getEnrichments().getPhenotype2());
		node_legend.setToolTipText("Phenotype * (1-P_value)");

		// second row - legend 1
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(0, 0, 0, 0);
		gridbag.setConstraints(node_legend, c);
		legends.add(node_legend);

		// If there are two datasets then we need to define the node border
		// legend as well.
		if (params.isTwoDatasets()) {

			// third row - circle
			c.gridx = 0;
			c.gridy = 2;
			c.insets = new Insets(5, 30, 5, 2);

			// represent the node border color as an png/gif instead of using
			// java to generate the representation
			URL nodeborderIconURL = this.getClass().getResource("node_border_color_small.png");

			if (nodeborderIconURL != null) {
				ImageIcon nodeborderIcon = new ImageIcon(nodeborderIconURL);
				JLabel nodeborderColorLabel = new JLabel(nodeborderIcon);
				gridbag.setConstraints(nodeborderColorLabel, c);
				legends.add(nodeborderColorLabel);
			}

			LegendPanel node_legend2 = new LegendPanel(EnrichmentMapVisualStyle.max_phenotype1, EnrichmentMapVisualStyle.max_phenotype2,
					map.getDataset(EnrichmentMap.DATASET2).getEnrichments().getPhenotype1(),
					map.getDataset(EnrichmentMap.DATASET2).getEnrichments().getPhenotype2());
			node_legend2.setToolTipText("Phenotype * (1-P_value)");

			// fourth row - legend 2
			c.gridx = 0;
			c.gridy = 3;
			c.insets = new Insets(0, 0, 0, 0);
			gridbag.setConstraints(node_legend2, c);
			legends.add(node_legend2);

		}

		c.gridx = 0;
		c.gridy = 4;
		c.insets = new Insets(0, 0, 0, 0);
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.LINE_START;
		SliderBarPanel pvalueSlider = createPvalueSlider(params);

		gridbag.setConstraints(pvalueSlider, c);
		legends.add(pvalueSlider);

		if (params.isFDR()) {
			SliderBarPanel qvalueSlider = createQvalueSlider(params);

			c.gridx = 0;
			c.gridy = 5;
			// qvalueSlider.setPreferredSize(new Dimension(summaryPanelWidth,
			// 20));

			gridbag.setConstraints(qvalueSlider, c);
			legends.add(qvalueSlider);

			SliderBarPanel similaritySlider = createSimilaritySlider(params);

			c.gridx = 0;
			c.gridy = 6;
			// qvalueSlider.setPreferredSize(new Dimension(summaryPanelWidth,
			// 20));

			gridbag.setConstraints(similaritySlider, c);
			legends.add(similaritySlider);

		} else {
			SliderBarPanel similaritySlider = createSimilaritySlider(params);

			c.gridx = 0;
			c.gridy = 5;
			// qvalueSlider.setPreferredSize(new Dimension(summaryPanelWidth,
			// 20));

			gridbag.setConstraints(similaritySlider, c);
			legends.add(similaritySlider);
		}

		return legends;

	}
	
	
	private SliderBarPanel createPvalueSlider(EnrichmentMapParameters params) {
		return pvalueSliderPanels.computeIfAbsent(params.getNetworkID(), suid -> {
			double pvalue_min = params.getPvalue_min();
			double pvalue = params.getPvalue();
			return new SliderBarPanel(
					((pvalue_min == 1 || pvalue_min >= pvalue) ? 0 : pvalue_min), pvalue,
					"P-value Cutoff", params, EnrichmentMapVisualStyle.PVALUE_DATASET1,
					EnrichmentMapVisualStyle.PVALUE_DATASET2, ParametersPanel.summaryPanelWidth, false, pvalue,
					cyApplicationManager, emManager);
		});
	}
	
	private SliderBarPanel createQvalueSlider(EnrichmentMapParameters params) {
		return qvalueSliderPanels.computeIfAbsent(params.getNetworkID(), suid -> {
			double qvalue_min = params.getQvalue_min();
			double qvalue = params.getQvalue();
			return new SliderBarPanel(
					((qvalue_min == 1 || qvalue_min >= qvalue) ? 0 : qvalue_min), qvalue,
					"Q-value Cutoff", params, EnrichmentMapVisualStyle.FDR_QVALUE_DATASET1,
					EnrichmentMapVisualStyle.FDR_QVALUE_DATASET2, ParametersPanel.summaryPanelWidth, false, qvalue,
					cyApplicationManager, emManager);
		});
	}
	
	private SliderBarPanel createSimilaritySlider(EnrichmentMapParameters params) {
		return similaritySliderPanels.computeIfAbsent(params.getNetworkID(), suid -> {
			double similarityCutOff = params.getSimilarityCutOff();
			return new SliderBarPanel(similarityCutOff, 1, "Similarity Cutoff", params,
					EnrichmentMapVisualStyle.SIMILARITY_COEFFICIENT, EnrichmentMapVisualStyle.SIMILARITY_COEFFICIENT,
					ParametersPanel.summaryPanelWidth, true, similarityCutOff, cyApplicationManager, emManager);
		});
	}
	
	@Override
	public void handleEvent(NetworkAboutToBeDestroyedEvent event) {
		Long suid = event.getNetwork().getSUID();
		pvalueSliderPanels.remove(suid);
		qvalueSliderPanels.remove(suid);
		similaritySliderPanels.remove(suid);
	}
	
	@Inject
	public void registerListener(CyServiceRegistrar registrar) {
		registrar.registerService(this, NetworkAboutToBeDestroyedListener.class, new Properties());
	}
	
	/**
	 * Shorten path name to only contain the parent directory
	 *
	 * @param pathname
	 *            - pathname to shorten
	 * @return shortened pathname
	 */
	private String shortenPathname(String pathname) {
		if (pathname != null) {
			String[] tokens = pathname.split("\\" + File.separator);

			int num_tokens = tokens.length;

			String new_pathname;
			if (num_tokens >= 2)
				new_pathname = "..." + File.separator + tokens[num_tokens - 2] + File.separator + tokens[num_tokens - 1];
			else
				new_pathname = pathname;

			return new_pathname;
		}
		return pathname;

	}

	private String resolveGseaReportFilePath(EnrichmentMapParameters params, int dataset) {
		String reportFile = null;
		String netwAttrName = null;
		if (dataset == 1) {
			if (map.getParams().getFiles().containsKey(EnrichmentMap.DATASET1)) {
				reportFile = map.getParams().getFiles().get(EnrichmentMap.DATASET1).getGseaHtmlReportFile();
				netwAttrName = EnrichmentMapVisualStyle.NETW_REPORT1_DIR;
			}
		} else {
			if (map.getParams().getFiles().containsKey(EnrichmentMap.DATASET2)) {
				reportFile = map.getParams().getFiles().get(EnrichmentMap.DATASET2).getGseaHtmlReportFile();
				netwAttrName = EnrichmentMapVisualStyle.NETW_REPORT2_DIR;
			}
		}

		// Try the path that is stored in the params:
		if (reportFile != null && new File(reportFile).canRead()) {
			return reportFile;
		} else if (netwAttrName != null) { // if not: try from Network
											// attributes:
			CyNetwork network = cyApplicationManager.getCurrentNetwork();
			CyTable networkTable = network.getDefaultNetworkTable();
			String tryPath = networkTable.getRow(network.getSUID()).get(netwAttrName, String.class);

			String tryReportFile = tryPath + File.separator + "index.html";
			if (new File(tryReportFile).canRead()) {
				return tryReportFile;
			} else { // we found nothing
				if (reportFile == null || reportFile.equalsIgnoreCase("null"))
					return null;
				else
					return reportFile;
			}
		} else
			return null;
	}

	public Component getComponent() {
		return this;
	}

	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	public Icon getIcon() {
		URL EMIconURL = this.getClass().getResource("enrichmentmap_logo_notext_small.png");
		ImageIcon EMIcon = null;
		if (EMIconURL != null) {
			EMIcon = new ImageIcon(EMIconURL);
		}
		return EMIcon;
	}

	public String getTitle() {
		return "Legend";
	}
}
