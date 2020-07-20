package mainpackage;

/**
 * Assumes that the waypoint list is sorted by mobileID, which are consecutive
 * integers starting at 0. This could be fixed.
 * Working correctly 7.17.20. Needs tuning.
 */

import java.util.HashSet;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;

import simulators.PlaceDependentContactMaker;
import utilities.GenericWaypointCSVReader;
import utilities.SetOfIntegersCSVWriter;

public class MainClass {
	final double sojournWidth = 1.0 / 48.0; // unit = days
	final double meanInfectionProbability = 0.1;
	final double initialInfectionRate = 0.010; // determines # sources
	final long seed = 1000000;
	final boolean probabilityVariesByPlaceForTargets = true;
	private GenericWaypointCSVReader wpReader; // mobileID and placeID are integers
	private PlaceDependentContactMaker<Integer, Integer> contact;
	private Set<Integer> sourceMobileIDs;
	Random g;

	public MainClass(String waypointFilename) {
		this.wpReader = new GenericWaypointCSVReader(waypointFilename);
		int numMobileIDs = this.wpReader.lastMobileID(); // last one on the list, plus 1.
		/*
		 * Number of sources is Poisson(mu); mu = # mobileIDs * infection rate.
		 * Approximate by rounding mu + Z*sqrt(mu), where Z is Gaussian.
		 */
		double mu = (double) numMobileIDs * this.initialInfectionRate;
		g = new Random();
		int sourceNumber = (int) Math.round(mu + Math.sqrt(mu) * (g.nextGaussian()));
		this.sourceMobileIDs = new HashSet<>();
		/*
		 * Randomly select the sources from among all mobileIDs
		 */
		while (this.sourceMobileIDs.size() < sourceNumber) {
			this.sourceMobileIDs.add(Integer.valueOf(g.nextInt(numMobileIDs)));
		}
		System.out.println("A random subset of " + sourceNumber + " mobileIDs has been selected as sources.");

		this.contact = new PlaceDependentContactMaker<Integer, Integer>(this.sojournWidth,
				this.meanInfectionProbability, this.seed, this.sourceMobileIDs, this.wpReader.getWaypointList());

		System.out.println("Exposures and infections have been simulated.");
		System.out.println();
	}

	/**
	 * 
	 * @param args path to input file of FractalRabbit waypoints
	 */
	public static void main(String[] args) {
		// boilerplate
		System.out.println("Java Runtime " + Runtime.version().toString());
		Runtime rt = Runtime.getRuntime();
		System.out.println("Available processors: " + rt.availableProcessors());
		double gB = 1074741824.0;
		System.out.println("Maximum available memory: " + (double) rt.maxMemory() / gB + " Gb.");
		boolean verboseStatistics = true;
		// contact rabbit main class
		String filename = args[0];
		MainClass mainClass = new MainClass(filename);

		/////////////////////////////// DIAGNOSTICS
		/////////////////////////////// ////////////////////////////////////////////////
		System.out.println("_/ _/ _/ Contact Maker Results / _/ _/");
		System.out.println("If every proximity of " + mainClass.sojournWidth + " days results in an exposure, then");
		System.out.println(mainClass.contact.getExposedMobileIDs().size() + " mobileIDs are exposed");
		System.out.println("_/ _/ _/ Tally of exposure statistics _/ _/ _/ ");
		LongSummaryStatistics exposureCountSummary = mainClass.contact.exposureCountSummary();
		System.out.println(
				"Average number of exposures, given at least one exposure: " + exposureCountSummary.getAverage());
		System.out
				.println("Maximum number of exposures, given at least one exposure: " + exposureCountSummary.getMax());
		System.out.println("Total number of exposures, given at least one exposure: " + exposureCountSummary.getSum());
		if (verboseStatistics) {
			for (Map.Entry<Long, Long> e : mainClass.contact.tallyExposureStatistics().entrySet()) {
				System.out.println(e.getValue() + " instances of " + e.getKey() + " exposures.");
			}
		}
		System.out.println("Testing constancy of probability for each place.");
		int counter = 0;
		for(Map.Entry<Integer,Double> e : mainClass.contact.getProbabilitiesForEachPlace().entrySet()) {
			counter++;
			if(counter % 1000 ==0 ) {
				System.out.println("Exposure at place " + e.getKey() + " leads to infection with probability " + e.getValue());
			}
		}
		
		
		
		System.out.println("Sources: " + mainClass.sourceMobileIDs.toString());
		System.out.println("Constant Probability Final Result: " + mainClass.sourceMobileIDs.size() + " sources led to "
				+ mainClass.contact.getInfectedMobileIDs().size() + " infected targets.");
		System.out.println("Targets: " + mainClass.contact.getInfectedMobileIDs().toString());
		System.out.println("Variable Probability Final Result: " + mainClass.sourceMobileIDs.size() + " sources led to "
				+ mainClass.contact.getVariableRateInfectedMobileIDs().size() + " infected targets.");
		System.out.println("Targets: " + mainClass.contact.getVariableRateInfectedMobileIDs().toString());

		/////////////////////////////////// CSV OUTPUT
		/////////////////////////////////// /////////////////////////////////////////////
		long identifier = System.currentTimeMillis() % 1000000;
		String prefix = FilenameUtils.removeExtension(filename); // removes ".csv"
		SortedSet<Integer> sourcesSorted = new TreeSet<>(mainClass.sourceMobileIDs);
		SetOfIntegersCSVWriter cw1 = new SetOfIntegersCSVWriter(sourcesSorted);
		cw1.writeElements(prefix + "-SOURCES-" + identifier);
		/*
		 * Either print the variable probability target list, or the constant
		 * probability target list
		 */
		SortedSet<Integer> targetsSorted = new TreeSet<>();
		if (mainClass.probabilityVariesByPlaceForTargets) {
			targetsSorted.addAll(mainClass.contact.getVariableRateInfectedMobileIDs());
		} else {
			targetsSorted.addAll(mainClass.contact.getInfectedMobileIDs());
		}
		SetOfIntegersCSVWriter cw2 = new SetOfIntegersCSVWriter(targetsSorted);
		cw2.writeElements(prefix + "-TARGETS-" + identifier);
		System.out.println("Sources and targets written to file.");
	}
}
