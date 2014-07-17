package org.baderlab.csplugins.enrichmentmap.autoannotate.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.baderlab.csplugins.enrichmentmap.autoannotate.model.AnnotationSet;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.Cluster;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;

/**
 * @author arkadyark
 * <p>
 * Date   July 2, 2014<br>
 * Time   14:35:53 PM<br>
 */

public class AutoAnnotatorDisplayPanel extends JPanel implements CytoPanelComponent{

	private static final long serialVersionUID = 6589442061666054048L;
	
	private static AutoAnnotatorDisplayPanel instance;
	
	private CySwingApplication application;
	private HashMap<String, AnnotationSet> clusterSets;
	private HashMap<AnnotationSet, JPanel> clustersToTables;
	private JLabel networkLabel;
	private JPanel mainPanel;
	private HashMap<CyNetworkView, JComboBox> networkViewToClusterSetDropdown;
	private JComboBox clusterSetDropdown;

	private CyNetworkView selectedView;

	private JCheckBox wordCloudDisplayToggle;

	


	public static AutoAnnotatorDisplayPanel getInstance() {
		return instance;
	}

	public AutoAnnotatorDisplayPanel(CySwingApplication application) {
		instance = this;
		this.application = application;
		this.clusterSets = new HashMap<String, AnnotationSet>();
		this.clustersToTables = new HashMap<AnnotationSet, JPanel>();
		this.mainPanel = createMainPanel();
		this.networkViewToClusterSetDropdown = new HashMap<CyNetworkView, JComboBox>();
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.EAST);
	}
	
	public void addClusters(AnnotationSet clusters) {
		// If this is the view's first AnnotationSet
		if (!networkViewToClusterSetDropdown.containsKey(clusters.view)) {
			addNetworkView(clusters.view);
		}
		
		clusterSets.put(clusters.name, clusters);
		
		JPanel clusterTable = createClusterSetTablePanel(clusters);
		clustersToTables.put(clusters, clusterTable);
		JScrollPane clusterTableScroll = new JScrollPane(clusterTable);
		clusterTableScroll.setColumnHeaderView(((JTable) clusterTable.getComponent(0)).getTableHeader());
		clusterTableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(clusterTableScroll, BorderLayout.WEST);

		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(clusters.view);
		clusterSetDropdown.addItem(clusters);
		clusterSetDropdown.setSelectedIndex(clusterSetDropdown.getItemCount()-1);
	}
	
	public void addNetworkView(CyNetworkView view) {
		// Create dropdown with cluster sets of this networkView
		final JComboBox clusterSetDropdown = new JComboBox(); // Final so that the item listener can access it
		clusterSetDropdown.setPreferredSize(new Dimension(180, 30));
		clusterSetDropdown.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					AnnotationSet clusters = (AnnotationSet) itemEvent.getItem();
					clusters.drawAnnotations();							
					clustersToTables.get(clusters).getParent().getParent().setVisible(true); // Show selected table
					((JPanel) clusterSetDropdown.getParent()).updateUI();
				} else if (itemEvent.getStateChange() == ItemEvent.DESELECTED) {
					AnnotationSet clusters = (AnnotationSet) itemEvent.getItem();
	         		clusters.eraseAnnotations();
					clustersToTables.get(clusters).getParent().getParent().setVisible(false);
					((JPanel) clusterSetDropdown.getParent()).updateUI();
				}
            }
		});
		mainPanel.add(clusterSetDropdown);
		networkViewToClusterSetDropdown.put(view, clusterSetDropdown);
		selectedView = view;
		networkLabel.setText(view.getModel().toString());
		mainPanel.updateUI();
	}
	
	public void removeClusters(AnnotationSet clusters) {
		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
		clusterSetDropdown.removeItem(clusterSets.get(clusters.name));
		clusterSets.remove(clusters.name);
	}
	
	private JPanel createMainPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		// Label showing network label
		networkLabel = new JLabel("");
		Font font = networkLabel.getFont();
		networkLabel.setFont(new Font(font.getFamily(), font.getStyle(), 18));
		mainPanel.add(networkLabel);
		
        // Button to remove all annotations
        JButton clearButton = new JButton("Remove Annotation Set");
        ActionListener clearActionListener = new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem();
        		CyNetworkView networkView = clusters.clusterSet.firstEntry().getValue().getNetworkView();
        		CyNetwork network = networkView.getModel();
        		// Delete WordInfo column created by WordCloud
         		for (CyColumn column : network.getDefaultNodeTable().getColumns()) {
         			String name = column.getName();
         			if (name.equals("WC_Word") || name.equals("WC_FontSize") || name.equals("WC_Cluster") || name.equals("WC_Number")) {
         				// Problem - leaves them floating around the cloud manager in WordCloud - may have to do another Tuneable Task
         				network.getDefaultNodeTable().deleteColumn(name);
         			}
         		}
        		// Delete all annotations
         		clusters.destroyAnnotations();
         		CytoPanel panel = application.getCytoPanel(getCytoPanelName());
         		panel.setSelectedIndex(panel.indexOfComponent(AutoAnnotatorDisplayPanel.getInstance()));
         		clusterSetDropdown.removeItem(clusterSetDropdown.getSelectedItem());
         		remove(clustersToTables.get(clusters).getParent());
         		clustersToTables.remove(clusters);
        	}
        };
        clearButton.addActionListener(clearActionListener); 
        mainPanel.add(clearButton);
        
        // Button to update annotations and the table
        JButton updateButton = new JButton("Update Annotation Set");
        ActionListener updateActionListener = new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem(); 
        		clusters.updateCoordinates();
        		clusters.updateLabels();
        		clusters.eraseAnnotations(); 
        		clusters.drawAnnotations();
        		// Update the table if the value has changed (WordCloud has been updated)
        		DefaultTableModel model = (DefaultTableModel) ((JTable) clustersToTables.get(clusters).getComponent(0)).getModel();
        		int i = 0;
        		for (Cluster cluster : clusters.clusterSet.values()) {
        			if (!(model.getValueAt(i, 1).equals(cluster.getLabel()))) {
        				model.setValueAt(cluster.getLabel(), i, 1);
        			}
        			i++;
        		}
        	}
        };
        updateButton.addActionListener(updateActionListener); 
        mainPanel.add(updateButton);
        
        wordCloudDisplayToggle = new JCheckBox("Show WordCloud display on selection");
        mainPanel.add(wordCloudDisplayToggle);
        
		return mainPanel;
	}
	
	public void updateAnnotations() {
		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem(); 
		clusters.updateCoordinates();
		clusters.eraseAnnotations(); 
		clusters.drawAnnotations();
	}
	
	private JPanel createClusterSetTablePanel(final AnnotationSet clusters) {
		
		JPanel tablePanel = new JPanel();
		
		DefaultTableModel model = new DefaultTableModel() {
			private static final long serialVersionUID = -1277709187563893042L;

			@Override
		    public boolean isCellEditable(int row, int column) {
		        return column == 0 ? false : true;
		    }
		};
		model.addColumn("Cluster number");
		model.addColumn("Label");

		final JTable table = new JTable(model); // Final to be able to use inside of listener
		
		model.addTableModelListener(new TableModelListener() { // Update the label value
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE || e.getColumn() == 1) {
					int editedRowIndex = e.getFirstRow() == table.getSelectedRow()? e.getLastRow() : e.getFirstRow(); 
					Cluster editedCluster = clusters.clusterSet.get(editedRowIndex + 1);
					editedCluster.setLabel((String) table.getValueAt(editedRowIndex, 1));
					editedCluster.erase();
					editedCluster.drawAnnotations();
				}
			}
		});
		for (Cluster cluster : clusters.clusterSet.values()) {
			Object[] rowData = {cluster , cluster.getLabel()};
			model.addRow(rowData);
		}
		
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) { // Down-click and up-click are separate events
					int selectedRowIndex = table.getSelectedRow();
					Cluster selectedCluster = (Cluster) table.getValueAt(selectedRowIndex, 0); 
					selectedCluster.select();
					if (!wordCloudDisplayToggle.isSelected()) {
						try {
							Thread.sleep(900); // Give WordCloud time to switch to its panel
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						CytoPanel panel = application.getCytoPanel(getCytoPanelName());
						int index = panel.indexOfComponent(AutoAnnotatorDisplayPanel.getInstance());
						if (index != -1) {
							panel.setSelectedIndex(panel.indexOfComponent(AutoAnnotatorDisplayPanel.getInstance()));
						}
					}
				}
			}
		});
		
		Dimension d = new Dimension(500, table.getPreferredSize().height);
		table.setPreferredSize(d);
		table.setPreferredScrollableViewportSize(d);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		tablePanel.add(table, BorderLayout.CENTER);
		return tablePanel;
	}
	
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.SOUTH;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return "Annotation Display";
	}

	public void updateSelectedView(CyNetworkView view) {
		if (networkViewToClusterSetDropdown.containsKey(view)) {
			// Hide previously selected dropdown
			clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
			clusterSetDropdown.setVisible(false);				
			selectedView = view;
			// Update the label with the name of the newly selected network
			networkLabel.setText(view.getModel().toString());
			mainPanel.updateUI();
			// Show newly selected dropdown
			clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
			clusterSetDropdown.setVisible(true);
			// Keep the panel visible (WordCloud switches too) - doesn't work!
//			CytoPanel panel = application.getCytoPanel(getCytoPanelName());
//			int index = panel.indexOfComponent(AutoAnnotatorDisplayPanel.getInstance());
//			if (index != -1) {
//				panel.setSelectedIndex(panel.indexOfComponent(AutoAnnotatorDisplayPanel.getInstance()));
//			}
		}
	}


}
