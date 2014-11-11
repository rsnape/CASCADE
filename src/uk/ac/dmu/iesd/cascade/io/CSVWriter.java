package uk.ac.dmu.iesd.cascade.io;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * This class can be used to create CSV (Comma Separated Values)files. After
 * creation of the class, the user can employ different methods to write or
 * append values (in columns) in the file. In most cases, the user should
 * manually call the close method. For convenience of writing one or two
 * dimensional arrays rapidly, the closing is done automatically without
 * allowing for further append. Currently, the files are created in the user
 * current directory.
 * 
 * TODO: Can be expanded/modified (e.g. closure of file policy, file location
 * options, etc).
 * 
 * @author Babak Mahdavi
 * @version $Revision: 1.0 $ $Date: 2011/07/11 12:00:00 $
 * 
 */

public class CSVWriter
{
	FileWriter writer;
	String fileName;

	public CSVWriter(String sFileName, boolean append)
	{
		try
		{
			this.writer = new FileWriter(sFileName, append);
			this.fileName = sFileName;

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void writeColHeader(String colName)
	{
		try
		{
			this.writer.append(colName);
			this.writer.append('\n');

			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void writeColHeaders(String[] colNames)
	{
		try
		{
			for (String colName : colNames)
			{
				this.writer.append(colName);
				this.writer.append(',');
			}
			this.writer.append('\n');

			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void appendCol(String[] values)
	{
		try
		{
			for (String value : values)
			{
				this.writer.append(value);
				this.writer.append('\n');
			}

			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void appendRow(double[] values)
	{
		try
		{
			for (double value : values)
			{
				this.writer.append("" + value);
				this.writer.append(',');

			}
			this.writer.append('\n');
			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void appendRow(int[] values)
	{
		try
		{
			for (int value : values)
			{
				this.writer.append("" + value);
				this.writer.append(',');
			}
			this.writer.append('\n');
			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void appendText(String text)
	{
		try
		{
			this.writer.append(text);
			this.writer.append('\n');
			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Usage: for example, IDs and values (written in the row and column of 2D
	 * array respectively) it can be written in one shut, using this method.
	 */

	public void appendCols(String[][] values)
	{
		try
		{
			for (String[] value : values)
			{
				for (int j = 0; j < values[0].length; j++)
				{
					this.writer.append(value[j]);
					this.writer.append(',');

				}
				this.writer.append('\n');
			}

			this.writer.flush();
		}

		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void appendCols(double[][] values)
	{
		try
		{
			for (double[] value : values)
			{
				for (int j = 0; j < values[0].length; j++)
				{
					this.writer.append("" + value[j]);
					this.writer.append(',');

				}
				this.writer.append('\n');
			}

			this.writer.flush();
		}

		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * This method is used to close the file. User should explicitly call this
	 * function after creating the file writer and writing/appending the values.
	 */

	public void close()
	{
		try
		{
			// writer.flush();
			this.writer.close();
			System.getProperty("user.dir");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * @param writeB
	 */
	public void appendRow(String[] s)
	{
		try
		{
			for (String element : s)
			{
				this.writer.append(element);
				this.writer.append(',');
			}
			this.writer.append('\n');
			this.writer.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
