package mainpackage;

/**
 * Assumes that the waypoint list is sorted by mobileID, which are consecutive
 * integers starting at 0.
 * Ran 7.2.2020
 */

import java.util.HashSet;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;

import simulators.contactMaker;
import utilities.SetOfIntegersCSVWriter;
import utilities.WaypointCSVReader;

public class MainClass {
	final double sojournWidth = 1.0 / 24.0; // unit = days
	final double infectionProbability = 0.1;
	final double initialInfectionRate = 0.02; // determines # sources
	private WaypointCSVReader wpReader;
	private contactMaker contact;
	private Set<Integer> sourceMobileIDs;
	Random g;

	public MainClass(String waypointFilename) {
		this.wpReader = new WaypointCSVReader(waypointFilename);
		int numMobileIDs = this.wpReader.lastMobileID();
		int sourceNumber = (int) Math.round((double) numMobileIDs * this.initialInfectionRate);
		g = new Random();
		this.sourceMobileIDs = new HashSet<>();
		/*
		 * Randomly select the sources from among all mobileIDs
		 */
		while (this.sourceMobileIDs.size() < sourceNumber) {
			this.sourceMobileIDs.add(Integer.valueOf(g.nextInt(numMobileIDs)));
		}
		System.out.println("A random subset of " + sourceNumber + " mobileIDs has been selected as sources.");
		this.contact = new contactMaker(this.sojournWidth, this.infectionProbability, this.sourceMobileIDs,
				this.wpReader.getWaypointList());
		System.out.println("Exposures and infections have been simulated.");
		System.out.println();
	}

	public static void main(String[] args) {
		// boilerplate
		System.out.println("Java Runtime " + Runtime.version().toString());
		Runtime rt = Runtime.getRuntime();
		System.out.println("Available processors: " + rt.availableProcessors());
		double gB = 1074741824.0;
		System.out.println("Maximum available memory: " + (double) rt.maxMemory() / gB + " Gb.");
		boolean verboseStatistics = false;
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
		System.out.println("Final Result: " + mainClass.sourceMobileIDs.size() + " sources led to "
				+ mainClass.contact.getInfectedMobileIDs().size() + " infected targets.");
		System.out.println("Sources: " + mainClass.sourceMobileIDs.toString());
		System.out.println("Targets: " + mainClass.contact.getInfectedMobileIDs().toString());

		/////////////////////////////////// CSV OUTPUT
		/////////////////////////////////// /////////////////////////////////////////////
		long identifier = System.currentTimeMillis() % 1000000;
		String prefix = FilenameUtils.removeExtension(filename); // removes ".csv"
		SortedSet<Integer> sourcesSorted = new TreeSet<>(mainClass.sourceMobileIDs);
		SetOfIntegersCSVWriter cw1 = new SetOfIntegersCSVWriter(sourcesSorted);
		cw1.writeElements(prefix + "-SOURCES-" + identifier);
		SortedSet<Integer> targetsSorted = new TreeSet<>(mainClass.contact.getInfectedMobileIDs());
		SetOfIntegersCSVWriter cw2 = new SetOfIntegersCSVWriter(targetsSorted);
		cw2.writeElements(prefix + "-TARGETS-" + identifier);
		System.out.println("Sources and targets written to file.");
	}
}
