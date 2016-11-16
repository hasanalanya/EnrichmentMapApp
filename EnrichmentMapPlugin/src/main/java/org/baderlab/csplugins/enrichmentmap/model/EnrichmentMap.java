package org.baderlab.csplugins.enrichmentmap.model;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.baderlab.csplugins.enrichmentmap.model.DataSet.Method;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/***
 * An Enrichment Map object contains the minimal information needed to build an enrichment map.
 */
public class EnrichmentMap {

	private final String name;
	private long networkID;
	
	//reference to the parameters used to create this map
	private final EMCreationParameters params;

	private Map<String, DataSet> datasets = new HashMap<>();

	//Hashmap of all the similarities between all the genesets
	//key = geneset1 + geneset2
	//value = geneset similarity 
	// MKTODO use SimilarityKey instead of String
	private Map<String, GenesetSimilarity> genesetSimilarity = new HashMap<>();

	//The set of genes defined in the Enrichment map
	private BiMap<Integer,String> genes = HashBiMap.create();

	//post analysis signature genesets associated with this map.
	private Map<String, GeneSet> signatureGenesets = new HashMap<>();

	private int NumberOfGenes = 0;
	
	
	/**
	 * Class Constructor Given - EnrichmentnMapParameters create a new
	 * enrichment map. The parameters contain all cut-offs and file names for the analysis
	 */
	public EnrichmentMap(String name, EMCreationParameters params) {
		this.params = params;
		this.name = name;
	}

	
	public void addDataSet(String name, DataSet dataset) {
		if(datasets.containsKey(name)) {
			throw new IllegalArgumentException("DataSet with name " + name + " already exists in this enrichment map");
		}
		
		DataSetFiles files = dataset.getDatasetFiles();
		if(isNullOrEmpty(files.getEnrichmentFileName1()) && isNullOrEmpty(files.getGMTFileName()) && isNullOrEmpty(files.getExpressionFileName())) {
			throw new IllegalArgumentException("At least one of the required files must be given");
		}
			
		datasets.put(name, dataset);
		initializeFiles(dataset);
	}
	
	/**
	 * Method to transfer files specified in the parameters to the objects they correspond to.
	 */
	private void initializeFiles(DataSet dataset) {
		DataSetFiles files = dataset.getDatasetFiles();
		if(!isNullOrEmpty(files.getGMTFileName()))
			dataset.getSetofgenesets().setFilename(files.getGMTFileName());

		//expression files
		if(!isNullOrEmpty(files.getExpressionFileName()))
			dataset.getExpressionSets().setFilename(files.getExpressionFileName());

		//enrichment results files
		if(!isNullOrEmpty(files.getEnrichmentFileName1()))
			dataset.getEnrichments().setFilename1(files.getEnrichmentFileName1());
		if(files.getEnrichmentFileName2() != null && !files.getEnrichmentFileName2().isEmpty())
			dataset.getEnrichments().setFilename2(files.getEnrichmentFileName2());

		//phenotypes
		if(!isNullOrEmpty(files.getPhenotype1()))
			dataset.getEnrichments().setPhenotype1(files.getPhenotype1());
		if(!isNullOrEmpty(files.getPhenotype2()))
			dataset.getEnrichments().setPhenotype2(files.getPhenotype2());

		//rank files - dataset1 
		if(!isNullOrEmpty(files.getRankedFile())) {
			if(dataset.getMethod() == Method.GSEA) {
				dataset.getExpressionSets().createNewRanking(Ranking.GSEARanking);
				dataset.getExpressionSets().getRanksByName(Ranking.GSEARanking).setFilename(files.getRankedFile());
			} else {
				dataset.getExpressionSets().createNewRanking(dataset.getName());
				dataset.getExpressionSets().getRanksByName(dataset.getName()).setFilename(files.getRankedFile());
			}
		}
	}

	/**
	 * Check to see that there are genes in the filtered genesets If the ids do
	 * not match up, after a filtering there will be no genes in any of the genesets
	 * 
	 * @return true if Genesets have genes, return false if all the genesets are empty
	 */
	public boolean checkGenesets() {
		for(DataSet dataset : datasets.values()) {
			Map<String, GeneSet> genesets = dataset.getSetofgenesets().getGenesets();
			for(GeneSet geneset : genesets.values()) {
				Set<Integer> genesetGenes = geneset.getGenes();
				//if there is at least one gene in any of the genesets then the ids match.
				if(!genesetGenes.isEmpty()) {
					return true;
				}
			}
		}
//		if(params.getMethod() == Method.Specialized)
//			return true;
		return false;
	}

	public boolean containsGene(String gene) {
		return genes.containsValue(gene);
	}

	public String getGeneFromHashKey(Integer hash) {
		return genes.get(hash);
	}
	
	public Integer getHashFromGene(String gene) {
		// MKTOD should I toUpperCase?
		return genes.inverse().get(gene);
	}
	
	public Collection<String> getAllGenes() {
		return genes.values();
	}
	
	public Optional<Integer> addGene(String gene) {
		gene = gene.toUpperCase();
		
		Map<String,Integer> geneToHash = genes.inverse();
		
		Integer hash = geneToHash.get(gene);
		if(hash != null)
			return Optional.of(hash);

		Integer newHash = ++NumberOfGenes;
		genes.put(newHash, gene);
		return Optional.of(newHash);
	}
	
	@Deprecated // MKTODO this is here to support legacy session loading, TEMPORARY until a builder is available
	public void addGene(String gene, int id) {
		genes.put(id, gene);
		if(id > NumberOfGenes)
			NumberOfGenes = id;
	}
	

	public int getNumberOfGenes() {
		return NumberOfGenes;
	}

	public void setNumberOfGenes(int numberOfGenes) {
		NumberOfGenes = numberOfGenes;
	}

	/*
	 * Given a set of genesets Go through the genesets and extract all the genes
	 * Return - hashmap of genes to hash keys (used to create an expression file
	 * when it is not present so user can use expression viewer to navigate
	 * genes in a geneset without have to generate their own dummy expression file)
	 */
	public Map<String, Integer> getGenesetsGenes(Collection<GeneSet> currentGenesets) {
		Map<String, Integer> genesetGenes = new HashMap<>();

		for(GeneSet geneset : currentGenesets) {
			//compare the HashSet of dataset genes to the HashSet of the current Geneset
			//only keep the genes from the geneset that are in the dataset genes
			for(Integer genekey : geneset.getGenes()) {
				//get the current geneName
				if(genes.containsKey(genekey)) {
					String name = genes.get(genekey);
					genesetGenes.put(name, genekey);
				}
			}
		}
		return genesetGenes;
	}

	/*
	 * Filter all the genesets by the dataset genes If there are multiple sets
	 * of genesets make sure to filter by the specific dataset genes
	 */
	public void filterGenesets() {
		for(DataSet dataset : datasets.values()) {
			//only filter the genesets if dataset genes are not null or empty
			if(dataset.getDatasetGenes() != null && !dataset.getDatasetGenes().isEmpty()) {
				dataset.getSetofgenesets().filterGenesets(dataset.getDatasetGenes());
			}
		}
	}

	public String getName() {
		return name;
	}


	/*
	 * Return a hash of all the genesets in the set of genesets regardless of which dataset it comes from.
	 */
	public Map<String, GeneSet> getAllGenesets() {
		//go through each dataset and get the genesets from each
		Map<String, GeneSet> allGenesets = new HashMap<>();
		
		for(DataSet dataset : datasets.values()) {
			Map<String, GeneSet> genesets = dataset.getSetofgenesets().getGenesets();
			allGenesets.putAll(genesets);
		}
		if(signatureGenesets != null && !signatureGenesets.isEmpty())
			allGenesets.putAll(signatureGenesets);
		
		return allGenesets;
	}

	/*
	 * Return a hash of all the genesets but not inlcuding the signature genesets.
	 */
	public Map<String, GeneSet> getEnrichmentGenesets() {
		//go through each dataset and get the genesets from each
		Map<String, GeneSet> allGenesets = new HashMap<>();
		for(DataSet dataset : datasets.values()) {
			Map<String, GeneSet> genesets = dataset.getSetofgenesets().getGenesets();
			allGenesets.putAll(genesets);
		}
		return allGenesets;
	}

	/*
	 * Return a hash of all the genesets in the set of genesets of interest
	 * regardless of which dataset it comes from
	 */
	public Map<String, GeneSet> getAllGenesetsOfInterest() {
		//go through each dataset and get the genesets from each
		Map<String, GeneSet> allGenesetsOfInterest = new HashMap<>();
		
		for(DataSet dataset : datasets.values()) {
			Map<String, GeneSet> genesets = dataset.getGenesetsOfInterest().getGenesets();
			allGenesetsOfInterest.putAll(genesets);
		}
		//if there are post analysis genesets, add them to the set of all genesets
		if(signatureGenesets != null && !signatureGenesets.isEmpty())
			allGenesetsOfInterest.putAll(signatureGenesets);
		
		return allGenesetsOfInterest;
	}

	public Map<String, GenesetSimilarity> getGenesetSimilarity() {
		return genesetSimilarity;
	}

	public void setGenesetSimilarity(Map<String, GenesetSimilarity> genesetSimilarity) {
		this.genesetSimilarity = genesetSimilarity;
	}
	
	public Map<String, DataSet> getDatasets() {
		return datasets;
	}
	
	/**
	 * Returns all the DataSets in a predictable order.
	 */
	public List<DataSet> getDatasetList() {
		List<DataSet> datasetList = new ArrayList<>(datasets.values());
		datasetList.sort(Comparator.naturalOrder());
		return datasetList;
	}

	public void setDatasets(Map<String, DataSet> datasets) {
		this.datasets = datasets;
	}

	public int getDataSetCount() {
		return datasets.size();
	}
	
	public DataSet getDataset(String datasetname) {
		return datasets.get(datasetname);
	}
	
	/**
	 * Returns all the DataSet names in a predictable order.
	 */
	public List<String> getDatasetNames() {
		return getDatasetList().stream().map(DataSet::getName).collect(Collectors.toList());
	}

	public EMCreationParameters getParams() {
		return params;
	}

	public long getNetworkID() {
		return networkID;
	}

	public void setNetworkID(long networkID) {
		this.networkID = networkID;
	}

	
	public Set<String> getAllRankNames() {
		Set<String> allRankNames = new HashSet<>();
		//go through each Dataset
		for(DataSet dataset : datasets.values()) {
			//there could be duplicate ranking names for two different datasets. Add the dataset to the ranks name
			Set<String> all_names = dataset.getExpressionSets().getAllRanksNames();
			for(String name : all_names) {
				allRankNames.add(name + "-" + dataset.getName());
			}

		}
		return allRankNames;
	}

	public Map<String, Ranking> getAllRanks() {
		Map<String, Ranking> allranks = new HashMap<>();
		for(DataSet dataset : datasets.values()) {
			allranks.putAll(dataset.getExpressionSets().getRanks());
		}
		return allranks;
	}

	public Ranking getRanksByName(String ranks_name) {

		//break the ranks file up by "-"
		//check to see if the rank file is dataset specific
		//needed for encoding the same ranking file name from two different dataset in the interface
		String ds = "";
		String rank = "";
		if(ranks_name.split("-").length == 2) {
			ds = ranks_name.split("-")[1];
			rank = ranks_name.split("-")[0];
		}

		for(Iterator<String> k = datasets.keySet().iterator(); k.hasNext();) {
			String current_dataset = k.next();
			if(!ds.equalsIgnoreCase("") && !rank.equalsIgnoreCase("")) {
				//check that this is the right dataset
				if(ds.equalsIgnoreCase(current_dataset)
						&& (datasets.get(current_dataset)).getExpressionSets().getAllRanksNames().contains(rank)) {
					return datasets.get(current_dataset).getExpressionSets().getRanksByName(rank);
				}
			} else if((datasets.get(current_dataset)).getExpressionSets().getAllRanksNames().contains(ranks_name)) {
				return datasets.get(current_dataset).getExpressionSets().getRanksByName(ranks_name);
			}
		}
		return null;
	}

	/*
	 * Return a hash of all different type of genesets from all the datasets
	 * regardless of which dataset it comes from
	 */
	public Set<String> getAllGenesetTypes() {
		//go through each dataset and get the genesets from each
		Set<String> allGenesetTypes = new HashSet<>();
		for(DataSet dataset : datasets.values()) {
			Set<String> genesetsTypes = dataset.getSetofgenesets().getGenesetTypes();
			allGenesetTypes.addAll(genesetsTypes);
		}
		return allGenesetTypes;
	}

	/**
	 * @param signatureGenesets
	 *            the signatureGenesets to set
	 */
	public void setSignatureGenesets(Map<String, GeneSet> signatureGenesets) {
		this.signatureGenesets = signatureGenesets;
	}

	/**
	 * @return the signatureGenesets
	 */
	public Map<String, GeneSet> getSignatureGenesets() {
		return signatureGenesets;
	}

}
