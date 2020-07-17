/**
 * Purpose: given a "source" subset of the mobileID's, and the waypoints,
 * perform random trials in which space-time proximity to "sources"
 * infects a set of "target" mobileIDs. 
 * 
 * When a mobileID has had k exposures
 * to a source, it is infected with probability 1 - (1-p)^k, where p is supplied in the constructor.
 * 
 * An exposure means that a mobileID waypoint has time stamp s with
 * t <= s < t + w
 * for some time stamp t of a source at the SAME placeID.
 * The width w is supplied in the constructor.
 * 
 * Variation: TODO
 * make probability p vary with the placeID, according to a Beta distribution
 */
package simulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import utilities.Sojourn;
import utilities.Waypoint;

/**
 * @author rwdarli
 *
 */
public class CountBasedContactMaker implements ContactMaker<Integer> {

	private Map<Integer, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs
	private Set<Integer> sourceMobileIDs, exposedMobileIDs, infectedMobileIDs;
	/*
	 * ExposureCounts.get(j) counts the exposures of mobileID j. Could be 0. Keys
	 * are mobileIDs in complement of sourceMobileIDs which visit at least one place
	 * visited by sources.
	 */
	private Map<Integer, Long> exposureCounts;
	List<Waypoint> waypointList;
	SplittableRandom g;
	double timeWidth, transferProb;
	private Predicate<Waypoint> waypointComesFromSourceID;
	private Predicate<Double> infectedGivenExposures;

	/**
	 * 
	 */
	public CountBasedContactMaker(double width, double probability, Set<Integer> sources, List<Waypoint> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability;
		this.sourceMobileIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.waypointComesFromSourceID = (wp) -> this.sourceMobileIDs.contains(Integer.valueOf(wp.mobileID()));
		g = new SplittableRandom();
		this.infectedGivenExposures = (k) -> (g.nextDouble() > Math.pow(1.0 - this.transferProb, k));
		this.aggregateSojournsForEachPlace();
		this.countExposuresSimulateInfections();
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

	/**
	 * Random trials, in which targets are exposed to sources' sojourns.
	 */
	private void countExposuresSimulateInfections() {
		/*
		 * It suffices to restrict to waypoints where mobileID is NOT among sources, and
		 * placeID is among the key set of this.sojournsForEachPlace
		 */
		Predicate<Waypoint> isVulnerable = (wp) -> this.sojournsForEachPlace.keySet()
				.contains(Integer.valueOf(wp.placeID())); // place selector
		// Logical AND
		Predicate<Waypoint> isSusceptible = isVulnerable.and((this.waypointComesFromSourceID).negate());
		List<Waypoint> susceptibleWaypoints = this.waypointList.parallelStream().filter(isSusceptible::test)
				.collect(Collectors.toList());
		/*
		 * Set the exposed mobile IDs as those which are NOT sources, and which sometime
		 * visited a place visited by a source.
		 */
		this.exposedMobileIDs = susceptibleWaypoints.parallelStream().mapToInt(wp -> wp.mobileID()).distinct().boxed()
				.collect(Collectors.toSet());
		System.out.println("Number of non-source mobileIDs which visit places also visited by sources: "
				+ this.exposedMobileIDs.size());
		/*
		 * Key of "exposureCounts" is mobileID. For loop will set values. By
		 * construction, every waypoint in "susceptibleWaypoints" has a placeID visited
		 * by one or more sources
		 */
		this.exposureCounts = this.exposedMobileIDs.stream().collect(Collectors.toMap(id -> id, id -> Long.valueOf(0)));
		long sum, value;
		Integer place, mobileID;
		for (Waypoint wp : susceptibleWaypoints) {
			place = Integer.valueOf(wp.placeID());
			sum = 0;
			for (Sojourn soj : this.sojournsForEachPlace.get(place)) {
				if (soj.contains(wp.timeStamp())) {
					sum++;
				}
			}
			if (sum > 0) {
				mobileID = Integer.valueOf(wp.mobileID());
				value = exposureCounts.get(mobileID);
				exposureCounts.put(mobileID, value + sum);
			}
		}
		System.out.println("Exposures computed");
		/*
		 * Exposed means having k >=1 exposures (no probability mechanism)
		 */
		this.exposedMobileIDs = this.exposureCounts.entrySet().stream().filter(e -> (e.getValue() > 0))
				.map(e -> e.getKey()).collect(Collectors.toSet());
		/*
		 * K exposures leads to infection with probability 1 - (1-p)^k
		 */
		this.infectedMobileIDs = this.exposureCounts.entrySet().stream()
				.filter(e -> this.infectedGivenExposures.test((double) e.getValue())).map(e -> e.getKey())
				.collect(Collectors.toSet());
		System.out.println("Infections computed");

	}

	/**
	 * @return the exposureCounts
	 */
	public Map<Integer, Long> getExposureCounts() {
		return exposureCounts;
	}

	/**
	 * @override
	 * @return pairs (k, N(k)), where N(k) is the number of mobileIDs with k
	 *         exposures
	 */
	public Map<Long, Long> tallyExposureStatistics() {
		return this.exposureCounts.values().stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	/**
	 * @override Summary statistics for number of exposures, conditional on at least
	 *           one exposure event
	 */
	public LongSummaryStatistics exposureCountSummary() {
		return this.exposureCounts.values().stream().filter(v -> (v > 0)).collect(Collectors.summarizingLong(v -> v));
	}

	/**
	 * @return the sojournsForEachPlace
	 */
	public Map<Integer, List<Sojourn>> getSojournsForEachPlace() {
		return sojournsForEachPlace;
	}

	/**
	 * @override
	 * @return the exposedMobileIDs
	 */
	public Set<Integer> getExposedMobileIDs() {
		return exposedMobileIDs;
	}

	/**
	 * @override
	 * @return the infectedMobileIDs
	 */
	public Set<Integer> getInfectedMobileIDs() {
		return infectedMobileIDs;
	}
}
