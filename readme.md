### JMH Benchmark Jenkins Plugin ###

The JMH Benchmark plugin allows you to integrate a JMH benchmark result with jenkins by publishing the result to each build and provides a visualization for trending build data. It also compares performance degradation/gain between a given build and the previous and baseline builds. 

JMH is a Java harness for building, running, and analyzing nano/micro/milli/macro benchmarks written in Java and other languages targetting the JVM. For more details about JMH, see http://openjdk.java.net/projects/code-tools/jmh/.

### Continuous Integration with the JMH Benchmark Jenkins Plugin ###

1. As a build step, the JMH benchmark tests are run using a build automation tool such as Gradle. The test results are saved as a CSV format into a local file where the file location is specified relative to the `WORKSPACE` of the Jenkins project. The raw benchmark result in the CSV format is also copied to the master. As an example, if you use the JMH Gradle plugin, available in https://github.com/blackboard/jmh-gradle-plugin, here is how you may configure the build step.
    * *Switches*: `-P-rf=csv -P-rff="${WORKSPACE}/learn-apis-platform_mainline-jmh-benchmark.csv"`
    * *Tasks*: `benchmarkJmh`
    * *Build File*: `mainline/projects/build.gradle`
2. As a post-build action, the JMH Benchmark plugin will post the benchmark results to each build. Currently, the configuration accepts four input parameters: 
    * *Baseline Build Number* - the build number that will be used as a baseline. `0` is the default value if no baseline exists..
    * *Performance Degradation Threshold (in %)* - this threshold applies between the current and previous successful build as well the current and baseline build if the latter is specified. The default threshold is -20%.
    * *Performance Increase Threshold (in %)* - this threshold is an indicator for a performance improvement in the current build compared to the previous successful build and the baseline build if baseline is defined. The default threshold is +20%
    * *Decimal Places in Benchmark Report* - the number of decimal places used in the benchmark report. 

The plugin provides the following two links to view the build and trend data:  
 
 * *JMH Benchmark Report* - this is accessed for each build and the benchmark output is available in a tabular form for a given build. In addition to the benchmark report, data on the the percentage gain/loss of each benchmark is given in comparison to the previous and baseline builds.
 * *JMH Report Trend* - this is accessed from the project page. This report trends data in a visual form for each benchmark over a specified number of past builds.

*Note:* currently, the plugin can mark a build as unstable if at least one benchmark has a performance less than the degradation threshold. But, the plugin doesn't fail a build based on the benchmark test result.


### Testing the plugin in Jenkins ###

* Download the source and build it: `$ mvn clean install`. *jmhbenchmark.hpi* is created under the target folder. 
*  Install `jmhbenchmark.hpi` in Jenkins.

### Development ###

To import the project in Eclipse and develop:

* Run `$ mvn -DdownloadSources=true -DdownloadJavadocs=true -DoutputDirectory=target/eclipse-classes eclipse:eclipse`
* Use "Import..." (under the File menu in Eclipse) and select "General" > "Existing Projects into Workspace". 
* Install Maven Integration for Eclipse plugin. 