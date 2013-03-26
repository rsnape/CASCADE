/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author jsnape
 *
 */
public class NameStartsFilter implements FilenameFilter
{
		private String nameStart;
		
		/* (non-Javadoc)
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(File dir, String name)
		{
			return name.startsWith(nameStart);
		}
		
		public NameStartsFilter(String name)
		{
			this.nameStart = name;
		}


}
