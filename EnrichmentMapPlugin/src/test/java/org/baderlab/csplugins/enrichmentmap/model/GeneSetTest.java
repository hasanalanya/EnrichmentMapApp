package org.baderlab.csplugins.enrichmentmap.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/*
 * A Geneset is a basic element of the Enrichment map
 * A Geneset consists of name, description and a list of genes
 * *Optional* - gene set can also have a source specified.  The only way to specify
 * 		a source is through the format of the name. If the geneset name can be divided
 * 		into tokens using the tokenizer "%" then the second token is taken to be the source.
 * The list of genes is converted when loaded to a set of integers to speed up search, filter and other operations
 * and minimize amount of memory required to store them.
 */

public class GeneSetTest {

	@Test
	public void testCreateEmptyGeneSet(){
		//create a new GeneSet
		GeneSet.Builder builder = new GeneSet.Builder("Gene Set 1", "fake geneset");
		
		builder.addGene(10);
		builder.addGene(12);
		builder.addGene(-1);
		builder.addGene(0);
		//check if it handles duplicates
		builder.addGene(10);
		
		GeneSet gs = builder.build();
		assertEquals("Gene Set 1", gs.getName());
		assertEquals("fake geneset", gs.getDescription());
		
		assertEquals(4, gs.getGenes().size());
		
	}
	
	@Test
	public void testCreateGenesetFromStringArray(){
		
		//String Array is what we use to create a saved geneset in cytoscape
		String[] saved_gs = new String[7];
		
		//the first object in the array is the hashmap key, which is the name of the 
		//geneset.
		saved_gs[0] = "Gene Set 1";
		saved_gs[1] = "Gene Set 1";
		saved_gs[2] = "fake geneset";
		saved_gs[3] = "10";
		saved_gs[4] = "12";
		saved_gs[5] = "-1";
		saved_gs[6] = "0";
		
		
		GeneSet.Builder builder = new GeneSet.Builder(saved_gs);
		GeneSet gs = builder.build();
		
		assertEquals("Gene Set 1", gs.getName());
		assertEquals("fake geneset", gs.getDescription());
		
		assertEquals(4, gs.getGenes().size());
		
		List<Integer> geneIds = new ArrayList<>(gs.getGenes());
		Collections.sort(geneIds);
		assertEquals(Arrays.asList(-1,0,10,12), geneIds);
		
		//test equals function
		GeneSet.Builder builder2 = new GeneSet.Builder("Gene Set 1", "fake geneset");
		builder2.addGene(10);
		builder2.addGene(12);
		builder2.addGene(-1);
		
		GeneSet gs2 = builder2.build();
		assertFalse(gs.equals(gs2));
		
		builder2 = GeneSet.Builder.from(gs2);
		builder2.addGene(0);
		
		gs2 = builder2.build();
		assertTrue(gs.equals(gs2));
	}
	
	@Test
	public void testImbeddedSource(){
			
		//create a new GeneSet from the structure used in the internally generated gene set files
		GeneSet gs = new GeneSet.Builder("alanine biosynthesis II%HumanCyc%ALANINE-SYN2-PWY", "alanine biosynthesis II").build();
		
		assertEquals("alanine biosynthesis II%HumanCyc%ALANINE-SYN2-PWY", gs.getName());
		assertEquals("alanine biosynthesis II", gs.getDescription());
		assertEquals("HumanCyc", gs.getSource().get());
		
	}
}
