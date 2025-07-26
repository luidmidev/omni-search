package io.github.luidmidev.omnisearch.core;

import io.github.luidmidev.omnisearch.core.schemas.Pagination;
import io.github.luidmidev.omnisearch.core.schemas.Sort;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;


@Getter
public class OmniSearchOptions extends OmniSearchBaseOptions {

    private Sort sort = Sort.unsorted();
    private Pagination pagination = Pagination.unpaginated();

    @Override
    public OmniSearchOptions search(String search) {
        return (OmniSearchOptions) super.search(search);
    }

    @Override
    public OmniSearchOptions joins(@NotNull Set<String> joins) {
        return (OmniSearchOptions) super.joins(joins);
    }

    @Override
    public OmniSearchOptions joins(String @NotNull ... joins) {
        return (OmniSearchOptions) super.joins(joins);
    }

    @Override
    public OmniSearchOptions conditions(Node conditions) {
        return (OmniSearchOptions) super.conditions(conditions);
    }

    public OmniSearchOptions sort(@NotNull Sort sort) {
        this.sort = sort;
        return this;
    }

    public OmniSearchOptions sort(Sort.Order @NotNull ... orders) {
        return sort(new Sort(orders));
    }

    public OmniSearchOptions sort(@NotNull List<Sort.Order> orders) {
        return sort(new Sort(orders));
    }

    public OmniSearchOptions pagination(@NotNull Pagination page) {
        this.pagination = page;
        return this;
    }

    public OmniSearchOptions pagination(int pageNumber, int pageSize) {
        return pagination(new Pagination(pageNumber, pageSize));
    }

}
