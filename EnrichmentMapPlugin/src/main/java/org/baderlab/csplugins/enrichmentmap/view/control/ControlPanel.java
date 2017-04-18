package org.baderlab.csplugins.enrichmentmap.view.control;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.CENTER;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static org.baderlab.csplugins.enrichmentmap.style.ColorScheme.CONTRASTING;
import static org.baderlab.csplugins.enrichmentmap.style.ColorScheme.CUSTOM;
import static org.baderlab.csplugins.enrichmentmap.style.ColorScheme.MODULATED;
import static org.baderlab.csplugins.enrichmentmap.style.ColorScheme.RAINBOW;
import static org.baderlab.csplugins.enrichmentmap.style.ColorScheme.RANDOM;
import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.makeSmall;
import static org.cytoscape.util.swing.IconManager.ICON_COG;
import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;

import org.baderlab.csplugins.enrichmentmap.AfterInjection;
import org.baderlab.csplugins.enrichmentmap.EnrichmentMapBuildProperties;
import org.baderlab.csplugins.enrichmentmap.actions.ShowAboutDialogAction;
import org.baderlab.csplugins.enrichmentmap.model.AbstractDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMSignatureDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.style.ChartData;
import org.baderlab.csplugins.enrichmentmap.style.ChartType;
import org.baderlab.csplugins.enrichmentmap.style.ColorGradient;
import org.baderlab.csplugins.enrichmentmap.style.ColorScheme;
import org.baderlab.csplugins.enrichmentmap.util.NetworkUtil;
import org.baderlab.csplugins.enrichmentmap.view.util.SliderBarPanel;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyDisposable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("serial")
public class ControlPanel extends JPanel implements CytoPanelComponent2, CyDisposable {
	
	public static final String ID = "enrichmentmap.view.ControlPanel";
	
	private static final String BORDER_COLOR_KEY = "Separator.foreground";
	
	@Inject private CyServiceRegistrar serviceRegistrar;
	@Inject private CyNetworkViewManager networkViewManager;
	@Inject private IconManager iconManager;
	@Inject private EnrichmentMapManager emManager;
	@Inject private ShowAboutDialogAction showAboutDialogAction;
	
	private JPanel ctrlPanelsContainer;
	private final CardLayout cardLayout = new CardLayout();
	private final NullViewControlPanel nullViewCtrlPanel = new NullViewControlPanel();
	private JComboBox<CyNetworkView> emViewCombo;
	private JButton createEmButton;
	private JButton optionsButton;
	private JButton aboutButton;
	private JButton closePanelButton;
	
	private Map<Long/*CynetworkView SUID*/, EMViewControlPanel> emViewCtrlPanels = new HashMap<>();
	
	@Override
	public void dispose() {
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return "EnrichmentMap";
	}

	@Override
	public Icon getIcon() {
		String path = "org/baderlab/csplugins/enrichmentmap/view/enrichmentmap_logo_notext_small.png";
		URL url = getClass().getClassLoader().getResource(path);
		return url == null ? null : new ImageIcon(url);
	}

	@Override
	public String getIdentifier() {
		return ID;
	}

	@Override
	public Component getComponent() {
		return this;
	}
	
	public boolean contains(CyNetworkView networkView) {
		return emViewCtrlPanels.containsKey(networkView.getSUID());
	}
	
	@AfterInjection
	private void createContents() {
		setMinimumSize(new Dimension(390, 400));
		setPreferredSize(new Dimension(390, 600));
		
		JButton helpButton = SwingUtil.createOnlineHelpButton(EnrichmentMapBuildProperties.USER_MANUAL_URL,
				"Online Manual...", serviceRegistrar);
		
		makeSmall(getAboutButton(), getClosePanelButton());
		
		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(LookAndFeelUtil.isWinLAF());
		layout.setAutoCreateGaps(!LookAndFeelUtil.isAquaLAF());
		
   		layout.setHorizontalGroup(layout.createParallelGroup(CENTER, true)
   				.addGroup(layout.createSequentialGroup()
   						.addComponent(getEmViewCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
   						.addPreferredGap(ComponentPlacement.RELATED)
   						.addComponent(getCreateEmButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addPreferredGap(ComponentPlacement.RELATED)
   						.addComponent(getOptionsButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   				)
				.addComponent(getCtrlPanelsContainer(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
   						.addComponent(helpButton, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addComponent(getAboutButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addGap(0, 0, Short.MAX_VALUE)
   						.addComponent(getClosePanelButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   				)
   		);
   		layout.setVerticalGroup(layout.createSequentialGroup()
   				.addGroup(layout.createParallelGroup(CENTER, false)
   						.addComponent(getEmViewCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addComponent(getCreateEmButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addComponent(getOptionsButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   				)
   				.addComponent(getCtrlPanelsContainer(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
   				.addGroup(layout.createParallelGroup(CENTER, false)
   						.addComponent(helpButton, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addComponent(getAboutButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   						.addComponent(getClosePanelButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   				)
   		);
		
		if (LookAndFeelUtil.isAquaLAF())
			setOpaque(false);
	}
	
	JComboBox<CyNetworkView> getEmViewCombo() {
		if (emViewCombo == null) {
			emViewCombo = new JComboBox<>();
			
			emViewCombo.setRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					
					if (value instanceof CyNetworkView)
						this.setText(NetworkUtil.getTitle((CyNetworkView) value));
					else
						this.setText("-- Select EnrichmentMap View --");
					
					return this;
				}
			});
		}
		
		return emViewCombo;
	}
	
	JButton getCreateEmButton() {
		if (createEmButton == null) {
			createEmButton = new JButton(IconManager.ICON_PLUS);
			styleHeaderButton(createEmButton, iconManager.getIconFont(16.0f));
		}
		
		return createEmButton;
	}
	
	JButton getOptionsButton() {
		if (optionsButton == null) {
			optionsButton = new JButton(ICON_COG);
			optionsButton.setToolTipText("Options...");
			styleHeaderButton(optionsButton, iconManager.getIconFont(18.0f));
		}
		
		return optionsButton;
	}
	
	JButton getAboutButton() {
		if (aboutButton == null) {
			aboutButton = new JButton(showAboutDialogAction);
		}
		
		return aboutButton;
	}
	
	JButton getClosePanelButton() {
		if (closePanelButton == null) {
			closePanelButton = new JButton("Close");
		}
		
		return closePanelButton;
	}
	
	JPanel getCtrlPanelsContainer() {
		if (ctrlPanelsContainer == null) {
			ctrlPanelsContainer = new JPanel();
			ctrlPanelsContainer.setLayout(cardLayout);
			ctrlPanelsContainer.add(nullViewCtrlPanel, nullViewCtrlPanel.getName());
			
			if (LookAndFeelUtil.isAquaLAF())
				ctrlPanelsContainer.setOpaque(false);
		}
		
		return ctrlPanelsContainer;
	}
	
	EMViewControlPanel addEnrichmentMapView(CyNetworkView netView) {
		updateEmViewCombo();
		
		if (getViewControlPanel(netView) == null) {
			EMViewControlPanel p = new EMViewControlPanel(netView);
			getCtrlPanelsContainer().add(p, p.getName());
			emViewCtrlPanels.put(netView.getSUID(), p);
			
			return p;
		}
		
		return null;
	}
	
	void removeEnrichmentMapView(CyNetworkView netView) {
		removeViewControlPanel(netView);
		updateEmViewCombo();
	}
	
	void update(CyNetworkView currentView) {
		// Is there an EnrichmentMap for this view?
		EnrichmentMap em = currentView != null ? emManager.getEnrichmentMap(currentView.getModel().getSUID()) : null;
		
		if (em == null)
			currentView = null; // This view is not an EnrichmentMap one!
			
		updateEmViewComboSelection(currentView);
		showViewControlPanel(currentView);
	}
	
	void updateEmViewCombo() {
		CyNetworkView selectedItem = (CyNetworkView) getEmViewCombo().getSelectedItem();
		
		Map<Long, EnrichmentMap> emMap = emManager.getAllEnrichmentMaps();
		getEmViewCombo().setEnabled(!emMap.isEmpty());
		getEmViewCombo().removeAllItems();
		
		if (!emMap.isEmpty()) {
			Set<CyNetworkView> allViews = networkViewManager.getNetworkViewSet();
			List<CyNetworkView> emViews = allViews.stream()
			        .filter(v -> emManager.isEnrichmentMap(v))
			        .collect(Collectors.toCollection(ArrayList::new));
			
			Collator collator = Collator.getInstance(Locale.getDefault());
			emViews.sort((CyNetworkView v1, CyNetworkView v2) -> {
				return collator.compare(NetworkUtil.getTitle(v1), NetworkUtil.getTitle(v2));
			});
			
			emViews.forEach((item) -> getEmViewCombo().addItem(item));
			
			getEmViewCombo().setSelectedItem(selectedItem);
		}
	}
	
	private void updateEmViewComboSelection(CyNetworkView currentView) {
		EnrichmentMap em = currentView != null ? emManager.getEnrichmentMap(currentView.getModel().getSUID()) : null;
		getEmViewCombo().setSelectedItem(em != null ? currentView : null);
	}
	
	private void removeViewControlPanel(CyNetworkView netView) {
		EMViewControlPanel p = emViewCtrlPanels.remove(netView.getSUID());
		
		if (p != null) {
			cardLayout.removeLayoutComponent(p);
			getCtrlPanelsContainer().remove(p);
		}
	}
	
	private void showViewControlPanel(CyNetworkView netView) {
		if (netView == null) {
			cardLayout.show(getCtrlPanelsContainer(), nullViewCtrlPanel.getName());
		} else {
			EMViewControlPanel p = getViewControlPanel(netView);
			
			if (p != null)
				cardLayout.show(getCtrlPanelsContainer(), p.getName());
		}
	}
	
	EMViewControlPanel getViewControlPanel(CyNetworkView netView) {
		return netView != null ? emViewCtrlPanels.get(netView.getSUID()) : null;
	}
	
	private void styleHeaderButton(final AbstractButton btn, final Font font) {
		btn.setFont(font);
		btn.setBorder(null);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);
		
		int h = getEmViewCombo().getPreferredSize().height;
		btn.setMinimumSize(new Dimension(h, h));
		btn.setPreferredSize(new Dimension(h, h));
	}
	
	class EMViewControlPanel extends JPanel {
		
		private final Color BLUE = new Color(44, 123, 182);
		private final Color DARK_BLUE = new Color(37, 52, 148);
		private final Color BROWN = new Color(166, 97, 26);
		private final Color ORANGE = new Color(253, 141, 60);
		private final Color PURPLE = new Color(118, 42, 131);
		private final Color RED = new Color(215, 25, 28);
		private final Color DARK_RED = new Color(165, 15, 21);
		
		private final ColorScheme[] REGULAR_COLOR_SCHEMES = new ColorScheme[] {
				CONTRASTING, MODULATED, RAINBOW, RANDOM
		};
		private final ColorScheme[] HEAT_MAP_SCHEMES;
		
		private JRadioButton pValueRadio;
		private JRadioButton qValueRadio;
		private final ButtonGroup nodeCutoffGroup = new ButtonGroup();
		private SliderBarPanel pValueSliderPanel;
		private SliderBarPanel qValueSliderPanel;
		private SliderBarPanel similaritySliderPanel;
		
		private JLabel chartDataLabel = new JLabel("Chart Data:");
		private JLabel chartTypeLabel = new JLabel("Chart Type:");
		private JLabel chartColorsLabel = new JLabel("Chart Colors:");
		private JLabel dsFilterLabel = new JLabel("Data Sets:");
		
		private DataSetSelector dataSetSelector;
		private JCheckBox publicationReadyCheck;
		private JButton setEdgeWidthButton;
		private JButton resetStyleButton;
		
		private JComboBox<ChartData> chartDataCombo;
		private JComboBox<ChartType> chartTypeCombo;
		private JComboBox<ColorScheme> chartColorsCombo;
		private JCheckBox showChartLabelsCheck;
		
		private final CyNetworkView networkView;
		
		private EMViewControlPanel(final CyNetworkView networkView) {
			this.networkView = networkView;
			setName("__EM_VIEW_CONTROL_PANEL_" + networkView.getSUID());
			setBorder(BorderFactory.createLineBorder(UIManager.getColor(BORDER_COLOR_KEY)));
			
			// Init colors
			final List<ColorScheme> heatSchemeList = new ArrayList<>();
			
			for (final ColorGradient cg : ColorGradient.values()) {
				if (cg.getColors().size() == 3)
					heatSchemeList.add(new ColorScheme(cg));
			}
			
			heatSchemeList.add(CUSTOM);
			
			HEAT_MAP_SCHEMES = heatSchemeList.toArray(new ColorScheme[heatSchemeList.size()]);
			
			final JPanel filterPanel = createFilterPanel();
			final JPanel stylePanel = createStylePanel();
			
			final GroupLayout layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(LookAndFeelUtil.isWinLAF());
			layout.setAutoCreateGaps(!LookAndFeelUtil.isAquaLAF());
			
	   		layout.setHorizontalGroup(layout.createParallelGroup(CENTER, true)
					.addComponent(filterPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(stylePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
	   		);
	   		layout.setVerticalGroup(layout.createSequentialGroup()
	   				.addComponent(filterPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(stylePanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
	   		);
			
			if (LookAndFeelUtil.isAquaLAF())
				setOpaque(false);
			
			update();
		}
		
		Set<AbstractDataSet> getCheckedDataSets() {
			return getDataSetSelector().getCheckedItems();
		}
		
		@SuppressWarnings("unchecked")
		Set<EMSignatureDataSet> getSelectedSignatureDataSets() {
			final Set<?> dataSets = getDataSetSelector().getSelectedItems();
			Set<EMSignatureDataSet> sigDataSets = (Set<EMSignatureDataSet>) dataSets.stream()
					.filter(ds -> ds instanceof EMSignatureDataSet)
					.collect(Collectors.toSet());
			
			return sigDataSets;
		}
		
		void update() {
			updateDataSetSelector();
			updateChartCombos();
		}
		
		void updateDataSetSelector() {
			getDataSetSelector().update();
		}
		
		void updateChartCombos() {
			updateChartTypeCombo();
			updateChartColorsCombo();
			updateChartLabelsCheck();
		}
		
		void updateChartDataCombo() {
			long dsCount = getDataSetSelector().getCheckedItems()
					.stream()
					.filter(ds -> ds instanceof EMDataSet) // Ignore Signature Data Sets in charts
					.count();
			
			boolean b1 = getChartDataCombo().isEnabled();
			boolean b2 = dsCount > 1; // Can't have charts with less than 2 columns!
			
			if (b1 != b2) {
				getChartDataCombo().setEnabled(b2);
				updateChartCombos();
			}
		}
		
		void updateChartTypeCombo() {
			ChartData data = (ChartData) getChartDataCombo().getSelectedItem();
			getChartTypeCombo().setEnabled(getChartDataCombo().isEnabled() && data != ChartData.NONE);
		}
		
		void updateChartColorsCombo() {
			ColorScheme selectedItem = (ColorScheme) getChartColorsCombo().getSelectedItem();
			ChartData data = (ChartData) getChartDataCombo().getSelectedItem();
			
			getChartColorsCombo().removeAllItems();
			
			if (data != ChartData.NONE) {
				ChartType type = (ChartType) getChartTypeCombo().getSelectedItem();
				ColorScheme[] colorSchemes = null;
				
				switch (type) {
					case RADIAL_HEAT_MAP:
					case HEAT_MAP:
					case HEAT_STRIPS:
						colorSchemes = HEAT_MAP_SCHEMES;
						break;
					default:
						colorSchemes = REGULAR_COLOR_SCHEMES;
						break;
				}
				
				for (ColorScheme scheme : colorSchemes)
					getChartColorsCombo().addItem(scheme);
				
				if (selectedItem != null)
					getChartColorsCombo().setSelectedItem(selectedItem);
			}
			
			getChartColorsCombo().setEnabled(getChartTypeCombo().isEnabled() && data != ChartData.NONE);
		}
		
		void updateChartLabelsCheck() {
			ChartData data = (ChartData) getChartDataCombo().getSelectedItem();
			getShowChartLabelsCheck().setEnabled(getChartTypeCombo().isEnabled() && data != ChartData.NONE);
		}
		
		CyNetworkView getNetworkView() {
			return networkView;
		}
		
		private EnrichmentMap getEnrichmentMap() {
			return emManager.getEnrichmentMap(networkView.getModel().getSUID());
		}
		
		private JPanel createFilterPanel() {
			final JPanel panel = new JPanel();
			panel.setBorder(LookAndFeelUtil.createTitledBorder("Filter"));
			
			final GroupLayout layout = new GroupLayout(panel);
	       	panel.setLayout(layout);
	   		layout.setAutoCreateContainerGaps(true);
	   		layout.setAutoCreateGaps(!LookAndFeelUtil.isAquaLAF());
	   		
			ParallelGroup hGroup = layout.createParallelGroup(CENTER, true);
			SequentialGroup vGroup = layout.createSequentialGroup();
			layout.setHorizontalGroup(hGroup);
			layout.setVerticalGroup(vGroup);
			
			EnrichmentMap map = getEnrichmentMap();
			
			if (map != null) {
				EMCreationParameters params = map.getParams();
				List<JTextField> sliderPanelFields = new ArrayList<>();
				
				JLabel nodeCutoffLabel = new JLabel();
				makeSmall(nodeCutoffLabel);
				
				if (params != null) {
					if (params.isFDR()) {
						nodeCutoffLabel.setText("Node Cutoff:");
					
						qValueSliderPanel = createQvalueSlider(map);
						sliderPanelFields.add(qValueSliderPanel.getTextField());
						
						hGroup.addGroup(layout.createSequentialGroup()
								.addComponent(nodeCutoffLabel)
								.addGap(10, 20, Short.MAX_VALUE)
								.addComponent(getPValueRadio())
								.addComponent(getQValueRadio())
						);
						vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
								.addComponent(nodeCutoffLabel)
								.addComponent(getPValueRadio())
								.addComponent(getQValueRadio())
						);
						
						hGroup.addComponent(qValueSliderPanel);
						vGroup.addComponent(qValueSliderPanel);
						
						nodeCutoffGroup.add(getPValueRadio());
						nodeCutoffGroup.add(getQValueRadio());
						nodeCutoffGroup.setSelected(getQValueRadio().getModel(), true);
					} else {
						nodeCutoffLabel.setText("Node Cutoff (P-value):");
						hGroup.addComponent(nodeCutoffLabel, Alignment.LEADING);
						vGroup.addComponent(nodeCutoffLabel);
					}
				}
				
				pValueSliderPanel = createPvalueSlider(map);
				sliderPanelFields.add(pValueSliderPanel.getTextField());
				
				hGroup.addComponent(pValueSliderPanel);
				vGroup.addComponent(pValueSliderPanel);
				
				similaritySliderPanel = createSimilaritySlider(map);
				sliderPanelFields.add(similaritySliderPanel.getTextField());
				
				hGroup.addComponent(similaritySliderPanel);
				vGroup.addComponent(similaritySliderPanel);
				
				LookAndFeelUtil.equalizeSize(sliderPanelFields.toArray(new JComponent[sliderPanelFields.size()]));
			}
			
			JPanel datasetListPanel = createDataSetListPanel();
			hGroup.addComponent(datasetListPanel);
			vGroup.addComponent(datasetListPanel);
	   		
			if (LookAndFeelUtil.isAquaLAF())
				panel.setOpaque(false);
			
			updateFilterPanel();
			
			return panel;
		}
		
		private JPanel createStylePanel() {
			makeSmall(chartDataLabel, chartTypeLabel, chartColorsLabel);
			makeSmall(getChartDataCombo(), getChartTypeCombo(), getChartColorsCombo(), getShowChartLabelsCheck());
			makeSmall(getPublicationReadyCheck(), getSetEdgeWidthButton(), getResetStyleButton());
			
			final JPanel panel = new JPanel();
			panel.setBorder(LookAndFeelUtil.createTitledBorder("Style"));
			
			final GroupLayout layout = new GroupLayout(panel);
	       	panel.setLayout(layout);
	   		layout.setAutoCreateContainerGaps(true);
	   		layout.setAutoCreateGaps(!LookAndFeelUtil.isAquaLAF());
	   		
			layout.setHorizontalGroup(layout.createParallelGroup(CENTER, true)
					.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(TRAILING, true)
									.addComponent(chartDataLabel)
									.addComponent(chartTypeLabel)
									.addComponent(chartColorsLabel)
							)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(layout.createParallelGroup(LEADING, true)
									.addComponent(getChartDataCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(getChartTypeCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(getChartColorsCombo(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(getShowChartLabelsCheck(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
									.addComponent(getPublicationReadyCheck(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
							)
					)
					.addGroup(layout.createSequentialGroup()
							.addComponent(getSetEdgeWidthButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(getResetStyleButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(CENTER, false)
							.addComponent(chartDataLabel)
							.addComponent(getChartDataCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addGroup(layout.createParallelGroup(CENTER, false)
							.addComponent(chartTypeLabel)
							.addComponent(getChartTypeCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
					.addGroup(layout.createParallelGroup(CENTER, false)
							.addComponent(chartColorsLabel)
							.addComponent(getChartColorsCombo(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addComponent(getShowChartLabelsCheck(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getPublicationReadyCheck(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(CENTER, false)
							.addComponent(getSetEdgeWidthButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getResetStyleButton(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
			);
			
			if (LookAndFeelUtil.isAquaLAF())
				panel.setOpaque(false);
			
			return panel;
		}
		
		private SliderBarPanel createPvalueSlider(EnrichmentMap map) {
			double pvalueMin = map.getParams().getPvalueMin();
			double pvalue = map.getParams().getPvalue();
			
			return new SliderBarPanel(
					(pvalueMin == 1 || pvalueMin >= pvalue ? 0 : pvalueMin),
					pvalue,
					null,
					pvalue
			);
		}
		
		private SliderBarPanel createQvalueSlider(EnrichmentMap map) {
			double qvalueMin = map.getParams().getQvalueMin();
			double qvalue = map.getParams().getQvalue();
			
			return new SliderBarPanel(
					(qvalueMin == 1 || qvalueMin >= qvalue ? 0 : qvalueMin),
					qvalue,
					null,
					qvalue
			);
		}
		
		private SliderBarPanel createSimilaritySlider(EnrichmentMap map) {
			double similarityCutOff = map.getParams().getSimilarityCutoff();
			
			return new SliderBarPanel(
					similarityCutOff,
					1,
					"Edge Cutoff (Similarity):",
					similarityCutOff
			);
		}
		
		JRadioButton getPValueRadio() {
			if (pValueRadio == null) {
				pValueRadio = new JRadioButton("P-value");
				makeSmall(pValueRadio);
			}
			
			return pValueRadio;
		}
		
		JRadioButton getQValueRadio() {
			if (qValueRadio == null) {
				qValueRadio = new JRadioButton("Q-value");
				makeSmall(qValueRadio);
			}
			
			return qValueRadio;
		}
		
		SliderBarPanel getPValueSliderPanel() {
			return pValueSliderPanel;
		}
		
		SliderBarPanel getQValueSliderPanel() {
			return qValueSliderPanel;
		}
		
		SliderBarPanel getSimilaritySliderPanel() {
			return similaritySliderPanel;
		}
		
		DataSetSelector getDataSetSelector() {
			if (dataSetSelector == null) {
				dataSetSelector = new DataSetSelector(getEnrichmentMap(), serviceRegistrar);
			}

			return dataSetSelector;
		}

		public JComboBox<ChartData> getChartDataCombo() {
			if (chartDataCombo == null) {
				chartDataCombo = new JComboBox<>();
				chartDataCombo.addItem(ChartData.NONE);
				chartDataCombo.addItem(ChartData.NES_VALUE);
				chartDataCombo.addItem(ChartData.P_VALUE);
				
				EnrichmentMap map = getEnrichmentMap();
				
				if (map != null) {
					EMCreationParameters params = map.getParams();
					
					if (params != null && params.isFDR())
						chartDataCombo.addItem(ChartData.FDR_VALUE);
				}
			}
			
			return chartDataCombo;
		}
		
		JComboBox<ChartType> getChartTypeCombo() {
			if (chartTypeCombo == null) {
				chartTypeCombo = new JComboBox<>();
				
				for (ChartType chart : ChartType.values())
					chartTypeCombo.addItem(chart);
			}
			
			return chartTypeCombo;
		}
		
		JComboBox<ColorScheme> getChartColorsCombo() {
			if (chartColorsCombo == null) {
				chartColorsCombo = new JComboBox<>();
			}
			
			return chartColorsCombo;
		}
		
		JCheckBox getShowChartLabelsCheck() {
			if (showChartLabelsCheck == null) {
				showChartLabelsCheck = new JCheckBox("Show Chart Labels");
			}
			
			return showChartLabelsCheck;
		}
		
		JCheckBox getPublicationReadyCheck() {
			if (publicationReadyCheck == null) {
				publicationReadyCheck = new JCheckBox("Publication-Ready");
			}
			
			return publicationReadyCheck;
		}
		
		public JButton getSetEdgeWidthButton() {
			if (setEdgeWidthButton == null) {
				setEdgeWidthButton = new JButton("Set Signature Edge Width...");
				
				if (isAquaLAF())
					setEdgeWidthButton.putClientProperty("JButton.buttonType", "gradient");
			}
			
			return setEdgeWidthButton;
		}
		
		JButton getResetStyleButton() {
			if (resetStyleButton == null) {
				resetStyleButton = new JButton(IconManager.ICON_REFRESH);
				resetStyleButton.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(13.0f));
				resetStyleButton.setToolTipText("Reset Style");
				
				if (isAquaLAF())
					resetStyleButton.putClientProperty("JButton.buttonType", "gradient");
			}
			
			return resetStyleButton;
		}
		
		private JPanel createDataSetListPanel() {
			SwingUtil.makeSmall(dsFilterLabel);
			
			final JPanel panel = new JPanel();
			final GroupLayout layout = new GroupLayout(panel);
	       	panel.setLayout(layout);
	   		layout.setAutoCreateContainerGaps(false);
	   		layout.setAutoCreateGaps(!LookAndFeelUtil.isAquaLAF());
	   		
	   		layout.setHorizontalGroup(layout.createParallelGroup(LEADING, true)
	   				.addComponent(dsFilterLabel)
	   				.addComponent(getDataSetSelector(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
	   		);
	   		layout.setVerticalGroup(layout.createSequentialGroup()
	   				.addComponent(dsFilterLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
	   				.addComponent(getDataSetSelector(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
	   		);
			
			if (LookAndFeelUtil.isAquaLAF())
				panel.setOpaque(false);
			
			return panel;
		}
		
		private List<Color> getColors(final ColorScheme scheme, final Map<String, List<Double>> data) {
			List<Color> colors = null;
			
			if (scheme != null && data != null && !data.isEmpty()) {
				int nColors = 0;
				
				for (final List<Double> values : data.values()) {
					if (values != null)
						nColors = Math.max(nColors, values.size());
				}
				
				colors = scheme.getColors(nColors);
			}
			
			return colors;
		}
		
		void updateFilterPanel() {
			if (nodeCutoffGroup.getSelection() != null) {
				boolean isQValue = nodeCutoffGroup.getSelection().equals(getQValueRadio().getModel());
				getPValueSliderPanel().setVisible(!isQValue);
				getQValueSliderPanel().setVisible(isQValue);
			}
		}
	}
	
	private class NullViewControlPanel extends JPanel {

		public static final String NAME = "__NULL_VIEW_CONTROL_PANEL";
		
		private JLabel infoLabel;
		
		NullViewControlPanel() {
			setName(NAME);
			setBorder(BorderFactory.createLineBorder(UIManager.getColor(BORDER_COLOR_KEY)));
			
			final GroupLayout layout = new GroupLayout(this);
			this.setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(getInfoLabel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(getInfoLabel())
					.addGap(0, 0, Short.MAX_VALUE)
			);
			
			if (LookAndFeelUtil.isAquaLAF())
				setOpaque(false);
		}
		
		private JLabel getInfoLabel() {
			if (infoLabel == null) {
				infoLabel = new JLabel("No EnrichmentMap View selected");
				infoLabel.setEnabled(false);
				infoLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
			}
			
			return infoLabel;
		}
	}
}
