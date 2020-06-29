package mainpackage;

import utilities.waypointCSVReader;

public class Main {

	public Main() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String filename = args[0];
		waypointCSVReader wayread = new waypointCSVReader(filename);

	}

}
