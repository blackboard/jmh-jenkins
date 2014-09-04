package org.jenkinsci.plugins.jmhbenchmark;

import java.util.HashMap;
import java.util.Map;

/**
 * BenchmarkResult contains properties associated with a JMH benchmark such as name of the benchmark, mode the benchmark is run, 
 * number of threads, samples, mean, mean error, unit, list of parameters if any. Since the benchmark is also expected to run in 
 * a Continuous Integration, BenchmarkResult also contains data on the percentage change of the benchmark score from previous 
 * builds and from a baseline build.
 *
 */
public class BenchmarkResult
{
  private String _benchmarkName;
  private String _shortBenchmarkName;
  private String _mode;
  private int _threads;
  private int _samples;
  private double _mean;
  private double _meanError;
  private String _unit;
  private Map<String, String> _params;
  private double _meanChangeFromPrev;
  private double _meanChangeFromBaseline;
  private String _changeIndicator;

  public BenchmarkResult()
  {
    _params = new HashMap<String, String>();
    _changeIndicator = "";
  }

  public String getBenchmarkName()
  {
    return _benchmarkName;
  }

  public void setBenchmarkName( String benchmarkName )
  {
    _benchmarkName = benchmarkName;
  }

  public String getShortBenchmarkName()
  {
    return _shortBenchmarkName;
  }

  public void setShortBenchmarkName( String shortBenchmarkName )
  {
    _shortBenchmarkName = shortBenchmarkName;
  }

  public String getMode()
  {
    return _mode;
  }

  public void setMode( String mode )
  {
    _mode = mode;
  }

  public int getThreads()
  {
    return _threads;
  }

  public void setThreads( int threads )
  {
    _threads = threads;
  }

  public int getSamples()
  {
    return _samples;
  }

  public void setSamples( int samples )
  {
    _samples = samples;
  }

  public double getMean()
  {
    return _mean;
  }

  public void setMean( double mean )
  {
    _mean = mean;
  }

  public double getMeanError()
  {
    return _meanError;
  }

  public void setMeanError( double meanError )
  {
    _meanError = meanError;
  }

  public String getUnit()
  {
    return _unit;
  }

  public void setUnit( String unit )
  {
    _unit = unit;
  }

  public Map<String, String> getParams()
  {
    return _params;
  }

  public void addParams( String paramName, String paramValue )
  {
    _params.put( paramName, paramValue );
  }

  public double getMeanChangeFromPrev()
  {
    return _meanChangeFromPrev;
  }

  public void setMeanChangeFromPrev( double meanChangeFromPrev )
  {
    _meanChangeFromPrev = meanChangeFromPrev;
  }

  public double getMeanChangeFromBaseline()
  {
    return _meanChangeFromBaseline;
  }

  public void setMeanChangeFromBaseline( double meanChangeFromBaseline )
  {
    _meanChangeFromBaseline = meanChangeFromBaseline;
  }
  
  public String getChangeIndicator()
  {
    return _changeIndicator;
  }

  public void setChangeIndicator( String changeIndicator )
  {
    _changeIndicator = changeIndicator;
  }

  public String getKey()
  {
    StringBuilder sb = new StringBuilder( 100 );
    sb.append( _shortBenchmarkName );
    for ( Map.Entry<String, String> entry : _params.entrySet() )
    {
      if ( !entry.getValue().equals( "" ) )
      {
        sb.append( ":" );
        sb.append( entry.getValue() );
      }
    }
    return sb.toString();
  }
}
