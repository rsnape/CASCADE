/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;

import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.io.CSVWriter;

/**
 * @author jsnape
 * 
 */
public class OutputCollator
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String baseDir = "";
		String fileToCollat = "";
		try
		{
			baseDir = args[0];
			fileToCollat = args[1];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			OutputCollator.usage(e);
			System.exit(1);
		}
		int option;

		if (args.length == 3)
		{
			option = Integer.valueOf(args[2]);
		}
		else
		{
			option = 0;
		}

		switch (option)
		{
		case 0:
			OutputCollator.collateBandD(baseDir, fileToCollat);
			break;
		case 1:
			OutputCollator.collateAggregateDemand(baseDir, fileToCollat);
		default:
			break;
		}

	}

	/**
	 * @param baseDir
	 * @param fileToCollat
	 */
	private static void collateAggregateDemand(String baseDir, String fileToCollat)
	{
		File d = new File(baseDir);
		if (!d.exists() || !d.isDirectory())
		{
			OutputCollator.usage(new FileNotFoundException("The directory you specified is not available"));
		}

		FilenameFilter filter = new NameStartsFilter(fileToCollat);
		File[] subFiles = d.listFiles(filter);
		CSVWriter out = new CSVWriter(baseDir.concat(File.separator).concat("CollatedOutput.csv"), false);
		String[][] writeDS = null;
		int fileNum = 0;
		for (File sd : subFiles)
		{
			CSVReader in = null;
			try
			{
				in = new CSVReader(sd.getAbsolutePath());
			}
			catch (FileNotFoundException e)
			{
				OutputCollator.usage(e);
			}
			in.parseByColumn();
			String[] D = in.getColumn("NetDemand");
			String[] S = in.getColumn("CurrentPriceSignal");

			if (fileNum == 0)
			{
				writeDS = new String[subFiles.length * 2 + 1][S.length + 1];
				String[] tick = in.getColumn("Tick");
				writeDS[0] = tick;
			}

			System.arraycopy(D, 0, writeDS[fileNum * 2 + 1], 1, D.length);
			System.arraycopy(S, 0, writeDS[fileNum * 2 + 2], 1, S.length);

			writeDS[fileNum * 2 + 1][0] = "D : ".concat(sd.getName());
			writeDS[fileNum * 2 + 2][0] = "S : ".concat(sd.getName());
			fileNum++;
		}
		out.appendCols(writeDS);

	}

	/**
	 * @param baseDir
	 * @param fileToCollat
	 */
	private static void collateBandD(String baseDir, String fileToCollat)
	{
		File d = new File(baseDir);
		if (!d.exists() || !d.isDirectory())
		{
			OutputCollator.usage(new FileNotFoundException("The directory you specified is not available"));
		}

		File[] subFiles = d.listFiles();
		ArrayList<File> subDirs = new ArrayList<File>();
		for (File f : subFiles)
		{
			if (f.isDirectory())
			{
				subDirs.add(f);
			}
		}

		if (subDirs.size() == 0)
		{
			OutputCollator.usage(new FileNotFoundException("The directory you specified does not contain sub-directories"));
		}

		CSVWriter out = new CSVWriter(baseDir.concat(File.separator).concat("CollatedOutput.csv"), false);

		for (File sd : subDirs)
		{
			CSVReader in = null;
			try
			{
				in = new CSVReader(sd.getAbsolutePath().concat(File.separator).concat(fileToCollat));
			}
			catch (FileNotFoundException e)
			{
				OutputCollator.usage(e);
			}
			in.parseRaw();
			String[] B = in.getRow(7);
			String[] D = in.getRow(9);

			String[] writeB = new String[B.length + 1];
			String[] writeD = new String[D.length + 1];
			System.arraycopy(B, 0, writeB, 1, B.length);
			System.arraycopy(D, 0, writeD, 1, D.length);

			writeB[0] = "B : ".concat(sd.getName());
			writeD[0] = "D : ".concat(sd.getName());

			out.appendRow(writeB);
			out.appendRow(writeD);

		}
	}

	/**
	 * 
	 */
	private static void usage(Exception e)
	{
		System.err.println("the output collator halted, likely due to incorrect usage");
		System.err.println("Usage : java OutputCollator.class <dirName> <fileName> [option]");
		System.err.println("dirName : directory containing a number of subdirectories which hold the files to be collated");
		System.err.println("fileName : the name of the file in each directory which should be collated together");
		System.err
				.println("option : the numbered option for collation - currently only one which is the default.  Note this argument is optional");
		System.err.println("Program halted with exception" + e.toString());
		System.exit(1);
	}

}
