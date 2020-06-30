/**
 * Purpose: given a "source" subset of the mobileID's, and the waypoints,
 * perform random trials in which space-time proximity to "sources"
 * creates a set of "target" mobileIDs
 */
package simulators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import utilities.Sojourn;
import utilities.Waypoint;

/**
 * @author rwdarli
 *
 */
public class contactMaker {

	private Map<Integer, List<Sojourn>> sojournsForEachPlace; // keys are placeIDs
	private Set<Integer> mobileSourceIDs, mobileTargetIDs;
	private Map<Integer, Integer> numberOfExposures; // key set is complement of mobileSourceIDs
	List<Waypoint> waypointList;
	SplittableRandom g;
	double timeWidth, transferProb;

	/**
	 * 
	 */
	public contactMaker(double width, double probability, Set<Integer> sources, List<Waypoint> waypoints) {
		this.timeWidth = width;
		this.transferProb = probability;
		this.mobileSourceIDs = sources;
		this.waypointList = Collections.unmodifiableList(waypoints);
		this.mobileTargetIDs = new HashSet<>();
		this.aggregateSojournsForEachPlace();

	}
	/**
	 * Create a deterministic structure, based on the mobileSourceID's waypoints
	 */
	private void aggregateSojournsForEachPlace() {
		// Select the "hot" waypoints associated with mobileSourceIDs
		List<Waypoint> sourceWaypoints = this.waypointList.parallelStream()
				.filter(wp -> this.mobileSourceIDs.contains(Integer.valueOf(wp.mobileID())))
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
	 * Random trials, in which targets are exposed to sources' sojourns
	 */
	private void transferToTargetMobileIDs() {
		/*
		 *  It suffices to restrict to waypoints where mobileID is NOT among sources,
		 *  and placeID is among the key set of this.sojournsForEachPlace
		 */
		
	}

}
