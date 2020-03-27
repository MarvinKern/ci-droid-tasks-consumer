package com.societegenerale.cidroid.tasks.consumer.services.model.gitLab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.societegenerale.cidroid.tasks.consumer.services.model.PullRequestEvent;
import com.societegenerale.cidroid.tasks.consumer.services.model.github.Repository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class MergeRequestEvent extends PullRequestEvent {

    private String action;

    @JsonProperty("number")
    private int prNumber;

    private Repository repository;

    @Override
    public Repository getRepository() {
        return repository;
    }

}
