package filamentCalculation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PrintTracker {

	static String filamentFile = "filaments.csv";
	static String historyFile = "print_history.csv";
	static Scanner scanner = new Scanner(System.in);

	// filament inventory arrays - shared across the whole program
	static String[] colour;
	static double[] costPerGram;
	static double[] gramsRemaining;

	// print history arrays
	static String[] modelName;
	static String[] colourBreakdown; // e.g. "Red:50.0;Blue:30.0" for multicoloured prints
	static double[] gramsUsed; // total grams across all colours in that print
	static double[] printTimeHours;
	static double[] printTotalCost; // total cost across all colours in that print

	public static void main(String[] args) throws IOException {

		loadFilaments();
		loadHistory();

		boolean running = true;
		while (running) {
			System.out.println("\n=== 3D Print Tracker ===");
			System.out.println("1. View filament inventory");
			System.out.println("2. Add new filament colour");
			System.out.println("3. Log a new print");
			System.out.println("4. View print history and summary");
			System.out.println("5. Remove a filament");
			System.out.println("6. Exit");
			System.out.print("Choose an option: ");

			String choice = scanner.nextLine();

			switch (choice) {
			case "1":
				viewFilaments();
				break;
			case "2":
				addFilament();
				break;
			case "3":
				logPrint();
				break;
			case "4":
				printHistory(modelName, colourBreakdown, gramsUsed, printTimeHours, printTotalCost);
				printSummary(modelName, gramsUsed, printTimeHours, printTotalCost);
				break;
			case "5":
				removeFilament();
				break;
			case "6":
				running = false;
				break;
			default:
				System.out.println("Not a valid option, try again.");
			}
		}

		scanner.close();
	}

	// ---------- FILAMENT LOADING ----------

	public static void loadFilaments() throws IOException {
		int count = countValidFilaments(filamentFile);
		colour = new String[count];
		costPerGram = new double[count];
		gramsRemaining = new double[count];

		if (count > 0) {
			readFilaments(filamentFile, colour, costPerGram, gramsRemaining);
		}
	}

	public static int countValidFilaments(String fileName) throws IOException {
		int validCount = 0;
		BufferedReader bufferReader;

		try {
			bufferReader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			return 0; // no filament file yet
		}

		bufferReader.readLine(); // skip header
		String line = bufferReader.readLine();
		while (line != null) {
			if (isValidFilamentRow(line)) {
				validCount++;
			}
			line = bufferReader.readLine();
		}
		bufferReader.close();
		return validCount;
	}

	public static void readFilaments(String fileName, String[] colourArr, double[] costArr, double[] gramsArr)
			throws IOException {
		BufferedReader bufferReader = new BufferedReader(new FileReader(fileName));
		bufferReader.readLine(); // skip header

		int i = 0;
		String line = bufferReader.readLine();
		while (line != null) {
			if (isValidFilamentRow(line)) {
				String[] parts = line.split(",");
				colourArr[i] = parts[0];
				costArr[i] = Double.parseDouble(parts[1]);
				gramsArr[i] = Double.parseDouble(parts[2]);
				i++;
			}
			line = bufferReader.readLine();
		}
		bufferReader.close();
	}

	public static boolean isValidFilamentRow(String line) {
		if (line == null) {
			return false;
		}
		String[] parts = line.split(",");
		if (parts.length != 3) {
			return false;
		}
		if (parts[0].length() < 1) {
			return false;
		}
		try {
			Double.parseDouble(parts[1]);
			Double.parseDouble(parts[2]);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static void writeFilaments(String fileName, String[] colourArr, double[] costArr, double[] gramsArr)
			throws IOException {
		BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(fileName));
		bufferWriter.write("Colour,CostPerGram,GramsRemaining");
		bufferWriter.newLine();

		for (int i = 0; i < colourArr.length; i++) {
			bufferWriter.write(colourArr[i] + "," + costArr[i] + "," + gramsArr[i]);
			bufferWriter.newLine();
		}
		bufferWriter.close();
	}

	// ---------- FILAMENT ACTIONS ----------

	// looks for an existing filament with this colour name (not case-sensitive)
	// returns its index, or -1 if it isn't found
	public static int findFilamentByColour(String colourName) {
		for (int i = 0; i < colour.length; i++) {
			if (colour[i].equalsIgnoreCase(colourName)) {
				return i;
			}
		}
		return -1;
	}

	// capitalises the first letter and makes "pla" always show as "PLA",
	// but only for display - the actual stored name is never changed,
	// so matching by colour name elsewhere in the program still works normally
	public static String formatColourName(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		String formatted = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		formatted = formatted.replaceAll("(?i)pla", "PLA");
		return formatted;
	}

	public static void viewFilaments() {
		if (colour.length == 0) {
			System.out.println("No filaments logged yet.");
			return;
		}
		for (int i = 0; i < colour.length; i++) {
			System.out.printf("%d. %s - %.1fg left @ %.4f/g%n", i + 1, formatColourName(colour[i]), gramsRemaining[i],
					costPerGram[i]);
		}
	}

	public static void addFilament() throws IOException {
		System.out.print("Colour name (e.g. Red PLA): ");
		String newColourName = scanner.nextLine();

		int existingIndex = findFilamentByColour(newColourName);

		if (existingIndex != -1) {
			// already have this colour - add to the existing spool instead of duplicating
			// it
			double extraGrams = readDouble("Adding to existing stock. Grams to add: ");
			gramsRemaining[existingIndex] = gramsRemaining[existingIndex] + extraGrams;

			System.out.print("Has the price changed? (y/n): ");
			if (scanner.nextLine().equalsIgnoreCase("y")) {
				double costPerKg = readDouble("New cost per kg: ");
				costPerGram[existingIndex] = costPerKg / 1000;
			}

			writeFilaments(filamentFile, colour, costPerGram, gramsRemaining);
			System.out.println("Stock updated.");
			return;
		}

		// grow the arrays by one for a genuinely new colour
		String[] newColour = new String[colour.length + 1];
		double[] newCost = new double[colour.length + 1];
		double[] newGrams = new double[colour.length + 1];

		for (int i = 0; i < colour.length; i++) {
			newColour[i] = colour[i];
			newCost[i] = costPerGram[i];
			newGrams[i] = gramsRemaining[i];
		}

		newColour[colour.length] = newColourName;
		double costPerKg = readDouble("Cost per kg: ");
		newCost[colour.length] = costPerKg / 1000; // stored internally as cost per gram
		newGrams[colour.length] = readDouble("Grams in the spool: ");

		colour = newColour;
		costPerGram = newCost;
		gramsRemaining = newGrams;

		writeFilaments(filamentFile, colour, costPerGram, gramsRemaining);
		System.out.println("Filament added.");
	}

	public static void removeFilament() throws IOException {
		if (colour.length == 0) {
			System.out.println("No filaments to remove.");
			return;
		}

		viewFilaments();
		int choice = (int) readDouble("Choose a filament number to remove: ");
		int index = choice - 1;

		if (index < 0 || index >= colour.length) {
			System.out.println("Not a valid filament number.");
			return;
		}

		System.out.print("Remove " + colour[index] + "? (y/n): ");
		if (!scanner.nextLine().equalsIgnoreCase("y")) {
			return;
		}

		// shrink the arrays by one, skipping over the removed index
		String[] newColour = new String[colour.length - 1];
		double[] newCost = new double[colour.length - 1];
		double[] newGrams = new double[colour.length - 1];

		int j = 0;
		for (int i = 0; i < colour.length; i++) {
			if (i != index) {
				newColour[j] = colour[i];
				newCost[j] = costPerGram[i];
				newGrams[j] = gramsRemaining[i];
				j++;
			}
		}

		colour = newColour;
		costPerGram = newCost;
		gramsRemaining = newGrams;

		writeFilaments(filamentFile, colour, costPerGram, gramsRemaining);
		System.out.println("Filament removed.");
	}

	// ---------- PRINT HISTORY LOADING ----------

	public static void loadHistory() throws IOException {
		int count = countValidPrints(historyFile);
		modelName = new String[count];
		colourBreakdown = new String[count];
		gramsUsed = new double[count];
		printTimeHours = new double[count];
		printTotalCost = new double[count];

		if (count > 0) {
			readPrints(historyFile, modelName, colourBreakdown, gramsUsed, printTimeHours, printTotalCost);
		}
	}

	public static int countValidPrints(String fileName) throws IOException {
		int validCount = 0;
		BufferedReader bufferReader;

		try {
			bufferReader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			return 0; // no history file yet
		}

		bufferReader.readLine(); // skip header
		String line = bufferReader.readLine();
		while (line != null) {
			if (isValidPrintRow(line)) {
				validCount++;
			}
			line = bufferReader.readLine();
		}
		bufferReader.close();
		return validCount;
	}

	public static void readPrints(String fileName, String[] modelArr, String[] breakdownArr, double[] gramsArr,
			double[] hoursArr, double[] costArr) throws IOException {
		BufferedReader bufferReader = new BufferedReader(new FileReader(fileName));
		bufferReader.readLine(); // skip header

		int i = 0;
		String line = bufferReader.readLine();
		while (line != null) {
			if (isValidPrintRow(line)) {
				String[] parts = line.split(",");
				modelArr[i] = parts[0];
				breakdownArr[i] = parts[1];
				gramsArr[i] = Double.parseDouble(parts[2]);
				hoursArr[i] = Double.parseDouble(parts[3]);
				costArr[i] = Double.parseDouble(parts[4]);
				i++;
			}
			line = bufferReader.readLine();
		}
		bufferReader.close();
	}

	public static boolean isValidPrintRow(String line) {
		if (line == null) {
			return false;
		}
		String[] parts = line.split(",");
		if (parts.length != 5) {
			return false;
		}
		if (parts[0].length() < 1 || parts[1].length() < 1) {
			return false;
		}
		for (int i = 2; i < 5; i++) {
			try {
				Double.parseDouble(parts[i]);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	public static void writeHistory(String fileName, String[] modelArr, String[] breakdownArr, double[] gramsArr,
			double[] hoursArr, double[] costArr) throws IOException {
		BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(fileName));
		bufferWriter.write("ModelName,ColourBreakdown,TotalGramsUsed,PrintTimeHours,TotalCost");
		bufferWriter.newLine();

		for (int i = 0; i < modelArr.length; i++) {
			bufferWriter.write(
					modelArr[i] + "," + breakdownArr[i] + "," + gramsArr[i] + "," + hoursArr[i] + "," + costArr[i]);
			bufferWriter.newLine();
		}
		bufferWriter.close();
	}

	// ---------- LOGGING A PRINT ----------

	public static void logPrint() throws IOException {
		if (colour.length == 0) {
			System.out.println("You need to add a filament first (option 2).");
			return;
		}

		System.out.print("Model name: ");
		String newModel = scanner.nextLine();

		System.out.print("Is this print multicoloured? (y/n): ");
		boolean multicoloured = scanner.nextLine().equalsIgnoreCase("y");

		int numColours = 1;
		if (multicoloured) {
			while (true) {
				numColours = (int) readDouble("How many colours (2-4): ");
				if (numColours >= 2 && numColours <= 4) {
					break;
				}
				System.out.println("Please enter a number between 2 and 4.");
			}
		}

		// collect the filament choice and grams used for each colour first,
		// before touching any stock, in case the user cancels partway through
		int[] selectedIndex = new int[numColours];
		double[] selectedGrams = new double[numColours];

		for (int c = 0; c < numColours; c++) {
			System.out.println("\nColour " + (c + 1) + " of " + numColours + ":");
			viewFilaments();
			int choice = (int) readDouble("Choose a filament number: ");
			int index = choice - 1;

			if (index < 0 || index >= colour.length) {
				System.out.println("Not a valid filament number, cancelling this print.");
				return;
			}

			double grams = readDouble("Grams of " + formatColourName(colour[index]) + " used: ");

			if (grams > gramsRemaining[index]) {
				System.out.printf("Warning: only %.1fg left on this spool, but you're using %.1fg.%n",
						gramsRemaining[index], grams);
				System.out.print("Log it anyway? (y/n): ");
				if (!scanner.nextLine().equalsIgnoreCase("y")) {
					return;
				}
			}

			selectedIndex[c] = index;
			selectedGrams[c] = grams;
		}

		double newHours = readDouble("Print time in hours: ");

		// once colour selected, remove from existing stock
		for (int c = 0; c < numColours; c++) {
			gramsRemaining[selectedIndex[c]] = gramsRemaining[selectedIndex[c]] - selectedGrams[c];
		}
		writeFilaments(filamentFile, colour, costPerGram, gramsRemaining);

		// build the colour breakdown string, e.g. "Red:50.0;Blue:30.0"
		String breakdown = "";
		double totalGrams = 0;
		double totalCost = 0;
		for (int c = 0; c < numColours; c++) {
			if (c > 0) {
				breakdown = breakdown + ";";
			}
			breakdown = breakdown + colour[selectedIndex[c]] + ":" + selectedGrams[c];
			totalGrams = totalGrams + selectedGrams[c];
			totalCost = totalCost + (selectedGrams[c] * costPerGram[selectedIndex[c]]);
		}

		// grow the history arrays by one
		String[] newModelArr = new String[modelName.length + 1];
		String[] newBreakdownArr = new String[modelName.length + 1];
		double[] newGramsArr = new double[modelName.length + 1];
		double[] newHoursArr = new double[modelName.length + 1];
		double[] newCostArr = new double[modelName.length + 1];

		for (int i = 0; i < modelName.length; i++) {
			newModelArr[i] = modelName[i];
			newBreakdownArr[i] = colourBreakdown[i];
			newGramsArr[i] = gramsUsed[i];
			newHoursArr[i] = printTimeHours[i];
			newCostArr[i] = printTotalCost[i];
		}

		newModelArr[modelName.length] = newModel;
		newBreakdownArr[modelName.length] = breakdown;
		newGramsArr[modelName.length] = totalGrams;
		newHoursArr[modelName.length] = newHours;
		newCostArr[modelName.length] = totalCost;

		modelName = newModelArr;
		colourBreakdown = newBreakdownArr;
		gramsUsed = newGramsArr;
		printTimeHours = newHoursArr;
		printTotalCost = newCostArr;

		writeHistory(historyFile, modelName, colourBreakdown, gramsUsed, printTimeHours, printTotalCost);

		System.out.printf("Logged. Total cost: %.2f%n", totalCost);
	}

	// ---------- REPORTING ----------

	// takes a stored breakdown like "red pla:50.0;blue pla:30.0" and formats
	// just the colour names inside it for display, without changing what's
	// actually stored in the file
	public static String formatBreakdownForDisplay(String breakdown) {
		String[] pairs = breakdown.split(";");
		String result = "";
		for (int i = 0; i < pairs.length; i++) {
			String[] parts = pairs[i].split(":");
			if (i > 0) {
				result = result + ";";
			}
			result = result + formatColourName(parts[0]) + ":" + parts[1];
		}
		return result;
	}

	public static void printHistory(String[] modelArr, String[] breakdownArr, double[] gramsArr, double[] hoursArr,
			double[] costArr) {
		if (modelArr.length == 0) {
			System.out.println("No prints logged yet.");
			return;
		}
		for (int i = 0; i < modelArr.length; i++) {
			System.out.printf("%s (%s) - %.1fg, %.1fh - cost: %.2f%n", modelArr[i],
					formatBreakdownForDisplay(breakdownArr[i]), gramsArr[i], hoursArr[i], costArr[i]);
		}
	}

	public static void printSummary(String[] modelArr, double[] gramsArr, double[] hoursArr, double[] costArr) {
		if (modelArr.length == 0) {
			return;
		}

		double totalGrams = 0;
		double totalHours = 0;
		double totalCost = 0;

		for (int i = 0; i < modelArr.length; i++) {
			totalGrams += gramsArr[i];
			totalHours += hoursArr[i];
			totalCost += costArr[i];
		}

		double averageCost = totalCost / modelArr.length;

		System.out.println("\n=== Summary ===");
		System.out.printf("Total prints: %d%n", modelArr.length);
		System.out.printf("Total filament used: %.1fg%n", totalGrams);
		System.out.printf("Total print time: %.1f hours%n", totalHours);
		System.out.printf("Total cost: %.2f%n", totalCost);
		System.out.printf("Average cost per print: %.2f%n", averageCost);
	}

	// ---------- HELPER ----------

	public static double readDouble(String prompt) {
		while (true) {
			System.out.print(prompt);
			String input = scanner.nextLine();
			try {
				return Double.parseDouble(input);
			} catch (NumberFormatException e) {
				System.out.println("Please enter a valid number.");
			}
		}
	}
}