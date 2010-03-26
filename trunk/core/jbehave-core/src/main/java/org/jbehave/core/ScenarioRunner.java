package org.jbehave.core;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.errors.ErrorStrategy;
import org.jbehave.core.errors.PendingError;
import org.jbehave.core.errors.PendingErrorStrategy;
import org.jbehave.core.reporters.ScenarioReporter;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.Step;
import org.jbehave.core.steps.StepCreator;
import org.jbehave.core.steps.StepResult;
import org.jbehave.core.steps.StepCreator.Stage;

/**
 * Runs the steps of each core in a story and describes the results to the {@link ScenarioReporter}.
 * 
 * @author Elizabeth Keogh
 * @author Mauro Talevi
 * @author Paul Hammant
 */
public class ScenarioRunner {

    private State state = new FineSoFar();
    private ErrorStrategy currentStrategy;
    private PendingErrorStrategy pendingStepStrategy;
    private ScenarioReporter reporter;
    private ErrorStrategy errorStrategy;
    private Throwable throwable;
    private StepCreator stepCreator;

    public void run(Class<? extends RunnableScenario> scenarioClass, Configuration configuration, CandidateSteps... candidateSteps) throws Throwable {
		Story story = configuration.forDefiningScenarios().loadScenarioDefinitionsFor(scenarioClass);
		story.namedAs(scenarioClass.getSimpleName());
	    // always start in a non-embedded mode
        run(story, configuration, false, candidateSteps);
    }

    public void run(String scenarioPath, Configuration configuration, boolean embeddedStory, CandidateSteps... candidateSteps) throws Throwable {
		Story story = configuration.forDefiningScenarios().loadScenarioDefinitionsFor(scenarioPath);
        story.namedAs(new File(scenarioPath).getName());
		run(story, configuration, embeddedStory, candidateSteps);
    }    

    public void run(Story story, Configuration configuration, boolean embeddedStory, CandidateSteps... candidateSteps) throws Throwable {
        stepCreator = configuration.forCreatingSteps();
        reporter = configuration.forReportingScenarios();
        pendingStepStrategy = configuration.forPendingSteps();
        errorStrategy = configuration.forHandlingErrors();
        currentStrategy = ErrorStrategy.SILENT;
        throwable = null;
        
        reporter.beforeStory(story, embeddedStory);          
        runStorySteps(story, embeddedStory, StepCreator.Stage.BEFORE, candidateSteps);
        for (Scenario scenario : story.getScenarios()) {
    		reporter.beforeScenario(scenario.getTitle());
        	runGivenScenarios(configuration, scenario, candidateSteps); // first run any given scenarios, if any
        	if ( isExamplesTableScenario(scenario) ){ // run examples table core
        		runExamplesTableScenario(configuration, scenario, candidateSteps);
        	} else { // run plain old core
            	runScenarioSteps(configuration, scenario, new HashMap<String, String>(), candidateSteps);        		
        	}
    		reporter.afterScenario();
        }
        runStorySteps(story, embeddedStory, StepCreator.Stage.AFTER, candidateSteps);
        reporter.afterStory(embeddedStory);            
        currentStrategy.handleError(throwable);
    }

    private void runGivenScenarios(Configuration configuration,
			Scenario scenario, CandidateSteps... candidateSteps)
			throws Throwable {
		List<String> givenScenarios = scenario.getGivenScenarios();
		if ( givenScenarios.size() > 0 ){
			reporter.givenScenarios(givenScenarios);
			for ( String scenarioPath : givenScenarios ){
			    // run in embedded mode
				run(scenarioPath, configuration, true, candidateSteps);
			}
		}
	}

	private boolean isExamplesTableScenario(Scenario scenario) {
		ExamplesTable table = scenario.getTable();
		return table != null && table.getRowCount() > 0;
	}

	private void runExamplesTableScenario(Configuration configuration,
			Scenario scenario, CandidateSteps... candidateSteps) {
		ExamplesTable table = scenario.getTable();
        reporter.beforeExamples(scenario.getSteps(), table);
		for (Map<String,String> tableRow : table.getRows() ) {
			reporter.example(tableRow);
			runScenarioSteps(configuration, scenario, tableRow, candidateSteps);
		}
		reporter.afterExamples();
	}

    private void runStorySteps(Story story, boolean embeddedStory, Stage stage, CandidateSteps... candidateSteps) {
        Step[] steps = stepCreator.createStepsFrom(story, stage, embeddedStory, candidateSteps);
        runSteps(steps);
    }

	private void runScenarioSteps(Configuration configuration,
			Scenario scenario, Map<String, String> tableRow, CandidateSteps... candidateSteps) {
        Step[] steps = stepCreator.createStepsFrom(scenario, tableRow, candidateSteps);
		runSteps(steps);		
	}

    /**
     * Runs a list of steps. 
     * 
     * @param steps the Steps to run
     */
    private void runSteps(Step[] steps) {
        if ( steps == null || steps.length == 0 ) return;
        state = new FineSoFar();
        for (Step step : steps) {
            state.run(step);
        }
    }
    
    private class SomethingHappened implements State {
        public void run(Step step) {
            StepResult result = step.doNotPerform();
            result.describeTo(reporter);
        }
    }

    private final class FineSoFar implements State {

        public void run(Step step) {

            StepResult result = step.perform();
            result.describeTo(reporter);
            Throwable thisScenariosThrowable = result.getThrowable();
            if (thisScenariosThrowable != null) {
                state = new SomethingHappened();
                throwable = mostImportantOf(throwable, thisScenariosThrowable);
                currentStrategy = strategyFor(throwable);
            }
        }

        private Throwable mostImportantOf(
                Throwable throwable1,
                Throwable throwable2) {
            return throwable1 == null ? throwable2 : 
                throwable1 instanceof PendingError ? (throwable2 == null ? throwable1 : throwable2) :
                    throwable1;
        }

        private ErrorStrategy strategyFor(Throwable throwable) {
            if (throwable instanceof PendingError) {
                return pendingStepStrategy;
            } else {
                return errorStrategy;
            }
        }
    }

    private interface State {
        void run(Step step);
    }
}
