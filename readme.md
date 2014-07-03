
### JMH Benchmark Jenkins Plugin ###

The JMH Benchmark plugin allows you to integrate a JMH benchmark result with jenkins. It publishes the result to each build and provides a visualization for trending build data. It also compares performance gain/loss between a build and a previous build. For details of what a JMH benchmark is, see http://openjdk.java.net/projects/code-tools/jmh/.

### CI with JMH Benchmark Jenkins Plugin ###

For Continuous Integration (CI), the JMH Benchmark plugin is intended to be used in a post-build action to publish a build output, which in this case is the benchmark result. The following is how it can be used in a Jenkins integration.
1. As a build step, the JMH benchmark tests are run using a build automation tool such as Ant or Gradle. The test results are saved as a CSV format into a local file where the file location must be specified. 
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Currently, it accepts two input parameters. The first one is "Path to Benchmark Output File", which is the full path the benchmark result is saved. The second one is the number of decimal places used in the benchmark report.

The plugin provides links to view:
a) the benchmark output in a tabular form for a given build. In addition to the benchmark report, data on the the percentage gain/loss of each benchmark is given in comparison to the previous build.
b) trending data in a visual form for each benchmark over a specified number past builds

*Note:* currently, the plugin doesn't fail a build or make it unstable based on the benchmark test result. But, this is the feature we want to add to the plugin soon. With this feature, impacted code committers can be notified when builds fail or unstable.

### CI for Blackboard JMH Benchmark ###

Gradle is the build automation tool used at Blackboard. We have created a JMH Gradle plugin to make it very simple to run JMH Benchmark tests. For details of the JMH Gradle plugin, see https://stash.cloud.local/projects/PERF/repos/jmh-gradle-plugin/browse. The detailed steps for configuring a Jenkins Job for a benchmark CI is as follows.
1. As a build step, the following values can be used for the different parameters of the builds step that's based on gradle. Note that this configuration will run all the benchmarks created in the apis/platform project.
** Switches: `-P-rf=csv -P-rff="C:\temp\jmhreport\jmh_result.csv" -P-jvmArgs="-Dbbservices_config=C:/bb/blackboard/config/service-config-unittest.properties"` 
** Tasks: `benchmarkJmh`
** Build File: `C:\p4\LS\mainline\projects\apis\platform\build.gradle`
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. The following values can be used for the different parameters in this step.
** Path to Benchmark Output File: `C:\temp\jmhreport\jmh_result.csv`
** Decimal Places in Benchmark Report: `4`
