/**
 * This class creates target sets both for
 * (a) infection probability constant (= p) across all places, and
 * (b) infection probability Z_v at place v, where (Z_v) are independent 1/(1+Y) distributed, Y Exponential.
 * Here the mean of Z_v is equal to p.
 */

package simulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import utilities.GenericWaypoint;
import utilities.Sojourn;

/**
 * @since 2020
 * @author rwdarli
 * @param <M> type of the mobileID
 * @param <P> type of the placeID. The distinction between M and P types helps
 *            to clarify the code.
 */
public class PlaceDependentContactMaker<M, P> implements ContactMaker<M> {
	List<GenericWaypoint<M, P>> waypointList;
	private Map<P, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs visited by sources
	private Map<P, Double> probabilitiesForEachPlace; // keys are placeIDs visited by ALL mobileIDs
	/**
	 * Key is infectable mobileID, while value is list of places at which exposures
	 * occurred. Each item in this list is a single exposure.
	 */
	private Map<M, List<P>> exposurePlaceListByID;
	private Set<M> sourceMobileIDs, vulnerableMobileIDs, infectedMobileIDs, variableRateInfectedMobileIDs;
	double timeWidth, transferProb;
	private long seed; // for random simulation of probabilities at each place
	private Predicate<GenericWaypoint<M, P>> waypointComesFromSourceID;

	public PlaceDependentContactMaker(double width, double probability, long seed, Set<M> sources,
			List<GenericWaypoint<M, P>> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability; // must be > 0 and < 1
		this.seed = seed;
		this.sourceMobileIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.waypointComesFromSourceID = (wp) -> this.sourceMobileIDs.contains(wp.mobileID());
		/*
		 * Deterministic extraction from waypoints
		 */
		this.aggregateSojournsForEachPlace(); // purely deterministic
		this.setExposurePlaceList();
		/*
		 * Simulation of infection occurs here.
		 */
		this.simulateConstantRateInfections();
		this.probabilitiesForEachPlace = new HashMap<>();
		this.generateProbabilitiesForAllPlaces();
		this.simulateVariableRateInfections();
	}

	/**
	 * Create a deterministic structure, based on the mobileSourceID's waypoints
	 */
	private void aggregateSojournsForEachPlace() {

		/*
		 * Extract from the waypoint list the "hot" waypoints associated with
		 * sourceMobileIDs.
		 */
		List<GenericWaypoint<M, P>> sourceWaypoints = this.waypointList.parallelStream()
				.filter(this.waypointComesFromSourceID::test).collect(Collectors.toUnmodifiableList());
		System.out.println("Number of waypoints attributed to source mobileIDs: " + sourceWaypoints.size());
		/*
		 * Determine the set of places occurring in the list of "hot" waypoints
		 */
		Set<P> sourcePlaces = sourceWaypoints.parallelStream().map(wp -> wp.placeID()).distinct()
				.collect(Collectors.toUnmodifiableSet());
		System.out.println("Number of distinct placeIDs for source mobileIDs: " + sourcePlaces.size());
		this.sojournsForEachPlace = sourcePlaces.parallelStream()
				.collect(Collectors.toMap(place -> place, place -> new ArrayList<Sojourn>()));
		/*
		 * Whenever a source mobileID visits a hot place, an episode is created for this
		 * hot place. Append this episode to the list of episodes for this place.
		 */
		for (GenericWaypoint<M, P> wp : sourceWaypoints) {
			List<Sojourn> currentEpisodes = this.sojournsForEachPlace.get(wp.placeID());
			currentEpisodes.add(new Sojourn(wp.timeStamp(), wp.timeStamp() + timeWidth));
			this.sojournsForEachPlace.put(wp.placeID(), currentEpisodes);
		}
	}

	/**
	 * Deterministic listing of infectable mobileIDs' exposures, tagged by place
	 */
	private void setExposurePlaceList() {
		/*
		 * It suffices to restrict to waypoints where mobileID is NOT among sources, and
		 * placeID is among the key set of this.sojournsForEachPlace
		 */
		Predicate<GenericWaypoint<M, P>> isVulnerable = (wp) -> this.sojournsForEachPlace.keySet()
				.contains(wp.placeID()); // place selector
		// Logical AND
		Predicate<GenericWaypoint<M, P>> isSusceptible = isVulnerable.and((this.waypointComesFromSourceID).negate());
		List<GenericWaypoint<M, P>> susceptibleWaypoints = this.waypointList.parallelStream()
				.filter(isSusceptible::test).collect(Collectors.toList());
		/*
		 * Set the vulnerable mobile IDs as those which are NOT sources, and which
		 * sometime visited a place visited by a source.
		 */
		this.vulnerableMobileIDs = susceptibleWaypoints.parallelStream().map(wp -> wp.mobileID()).distinct()
				.collect(Collectors.toSet());
		System.out.println("Number of non-source mobileIDs which visit places also visited by sources: "
				+ this.vulnerableMobileIDs.size());
		/*
		 * Key of "exposurePlaceListByID" is mobileID. Loop will populate list of
		 * places. By construction, every waypoint in "susceptibleWaypoints" has a
		 * placeID visited by one or more sources. Every value is a NONEMPTY list.
		 */
		this.exposurePlaceListByID = new HashMap<>();
		int counter = 0;
		for (GenericWaypoint<M, P> wp : susceptibleWaypoints) {
			for (Sojourn soj : this.sojournsForEachPlace.get(wp.placeID())) {
				if (soj.contains(wp.timeStamp())) {
					if (!this.exposurePlaceListByID.keySet().contains(wp.mobileID())) {
						this.exposurePlaceListByID.put(wp.mobileID(), new ArrayList<P>());
					}
					// appends place to end of exposure list
					this.exposurePlaceListByID.get(wp.mobileID()).add(wp.placeID());
					counter++;
				}
			}
		}
		///////////////////// DIAGNOSTICS///////////////////////////////////////////////////////////
		System.out.println(counter + " exposures computed.");
		System.out.println("Number of exposed mobileIDs is " + this.exposurePlaceListByID.keySet().size());
		Iterator<M> mobileIt = this.exposurePlaceListByID.keySet().iterator();
		M mobileID = mobileIt.next();
		System.out.println(
				"MobileID " + mobileID + " has exposures at " + this.exposurePlaceListByID.get(mobileID).toString());
		/*
		 * Exposed means having k >=1 exposures (no probability mechanism)
		 */
	}

	/*
	 * Only depends on NUMBER of exposures, not on where they occurred.
	 */
	public void simulateConstantRateInfections() {
		Random g = new Random();
		/*
		 * Map<M,Integer> exposureCounts =
		 * this.exposurePlaceListByID.entrySet().stream()
		 * .collect(Collectors.toMap(e->e.getKey(),
		 * e->Integer.valueOf(e.getValue().size() ) ) ); IntPredicate
		 * infectedGivenExposures = (k) -> (g.nextDouble() > Math.pow(1.0 -
		 * this.transferProb, (double) k));
		 * 
		 * this.infectedMobileIDs = exposureCounts.entrySet().stream() .filter(e ->
		 * infectedGivenExposures.test(e.getValue().intValue())).map(e -> e.getKey())
		 * .collect(Collectors.toSet());
		 */
		/*
		 * k exposures leads to infection with probability 1 - (1-p)^k
		 */
		this.infectedMobileIDs = new HashSet<M>();
		double k;
		for (Map.Entry<M, List<P>> e : this.exposurePlaceListByID.entrySet()) {
			k = (double) e.getValue().size();
			if (g.nextDouble() > Math.pow(1.0 - this.transferProb, k)) {
				this.infectedMobileIDs.add(e.getKey());
			}
		}
		System.out.println(this.infectedMobileIDs.size() + " Infections computed");
	}

	/*
	 * Given Exponential(1) Z, take random infection probability 1 / (1 + b Z) where
	 * b is the scaleFactor, which we chose so the mean of 1 / (1 + b Z) is p. Based
	 * on look up table below.
	 */
	private void generateProbabilitiesForAllPlaces() {
		/*
		 * Since we may run ALTERNATIVE source lists against the SAME waypoint list, we
		 * must assign probabilities to ALL places, regardless of whether sources visit
		 * them.
		 */
		List<P> allPlaceList = this.waypointList.stream().map(wp -> wp.placeID()).distinct()
				.collect(Collectors.toList());
		Random rg = new Random(this.seed);
		/*
		 * Given Exponential(1) Z, take random infection probability 1 / (1 + b Z) where
		 * b is the scaleFactor, chosen so that mean is p. Tested 7.17.2020.
		 */
		double scaleFactor = this.multipliers[(int) Math.ceil(200.0 * this.transferProb) - 1];
		DoubleUnaryOperator pickP = (u) -> 1.0 / (1.0 - scaleFactor * Math.log(u));
		/*
		 * Determine random infection probability for each place
		 */
		for (P place : allPlaceList) {
			this.probabilitiesForEachPlace.put(place, pickP.applyAsDouble(rg.nextDouble()));
		}
	}

	/*
	 * Infection depends on WHERE exposure occurred.
	 */
	public void simulateVariableRateInfections() {
		/*
		 * Compute the non-infection probability Prod_i(1 - p_i) for each exposed
		 * mobileID, using the probabilities associated with the places where exposed.
		 * Map this mobileID to this non-infection probability.
		 */
		Map<M, Double> nonInfectionProbabilityMap = new HashMap<>();
		double product;
		for (Map.Entry<M, List<P>> e : this.exposurePlaceListByID.entrySet()) {
			if (e.getValue().size() > 0) {
				/*
				 * Multiply non-infection probabilities, over places where mobileID was exposed.
				 */
				product = e.getValue().stream().map(place -> 1.0 - this.probabilitiesForEachPlace.get(place))
						.reduce(1.0, (p, q) -> p * q);
				nonInfectionProbabilityMap.put(e.getKey(), product);
			}
		}
		/*
		 * Select losers in Bernoulli trials where success means remaining uninfected.
		 */
		Random g = new Random();
		this.variableRateInfectedMobileIDs = nonInfectionProbabilityMap.entrySet().stream()
				.filter(e -> (g.nextDouble() > e.getValue())).map(e -> e.getKey()).collect(Collectors.toSet());
	}

	/**
	 * @override Summary statistics for number of exposures, conditional on at least
	 *           one exposure event
	 */
	public LongSummaryStatistics exposureCountSummary() {
		return this.exposurePlaceListByID.values().stream().mapToLong(list -> (long) list.size()).filter(v -> (v > 0))
				.boxed().collect(Collectors.summarizingLong(v -> v));
	};

	/**
	 * @override
	 * @return pairs (k, N(k)), where N(k) is the number of mobileIDs with k
	 *         exposures
	 */
	public Map<Long, Long> tallyExposureStatistics() {
		return this.exposurePlaceListByID.values().stream()
				.collect(Collectors.groupingBy(list -> (long) list.size(), Collectors.counting()));
	};

	/**
	 * @override
	 * @return the vulnerableMobileIDs
	 */
	public Set<M> getExposedMobileIDs() {
		return this.exposurePlaceListByID.keySet();
	};

	/**
	 * @override
	 * @return the infectedMobileIDs
	 */
	public Set<M> getInfectedMobileIDs() {
		return infectedMobileIDs;
	};

	/**
	 * @return the probabilitiesForEachPlace
	 */
	public Map<P, Double> getProbabilitiesForEachPlace() {
		return probabilitiesForEachPlace;
	}

	/**
	 * @return the exposurePlaceListByID
	 */
	public Map<M, List<P>> getExposurePlaceListByID() {
		return exposurePlaceListByID;
	}

	/**
	 * @return the vulnerableMobileIDs
	 */
	public Set<M> getVulnerableMobileIDs() {
		return vulnerableMobileIDs;
	}

	/**
	 * @return the variableRateInfectedMobileIDs
	 */
	public Set<M> getVariableRateInfectedMobileIDs() {
		return variableRateInfectedMobileIDs;
	}

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
