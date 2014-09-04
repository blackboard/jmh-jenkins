package org.jenkinsci.plugins.jmhbenchmark;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.model.TaskListener;

/**
 * BenchmarkBuildActionDisplay is a {@link ModelOject} that contains the benchmark report ({@link BenchmarkReport}). This object is 
 * created when the JMH Benchmark Report link is clicked from the build page.  
 *
 */
public class BenchmarkBuildActionDisplay implements ModelObject
{
  private transient BenchmarkBuildAction _buildAction; 
  private final int _decimalPlaces;
  private final BenchmarkReport _currentReport;
  
  public BenchmarkBuildActionDisplay( final BenchmarkBuildAction buildAction, TaskListener listener, int decimalPlaces )
  {
    _buildAction = buildAction;
    _currentReport = _buildAction.getPerformanceReport();    
    _currentReport.setBuildAction( buildAction );
    _decimalPlaces = decimalPlaces;
  }
  
  public String getDisplayName()
  {
    return "Benchmark Report";
  }

  public AbstractBuild<?, ?> getBuild()
  {
    return _buildAction.getBuild();
  }

  public BenchmarkReport getJmhPerfReport()
  {
    return _currentReport;
  }
  
  public double getFormattedNumber(double num)
  {
    int multiplier = (int) Math.pow( 10, _decimalPlaces );    
    return (double) Math.round( num * multiplier )/multiplier;
  }
}
