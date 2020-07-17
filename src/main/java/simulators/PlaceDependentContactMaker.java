/**
 * This class creates target sets both for
 * (a) infection probability constant (= p) across all places, and
 * (b) infection probability Z_v at place v, where (Z_v) are independent Beta distributed.
 * Here the mean and s.d. of Z_v is equal to p. THe pdf is typically strictly decreasing on (0,1).
 */

package simulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import utilities.Sojourn;
import utilities.Waypoint;

public class PlaceDependentContactMaker implements ContactMaker<Integer> {
	private Map<Integer, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs visited by sources
	private Map<Integer, Double> probabilitiesForEachPlace; // keys are placeIDs visited by sources
	private Set<Integer> sourceMobileIDs, exposedMobileIDs, infectedMobileIDs;
	List<Waypoint> waypointList;
	SplittableRandom g;
	double timeWidth, transferProb, scaleFactor;
	private long seed; // for random simulation of probabilities at each place
	private Predicate<Waypoint> waypointComesFromSourceID;

	public PlaceDependentContactMaker(double width, double probability, long seed, Set<Integer> sources,
			List<Waypoint> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability; // must be > 0 and < 1
		/*
		 * Given Exponential(1) Z, take random infection probability 1 / (1 + b Z) where
		 * b is the scaleFactor.
		 */
		this.scaleFactor = this.multipliers[(int) Math.ceil(200.0 * this.transferProb) - 1];
		this.seed = seed;
		this.sourceMobileIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.waypointComesFromSourceID = (wp) -> this.sourceMobileIDs.contains(Integer.valueOf(wp.mobileID()));
		this.aggregateSojournsForEachPlace(); // purely deterministic
		this.probabilitiesForEachPlace = new HashMap<>();
		if (this.transferProb < 0.5) {
			this.generateProbabilitiesForSourceWaypoints();
		}
	}

	/**
	 * Create a deterministic structure, based on the mobileSourceID's waypoints
	 */
	private void aggregateSojournsForEachPlace() {

		/*
		 * Extract from the waypoint list the "hot" waypoints associated with
		 * sourceMobileIDs.
		 */
		List<Waypoint> sourceWaypoints = this.waypointList.parallelStream().filter(this.waypointComesFromSourceID::test)
				.collect(Collectors.toUnmodifiableList());
		System.out.println("Number of waypoints attributed to source mobileIDs: " + sourceWaypoints.size());
		/*
		 * Determine the set of places occurring in the list of "hot" waypoints
		 */
		Set<Integer> sourcePlaces = sourceWaypoints.parallelStream().mapToInt(wp -> wp.placeID()).distinct().boxed()
				.collect(Collectors.toUnmodifiableSet());
		System.out.println("Number of distinct placeIDs for source mobileIDs: " + sourcePlaces.size());
		this.sojournsForEachPlace = sourcePlaces.parallelStream()
				.collect(Collectors.toMap(place -> place, place -> new ArrayList<Sojourn>()));
		/*
		 * Whenever a source mobileID visits a hot place, an episode is created for this
		 * hot place. Append this episode to the list of episodes for this place.
		 */
		for (Waypoint wp : sourceWaypoints) {
			List<Sojourn> currentEpisodes = this.sojournsForEachPlace.get(wp.placeID());
			currentEpisodes.add(new Sojourn(wp.timeStamp(), wp.timeStamp() + timeWidth));
			this.sojournsForEachPlace.put(Integer.valueOf(wp.placeID()), currentEpisodes);
		}
	}

	/*
	 * Parameters of beta distribution are chosen to have mean and standard
	 * deviation both equal to p. Formulas are valid only when p < 0.5
	 */
	private void generateProbabilitiesForSourceWaypoints() {
		/*
		 * To avoid any doubt about the ordering of source place list, sort it first.
		 */
		List<Integer> sourcePlaceList = new ArrayList<>(this.sojournsForEachPlace.keySet());
		Collections.sort(sourcePlaceList, (x, y) -> Integer.compare(x, y));
		/*
		 * Set up a generator for 1 / (1 + b Z), where
		 * Z ~ Exponential(1) Z, b is the scale factor so that mean is p.
		 * Based on look up table below.
		 */
		Random rg = new Random(this.seed);		
		DoubleUnaryOperator pickP = (u)->1.0/(1.0 - this.scaleFactor*Math.log(u));
		/*
		 * Determine random infection probability for each place
		 */
		for (Integer place : sourcePlaceList) {
			this.probabilitiesForEachPlace.put(place, pickP.applyAsDouble(rg.nextDouble()));
		}

	}

	/**
	 * @override Summary statistics for number of exposures, conditional on at least
	 *           one exposure event
	 */
	public LongSummaryStatistics exposureCountSummary() {

	};

	/**
	 * @override
	 * @return pairs (k, N(k)), where N(k) is the number of mobileIDs with k
	 *         exposures
	 */
	public Map<Long, Long> tallyExposureStatistics() {

	};

	/**
	 * @override
	 * @return the exposedMobileIDs
	 */
	public Set<M> getExposedMobileIDs() {

	};

	/**
	 * @override
	 * @return the infectedMobileIDs
	 */
	public Set<M> getInfectedMobileIDs() {

	};

	/**
	 * multipliers[j-1] is the value b such that the mean of 1/(1 - b log(U)) has
	 * mean p = j/200. Computed in Mathematica using a root of incomplete Gamma
	 * Function. Given p>0, choose b as multipliers[j-1], where j =
	 * Math.ceil(200*p).
	 */
	final double[] multipliers = new double[] { 1323.28, 579.701, 353.99, 248.013, 187.466, 148.714, 121.994, 102.572,
			87.8874, 76.4401, 67.2961, 59.8446, 53.6707, 48.4828, 44.0706, 40.2787, 36.9899, 34.1144, 31.5821, 29.3377,
			27.3369, 25.5439, 23.9294, 22.4693, 21.1436, 19.9356, 18.8309, 17.8176, 16.8854, 16.0255, 15.2302, 14.493,
			13.8081, 13.1705, 12.5756, 12.0197, 11.4992, 11.0111, 10.5527, 10.1215, 9.71524, 9.33207, 8.97017, 8.62795,
			8.30396, 7.99688, 7.70551, 7.42878, 7.16569, 6.91533, 6.67686, 6.44954, 6.23265, 6.02555, 5.82765, 5.6384,
			5.45728, 5.28384, 5.11762, 4.95824, 4.80531, 4.65849, 4.51744, 4.38187, 4.25148, 4.12602, 4.00524, 3.88891,
			3.7768, 3.66871, 3.56446, 3.46386, 3.36673, 3.27294, 3.18231, 3.09471, 3.01001, 2.92808, 2.8488, 2.77205,
			2.69774, 2.62576, 2.55601, 2.4884, 2.42285, 2.35928, 2.2976, 2.23775, 2.17965, 2.12323, 2.06844, 2.01521,
			1.96348, 1.9132, 1.86432, 1.81679, 1.77055, 1.72557, 1.6818, 1.63919, 1.59771, 1.55732, 1.51798, 1.47966,
			1.44232, 1.40594, 1.37047, 1.3359, 1.30219, 1.26931, 1.23725, 1.20596, 1.17544, 1.14565, 1.11658, 1.0882,
			1.06049, 1.03344, 1.00701, 0.981206, 0.955994, 0.931362, 0.907292, 0.88377, 0.860779, 0.838305, 0.816333,
			0.79485, 0.773842, 0.753297, 0.733202, 0.713545, 0.694314, 0.675499, 0.657087, 0.63907, 0.621436, 0.604176,
			0.58728, 0.57074, 0.554545, 0.538688, 0.52316, 0.507954, 0.49306, 0.478472, 0.464183, 0.450184, 0.43647,
			0.423033, 0.409866, 0.396965, 0.384322, 0.371931, 0.359787, 0.347884, 0.336217, 0.32478, 0.313568, 0.302576,
			0.2918, 0.281234, 0.270874, 0.260715, 0.250754, 0.240986, 0.231407, 0.222012, 0.212799, 0.203763, 0.1949,
			0.186207, 0.177681, 0.169318, 0.161115, 0.153069, 0.145176, 0.137434, 0.12984, 0.12239, 0.115082, 0.107913,
			0.100881, 0.0939831, 0.0872166, 0.0805792, 0.0740686, 0.0676823, 0.0614183, 0.0552743, 0.0492482, 0.043338,
			0.0375416, 0.0318572, 0.0262828, 0.0208166, 0.0154569, 0.010202, 0.00505025, 0.0 };

}
