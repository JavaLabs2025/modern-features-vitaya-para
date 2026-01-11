package org.lab;

import org.lab.model.*;
import org.lab.service.SmartPermissionChecker;
import org.lab.service.ProjectManagementService;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        var service = new ProjectManagementService();
        var manager = service.registerUser("alice", "alice@company.com", "Alice Manager");
        var teamLead = service.registerUser("bob", "bob@company.com", "Bob TeamLead");
        var developer = service.registerUser("charlie", "charlie@company.com", "Charlie Dev");
        var project = service.createProject("E-Commerce", "Online store", manager.id());
        service.addTeamMember(project.id(), developer.id(), new Role.Developer(), manager.id());

        var ticket1 = service.createTicket("Implement checkout", "Add checkout flow", project.id(), null, manager.id());
        var ticket2 = service.createTicket("Fix payment bug", "Payment processing issue", project.id(), null, manager.id());

        service.assignDevelopersToTicket(ticket1.id(), Set.of(developer.id()), manager.id());
        service.assignDevelopersToTicket(ticket2.id(), Set.of(developer.id()), manager.id());
        service.updateTicketStatus(ticket2.id(), TicketStatus.ACCEPTED, manager.id());
        service.updateTicketStatus(ticket2.id(), TicketStatus.IN_PROGRESS, developer.id());
        service.updateTicketStatus(ticket2.id(), TicketStatus.COMPLETED, developer.id());

        ticket1 = service.getTicket(ticket1.id()).orElseThrow();
        ticket2 = service.getTicket(ticket2.id()).orElseThrow();

        System.out.println(SmartPermissionChecker.canModifyTicket(manager, new Role.Manager(), ticket2));
        System.out.println(SmartPermissionChecker.canModifyTicket(teamLead, new Role.TeamLeader(), ticket2));
        System.out.println(SmartPermissionChecker.canModifyTicket(developer, new Role.Developer(), ticket1));
        System.out.println(SmartPermissionChecker.canModifyTicket(developer, new Role.Developer(),
            service.createTicket("New feature", "Description", project.id(), null, manager.id())));

        var criticalBug = service.createBugReport("System crash", "Application crashes on startup", project.id(), developer.id(), "critical");
        var lowBug = service.createBugReport("Typo in UI", "Small text issue", project.id(), developer.id(), "low");

        System.out.println(SmartPermissionChecker.calculatePriority(criticalBug));
        service.assignBugReport(criticalBug.id(), developer.id(), manager.id());
        service.updateBugReportStatus(criticalBug.id(), BugReportStatus.FIXED, developer.id());
        System.out.println(SmartPermissionChecker.calculatePriority(service.getBugReport(criticalBug.id()).orElseThrow()));
        System.out.println(SmartPermissionChecker.calculatePriority(lowBug));

        var milestone1 = service.createMilestone("Sprint 1", "First sprint", project.id(),
            java.time.LocalDate.now(), java.time.LocalDate.now().plusWeeks(2), manager.id());

        System.out.println(SmartPermissionChecker.canActivateMilestone(milestone1, 2));
        System.out.println(SmartPermissionChecker.canActivateMilestone(milestone1, 5));
        milestone1.addTicket(ticket1.id());
        System.out.println(SmartPermissionChecker.canActivateMilestone(milestone1, 5));
    }
}
