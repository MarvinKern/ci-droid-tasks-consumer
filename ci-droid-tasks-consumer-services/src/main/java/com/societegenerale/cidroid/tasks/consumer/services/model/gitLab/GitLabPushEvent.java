package com.societegenerale.cidroid.tasks.consumer.services.model.gitLab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.societegenerale.cidroid.tasks.consumer.services.model.PushEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.Repository;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabPushEvent extends PushEvent {

    private Repository repository;

    private GitLabProject project;

    @Override
    public Repository getRepository() {
        return repository;
    }

}
