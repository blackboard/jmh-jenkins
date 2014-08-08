package org.jenkinsci.plugins.jmhbenchmark;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.util.StreamTaskListener;

import java.lang.ref.WeakReference;

import org.kohsuke.stapler.StaplerProxy;

public class BenchmarkBuildAction implements Action, StaplerProxy
{
  private final AbstractBuild<?, ?> _build;
  private final BenchmarkReport _performanceReport;
  private int _decimalPlaces;
  private transient WeakReference<BenchmarkBuildActionDisplay> _buildActionDisplay;

  public BenchmarkBuildAction( AbstractBuild<?, ?> pBuild, BenchmarkReport performanceReport, int decimalPlaces )
  {
    _build = pBuild;
    _performanceReport = performanceReport;
    _decimalPlaces = decimalPlaces;
  }

  public String getIconFileName()
  {
    return "graph.gif";
  }

  public String getDisplayName()
  {
    return "JMH Benchmark Report";
  }

  // this must be the name of the plugin (what's also the artifactId in the pom file).
  // how do we have different jelly files and how can we address each of them? I.e. what's the link?
  public String getUrlName()
  {
    return "jmhbenchmark";
  }

  public AbstractBuild<?, ?> getBuild()
  {
    return _build;
  }

  public BenchmarkReport getPerformanceReport()
  {
    return _performanceReport;
  }

  public BenchmarkBuildActionDisplay getBuildActionDisplay()
  {
    BenchmarkBuildActionDisplay buildDisplay = null;
    WeakReference<BenchmarkBuildActionDisplay> wr = _buildActionDisplay;

    if ( wr != null )
    {
      buildDisplay = wr.get();
      if ( buildDisplay != null )
        return buildDisplay;
    }

    buildDisplay = new BenchmarkBuildActionDisplay( this, StreamTaskListener.fromStdout(), _decimalPlaces );

    _buildActionDisplay = new WeakReference<BenchmarkBuildActionDisplay>( buildDisplay );
    return buildDisplay;
  }

  public void setBuildActionDisplay( WeakReference<BenchmarkBuildActionDisplay> buildActionDisplay )
  {
    _buildActionDisplay = buildActionDisplay;
  }

  public BenchmarkBuildActionDisplay getTarget()
  {
    return getBuildActionDisplay();
  }
}
