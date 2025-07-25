package io.github.luidmidev.omnisearch.core;

import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


@Getter
public class OmniSearchBaseOptions {

    private String search = null;
    private Set<String> joins = Set.of();
    private Node conditions = null;

    public OmniSearchBaseOptions search(String search) {
        this.search = search;
        return this;
    }

    public OmniSearchBaseOptions joins(@NotNull Set<String> joins) {
        this.joins = joins;
        return this;
    }

    public OmniSearchBaseOptions joins(String @NotNull ... joins) {
        return joins(Set.of(joins));
    }

    public OmniSearchBaseOptions conditions(Node conditions) {
        this.conditions = conditions;
        return this;
    }
}
