package mainpackage;

/**
 * Assumes that the waypoint list is sorted by mobileID, which are consecutive
 * integers starting at 0.
 * Ran 7.2.2020
 */

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import simulators.contactMaker;
import utilities.waypointCSVReader;

public class Main {
	final double sojournWidth = 0.1; // days
	final double infectionProbability = 1.0;
	final double initialInfectionRate = 0.02; // determines # sources
	private waypointCSVReader wpReader;
	private contactMaker contact;
	private Set<Integer> sourceMobileIDs;
	Random g;

	public Main(String waypointFilename) {
		this.wpReader = new waypointCSVReader(waypointFilename);
		System.out.println(this.wpReader.getWaypointList().size() + " waypoints have been read");
		double numMobileIDs = (double) this.wpReader.lastMobileID();
		int sourceNumber = (int) Math.round(numMobileIDs * this.initialInfectionRate);
		g = new Random();
		this.sourceMobileIDs = new HashSet<>();
		/*
		 * Randomly select the sources: TODO
		 */
		while (this.sourceMobileIDs.size() < sourceNumber) {
			this.sourceMobileIDs.add(Integer.valueOf(g.nextInt(sourceNumber)));
		}
		System.out.println("A random subset of " + sourceNumber + " mobileIDs has been selected as sources.");
		this.contact = new contactMaker(this.sojournWidth, this.infectionProbability, this.sourceMobileIDs,
				this.wpReader.getWaypointList());
		System.out.println("Exposures have been simulated.");
		System.out.println();
	}

	public static void main(String[] args) {
		// boilerplate
		System.out.println("Java Runtime " + Runtime.version().toString());
		Runtime rt = Runtime.getRuntime();
		System.out.println("Available processors: " + rt.availableProcessors());
		double gB = 1074741824.0;
		System.out.println("Maximum available memory: " + (double) rt.maxMemory() / gB + " Gb.");
		// contact rabbit main class
		String filename = args[0];
		Main main = new Main(filename);
		System.out.println("_/ _/ _/ Contact Maker Results / _/ _/");
		System.out.println("If every proximity of " + main.sojournWidth + " days results in an infection, then");
		System.out.println(main.contact.getExposedMobileIDs().size() + " mobileIDs are infected");
		

	}

}
