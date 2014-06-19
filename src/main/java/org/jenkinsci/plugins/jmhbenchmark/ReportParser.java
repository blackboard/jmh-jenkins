package org.jenkinsci.plugins.jmhbenchmark;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;

public abstract class ReportParser
{
  public abstract BenchmarkReport parse( AbstractBuild<?, ?> build, File report, TaskListener listener )
    throws IOException;

}
