/**
 * Writes a CSV file, such as
 * (1) list of source mobileIDs
 * (2) list of target mobileIDs, infected by the sources
 */
package utilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * @author rwdarli
 *
 */
public class SetOfIntegersCSVWriter {

	SortedSet<Integer> set;

	public SetOfIntegersCSVWriter(SortedSet<Integer> mySet) {
		this.set = mySet;
	}

	public void writeElements(String filename) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename + ".csv"));
				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);) {
			for (Integer k : this.set) {
					csvPrinter.printRecord(Integer.valueOf(k));				
			}
			csvPrinter.flush();
			writer.flush();
			writer.close();
		}
		catch (IOException ex) {
			System.out.println(ex.toString());
			System.out.println("Could not make output file.");
		}
	}

}
