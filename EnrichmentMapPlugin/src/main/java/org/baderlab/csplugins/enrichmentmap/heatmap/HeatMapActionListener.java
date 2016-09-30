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

// $Id: HeatMapActionListener.java 368 2009-09-25 14:49:59Z risserlin $
// $LastChangedDate: 2009-09-25 10:49:59 -0400 (Fri, 25 Sep 2009) $
// $LastChangedRevision: 368 $
// $LastChangedBy: risserlin $
// $HeadURL: svn+ssh://risserlin@server1.baderlab.med.utoronto.ca/svn/EnrichmentMap/trunk/EnrichmentMapPlugin/src/org/baderlab/csplugins/enrichmentmap/HeatMapActionListener.java $

package org.baderlab.csplugins.enrichmentmap.heatmap;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.model.Ranking;
import org.baderlab.csplugins.enrichmentmap.parsers.RanksFileReaderTask;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;

/**
 * Created by User: risserlin Date: Feb 12, 2009 Time: 10:04:33 AM
 * <p>
 * Heat map action listener
 */
public class HeatMapActionListener implements ActionListener {

	private HeatMapPanel edgeOverlapPanel;
	private HeatMapPanel nodeOverlapPanel;
	private FileUtil fileUtil;
	private StreamUtil streamUtil;
	private CySwingApplication application;

	private HeatMapParameters hmParams;
	private JComboBox box;

	//Need to add the enrichment map parameters here in order to add an additional ranks to the EM
	private EnrichmentMapParameters params;
	private EnrichmentMap map;

	public HeatMapActionListener(HeatMapPanel edgeOverlapPanel, HeatMapPanel nodeOverlapPanel, JComboBox box,
			HeatMapParameters hmParams, EnrichmentMap map, FileUtil fileUtil, StreamUtil streamUtil,
			CySwingApplication application) {
		this.edgeOverlapPanel = edgeOverlapPanel;
		this.nodeOverlapPanel = nodeOverlapPanel;
		this.fileUtil = fileUtil;
		this.application = application;
		this.streamUtil = streamUtil;
		this.hmParams = hmParams;
		this.box = box;
		this.params = map.getParams();
		this.map = map;
	}

	/**
	 * Update heat map according to action selection
	 */
	public void actionPerformed(ActionEvent evt) {

		boolean updateAscendingButton = false;

		edgeOverlapPanel.clearPanel();
		nodeOverlapPanel.clearPanel();
		String select = (String) box.getSelectedItem();

		if(select.equalsIgnoreCase("Data As Is")) {
			hmParams.setTransformation(HeatMapParameters.Transformation.ASIS);
		} else if(select.equalsIgnoreCase("Row Normalize Data")) {
			hmParams.setTransformation(HeatMapParameters.Transformation.ROWNORM);
		} else if(select.equalsIgnoreCase("Log Transform Data")) {
			hmParams.setTransformation(HeatMapParameters.Transformation.LOGTRANSFORM);
		} else if(select.equalsIgnoreCase(HeatMapParameters.sort_hierarchical_cluster)) {
			hmParams.setSort(HeatMapParameters.Sort.CLUSTER);
			hmParams.setSortIndex(-1);
		}
		//Add a ranking file
		else if(select.equalsIgnoreCase("Add Rankings ... ")) {
			Map<String, Ranking> all_ranks = map.getAllRanks();
			FileChooserFilter filter_rnk = new FileChooserFilter("rnk Files", "rnk");
			FileChooserFilter filter_txt = new FileChooserFilter("txt Files", "txt");
			FileChooserFilter filter_xls = new FileChooserFilter("xls Files", "xls");

			//the set of filter (required by the file util method
			ArrayList<FileChooserFilter> all_filters = new ArrayList<FileChooserFilter>();
			all_filters.add(filter_rnk);
			all_filters.add(filter_txt);
			all_filters.add(filter_xls);

			// Get the file name
			File file = fileUtil.getFile(this.application.getJFrame(), "Import rank File", FileUtil.LOAD, all_filters);
			//File file = fileUtil.getFile(this.nodeOverlapPanel,"Import rank File", FileUtil.LOAD, all_filters);

			if(file != null) {
				//find out from the user what they want to name these ranking

				String ranks_name = JOptionPane.showInputDialog(this.nodeOverlapPanel, "What would you like to name these Rankings?", "My Rankings");
				boolean noname = true;
				while(noname) {
					noname = false;
					//make sure the name is not already in the rankings
					for(Iterator j = all_ranks.keySet().iterator(); j.hasNext();) {
						String current_name = j.next().toString();
						if(current_name.equalsIgnoreCase(ranks_name)) {
							noname = true;
							ranks_name = JOptionPane.showInputDialog(this.nodeOverlapPanel,
									"Sorry that name already exists.Please choose another name.");
						}
					}
				}

				//load the new ranks file
				//the new rank file is not associated with a dataset.
				//simply add it to Dataset 1
				RanksFileReaderTask ranking1 = new RanksFileReaderTask(file.getAbsolutePath(), map.getDataset(EnrichmentMap.DATASET1), ranks_name, true);
				try {
					ranking1.parse(null);
				} catch(IOException e) {
					e.printStackTrace();
				}

				//add an index to the ascending array for the new rank file.
				boolean[] ascending = hmParams.getAscending();
				boolean[] new_ascending = new boolean[ascending.length + 1];

				//copy old ascending in to the new ascending array
				System.arraycopy(ascending, 0, new_ascending, 0, ascending.length);

				//set ordering for rank fall to ascending
				new_ascending[new_ascending.length - 1] = true;

				hmParams.setAscending(new_ascending);
			}
		} else if(select.equalsIgnoreCase(HeatMapParameters.sort_none)) {
			hmParams.setSort(HeatMapParameters.Sort.NONE);
			hmParams.setSortIndex(-1);
			hmParams.setRankFileIndex("");
		} else if(select.contains(HeatMapParameters.sort_column)) {
			hmParams.setRankFileIndex("");
			hmParams.setSort(HeatMapParameters.Sort.COLUMN);
			if(hmParams.isSortbycolumn_event_triggered()) {
				//reset sort column trigger
				hmParams.setSortbycolumn_event_triggered(false);

				//change the ascending boolean flag for the column we are about to sort by
				hmParams.flipAscending(hmParams.getSortIndex());
			}
		} else {
			Set<String> ranks = map.getAllRankNames();

			//iterate through all the rank files.
			//the order should always be the same get a counter to find which index to
			//associate this rank file for the ascending and descending order
			int i = 0;
			int columns = 0;
			//calculate the number of indexes used for the column names
			if(params.isData2() && map.getDataset(EnrichmentMap.DATASET2).getExpressionSets() != null
					&& !map.getDataset(EnrichmentMap.DATASET1).getExpressionSets().getFilename()
							.equalsIgnoreCase(map.getDataset(EnrichmentMap.DATASET2).getExpressionSets().getFilename()))
				columns = map.getDataset(EnrichmentMap.DATASET1).getExpressionSets().getColumnNames().length
						+ map.getDataset(EnrichmentMap.DATASET2).getExpressionSets().getColumnNames().length - 2;
			else
				columns = map.getDataset(EnrichmentMap.DATASET1).getExpressionSets().getColumnNames().length;

			for(Iterator j = ranks.iterator(); j.hasNext();) {
				String ranks_name = j.next().toString();
				if(ranks_name.equalsIgnoreCase(select)) {
					hmParams.setSort(HeatMapParameters.Sort.RANK);
					hmParams.setRankFileIndex(ranks_name);
					hmParams.setSortIndex(columns + i);

					updateAscendingButton = true;
				}
				i++;
			}
		}

		//if the object selected is associated with a sorting order update the sort direction button
		if(updateAscendingButton) {
			//the two types that can be associated with a direction as columns and ranks
			/*
			 * if(hmParams.getSort() == HeatMapParameters.Sort.COLUMN)
			 * hmParams.setCurrent_ascending(hmParams.isAscending(hmParams.
			 * getSortIndex())); else if(hmParams.getSort() ==
			 * HeatMapParameters.Sort.RANK)
			 * hmParams.setCurrent_ascending(hmParams.isAscending(1)); //TODO:
			 * need to specify column for rank files.
			 */
			//only swap the direction of the sort if a column sort action was triggered
			if(hmParams.isSortbycolumn_event_triggered()) {
				//reset sort column trigger
				hmParams.setSortbycolumn_event_triggered(false);

				//change the ascending boolean flag for the column we are about to sort by
				hmParams.flipAscending(hmParams.getSortIndex());
			}
		}

		hmParams.ResetColorGradient_ds1();

		if(params.isData2() && map.getDataset(EnrichmentMap.DATASET2).getExpressionSets() != null
				&& !map.getDataset(EnrichmentMap.DATASET1).getExpressionSets().getFilename()
						.equalsIgnoreCase(map.getDataset(EnrichmentMap.DATASET2).getExpressionSets().getFilename()))
			hmParams.ResetColorGradient_ds2();

		edgeOverlapPanel.updatePanel();
		nodeOverlapPanel.updatePanel();

	}
}
