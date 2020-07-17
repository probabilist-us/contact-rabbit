/**
 * @deprecated
 */
package deprecated;

import java.util.Random;

/**
 * @author rwdarli
 *
 */
public class SeededRandomTest {

	/**
	 * 
	 */
	Random g;
	public SeededRandomTest(long seed) {
		g = new Random(seed);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long s = 59682;
		int n = 12;
		SeededRandomTest test1 = new SeededRandomTest(s);
		System.out.println("First set of random numbers with seed " + s);
		for(int i = 0; i < n; i++) {
			System.out.print(test1.g.nextDouble() + ", ");
		}
		SeededRandomTest test2 = new SeededRandomTest(s);
		System.out.println("Second set of random numbers with seed " + s);
		for(int i = 0; i < n; i++) {
			System.out.print(test2.g.nextDouble() + ", ");
		}	

	}

}
