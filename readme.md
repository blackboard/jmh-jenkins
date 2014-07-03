### JMH Benchmark Jenkins Plugin ###

The JMH Benchmark plugin allows you to integrate a JMH benchmark result with jenkins. For details about JMH, see http://openjdk.java.net/projects/code-tools/jmh/. JMH is a Java harness for building, running, and analysing nano/micro/milli/macro benchmarks written in Java and other languages targetting the JVM.

The JMH Benchmark plugin publishes the benchmark result to each build and provides a visualization for trending build data. It also compares performance gain/loss between a given build and the previous build.

### CI with JMH Benchmark Jenkins Plugin ###

The JMH Benchmark plugin can be used for creating a performance Continuous Integration (CI).

1. As a build step, the JMH benchmark tests are run using a build automation tool such as Ant or Gradle. The test results are saved as a CSV format into a local file where the file location must be specified. 
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Currently, it accepts two input parameters. The first one is "Path to Benchmark Output File", which is the full path the benchmark result is saved. The second one is the number of decimal places used in the benchmark report. The plugin provides links to view:
 * the benchmark output in a tabular form for a given build. In addition to the benchmark report, data on the the percentage gain/loss of each benchmark is given in comparison to the previous build.
 * trending data in a visual form for each benchmark over a specified number of past builds

*Note:* currently, the plugin doesn't fail a build or make it unstable based on the benchmark test result. But, this is the feature we want to add soon. With this feature, impacted code committers can be notified when builds fail or become unstable.

### CI for Blackboard JMH Benchmark ###

Gradle is the build automation tool used at Blackboard. We have created a JMH Gradle plugin to simplify running JMH Benchmark tests. For details of the JMH Gradle plugin, see https://stash.cloud.local/projects/PERF/repos/jmh-gradle-plugin/browse. Steps for configuring a Jenkins Job for a benchmark CI is as follows.

1. As a build step, use the following values for the different parameters. Update the values based on your environment. Note that this configuration will run all the benchmarks created in the _apis/platform_ project.
 * Switches: `-P-rf=csv -P-rff="C:\temp\jmhreport\jmh_result.csv" -P-jvmArgs="-Dbbservices_config=C:/bb/blackboard/config/service-config-unittest.properties"` 
 * Tasks: `benchmarkJmh`
 * Build File: `C:\p4\LS\mainline\projects\apis\platform\build.gradle`
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Use the below values for the different parameters.
 * Path to Benchmark Output File: `C:\temp\jmhreport\jmh_result.csv`
 * Decimal Places in Benchmark Report: `4`