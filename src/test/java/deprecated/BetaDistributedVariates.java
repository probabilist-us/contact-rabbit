/**
 * @deprecated
 */

package deprecated;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.random.ISAACRandom;
import org.apache.commons.math3.random.RandomGenerator;

public class BetaDistributedVariates {
	BetaDistribution BetaD;
	RandomGenerator rg;
	final double alpha = 0.8;
	final double beta = 7.2;
	public BetaDistributedVariates(long seed) {
		this.rg = new ISAACRandom(seed);
		BetaD = new BetaDistribution(this.rg, this.alpha, this.beta);
	}

	public static void main(String[] args) {
		long s = 3000;
		BetaDistributedVariates bdv1 = new BetaDistributedVariates(s);
		int n = 12;
		
		System.out.println("First set of beta random reals with seed " + s);
		for(int i = 0; i < n; i++) {
			System.out.print(bdv1.BetaD.sample() + ", ");
		}
		System.out.println();
		BetaDistributedVariates bdv2 = new BetaDistributedVariates(s);
		System.out.println("Second set of beta random reals with seed " + s);
		for(int i = 0; i < n; i++) {
			System.out.print(bdv2.BetaD.sample() + ", ");
		}

	}

}
