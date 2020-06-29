/**
 * 
 */
package utilities;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * @author rwdarli
 *
 */
public final class waypointCSVReader {

	/**
	 * 
	 */
	final String csvFile;
	private List<Waypoint> waypointList;

	public waypointCSVReader(String filename) {
		this.csvFile = filename;
		this.waypointList = new ArrayList<>();
		this.readFile();
	}

	public void readFile() {
		try {
			Reader in = new FileReader(this.csvFile);
			Iterable<CSVRecord> triples = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
			for (CSVRecord triple : triples) {
				this.waypointList.add(new Waypoint(Integer.parseInt(triple.get(0)), Double.parseDouble(triple.get(1)),
						Integer.parseInt(triple.get(2))));
			}
			in.close();

		} catch (IOException ex) {
			System.out.println(ex.toString());
			System.out.println("Could not find input file.");
		}
		System.out.println("Number of waypoints read: " + this.waypointList.size());
		System.out.println("First waypoint: " + this.waypointList.get(0).toString());
	}

	/**
	 * @return unmodifiable version of the waypointList
	 */
	public List<Waypoint> getWaypointList() {
		return waypointList;
	}

}
