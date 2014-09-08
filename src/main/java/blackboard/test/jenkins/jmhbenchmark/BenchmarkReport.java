package blackboard.test.jenkins.jmhbenchmark;

import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BenchmarkReport contains one or more {@link BenchmarkResult} objects for all the benchmarks
 * run in the build.
 *
 */
public class BenchmarkReport implements Serializable
{
  private static final long serialVersionUID = 7490376469570293188L;

  private transient BenchmarkBuildAction _buildAction;
  private List<String> _header = new ArrayList<String>();
  private final Map<String, BenchmarkResult> _report = new TreeMap<String, BenchmarkResult>();

  public void addBenchmarkResult( String benchmarkName, BenchmarkResult data )
  {
    _report.put( benchmarkName, data );
  }

  public Map<String, BenchmarkResult> getReport()
  {
    return _report;
  }

  public List<String> getHeader()
  {
    return _header;
  }

  public void setHeader( List<String> header )
  {
    _header = header;
  }

  public void addHeaderColumn( String columnName )
  {
    _header.add( columnName );
  }

  public List<String> getHeaderWithoutParams()
  {
    List<String> header = new ArrayList<String>();
    for ( String columnName : _header )
    {
      if ( columnName.startsWith( "Param:" ) )
        continue;
      header.add( columnName );
    }
    return header;
  }

  public List<String> getHeaderParamsSorted()
  {
    List<String> headerParams = new ArrayList<String>();
    for ( String columnName : _header )
    {
      if ( columnName.startsWith( "Param:" ) )
      {
        headerParams.add( columnName );
      }
    }
    Collections.sort( headerParams );
    return headerParams;
  }

  public AbstractBuild<?, ?> getBuild()
  {
    return _buildAction.getBuild();
  }

  public BenchmarkBuildAction getBuildAction()
  {
    return _buildAction;
  }

  public void setBuildAction( BenchmarkBuildAction buildAction )
  {
    _buildAction = buildAction;
  }
}
