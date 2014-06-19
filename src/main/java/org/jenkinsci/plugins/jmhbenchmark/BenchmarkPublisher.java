package org.jenkinsci.plugins.jmhbenchmark;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class BenchmarkPublisher extends Recorder
{
  private String _outputFilePath;
  private int _decimalPlaces;
  ReportParser _parser;
  private static String BENCHMARK_OUTPUT_FOLDER = "jmh_benchmark_result";
  private static String BUILD_PROJECT_NAME;

  @DataBoundConstructor
  public BenchmarkPublisher( String outputFilePath, int decimalPlaces )
  {
    _outputFilePath = outputFilePath;
    _decimalPlaces = decimalPlaces;
  }

  public String getOutputFilePath()
  {
    return _outputFilePath;
  }

  public int getDecimalPlaces()
  {
    return _decimalPlaces;
  }

  @Override
  public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener )
    throws IOException, InterruptedException
  {
    BUILD_PROJECT_NAME = build.getProject().getName();

    File outFile = new File( _outputFilePath );

    File masterCopy = copyBenchmarkOutputToMaster( build, outFile, BUILD_PROJECT_NAME );
    // currently only a CSV parser is supported
    _parser = new CsvParser();
    BenchmarkReport parsedReport = _parser.parse( build, masterCopy, listener );

    // get previous build report to calculate the increase in mean value for each benchmark
    // and set the change indicator (i.e. an up or down arrow)
    AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
    if ( previousBuild != null )
    {
      BenchmarkBuildAction previousBuildAction = previousBuild.getAction( BenchmarkBuildAction.class );
      if ( previousBuildAction != null )
      {
        BenchmarkReport prevPerfReport = previousBuildAction.getBuildActionDisplay().getJmhPerfReport();
        Map<String, BenchmarkResult> apiTestReport = parsedReport.getApiTestReport();
        Map<String, BenchmarkResult> prevApiTestReport = prevPerfReport.getApiTestReport();

        for ( Map.Entry<String, BenchmarkResult> entry : apiTestReport.entrySet() )
        {
          String key = entry.getKey();
          BenchmarkResult currVal = entry.getValue();
          BenchmarkResult prevVal = prevApiTestReport.get( key );
          // decrease in mean from previous is calculated as ((prev - curr)/prev) * 100%
          double decreaseInMeanFromPrev = ( 1 - currVal.getMean() / prevVal.getMean() ) * 100.0;
          // two decimal places
          int multiplier = (int) Math.pow( 10, 2 );
          decreaseInMeanFromPrev = (double) Math.round( decreaseInMeanFromPrev * multiplier ) / multiplier;
          currVal.setMeanChangeFromPrev( decreaseInMeanFromPrev );
          if ( decreaseInMeanFromPrev >= 20 )
          {
            currVal.setChangeIndicator( "green" );
          }
          else if ( decreaseInMeanFromPrev <= -20 )
          {
            currVal.setChangeIndicator( "red" );
          }
        }
      }
    }

    BenchmarkBuildAction buildAction = new BenchmarkBuildAction( build, parsedReport, _decimalPlaces );
    build.addAction( buildAction );
    return true;
  }

  private File copyBenchmarkOutputToMaster( AbstractBuild<?, ?> build, File output, String projectFolderName )
    throws IOException, InterruptedException
  {
    File localReport = getPerformanceReport( build, projectFolderName, output.getName() );

    FilePath src = new FilePath( output );
    src.copyTo( new FilePath( localReport ) );
    return localReport;
  }

  public static File getPerformanceReport( AbstractBuild<?, ?> build, String projectFolderName,
                                           String benchmarkOutputFileName )
  {
    return new File( build.getRootDir(), getRelativePath( projectFolderName, benchmarkOutputFileName ) );
  }

  private static String getRelativePath( String... suffixes )
  {
    StringBuilder sb = new StringBuilder( 100 );
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
   * Descriptor for {@link BenchmarkPublisher}. Used as a singleton. The class is marked as public so that it can be
   * accessed from views.
   * <p>
   * See <tt>src/main/resources/org/jenkinsci/plugins/jmhbenchmark/BenchmarkPublisher/*.jelly</tt> for the actual HTML
   * fragment for the configuration screen.
   */
  @Extension
  // This indicates to Jenkins that this is an implementation of an extension point.
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

  }

  public BuildStepMonitor getRequiredMonitorService()
  {
    return BuildStepMonitor.BUILD;
  }
}
