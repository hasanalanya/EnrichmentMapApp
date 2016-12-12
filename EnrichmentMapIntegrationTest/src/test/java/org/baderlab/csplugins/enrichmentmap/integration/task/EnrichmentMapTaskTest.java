package org.baderlab.csplugins.enrichmentmap.integration.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.baderlab.csplugins.enrichmentmap.AfterInjectionModule;
import org.baderlab.csplugins.enrichmentmap.ApplicationModule;
import org.baderlab.csplugins.enrichmentmap.CytoscapeServiceModule;
import org.baderlab.csplugins.enrichmentmap.PropertyManager;
import org.baderlab.csplugins.enrichmentmap.integration.BaseIntegrationTest;
import org.baderlab.csplugins.enrichmentmap.integration.EdgeSimilarities;
import org.baderlab.csplugins.enrichmentmap.integration.SerialTestTaskManager;
import org.baderlab.csplugins.enrichmentmap.integration.TestUtils;
import org.baderlab.csplugins.enrichmentmap.model.DataSet.Method;
import org.baderlab.csplugins.enrichmentmap.model.DataSetFiles;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters.SimilarityMetric;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentResultFilterParams.NESFilter;
import org.baderlab.csplugins.enrichmentmap.model.LegacySupport;
import org.baderlab.csplugins.enrichmentmap.task.MasterMapTaskFactory;
import org.baderlab.csplugins.enrichmentmap.view.mastermap.DataSetParameters;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.TaskIterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.peaberry.osgi.OSGiModule;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(PaxExam.class)
public class EnrichmentMapTaskTest extends BaseIntegrationTest {
	
	private static final String PATH = "/EnrichmentMapTaskTest/";
	
	@Inject private CyNetworkManager networkManager;
	@Inject private BundleContext bc;
	
	
	protected void buildEnrichmentMap(EMCreationParameters params, DataSetFiles datasetFiles, String datasetName) {
		List<DataSetParameters> dataSets = Arrays.asList(new DataSetParameters(datasetName, Method.Generic, datasetFiles));
		
		Injector injector = Guice.createInjector(new OSGiModule(bc), new AfterInjectionModule(), new CytoscapeServiceModule(), new ApplicationModule());
		MasterMapTaskFactory.Factory masterMapTaskFactoryFactory = injector.getInstance(MasterMapTaskFactory.Factory.class);
		MasterMapTaskFactory taskFactory = masterMapTaskFactoryFactory.create(params, dataSets);
	    
		TaskIterator taskIterator = taskFactory.createTaskIterator();
	   	SerialTestTaskManager taskManager = new SerialTestTaskManager();
	   	taskManager.execute(taskIterator);
	}
	
	
	@Test
	public void testEnrichmentMapBuildMapTask() throws Exception {
		String geneSetsFile   = createTempFile(PATH, "gene_sets.gmt").getAbsolutePath();
		String expressionFile = createTempFile(PATH, "FakeExpression.txt").getAbsolutePath();
		String enrichmentFile = createTempFile(PATH, "fakeEnrichments.txt").getAbsolutePath();
		String rankFile       = createTempFile(PATH, "FakeRank.rnk").getAbsolutePath();
		
		PropertyManager pm = new PropertyManager();
		EMCreationParameters params = new EMCreationParameters("EM1_", pm.getDefaultPvalue(), pm.getDefaultQvalue(), NESFilter.ALL, Optional.empty(), SimilarityMetric.JACCARD, pm.getDefaultJaccardCutOff(), pm.getDefaultCombinedConstant());
		
		DataSetFiles dataset1files = new DataSetFiles();
		dataset1files.setGMTFileName(geneSetsFile);  
		dataset1files.setExpressionFileName(expressionFile);
		dataset1files.setEnrichmentFileName1(enrichmentFile);
		dataset1files.setRankedFile(rankFile);  
		
	    buildEnrichmentMap(params, dataset1files, LegacySupport.DATASET1);
	   	
	   	// Assert the network is as expected
	   	Set<CyNetwork> networks = networkManager.getNetworkSet();
	   	assertEquals(1, networks.size());
	   	CyNetwork network = networks.iterator().next();
	   	
	   	Map<String,CyNode> nodes = TestUtils.getNodes(network);
	   	assertEquals(4, nodes.size());
	   	assertTrue(nodes.containsKey("BOTTOM8_PLUS100"));
	   	assertTrue(nodes.containsKey("MIDDLE8_PLUS100"));
	   	assertTrue(nodes.containsKey("TOP8_PLUS100"));
	   	assertTrue(nodes.containsKey("TOP1_PLUS100"));
	   	
	   	EdgeSimilarities edges = TestUtils.getEdgeSimilarities(network);
	   	
	   	assertEquals(6, edges.size());
	   	assertTrue(edges.containsEdge("MIDDLE8_PLUS100", "Geneset_Overlap", "BOTTOM8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP8_PLUS100", "Geneset_Overlap", "MIDDLE8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP8_PLUS100", "Geneset_Overlap", "BOTTOM8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap", "TOP8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap", "MIDDLE8_PLUS100"));
	   	assertTrue(edges.containsEdge("TOP1_PLUS100", "Geneset_Overlap" ,"BOTTOM8_PLUS100"));
	}
	

}