/*
 * Created on 23-Dec-2004
 * 
 * (c) 2003-2004 ThoughtWorks Ltd
 *
 * See license.txt for license details
 */
package com.thoughtworks.jbehave.story.verifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.thoughtworks.jbehave.core.Verify;
import com.thoughtworks.jbehave.core.minimock.Mock;
import com.thoughtworks.jbehave.core.minimock.UsingMiniMock;
import com.thoughtworks.jbehave.story.domain.World;
import com.thoughtworks.jbehave.story.domain.Outcome;
import com.thoughtworks.jbehave.story.domain.Scenario;
import com.thoughtworks.jbehave.story.result.ScenarioResult;
import com.thoughtworks.jbehave.story.visitor.Visitor;

/**
 * @author <a href="mailto:ekeogh@thoughtworks.com">Elizabeth Keogh</a>
 */
public class VisitingScenarioVerifierBehaviour extends UsingMiniMock {
    private VisitingScenarioVerifier verifier;
    private Mock scenario;
    private World worldStub;
    
    public void setUp() {
        worldStub = (World)stub(World.class);
        verifier = new VisitingScenarioVerifier("story", worldStub);
        scenario = mock(Scenario.class);
    }
    
    public void shouldDispatchItselfAsVisitorToScenario() throws Exception {
        // given...
        // expect...
        scenario.expects("accept").with(verifier);
        
        // when...
        verifier.verify((Scenario)scenario);
    }
    
    public void shouldVerifyExpectationInEnvironment() throws Exception {
        // given...
        Mock expectation = mock(Outcome.class);

        // expect...
        expectation.expects("verify").with(same(worldStub));
        
        // when...
        verifier.visitExpectation((Outcome)expectation);
    }
        
    public void shouldReturnResultUsingMocksWhenScenarioSucceedsButExpectationUsesMocks() throws Exception {
        // expect...
        Mock expectation = mock(Outcome.class);
        expectation.expects("containsMocks").will(returnValue(true));
        scenario.expects("accept").will(visitExpectation((Outcome) expectation));
        
        // when...
        ScenarioResult result = verifier.verify((Scenario)scenario);
        
        // verify...
        Verify.that("should have used mocks", result.usedMocks());
    }
    
    /** Custom invocation handler so a Scenario can pass a component to the visitor */
    private InvocationHandler visitExpectation(final Outcome expectation) {
        return new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (method.getName().equals("accept")) {
                    Visitor visitor = (Visitor) args[0];
                    visitor.visitExpectation(expectation);
                }
                return null;
            }
        };
    }
}
