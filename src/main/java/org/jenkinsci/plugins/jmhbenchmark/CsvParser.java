package org.jenkinsci.plugins.jmhbenchmark;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CsvParser extends ReportParser
{
  private static String DECREASE_IN_MEAN_HEADER_NAME = "Decrease in Mean in %";

  public CsvParser()
  {
  }

  public BenchmarkReport parse( AbstractBuild<?, ?> build, File reportFile, TaskListener listener ) throws IOException
  {
    PrintStream logger = null;
    if ( listener != null )
    {
      logger = listener.getLogger();
    }

    final BenchmarkReport report = new BenchmarkReport();

    BufferedReader reader = null;
    try
    {
      List<String> headerColumns = null;
      reader = new BufferedReader( new FileReader( reportFile ) );
      String line = reader.readLine();
      if ( line != null )
      {
        // This is the first line. The CSV file has header at the first line.
        headerColumns = getHeadersList( line );
        report.setHeader( headerColumns );
        report.addHeaderColumn( DECREASE_IN_MEAN_HEADER_NAME );
        line = reader.readLine();
      }
      while ( line != null )
      {
        BenchmarkResult sample = getIndividualBenchmark( line, headerColumns );
        report.addApiTestData( sample.getKey(), sample );
        line = reader.readLine();
      }
    }
    finally
    {
      if ( reader != null )
      {
        reader.close();
      }
    }
    return report;

  }
                     
  /**
   * If the benchmark has no parameters, the output is in the order: 
   *    "Benchmark","Mode","Threads","Samples","Mean","Mean Error (99.9%)","Unit"
   * If the benchmark has parameters, the output is in the following form:
   *    "Benchmark","Mode","Threads","Samples","Mean","Mean Error (99.9%)","Unit", "Param: paramName1","Param: paramName2"
   * @param line
   * @param header
   * @return
   */
  private BenchmarkResult getIndividualBenchmark( String line, List<String> header )
  {
    BenchmarkResult sample = new BenchmarkResult();

    String[] values = line.split( "," );
    String benchmarkName = stripQuotes( values[ 0 ] );
    sample.setBenchmarkName( benchmarkName );
    sample.setShortBenchmarkName( getShortName(benchmarkName) );
    sample.setMode( stripQuotes( values[ 1 ] ) );
    sample.setThreads( Integer.valueOf( values[ 2 ] ) );
    sample.setSamples( Integer.valueOf( values[ 3 ] ) );
    sample.setMean( Double.valueOf( values[ 4 ] ) );
    if ( !values[ 5 ].equals( "NaN" ) )
    {
      sample.setMeanError( Double.valueOf( values[ 5 ] ) );
    }
    sample.setUnit( stripQuotes( values[ 6 ] ) );

    for ( int i = 7; i < values.length; i++ )
    {
      sample.addParams( header.get( i ), stripQuotes( values[ i ] ) );
    }

    return sample;
  }

  private String stripQuotes( String quotedStr )
  {
    return quotedStr.replace( "\"", "" );
  }

  private String getShortName( String name )
  {
    String[] splitNames = name.split( "\\." );
    StringBuilder sb = new StringBuilder( 100 );
    for ( int i = 0; i < splitNames.length - 2; i++ )
    {
      sb.append( splitNames[i].charAt( 0 ) );
      sb.append( "." );
    }
    sb.append( splitNames[splitNames.length - 2] );
    sb.append( "." );
    sb.append( splitNames[splitNames.length - 1] );

    return sb.toString();
  }
  
  private List<String> getHeadersList( String line )
  {
    List<String> headers = new ArrayList<String>();
    String[] columns = line.split( "," );
    for ( int i = 0; i < columns.length; i++ )
    {
      headers.add( stripQuotes( columns[ i ] ) );
    }
    return headers;
  }

}
