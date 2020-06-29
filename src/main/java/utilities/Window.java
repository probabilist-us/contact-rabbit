/**
 * 
 */
package utilities;

/**
 * @author rwdarli A value associated with a Map from placeID x. Refers to a
 *         time window at placeID x during which transfer is possible.
 */
public record Window(double startTime, double endTime) {

	public boolean contains(double t) {
		return (t >= this.startTime) && (t < this.endTime);
	}
}