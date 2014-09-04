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
  private static final String IMPROVEMENT_IN_MEAN_HEADER_NAME = "Score Improvement % (previous/baseline)";
  private static final int BENCHMARK_NAME = 0;  
  private static final int BENCHMARK_MODE = 1;
  private static final int THREADS = 2;
  private static final int SAMPLES = 3;
  private static final int MEAN = 4;
  private static final int MEAN_ERROR_99_9 = 5;
  private static final int UNIT = 6;

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
      logger.println("Parsing the file : " + reportFile.getName());
      reader = new BufferedReader( new FileReader( reportFile ) );
      String line = reader.readLine();
      if ( line != null )
      {
        // This is the first line. The CSV file has header at the first line.
        headerColumns = getHeadersList( line );
        report.setHeader( headerColumns );
        report.addHeaderColumn( IMPROVEMENT_IN_MEAN_HEADER_NAME );
        line = reader.readLine();
      }
      while ( line != null )
      {
        BenchmarkResult sample = getIndividualBenchmark( line, headerColumns );
        report.addBenchmarkResult( sample.getKey(), sample );
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
   * If a benchmark hasn't used parameters, the output is in the order: 
   *    "Benchmark","Mode","Threads","Samples","Score","Score Error (99.9%)","Unit"
   * If a benchmark used parameters, the output is in the order:
   *    "Benchmark","Mode","Threads","Samples","Score","Score Error (99.9%)","Unit", "Param: paramName1","Param: paramName2" 
   * @param line - a line of data from the csv file to which the benchmark result is written
   * @param header - the header of the csv file
   * @return
   */
  private BenchmarkResult getIndividualBenchmark( String line, List<String> header )
  {
    BenchmarkResult sample = new BenchmarkResult();

    String[] values = line.split( "," );
    String benchmarkName = stripQuotes( values[ BENCHMARK_NAME ] );
    sample.setBenchmarkName( benchmarkName );
    sample.setShortBenchmarkName( getShortName(benchmarkName) );
    sample.setMode( stripQuotes( values[ BENCHMARK_MODE ] ) );
    sample.setThreads( Integer.valueOf( values[ THREADS ] ) );
    sample.setSamples( Integer.valueOf( values[ SAMPLES ] ) );
    sample.setMean( Double.valueOf( values[ MEAN ] ) );
    if ( !values[ MEAN_ERROR_99_9 ].equals( "NaN" ) )
    {
      sample.setMeanError( Double.valueOf( values[ MEAN_ERROR_99_9 ] ) );
    }
    sample.setUnit( stripQuotes( values[ UNIT ] ) );

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
