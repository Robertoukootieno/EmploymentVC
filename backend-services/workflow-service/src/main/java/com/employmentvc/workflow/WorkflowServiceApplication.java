package com.employmentvc.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Workflow Service - Employment Lifecycle Orchestration
 * 
 * Responsibilities:
 * - Orchestrate employment verification workflows
 * - Coordinate credential issuance, verification, and revocation
 * - Manage employment lifecycle state machines
 * - Event-driven workflow automation
 * - Integration with external HR systems
 * 
 * Architecture Pattern: CQRS + Event Sourcing
 * Communication: REST API (external) + Event Bus (internal)
 */
@SpringBootApplication
public class WorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowServiceApplication.class, args);
    }
}
