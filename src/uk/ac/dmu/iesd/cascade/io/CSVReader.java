/**
 * 
 */
package uk.ac.dmu.iesd.cascade.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import uk.ac.dmu.iesd.cascade.base.Consts;

/**
 * Parses comma-separated variable (CSV) files and reads them into string
 * arrays.
 * 
 * This implementation is designed for reading data organised in columns with a
 * single header row and many data rows. Data files in other formats may be
 * read, however retrieval may be less efficient.
 * 
 * It can also return rows, however the implementation of this is currently
 * inefficient
 * 
 * Implementation detail: Column headers are stored as a String array, one
 * member per column. Data are stored in an ArrayList, with each member itself
 * being a String array (i.e. String[]) with one member per row.
 * 
 * @author jsnape
 * @version $Revision: 1.0 $ $Date: 2010/11/17 17:00:00 $
 * 
 */
public class CSVReader {

	private static final String DEFAULT_SEPERATOR = ",";
	private static final boolean DEFAULT_HAS_COL_HEADERS = true;
	BufferedReader myReader;
	String mySeperator;
	int numCols;
	int numRows;
	boolean hasColHeaders;
	String[] colHeaders;
	ArrayList<String>[] dataArray;
	WeakHashMap<String, String[]> contentsByColumn;
	String CSVFileName = null;
	int maxCols = 0;

	public void parseRaw() {
		boolean hasNextLine = true;
		String thisLine = null;
		ArrayList<ArrayList<String>> rawData = new ArrayList<ArrayList<String>>();

		while (hasNextLine) {
			try {
				thisLine = this.myReader.readLine();
			} catch (IOException e) {
				System.err
						.println("IO Exception occured whilst reading CSV line");
				e.printStackTrace();
			}
			
			System.err.println(thisLine);

			hasNextLine = (thisLine != null);
			if (hasNextLine) {
				if (this.numRows == 0 && this.hasColHeaders) {
					ArrayList<String> tempArray = new ArrayList<String>();
					StringTokenizer myTokenizer = new StringTokenizer(thisLine,
							this.mySeperator);
					this.numCols = myTokenizer.countTokens();

					this.colHeaders = new String[this.numCols];
					while (myTokenizer.hasMoreTokens()) {
						try {
							String thisHeader = myTokenizer.nextToken();
							thisHeader = thisHeader.replaceAll("^\"|\"$", "");
							tempArray.add(thisHeader);
						} catch (NoSuchElementException e) {
							System.err
									.println("No such element - is one of the column headers blank / null?");
							e.printStackTrace();
						}
					}

					tempArray.toArray(this.colHeaders);
				} else {
					ArrayList<String> thisRow = new ArrayList<String>();

					StringTokenizer s = new StringTokenizer(thisLine,
							this.mySeperator);
					if (s.countTokens() > this.maxCols) {
						this.maxCols = s.countTokens();
					}
					while (s.hasMoreTokens()) {
						thisRow.add(s.nextToken());
					}
					rawData.add(thisRow);
					this.numRows++;
				}
			}

			this.numCols = this.maxCols;
			this.colHeaders = new String[this.numCols];
			this.dataArray = new ArrayList[this.numCols];

			for (int i = 0; i < this.numCols; i++) {
				if (!this.hasColHeaders) {
					this.colHeaders[i] = "Column" + i;
				}

				ArrayList<String> col = new ArrayList<String>();
				for (int r = 0; r < this.numRows; r++) {
					ArrayList<String> rr = rawData.get(r);
					if (rr.size() > i) {
						col.add(rr.get(i));
					} else {
						col.add("");
					}
				}
				this.dataArray[i] = col;
			}

			this.contentsByColumn = new WeakHashMap<String, String[]>();
			for (int k = 0; k < this.numCols; k++) {
				this.contentsByColumn.put(this.colHeaders[k],
						this.dataArray[k].toArray(new String[this.numRows]));
			}
		}

		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).info(
				"Parsed file " + this.CSVFileName + ": " + this.numCols
						+ " columns and " + this.numRows + " rows.");
	}

	/*
	 * methods
	 */
	public void parseByColumn() {

		// TODO: This is much slower for a low number of long columns than many
		// columns with fewer data points.
		// TODO: Find out why and optimise

		boolean hasNextLine = true;
		String thisLine = null;
		try {
			thisLine = this.myReader.readLine();
		} catch (IOException e) {
			System.err
					.println("IO Exception occured whilst reading first CSV line");
			e.printStackTrace();
		}

		if (thisLine == null) {
			System.err.println("Supplied CSV is empty!!");
			hasNextLine = false;
		} else {
			ArrayList<String> tempArray = new ArrayList<String>();
			StringTokenizer myTokenizer = new StringTokenizer(thisLine,
					this.mySeperator);
			this.numCols = myTokenizer.countTokens();

			this.colHeaders = new String[this.numCols];
			while (myTokenizer.hasMoreTokens()) {
				try {
					String thisHeader = myTokenizer.nextToken();
					thisHeader = thisHeader.replaceAll("^\"|\"$", "");
					tempArray.add(thisHeader);
				} catch (NoSuchElementException e) {
					System.err
							.println("No such element - is one of the column headers blank / null?");
					e.printStackTrace();
				}
			}

			tempArray.toArray(this.colHeaders);
		}

		this.dataArray = new ArrayList[this.numCols];
		for (int t = 0; t < this.numCols; t++) {
			this.dataArray[t] = new ArrayList<String>();
		}

		while (hasNextLine) {
			try {
				thisLine = this.myReader.readLine();

			} catch (IOException e) {
				System.err
						.println("IO Exception occured whilst reading CSV line");
				e.printStackTrace();
			}

			hasNextLine = (thisLine != null);
			if (hasNextLine) {
				StringTokenizer myTokenizer = new StringTokenizer(thisLine,
						this.mySeperator);
				for (int i = 0; i < this.numCols; i++) {
					try {
						this.dataArray[i].add(myTokenizer.nextToken());
					} catch (NoSuchElementException e) {
						System.err
								.println("No such element - is one of the data rows blank / null - e.g. commas only?");
						e.printStackTrace();
					}
				}
				this.numRows++;
			}

			this.contentsByColumn = new WeakHashMap<String, String[]>();
			for (int k = 0; k < this.numCols; k++) {
				this.contentsByColumn.put(this.colHeaders[k],
						this.dataArray[k].toArray(new String[this.numRows]));
			}
		}

		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).info(
				"Parsed file " + this.CSVFileName + ": " + this.numCols
						+ " columns and " + this.numRows + " rows.");

	}

	public String[] getColumn(String colName) {
		String[] returnArray = null;
		if (this.contentsByColumn != null) {
			if (this.contentsByColumn.containsKey(colName)) {
				returnArray = this.contentsByColumn.get(colName);
			} else {
				System.err.println("File does not contain that column! ("
						+ colName + ") Available columns are :");
				System.err.println(this.contentsByColumn.keySet().toString());// Arrays.toString(colHeaders));
			}
		} else {
			System.err
					.println("CSVReader contentsByColumn is null!!  Check Parse output and file contents!");
			System.err.println("Number of columns is " + this.numCols
					+ " with names " + Arrays.toString(this.colHeaders));
		}

		return returnArray;
	}

	public String[] getRow(int rowIndex) {
		String[] returnArray = new String[this.dataArray.length];

		if (this.dataArray != null) {
			if (this.dataArray[0].size() > rowIndex) {
				for (int i = 0; i < this.dataArray.length; i++) {
					returnArray[i] = this.dataArray[i].get(rowIndex);
				}
			} else {
				System.err
						.println("File does not have that many rows!  Available rows are :");
				System.err.println(this.dataArray[0].size());
			}
		} else {
			System.err
					.println("CSVReader dataArray is null!!  Check Parse output and file contents!");
			System.err.println("Reader name is " + this.CSVFileName);
			System.err.println("Number of columns is " + this.numCols
					+ " with names " + Arrays.toString(this.colHeaders));
		}

		return returnArray;
	}

	/*
	 * Constructors
	 */
	public CSVReader(String myFilename) throws FileNotFoundException {
		this(myFilename, CSVReader.DEFAULT_SEPERATOR);
	}

	public CSVReader(File myFile) throws FileNotFoundException {
		this(myFile, CSVReader.DEFAULT_SEPERATOR);
	}

	public CSVReader(String myFilename, String seperator)
			throws FileNotFoundException {
		this(new FileReader(myFilename), seperator,
				CSVReader.DEFAULT_HAS_COL_HEADERS);
		this.CSVFileName = myFilename;
	}

	public CSVReader(File myFile, String seperator)
			throws FileNotFoundException {
		this(new FileReader(myFile), seperator,
				CSVReader.DEFAULT_HAS_COL_HEADERS);
		this.CSVFileName = myFile.getAbsolutePath();
	}

	public CSVReader(Reader myCSV, String seperator, boolean colHeaders) {
		this.myReader = new BufferedReader(myCSV);
		this.mySeperator = seperator;
		this.hasColHeaders = colHeaders;
	}

	public String[] getColumnNames() {
		return this.colHeaders;
	}

	/**
	 * @param string
	 * @return
	 */
	public int columnsStarting(String string) {
		int numCols = 0;
		for (String colHeader : this.colHeaders) {
			if (colHeader.startsWith(string)) {
				numCols++;
			}
		}
		return numCols;
	}

	/**
	 * @return
	 */
	public int getNumRows() {
		// TODO Auto-generated method stub
		return this.numRows;
	}

}
