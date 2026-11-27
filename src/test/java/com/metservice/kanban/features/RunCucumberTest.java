package com.metservice.kanban.features;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Runs every feature under {@code src/test/resources/com/metservice/kanban/features} on the Cucumber engine. */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("com.metservice.kanban.features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.metservice.kanban.features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class RunCucumberTest {}
