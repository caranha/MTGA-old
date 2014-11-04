package engine;

import java.io.*;

public class ExtensionFilter implements FilenameFilter {

	String myExtension;

	public
	ExtensionFilter( String extension )
	{
		myExtension = extension;
	}

//	*************************
//	* accept *
//	*************************

	public boolean
	accept( File dir, String name )
	{
		return( name.endsWith( myExtension ) );
	}

}
