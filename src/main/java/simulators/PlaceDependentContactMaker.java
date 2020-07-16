package simulators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.random.ISAACRandom;
import org.apache.commons.math3.random.RandomGenerator;

import utilities.Sojourn;
import utilities.Waypoint;

public class PlaceDependentContactMaker implements ContactMaker<Integer> {
	private Map<Integer, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs visited by sources
	private Map<Integer, Double> probabilitiesForEachPlace; // keys are placeIDs visited by sources
	private Set<Integer> sourceMobileIDs, exposedMobileIDs, infectedMobileIDs;
	List<Waypoint> waypointList;
	SplittableRandom g;
	double timeWidth, transferProb;
	private long seed; // for random simulation of probabilities at each place
	private Predicate<Waypoint> waypointComesFromSourceID;

	public PlaceDependentContactMaker(double width, double probability, long seed, Set<Integer> sources,
			List<Waypoint> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability;
		this.seed = seed;
		this.sourceMobileIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.waypointComesFromSourceID = (wp) -> this.sourceMobileIDs.contains(Integer.valueOf(wp.mobileID()));
		this.aggregateSojournsForEachPlace(); // purely deterministic
		this.probabilitiesForEachPlace = new HashMap<>();
		this.generateProbabilitiesForSourceWaypoints();
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
		 * Set up a Beta Distribution sampler, mean and s.d. equal to p
		 */
		double alpha = 1.0 - 2.0 * this.transferProb;
		double beta = alpha * (1.0 - this.transferProb) / this.transferProb;
		RandomGenerator rg = new ISAACRandom(this.seed); // when seed is the same, sample will be same.
		BetaDistribution betaDist = new BetaDistribution(rg, alpha, beta);
		/*
		 * Determine random infection probability for each place
		 */
		for (Integer place : sourcePlaceList) {
			this.probabilitiesForEachPlace.put(place, betaDist.sample());
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

}
