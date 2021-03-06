package com.societegenerale.cidroid.tasks.consumer.services;

import com.societegenerale.cidroid.tasks.consumer.services.eventhandlers.PushEventHandler;
import com.societegenerale.cidroid.tasks.consumer.services.model.PushEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.PRmergeableStatus;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.PullRequest;
import com.societegenerale.cidroid.tasks.consumer.services.monitoring.Event;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;

import static com.societegenerale.cidroid.tasks.consumer.services.monitoring.MonitoringAttributes.REPO;
import static com.societegenerale.cidroid.tasks.consumer.services.monitoring.MonitoringEvents.PUSH_EVENT_TO_PROCESS;
import static java.util.stream.Collectors.toList;

@Slf4j
public class PushEventOnDefaultBranchService {

    private RemoteSourceControl gitHub;

    private List<PushEventHandler> actionHandlers;

    @Setter
    private long sleepDurationBeforeTryingAgainToFetchMergeableStatus = 300;

    @Setter
    private int maxRetriesForMergeableStatus = 10;

    public PushEventOnDefaultBranchService(RemoteSourceControl gitHub, List<PushEventHandler> pushEventHandlers) {

        this.gitHub = gitHub;
        this.actionHandlers = pushEventHandlers;
    }

    public void onPushEvent(PushEvent pushEvent) {

        if (shouldNotProcess(pushEvent)) {
            return;
        }

        Event techEvent = Event.technical(PUSH_EVENT_TO_PROCESS);
        techEvent.addAttribute(REPO, pushEvent.getRepository().getFullName());

        StopWatch stopWatch = StopWatch.createStarted();

        List<PullRequest> openPRs = retrieveOpenPrs(pushEvent.getRepository().getFullName());

        List<PullRequest> openPRsWithDefinedMergeabilityStatus = figureOutMergeableStatusFor(openPRs, 0);

        if (log.isInfoEnabled()) {
            logPrMergeabilityStatus(openPRsWithDefinedMergeabilityStatus);
        }

        for (PushEventHandler pushEventHandler : actionHandlers) {

            try {
                pushEventHandler.handle(pushEvent, openPRsWithDefinedMergeabilityStatus);
            } catch (RuntimeException e) {
                log.warn("exception thrown during event handling by " + pushEventHandler.getClass(), e);
            }
        }

        stopWatch.stop();
        techEvent.addAttribute("processTime", String.valueOf(stopWatch.getTime()));
        techEvent.publish();

    }

    private boolean shouldNotProcess(PushEvent pushEvent) {

        if (!pushEvent.happenedOnDefaultBranch()) {
            log.warn("received an event from branch that is not default, ie {} - how is it possible ? ", pushEvent.getRef());
            return true;
        }

        return false;
    }

    private void logPrMergeabilityStatus(List<PullRequest> openPRsWithDefinedMergeabilityStatus) {
        if (openPRsWithDefinedMergeabilityStatus.size() > 0) {

            StringBuilder sb = new StringBuilder("PR status :\n");

            for (PullRequest pr : openPRsWithDefinedMergeabilityStatus) {
                sb.append("\t- PR #").append(pr.getNumber()).append(" : ").append(pr.getMergeStatus()).append("\n");
            }

            log.info(sb.toString());
        }
    }

    private List<PullRequest> figureOutMergeableStatusFor(List<PullRequest> openPRs, int nbRetry) {

        List<PullRequest> pullRequestsWithDefinedMergeabilityStatus = openPRs.stream()
                .filter(pr -> PRmergeableStatus.UNKNOWN != pr.getMergeStatus())
                .collect(toList());

        List<PullRequest> pullRequestsWithUnknownMergeabilityStatus = openPRs.stream()
                .filter(pr -> PRmergeableStatus.UNKNOWN == pr.getMergeStatus())
                .collect(toList());

        if (pullRequestsWithUnknownMergeabilityStatus.size() > 0 && nbRetry < maxRetriesForMergeableStatus) {

            if (log.isDebugEnabled()) {

                StringBuilder sb = new StringBuilder("these PRs don't have a mergeable status yet :\n");

                pullRequestsWithUnknownMergeabilityStatus
                        .forEach(pr -> sb.append("\t - ").append(pr.getNumber()).append("\n"));

                sb.append("waiting for ")
                        .append(sleepDurationBeforeTryingAgainToFetchMergeableStatus)
                        .append("ms before trying again for the ")
                        .append(nbRetry + 1)
                        .append("th time...");

                log.debug(sb.toString());
            }

            try {
                Thread.sleep(sleepDurationBeforeTryingAgainToFetchMergeableStatus);
            } catch (InterruptedException e) {
                log.error("interrupted while sleeping to get PR status", e);
            }

            List<PullRequest> prsWithUpdatedStatus = pullRequestsWithUnknownMergeabilityStatus.stream()
                    .map(pr -> gitHub.fetchPullRequestDetails(pr.getRepo().getFullName(), pr.getNumber()))
                    .collect(toList());

            pullRequestsWithDefinedMergeabilityStatus.addAll(figureOutMergeableStatusFor(prsWithUpdatedStatus, ++nbRetry));
        } else if (nbRetry >= maxRetriesForMergeableStatus) {

            log.warn("not able to retrieve merge status for below PRs after several tries.. giving up");
            pullRequestsWithUnknownMergeabilityStatus
                    .forEach(pr -> log.info("\t - {}", pr.getNumber()));
        }

        return pullRequestsWithDefinedMergeabilityStatus;
    }

    private List<PullRequest> retrieveOpenPrs(String repoFullName) {

        List<PullRequest> openPrs = gitHub.fetchOpenPullRequests(repoFullName);

        log.info("{} open PRs found on repo {}", openPrs.size(), repoFullName);

        return openPrs.stream()
                .map(pr -> gitHub.fetchPullRequestDetails(repoFullName, pr.getNumber()))
                .collect(toList());
    }


}
