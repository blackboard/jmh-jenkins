package org.jenkinsci.plugins.jmhbenchmark;

import hudson.util.ChartUtil.NumberOnlyBuildLabel;

import java.util.HashMap;
import java.util.Map;

public class BenchmarkTrend
{
  private String _benchmarkName;
  private String _mode;
  private int _threads;
  private int _samples;
  private String _unit;
  private Map<NumberOnlyBuildLabel, Double> _meanTrend;
  private Map<NumberOnlyBuildLabel, Double> _meanErrorTrend;

  public BenchmarkTrend()
  {
    _meanTrend = new HashMap<NumberOnlyBuildLabel, Double>();
    _meanErrorTrend = new HashMap<NumberOnlyBuildLabel, Double>();
  }

  public BenchmarkTrend( String benchmarkName, String mode, int threads, int samples, String unit )
  {
    _benchmarkName = benchmarkName;
    _mode = mode;
    _threads = threads;
    _samples = samples;
    _unit = unit;
    _meanTrend = new HashMap<NumberOnlyBuildLabel, Double>();
    _meanErrorTrend = new HashMap<NumberOnlyBuildLabel, Double>();
  }

  public String getBenchmarkName()
  {
    return _benchmarkName;
  }

  public String getMode()
  {
    return _mode;
  }

  public int getThreads()
  {
    return _threads;
  }

  public int getSamples()
  {
    return _samples;
  }

  public String getUnit()
  {
    return _unit;
  }

  public Map<NumberOnlyBuildLabel, Double> getMeanTrend()
  {
    return _meanTrend;
  }

  public Map<NumberOnlyBuildLabel, Double> getMeanErrorTrend()
  {
    return _meanErrorTrend;
  }

  public void addMeanTrend( NumberOnlyBuildLabel buildNo, Double value )
  {
    _meanTrend.put( buildNo, value );
  }

  public void addMeanErrorTrend( NumberOnlyBuildLabel buildNo, Double value )
  {
    _meanErrorTrend.put( buildNo, value );
  }
}
