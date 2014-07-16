### JMH Benchmark Jenkins Plugin ###

The JMH Benchmark plugin allows you to integrate a JMH benchmark result with jenkins. JMH is a Java harness for building, running, and analysing nano/micro/milli/macro benchmarks written in Java and other languages targetting the JVM. For more details about JMH, see http://openjdk.java.net/projects/code-tools/jmh/.

The JMH Benchmark plugin publishes the benchmark result to each build and provides a visualization for trending build data. It also compares performance degradation/gain between a given build and the previous build.

### CI with JMH Benchmark Jenkins Plugin ###

1. As a build step, the JMH benchmark tests are run using a build automation tool such as Ant or Gradle. The test results are saved as a CSV format into a local file where the file location must be specified. 
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Currently, the configuration accepts four input parameters:
 * `Path to Benchmark Output File` - the full path the benchmark result.
 * `Relative Performance Degradation Threshold Indicator (in %)`
 * `Relative Performance Increase Threshold Indicator (in %)`
 * `Decimal Places in Benchmark Report` - the number of decimal places used in the benchmark report. 

The plugin provides links to view:
 * the benchmark output in a tabular form for a given build. In addition to the benchmark report, data on the the percentage gain/loss of each benchmark is given in comparison to the previous build.
 * trending data in a visual form for each benchmark over a specified number of past builds

*Note:* currently, the plugin can mark a build as unstable if at least one benchmark has a performance less than the degradation threshold. But, the plugin doesn't fail a build based on the benchmark test result.

### CI for Blackboard JMH Benchmark ###

Gradle is the build automation tool used at Blackboard. We have created a JMH Gradle plugin to simplify running JMH Benchmark tests. For details of the JMH Gradle plugin, see https://stash.cloud.local/projects/PERF/repos/jmh-gradle-plugin/browse. Steps for configuring a Jenkins Job for a benchmark CI is as follows.

1. As a build step, use the following values for the different parameters. Update the values based on your environment. Note that this configuration will run all the benchmarks created in the _apis/platform_ project.
 * Switches: `--stacktrace --no-daemon -DjenkinsBuild -P-f=1 -P-rf=csv -P-rff="/tmp/apis_platform_jmh_result.csv" -P-jvmArgs="-Dbbservices_config=/usr/local/blackboard/config/service-config-unittest.properties"`
	 * if you need to exlude a benchmark from the CI, use the `e` option with a regex pattern to teh benchmark. Example is: `-P-e=".*BenchmarkToExclude.*"` 
 * Tasks: `benchmarkJmh`
 * Build File: `mainline/projects/build.gradle`
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Use the below values for the different parameters.
 * Path to Benchmark Output File: `/tmp/apis_platform_jmh_result.csv`
 * Relative Performance Degradation Threshold Indicator (in %): `-20`
 * Relative Performance Increase Threshold Indicator (in %): `20`
 * Decimal Places in Benchmark Report: `4`

### Development ###

To test the plugin in Jenkins:

* download the source and build it: `$ mvn clean install`
* `jmhbenchmark.hpi` is created under the target folder. 
*  Install `jmhbenchmark.hpi` in Jenkins

To import the project in Eclipse and develop:

* Run `$ mvn -DdownloadSources=true -DdownloadJavadocs=true -DoutputDirectory=target/eclipse-classes eclipse:eclipse`
* Use "Import..." (under the File menu in Eclipse) and select "General" > "Existing Projects into Workspace". 
* Install Maven Integration for Eclipse plugin 