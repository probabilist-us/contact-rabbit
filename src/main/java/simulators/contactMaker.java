/**
 * Purpose: given a "source" subset of the mobileID's, and the waypoints,
 * perform random trials in which space-time proximity to "sources"
 * creates a set of "target" mobileIDs
 */
package simulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import utilities.Sojourn;
import utilities.Waypoint;

/**
 * @author rwdarli
 *
 */
public class contactMaker {

	private Map<Integer, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs
	private Set<Integer> sourceMobileIDs, exposedMobileIDs;
	private Map<Integer, Long> exposureCounts; // key set is complement of sourceMobileIDs
	List<Waypoint> waypointList;
	SplittableRandom g;
	double timeWidth, transferProb;
	private Predicate<Waypoint> waypointComesFromSourceID;

	/**
	 * 
	 */
	public contactMaker(double width, double probability, Set<Integer> sources, List<Waypoint> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability;
		this.sourceMobileIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.waypointComesFromSourceID = (wp) -> this.sourceMobileIDs.contains(Integer.valueOf(wp.mobileID()));
		this.aggregateSojournsForEachPlace();
		this.countExposures();
	}

	/**
	 * Create a deterministic structure, based on the mobileSourceID's waypoints
	 */
	private void aggregateSojournsForEachPlace() {
		// Select the "hot" waypoints associated with sourceMobileIDs
		List<Waypoint> sourceWaypoints = this.waypointList.parallelStream().filter(this.waypointComesFromSourceID::test)
				.collect(Collectors.toList());
		// Initialize key set to be the union of the
		// placeIDs of "hot" waypoints, which we build first as a Set.
		Set<Integer> sourcePlaces = sourceWaypoints.parallelStream().mapToInt(wp -> wp.placeID()).distinct().boxed()
				.collect(Collectors.toUnmodifiableSet());
		this.sojournsForEachPlace = sourcePlaces.parallelStream()
				.collect(Collectors.toMap(place -> place, place -> new ArrayList<Sojourn>()));
		/*
		 * Associate each such source waypoint with a map entry from place to episode.
		 * Append this episode to the list of episdes for this place.
		 */
		for (Waypoint wp : sourceWaypoints) {
			List<Sojourn> currentEpisodes = this.sojournsForEachPlace.get(wp.placeID());
			currentEpisodes.add(new Sojourn(wp.timeStamp(), wp.timeStamp() + timeWidth));
			this.sojournsForEachPlace.put(Integer.valueOf(wp.placeID()), currentEpisodes);
		}
	}



	/**
	 * TODO Random trials, in which targets are exposed to sources' sojourns.
	 * Currently - deterministic
	 */
	private void countExposures() {
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
		 * visited a plac visited by a source
		 */
		this.exposedMobileIDs = susceptibleWaypoints.parallelStream().mapToInt(wp -> wp.mobileID()).distinct().boxed()
				.collect(Collectors.toSet());

		this.exposureCounts = this.exposedMobileIDs.stream()
				.collect(Collectors.toMap(id -> id, id -> Long.valueOf(0)));// key is a mobileID
		/*
		 * By construction, every waypoint in this list has a placeID visited by one or
		 * more sources
		 */
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
		/*
		 * No probability yet here -- every exposure causes infection
		 */
		this.exposedMobileIDs = this.exposureCounts.entrySet().stream().filter(e->(e.getValue() > 0))
				.map(e->e.getKey()).collect(Collectors.toSet());
	}

	/**
	 * @return the exposedMobileIDs
	 */
	public Set<Integer> getExposedMobileIDs() {
		return exposedMobileIDs;
	}

	/**
	 * @return the exposureCounts
	 */
	public Map<Integer, Long> getExposureCounts() {
		return exposureCounts;
	}

	/**
	 * @return the sojournsForEachPlace
	 */
	public Map<Integer, List<Sojourn>> getSojournsForEachPlace() {
		return sojournsForEachPlace;
	}
}
