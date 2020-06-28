/**
 * 
 */
package utilities;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

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

	public waypointCSVReader(String filename) {
		this.csvFile = filename;
		this.readFile();
	}

	public void readFile() {
		try {
			Reader in = new FileReader(this.csvFile);
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
			for (CSVRecord record : records) {
				String lastName = record.get("Last Name");
				String firstName = record.get("First Name");
			}

		} catch (IOException ex) {
			System.out.println(ex.toString());
			System.out.println("Could not find input file.");
		}
	}

}
