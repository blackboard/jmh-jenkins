package org.jenkinsci.plugins.jmhbenchmark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.Graph;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

public class BenchmarkProjectAction implements Action
{
  private static final String PLUGIN_NAME = "jmhbenchmark";
  private static final String DISPLAY_NAME = "JMH Report Trend";

  public final AbstractProject<?, ?> _project;

  Map<String, BenchmarkTrend> _benchmarkTrend = new TreeMap<String, BenchmarkTrend>();

  public BenchmarkProjectAction( @SuppressWarnings( "rawtypes" ) AbstractProject project )
  {
    _project = project;
  }

  public String getIconFileName()
  {
    return "graph.gif";
  }

  public String getDisplayName()
  {
    return DISPLAY_NAME;
  }

  public String getUrlName()
  {
    return PLUGIN_NAME;
  }

  public AbstractProject<?, ?> getProject()
  {
    return _project;
  }

  public Map<String, BenchmarkTrend> getBenchmarkTrend()
  {
    Map<String, BenchmarkResult> buildBenchmarkData = null;
    BenchmarkTrend trendBenchmarkData = null;
    List<? extends AbstractBuild<?, ?>> builds = getProject().getBuilds();

    _benchmarkTrend.clear();

    for ( AbstractBuild<?, ?> currentBuild : builds )
    {
      NumberOnlyBuildLabel buildNoLabel = new NumberOnlyBuildLabel( currentBuild );

      BenchmarkBuildAction jmhBenchmarkBuildAction = currentBuild.getAction( BenchmarkBuildAction.class );
      if ( jmhBenchmarkBuildAction == null )
      {
        continue;
      }

      BenchmarkReport perfReport = jmhBenchmarkBuildAction.getBuildActionDisplay().getJmhPerfReport();

      buildBenchmarkData = perfReport.getApiTestReport();

      for ( Map.Entry<String, BenchmarkResult> entry : buildBenchmarkData.entrySet() )
      {
        String key = entry.getKey();
        BenchmarkResult val = entry.getValue();
        if ( _benchmarkTrend.containsKey( key ) )
        {
          trendBenchmarkData = _benchmarkTrend.get( key );
          if ( isBenchmarkConfigSame( val, trendBenchmarkData ) )
          {
            trendBenchmarkData.addMeanTrend( buildNoLabel, val.getMean() );
            trendBenchmarkData.addMeanErrorTrend( buildNoLabel, val.getMeanError() );
          }
        }
        else
        {
          // this means the values taken for mode, threads, samples, unit are from the first occurrence in the report,
          // that starts from the latest build down...
          trendBenchmarkData = new BenchmarkTrend( val.getShortBenchmarkName(), val.getMode(), val.getThreads(),
                                                   val.getSamples(), val.getUnit() );
          trendBenchmarkData.addMeanTrend( buildNoLabel, val.getMean() );
          trendBenchmarkData.addMeanErrorTrend( buildNoLabel, val.getMeanError() );
          _benchmarkTrend.put( key, trendBenchmarkData );
        }
      }
    }

    return _benchmarkTrend;
  }

  private boolean isBenchmarkConfigSame( BenchmarkResult build, BenchmarkTrend topBuild )
  {
    if ( build.getMode().equals( topBuild.getMode() ) && ( build.getThreads() == topBuild.getThreads() )
         && ( build.getSamples() == topBuild.getSamples() ) && build.getUnit().equals( topBuild.getUnit() ) )
    {
      return true;
    }
    return false;
  }

  /**
   * Graph of metric points over time.
   */
  public void doSummarizerGraphForMetric( final StaplerRequest request, final StaplerResponse response )
    throws IOException
  {

    final String benchmarkKey = request.getParameter( "benchmarkKey" );
    final String benchmarkUnit = request.getParameter( "benchmarkUnit" );

    final String benchmarkThreads = request.getParameter( "benchmarkThreads" );
    final String benchmarkSamples = request.getParameter( "benchmarkSamples" );
    final String benchmarkMode = request.getParameter( "benchmarkMode" );

    BenchmarkTrend val = _benchmarkTrend.get( benchmarkKey );

    final Map<ChartUtil.NumberOnlyBuildLabel, Double> meanTrend = val.getMeanTrend();
    final Map<ChartUtil.NumberOnlyBuildLabel, Double> meanErrorTrend = val.getMeanErrorTrend();

    String graphTitle = benchmarkKey + ", threads=" + benchmarkThreads + ", samples=" + benchmarkSamples + ", mode="
                        + benchmarkMode;

    final Graph graph = new GraphImpl( graphTitle, benchmarkUnit )
      {

        protected DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> createDataSet()
        {
          DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dataSetBuilder = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

          for ( ChartUtil.NumberOnlyBuildLabel label : meanTrend.keySet() )
          {
            dataSetBuilder.add( meanTrend.get( label ), "Score", label );
          }

          for ( ChartUtil.NumberOnlyBuildLabel label : meanErrorTrend.keySet() )
          {
            dataSetBuilder.add( meanErrorTrend.get( label ), "Score Error (99.9%)", label );
          }

          return dataSetBuilder;
        }
      };

    graph.doPng( request, response );
  }

  private abstract class GraphImpl extends Graph
  {
    private final String _graphTitle;
    private final String _unit;

    protected GraphImpl( final String graphTitle, final String unit )
    {
      super( -1, 300, 200 ); // cannot use timestamp, since ranges may change
      _graphTitle = graphTitle;
      _unit = unit;
    }

    protected abstract DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> createDataSet();

    protected JFreeChart createGraph()
    {
      final CategoryDataset dataset = createDataSet().build();

      final JFreeChart chart = ChartFactory.createLineChart( _graphTitle, // title
                                                             "Build Number #", // category axis label
                                                             _unit, // value axis label
                                                             dataset, // data
                                                             PlotOrientation.VERTICAL, // orientation
                                                             true, // include legend
                                                             true, // tooltips
                                                             false // urls
          );

      chart.setBackgroundPaint( Color.white );

      final CategoryPlot plot = chart.getCategoryPlot();
      
      CategoryAxis domainAxis = new ShiftedCategoryAxis( null );
      plot.setDomainAxis( domainAxis );
      domainAxis.setCategoryLabelPositions( CategoryLabelPositions.UP_90 );
      domainAxis.setLowerMargin( 0.0 );
      domainAxis.setUpperMargin( 0.0 );
      domainAxis.setCategoryMargin( 0.0 );
      
      final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
      renderer.setBaseStroke( new BasicStroke( 3.0f ) );
      ColorPalette.apply( renderer );

      return chart;
    }
  }
}
