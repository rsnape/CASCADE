/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import org.jgap.*;
import org.jgap.impl.DoubleGene;

/**
 * @author jsnape
 *
 */
public class GASetupUtilities {
	
	public static Chromosome createChromosome(double[] arr)
	{
		
		Gene[] values = new DoubleGene[arr.length];
		int k = 0;
		
		for (double i : arr)
		{
			values[k].setAllele(i);
			k++;
		}
		
		return new Chromosome(values);
	}

}
