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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
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
	ArrayList<ArrayList<String>> rawData;
	WeakHashMap<String, List<String>> contentsByColumn;
	String CSVFileName = null;
	int maxCols = 0;

	
	/*
	 * Convenience method to parse the file and populate the columns map as well as reading in rows.
	 * 
	 * Throws: IOException if there is a problem with reading the input file.
	 */
	public void parseByColumn() {

		// TODO: This is much slower for a low number of long columns than many
		// columns with fewer data points.
		// TODO: Find out why and optimise

		boolean hasNextLine = true;
		Map<String, String> thisLine = null;
		
		CsvMapReader in = new CsvMapReader(this.myReader, CsvPreference.STANDARD_PREFERENCE);
		this.contentsByColumn = new WeakHashMap<String, List<String>>();
		this.rawData = new ArrayList<ArrayList<String>>();
		
		try
		{
			if (this.hasColHeaders)
			{
				this.colHeaders = in.getHeader(true);
				int namelessColumns = 0;
				for (int j = 0; j<this.colHeaders.length;j++)
				{
					if (this.colHeaders[j] == null)
					{
						this.colHeaders[j] = "Column_" + namelessColumns++;
					}
				}
				
				for (String c : this.colHeaders)
				{
					//Initialise an empty column per header
					contentsByColumn.put(c,new ArrayList<String>());
				}
			}
				
			while ((thisLine = in.read(this.colHeaders)) != null)
			{
				this.numCols = thisLine.size();
				ArrayList<String> orderedRow = new ArrayList<String>();
				for (String c : this.colHeaders)
				{
					String dataField = thisLine.get(c);
					List<String> tmp = contentsByColumn.get(c);
					tmp.add(dataField);
					orderedRow.add(dataField);
					contentsByColumn.put(c,tmp);
				}
				
				this.rawData.add(orderedRow);
			}
			
			System.err.println(contentsByColumn.get("Column_0"));
		}
		catch (IOException e)
		{
			Logger.getLogger(Consts.CASCADE_LOGGER_NAME).error("Error occurred when reading input file " + this.CSVFileName + ", data is likely missing!!!");
		}
		
		if (in.getRowNumber() < 1)
		{
			Logger.getLogger(Consts.CASCADE_LOGGER_NAME).error("Supplied CSV is empty!!");
			hasNextLine = false;
		} 
		
		this.numRows = in.getRowNumber();
		if (this.hasColHeaders)
		{
			this.numRows--;
		}

		Logger.getLogger(Consts.CASCADE_LOGGER_NAME).info(
				"Parsed file " + this.CSVFileName + ": " + this.numCols
						+ " columns and " + this.numRows + " rows.");

	}

	public String[] getColumn(String colName) {
		String[] returnArray = new String[0];
		if (this.contentsByColumn != null) {
			if (this.contentsByColumn.containsKey(colName)) {
				returnArray = this.contentsByColumn.get(colName).toArray(returnArray);
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

	private List<String> getRowAsList(int rowIndex) {
		
		if (rowIndex > this.numRows)
		{
			Logger.getLogger(Consts.CASCADE_LOGGER_NAME).error(
					"File does not have that many rows! There are " + this.numRows + " rows available.");
		}
		
		return rawData.get(rowIndex);
	}


	public String[] getRow(int rowIndex) {
		String[] returnArray = new String[this.numCols];
		 this.getRowAsList(rowIndex).toArray(returnArray);
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

	public List<String> getColumnNameList() {
		return Arrays.asList(this.colHeaders);
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
		return this.numRows;
	}

}
