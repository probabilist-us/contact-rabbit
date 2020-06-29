/**
 * Purpose: given a "source" subset of the mobileID's, and the waypoints,
 * perform random trials in which space-time proximity to "sources"
 * creates a set of "target" mobileIDs
 */
package simulators;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

import utilities.Waypoint;
import utilities.Window;

/**
 * @author rwdarli
 *
 */
public class contactMaker {

	private Map<Integer, Set<Window>> transferIntervals;
	private Set<Integer> mobileSourceIDs, mobileTargetIDs;
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
		this.transferIntervals = new HashMap<>();

	}

	private void setTransferIntervals() {
		List<Waypoint> sourceWaypoints = this.waypointList.parallelStream().parallel()
				.filter(wp -> this.mobileSourceIDs.contains(Integer.valueOf(wp.mobileID())))
				.collect(Collectors.toList());
	}

}
