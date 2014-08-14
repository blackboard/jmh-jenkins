package org.jenkinsci.plugins.jmhbenchmark;

import hudson.*;
import hudson.model.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.tasks.*;
import hudson.util.FormValidation;

import java.io.*;
import java.util.*;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class BenchmarkPublisher extends Recorder
{
  private int _performanceIncreaseThreshold;
  private int _performanceDegradationThreshold;
  private int _decimalPlaces;
  private int _baselineBuildNumber;
  private ReportParser _parser;
  private static String BENCHMARK_OUTPUT_FOLDER = "jmh_benchmark_result";
  private static String BENCHMARK_MODE_THRPT = "thrpt";
  private static String BUILD_PROJECT_NAME;
  // two decimal places are used to set changes from previous or baseline build
  private static int MULTIPLIER = 100;
  
  @DataBoundConstructor
  public BenchmarkPublisher(int performanceIncreaseThreshold, int performanceDegradationThreshold, int decimalPlaces, int baselineBuildNumber )
  {
    _performanceIncreaseThreshold = performanceIncreaseThreshold;
    _performanceDegradationThreshold = performanceDegradationThreshold;
    _decimalPlaces = decimalPlaces;
    _baselineBuildNumber = baselineBuildNumber;
  }

  public int getDecimalPlaces()
  {
    return _decimalPlaces;
  }
  
  public int getPerformanceIncreaseThreshold()
  {
    return _performanceIncreaseThreshold;
  }
  
  public int getPerformanceDegradationThreshold()
  {
    return _performanceDegradationThreshold;
  }
  
  public int getBaselineBuildNumber()
  {
    return _baselineBuildNumber;
  }

  @Override
  public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener )
    throws IOException, InterruptedException
  {
    BUILD_PROJECT_NAME = build.getProject().getName();
    boolean buildStable = true;
    Set<String> failedBenchmarks = new HashSet<String>();
    
    PrintStream logger = listener.getLogger();
    if ( build.getResult().isWorseThan( Result.UNSTABLE ) )
    {
      build.setResult( Result.FAILURE );
      return true;
    }
        
    FilePath[] files =  build.getWorkspace().list( "*.csv" );
    if( files.length <= 0)
    {
      build.setResult( Result.FAILURE );
      logger.println( "JMH Benchmark: benchmark file could not be found.");
      return true;      
    }
       
    File localReports = copyBenchmarkOutputToMaster( build, files[0], BUILD_PROJECT_NAME );
        
    // currently only a CSV parser is supported
    _parser = new CsvParser();
    BenchmarkReport parsedReport = _parser.parse( build, localReports, listener );

    // get previous successful build report to calculate the increase in mean value for each benchmark and set an indicator (i.e. 
    // green or red for each benchmark) depending on threshold set in the configuration. If there is at least a red for 
    // one benchmark, the build status will be unstable

    AbstractBuild<?, ?> prevSuccessfulBuild = build.getPreviousSuccessfulBuild();

    // if there is no previous successful build, there is no baseline build
    if ( prevSuccessfulBuild != null )
    {
      Map<String, BenchmarkResult> currentApiTestReport = parsedReport.getApiTestReport();
      BenchmarkBuildAction prevBuildAction = prevSuccessfulBuild.getAction( BenchmarkBuildAction.class );
      BenchmarkReport prevPerfReport = null;
      Map<String, BenchmarkResult> prevApiTestReport = null;

      if ( prevBuildAction != null )
      {
        prevPerfReport = prevBuildAction.getBuildActionDisplay().getJmhPerfReport();
        prevApiTestReport = prevPerfReport.getApiTestReport();
      }

      AbstractBuild<?, ?> baselineBuild = getNthBuild( build );
      Map<String, BenchmarkResult> baselineApiTestReport = null;
      if ( baselineBuild != null )
      {
        BenchmarkBuildAction baselineBuildAction = baselineBuild.getAction( BenchmarkBuildAction.class );
        BenchmarkReport baselinePerfReport = null;

        if ( baselineBuildAction != null )
        {
          baselinePerfReport = baselineBuildAction.getBuildActionDisplay().getJmhPerfReport();
          baselineApiTestReport = baselinePerfReport.getApiTestReport();
        }
      }

      double decreaseInMeanFromPrev = 0;
      double decreaseInMeanFromBaseline = 0;

      if ( prevApiTestReport != null )
      {
        for ( Map.Entry<String, BenchmarkResult> entry : currentApiTestReport.entrySet() )
        {
          String key = entry.getKey();
          BenchmarkResult currVal = entry.getValue();
          BenchmarkResult prevVal = prevApiTestReport.get( key );

          if ( prevVal != null )
          {
            // decrease in mean from previous is calculated as ((prev - curr)/prev) * 100%
            decreaseInMeanFromPrev = ( 1 - currVal.getMean() / prevVal.getMean() ) * 100.0;
            decreaseInMeanFromPrev = (double) Math.round( decreaseInMeanFromPrev * MULTIPLIER ) / MULTIPLIER;
            if ( currVal.getMode().equalsIgnoreCase( BENCHMARK_MODE_THRPT ) )
            {
              decreaseInMeanFromPrev = -1 * decreaseInMeanFromPrev;
            }
            currVal.setMeanChangeFromPrev( decreaseInMeanFromPrev );
          }

          if ( baselineApiTestReport != null )
          {
            BenchmarkResult baselineVal = baselineApiTestReport.get( key );

            if ( baselineVal != null )
            {
              // decrease in mean from baseline is calculated as ((baseline - curr)/baseline) * 100%
              decreaseInMeanFromBaseline = ( 1 - currVal.getMean() / baselineVal.getMean() ) * 100.0;
              decreaseInMeanFromBaseline = (double) Math.round( decreaseInMeanFromBaseline * MULTIPLIER ) / MULTIPLIER;
              if(currVal.getMode().equalsIgnoreCase( BENCHMARK_MODE_THRPT ))
              {
                decreaseInMeanFromBaseline = -1 * decreaseInMeanFromBaseline;
              }
              currVal.setMeanChangeFromBaseline( decreaseInMeanFromBaseline );              
            }
          }

          if ( decreaseInMeanFromBaseline >= _performanceIncreaseThreshold
               || decreaseInMeanFromPrev >= _performanceIncreaseThreshold )
          {
            currVal.setChangeIndicator( "green" );
          }
          else if ( decreaseInMeanFromBaseline <= _performanceDegradationThreshold
                    || decreaseInMeanFromPrev <= _performanceDegradationThreshold )
          {
            currVal.setChangeIndicator( "red" );
            failedBenchmarks.add( currVal.getBenchmarkName() );
            buildStable = false;
          }
        }
      }
    }
    
    BenchmarkBuildAction buildAction = new BenchmarkBuildAction( build, parsedReport, _decimalPlaces );
    build.addAction( buildAction );

    if ( !buildStable )
    {
      StringBuilder sb = new StringBuilder();
      for ( String bm : failedBenchmarks )
      {
        sb.append( "|" ).append( bm );
      }
      build.addAction( new ParametersAction( new StringParameterValue( "JMH_FAILED_BENCHMARKS", sb.toString() ) ) );
      build.setResult( Result.FAILURE );
    }

    return true;
  }

  private AbstractBuild getNthBuild( AbstractBuild build )
  {
    if( _baselineBuildNumber == 0)
      return null;
    
    AbstractBuild nthBuild = build;

    int nextBuildNumber = build.number - _baselineBuildNumber;

    for ( int i = 1; i <= nextBuildNumber; i++ )
    {
      nthBuild = nthBuild.getPreviousBuild();
      if ( nthBuild == null )
        return null;
      // this is required since old builds can be cleaned or the baseline builds are old builds that have been kept forever.
      if(nthBuild.number == _baselineBuildNumber)
        return nthBuild;
    }
    return nthBuild;
  }
  
  private File copyBenchmarkOutputToMaster( AbstractBuild<?, ?> build, FilePath output, String projectFolderName )
      throws IOException, InterruptedException
    {
      File localReport = getPerformanceReport( build, projectFolderName, output.getName() );
      output.copyTo( new FilePath( localReport ) );
      return localReport;
    }
  
  public static File getPerformanceReport( AbstractBuild<?, ?> build, String projectFolderName,
                                           String benchmarkOutputFileName )
  {
    return new File( build.getRootDir(), getRelativePath( projectFolderName, benchmarkOutputFileName ) );
  }

  private static String getRelativePath( String... suffixes )
  {
    StringBuilder sb = new StringBuilder( 150 );
    sb.append( BENCHMARK_OUTPUT_FOLDER );
    for ( String suffix : suffixes )
    {
      sb.append( File.separator ).append( suffix );
    }
    return sb.toString();
  }

  @Override
  public Action getProjectAction( AbstractProject<?, ?> project )
  {
    return new BenchmarkProjectAction( project );
  }

  @Override
  public DescriptorImpl getDescriptor()
  {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * The class is marked as public so that it can be accessed from views.
   * <p>
   * See <tt>src/main/resources/org/jenkinsci/plugins/jmhbenchmark/BenchmarkPublisher/*.jelly</tt> for the actual HTML
   * fragment for the configuration screen.
   */
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
  {
    public boolean isApplicable( @SuppressWarnings( "rawtypes" ) Class<? extends AbstractProject> jobType )
    {
      // Indicates that this builder can be used with all kinds of project types 
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName()
    {
      return "Publish JMH Test Result";
    }

    public FormValidation doCheckDecimalPlaces( @QueryParameter String decimalPlaces )
    {
      try
      {
        Integer.parseInt( decimalPlaces );
      }
      catch ( NumberFormatException ex )
      {
        return FormValidation.error( "Not a valid number" );
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckPerformanceDegradationThreshold( @QueryParameter String performanceDegradationThreshold )
    {
      try
      {
        Integer.parseInt( performanceDegradationThreshold );
      }
      catch ( NumberFormatException ex )
      {
        return FormValidation.error( "Not a valid number" );
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckPerformanceIncreaseThreshold( @QueryParameter String performanceIncreaseThreshold )
    {
      try
      {
        Integer.parseInt( performanceIncreaseThreshold );
      }
      catch ( NumberFormatException ex )
      {
        return FormValidation.error( "Not a valid number" );
      }
      return FormValidation.ok();
    }
    
    public FormValidation doCheckBaselineBuildNumber(@QueryParameter String baselineBuildNumber)
    {
      try
      {
        Integer.parseInt( baselineBuildNumber );
      }
      catch ( NumberFormatException ex )
      {
        return FormValidation.error( "Not a valid number" );
      }
      return FormValidation.ok();      
    }
  }

  public BuildStepMonitor getRequiredMonitorService()
  {
    return BuildStepMonitor.BUILD;
  }
}
