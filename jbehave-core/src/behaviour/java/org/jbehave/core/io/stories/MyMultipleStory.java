package org.jbehave.core.io.stories;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jbehave.core.JUnitStory;
import org.jbehave.core.configuration.PropertyBasedStoryConfiguration;
import org.jbehave.core.reporters.StoryReporter;
import org.jbehave.core.reporters.TxtOutput;

public abstract class MyMultipleStory extends JUnitStory {
    public MyMultipleStory() {
        // Making sure this doesn't output to the build while it's running
        useConfiguration(new PropertyBasedStoryConfiguration() {
            @Override
            public StoryReporter storyReporter() {
                return new TxtOutput(new PrintStream(new ByteArrayOutputStream()));
            }
        });
    }
}
