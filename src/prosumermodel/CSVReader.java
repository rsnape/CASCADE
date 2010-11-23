/**
 * 
 */
package prosumermodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

/**
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

	/*
	 * methods
	 */
	void parseByColumn(){

		boolean hasNextLine = true;		
		String thisLine = null;
		try {
			thisLine = myReader.readLine();
		} catch (IOException e) {
			System.err.println("IO Exception occured whilst reading first CSV line");
			e.printStackTrace();
		}

		if (thisLine == null)
		{
			System.err.println("Supplied CSV is empty!!");
			hasNextLine = false;
		}
		else
		{
			ArrayList<String> tempArray = new ArrayList<String>();
			StringTokenizer myTokenizer = new StringTokenizer(thisLine, mySeperator);
			numCols = myTokenizer.countTokens();

			colHeaders = new String[numCols];
			while(myTokenizer.hasMoreTokens())
			{
				tempArray.add(myTokenizer.nextToken());	
			}

			tempArray.toArray(colHeaders);
		}

		dataArray = new ArrayList[numCols];
		for (int t = 0; t < numCols; t++)
		{
			dataArray[t] = new ArrayList<String>();
		}

		while(hasNextLine)
		{
			try {
				thisLine = myReader.readLine();

			} catch (IOException e) {
				System.err.println("IO Exception occured whilst reading CSV line");
				e.printStackTrace();
			}


			hasNextLine = (thisLine != null);
			if (hasNextLine)
			{
				StringTokenizer myTokenizer = new StringTokenizer(thisLine, mySeperator);
				for (int i = 0; i < numCols; i++)
				{
					dataArray[i].add(myTokenizer.nextToken());
				}
				numRows++;
			}

			contentsByColumn = new WeakHashMap<String, String[]>();
			for (int k=0; k < numCols; k++){
				contentsByColumn.put(colHeaders[k], dataArray[k].toArray(new String[numRows]));
			}
		}
		
		System.out.println("Parsed file - " + numCols + " columns and " + numRows + " rows.");

	}

	public String[] getColumn(String colName){
		String[] returnArray = null;
		if (contentsByColumn != null)
		{
			if (contentsByColumn.containsKey(colName))
			{
				returnArray = contentsByColumn.get(colName);
			}
			else
			{
				System.err.println("File does not contain that column!  Available columns are :");
				System.err.println(Arrays.toString(colHeaders));
			}
		}
		else
		{
			System.err.println("CSVReader contentsByColumn is null!!  Check Parse output and file contents!");
			System.err.println("Reader name is " + CSVFileName);
			System.err.println("Number of columns is " + numCols + " with names " + Arrays.toString(colHeaders));
		}
		
		return returnArray;
	}

	/*
	 * Constructors
	 */	
	public CSVReader(String myFilename) throws FileNotFoundException
	{
		this(myFilename, DEFAULT_SEPERATOR);
	}

	public CSVReader(File myFile) throws FileNotFoundException
	{
		this(myFile, DEFAULT_SEPERATOR);
	}
	
	public CSVReader(String myFilename, String seperator) throws FileNotFoundException
	{
		this(new FileReader(myFilename), seperator, DEFAULT_HAS_COL_HEADERS);
		CSVFileName = myFilename;
	}

	public CSVReader(File myFile, String seperator) throws FileNotFoundException
	{
		this(new FileReader(myFile), seperator, DEFAULT_HAS_COL_HEADERS);
		CSVFileName = myFile.getAbsolutePath();
	}

	public CSVReader(Reader myCSV, String seperator, boolean colHeaders)
	{
		myReader = new BufferedReader(myCSV);
		mySeperator = seperator;
		hasColHeaders = colHeaders;
	}

}
