package org.baderlab.csplugins.enrichmentmap.task;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.baderlab.csplugins.enrichmentmap.CytoscapeServiceModule.Continuous;
import org.baderlab.csplugins.enrichmentmap.EdgeSimilarities;
import org.baderlab.csplugins.enrichmentmap.StreamUtil;
import org.baderlab.csplugins.enrichmentmap.TestUtils;
import org.baderlab.csplugins.enrichmentmap.model.DataSetFiles;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapParameters;
import org.baderlab.csplugins.enrichmentmap.model.FilterParameters;
import org.baderlab.csplugins.enrichmentmap.model.FilterType;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisParameters;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisParameters.AnalysisType;
import org.baderlab.csplugins.enrichmentmap.style.WidthFunction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.jukito.JukitoRunner;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Matchers;

import com.google.inject.Provider;


@RunWith(JukitoRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PostAnalysisTaskTest extends BaseNetworkTest {
	
	private static final String PATH = "src/test/resources/org/baderlab/csplugins/enrichmentmap/task/EMandPA/";
	
	public static class TestModule extends BaseNetworkTest.TestModule {
		@Override
		protected void configureTest() {
			super.configureTest();
		}
	}
	
	private static CyNetwork emNetwork;
	
	
	@Test
	public void test_1_EnrichmentMapBuildMapTask(CySessionManager sessionManager, CyApplicationManager applicationManager, CyNetworkManager networkManager) {
		EnrichmentMapParameters emParams = new EnrichmentMapParameters(sessionManager, new StreamUtil(), applicationManager);
		emParams.setMethod(EnrichmentMapParameters.method_generic);
		DataSetFiles dataset1files = new DataSetFiles();
		dataset1files.setGMTFileName(PATH + "gene_sets.gmt");  
		dataset1files.setExpressionFileName(PATH + "FakeExpression.txt");
		dataset1files.setEnrichmentFileName1(PATH + "fakeEnrichments.txt");
		dataset1files.setRankedFile(PATH + "FakeRank.rnk");  
		emParams.addFiles(EnrichmentMap.DATASET1, dataset1files);
		
	    buildEnrichmentMap(emParams);
	   	
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
	   	
	   	// Make the network available to the subsequent test methods (requires test methods are run in order)
	   	emNetwork = network;
	}
	
	
	/**
	 * Run post-analysis with the default mann-whitney test.
	 * Uses the network that was created by the previous test method.
	 */
	@Test
	public void test_2_PostAnalysisMannWhitney() throws Exception {
		PostAnalysisParameters.Builder builder = new PostAnalysisParameters.Builder();
		builder.setSignatureDataSet(EnrichmentMap.DATASET1);
		builder.setSignatureRankFile(EnrichmentMap.DATASET1);
		builder.setAnalysisType(AnalysisType.KNOWN_SIGNATURE);
		builder.setUniverseSize(11445);
		builder.setSignatureGMTFileName(PATH + "PA_top8_middle8_bottom8.gmt");
		builder.setAttributePrefix("EM1_");
		
		FilterParameters rankTest = new FilterParameters(FilterType.MANN_WHIT_TWO_SIDED);
		builder.setRankTestParameters(rankTest);
		
		runPostAnalysis(emNetwork, builder);
		// Assert that post-analysis created the new nodes correctly
		
		Map<String,CyNode> nodes = TestUtils.getNodes(emNetwork);
	   	assertEquals(5, nodes.size());
	   	assertTrue(nodes.containsKey("PA_TOP8_MIDDLE8_BOTTOM8"));
	   	
	   	EdgeSimilarities edges = TestUtils.getEdgeSimilarities(emNetwork);
	   	assertEquals(8, edges.size());
	   	
	   	CyEdge edge1 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) TOP8_PLUS100");
	   	assertNotNull(edge1);
	   	assertEquals(1.40E-6, emNetwork.getRow(edge1).get("EM1_Overlap_Mann_Whit_pVal", Double.class), 0.001);
	   	assertEquals(FilterType.MANN_WHIT_TWO_SIDED.toString(), emNetwork.getRow(edge1).get("EM1_Overlap_cutoff", String.class));
	   	
	   	CyEdge edge2 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) BOTTOM8_PLUS100");
	   	assertNotNull(edge2);
	   	assertEquals(1.40E-6, emNetwork.getRow(edge2).get("EM1_Overlap_Mann_Whit_pVal", Double.class), 0.001);
	   	assertEquals(FilterType.MANN_WHIT_TWO_SIDED.toString(), emNetwork.getRow(edge2).get("EM1_Overlap_cutoff", String.class));
	}
	
	
	/**
	 * Run post-analysis again, but with hypergeometric test this time.
	 * The result should keep the 2 edges that were created by the previous run
	 * plus add two new edges.
	 */
	@Test
	public void test_3_PostAnalysisHypergeometric_overlap() throws Exception {
		PostAnalysisParameters.Builder builder = new PostAnalysisParameters.Builder();
		builder.setSignatureDataSet(EnrichmentMap.DATASET1);
		builder.setSignatureRankFile(EnrichmentMap.DATASET1);
		builder.setAnalysisType(AnalysisType.KNOWN_SIGNATURE);
		builder.setUniverseSize(11445);
		builder.setSignatureGMTFileName(PATH + "PA_top8_middle8_bottom8.gmt");
		builder.setAttributePrefix("EM1_");
		
		FilterParameters rankTest = new FilterParameters(FilterType.HYPERGEOM, 0.25);
		builder.setRankTestParameters(rankTest);
		
		runPostAnalysis(emNetwork, builder);
		
		// Assert that post-analysis created the new nodes correctly
		Map<String,CyNode> nodes = TestUtils.getNodes(emNetwork);
	   	assertEquals(5, nodes.size());
	   	assertTrue(nodes.containsKey("PA_TOP8_MIDDLE8_BOTTOM8"));
	   	
	   	EdgeSimilarities edges = TestUtils.getEdgeSimilarities(emNetwork);
	   	assertEquals(10, edges.size());
	   	
	   	CyEdge edge1 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) TOP8_PLUS100");
	   	assertNotNull(edge1);
	   	assertEquals(1.40E-6,  emNetwork.getRow(edge1).get("EM1_Overlap_Mann_Whit_pVal", Double.class), 0.001);
	   	assertEquals(4.21E-11, emNetwork.getRow(edge1).get("EM1_Overlap_Hypergeom_pVal", Double.class), 0.001);
	   	assertEquals(FilterType.HYPERGEOM.toString(), emNetwork.getRow(edge1).get("EM1_Overlap_cutoff", String.class));
	   	
	   	CyEdge edge2 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) BOTTOM8_PLUS100");
	   	assertNotNull(edge2);
	   	assertEquals(1.40E-6,  emNetwork.getRow(edge2).get("EM1_Overlap_Mann_Whit_pVal", Double.class), 0.001);
	   	assertEquals(4.21E-11, emNetwork.getRow(edge2).get("EM1_Overlap_Hypergeom_pVal", Double.class), 0.001);
	   	assertEquals(FilterType.HYPERGEOM.toString(), emNetwork.getRow(edge2).get("EM1_Overlap_cutoff", String.class));
	   	
	   	CyEdge edge3 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) MIDDLE8_PLUS100");
	   	assertNotNull(edge3);
		assertNull(emNetwork.getRow(edge3).get("EM1_Overlap_Mann_Whit_pVal", Double.class));
	   	assertEquals(4.21E-11, emNetwork.getRow(edge3).get("EM1_Overlap_Hypergeom_pVal", Double.class), 0.001);
	   	assertEquals(FilterType.HYPERGEOM.toString(), emNetwork.getRow(edge3).get("EM1_Overlap_cutoff", String.class));
	   	
	   	CyEdge edge4 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) TOP1_PLUS100");
	   	assertNotNull(edge4);
	   	assertNull(emNetwork.getRow(edge4).get("EM1_Overlap_Mann_Whit_pVal", Double.class));
	   	assertEquals(0.19, emNetwork.getRow(edge4).get("EM1_Overlap_Hypergeom_pVal", Double.class), 0.01);
	   	assertEquals(FilterType.HYPERGEOM.toString(), emNetwork.getRow(edge4).get("EM1_Overlap_cutoff", String.class));
	}

	
	@Test
	public void test_4_WidthFunction(@Continuous VisualMappingFunctionFactory vmfFactoryContinuous, EnrichmentMapManager emManager, Provider<WidthFunction> widthFunctionProvider) {
		CyNetworkManager networkManager = mock(CyNetworkManager.class);
		when(networkManager.getNetworkSet()).thenReturn(Collections.singleton(emNetwork));
		
		ContinuousMapping<Double,Double> mockFunction = mock(ContinuousMapping.class);
		when(mockFunction.getMappedValue(Matchers.<CyRow>anyObject())).thenReturn(-1.0);
		when(vmfFactoryContinuous.createVisualMappingFunction(Matchers.<String>anyObject(), Matchers.<Class>anyObject(), Matchers.<VisualProperty>anyObject())).thenReturn(mockFunction);
	   	
		EdgeSimilarities edges = TestUtils.getEdgeSimilarities(emNetwork);
		
		CyEdge sigEdge1 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) TOP8_PLUS100");
		CyEdge sigEdge2 = edges.getEdge("PA_TOP8_MIDDLE8_BOTTOM8 (sig) TOP1_PLUS100");
		
		EnrichmentMap map = emManager.getEnrichmentMap(emNetwork.getSUID());
		assertNotNull(map);
		
		WidthFunction widthFunction = widthFunctionProvider.get();
		widthFunction.setEdgeWidths(emNetwork, "EM1_", null);
		
		String widthCol = "EM1_" + WidthFunction.EDGE_WIDTH_FORMULA_COLUMN;
		
		double sigWidth1 = emNetwork.getRow(sigEdge1).get(widthCol, Double.class);
		assertEquals(8.0, sigWidth1, 0.0);
		double sigWidth2 = emNetwork.getRow(sigEdge2).get(widthCol, Double.class);
		assertEquals(1.0, sigWidth2, 0.0);
	}
}
