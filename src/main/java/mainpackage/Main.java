package mainpackage;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import simulators.contactMaker;
import utilities.waypointCSVReader;

public class Main {
	final double sojournWidth = 0.1; //days
	final double infectionProbability = 1.0;
	final double initialInfectionRate = 0.02; // determines # sources
	private waypointCSVReader wpReader;
	private contactMaker contact;
	private Set<Integer> sourceMobileIDs;
	Random g;
	public Main(String waypointFilename) {
		this.wpReader = new waypointCSVReader(waypointFilename);
		double numMobileIDs = (double)this.wpReader.lastMobileID();
		int sourceNumber =  (int)Math.round(numMobileIDs*this.initialInfectionRate);
		g = new Random();
		this.sourceMobileIDs = new HashSet<>();
		/*
		 * Randomly select the sources: TODO
		 */
		
		
	}

	public static void main(String[] args) {
		String filename = args[0];
		waypointCSVReader wayread = new waypointCSVReader(filename);//remove
		int numMobileIDs =1000;

	}
	


}
