package com.societegenerale.cidroid.tasks.consumer.infrastructure.config;

import com.societegenerale.cidroid.tasks.consumer.infrastructure.SourceControlEventListener;
import com.societegenerale.cidroid.tasks.consumer.services.model.PullRequestEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.gitLab.GitLabPushEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.gitLab.MergeRequestEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.GitHubPushEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Consumer;

@Configuration
@Profile("gitLab")
public class GitLabConfig {
    @Bean(name = "push-on-default-branch")
    public Consumer<GitLabPushEvent> msgConsumerPush(SourceControlEventListener actionToPerformListener) {

        return  event -> {actionToPerformListener.onPushEventOnDefaultBranch(event);};
    }

    @Bean(name = "push-on-non-default-branch")
    public Consumer<GitLabPushEvent> msgConsumerPushNonDefaultBranch(SourceControlEventListener actionToPerformListener) {

        return  event -> {actionToPerformListener.onPushEventOnNonDefaultBranch(event);};
    }

    @Bean(name = "merge-request-event")
    public Consumer<MergeRequestEvent> msgConsumerPREvent(SourceControlEventListener actionToPerformListener) {

        return  event -> {actionToPerformListener.onPullRequestEvent(event);};
    }
}
