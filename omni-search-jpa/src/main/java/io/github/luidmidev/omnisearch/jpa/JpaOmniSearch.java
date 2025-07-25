package io.github.luidmidev.omnisearch.jpa;

import io.github.luidmidev.omnisearch.core.OmniSearch;
import io.github.luidmidev.omnisearch.core.OmniSearchBaseOptions;
import io.github.luidmidev.omnisearch.core.OmniSearchOptions;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


@Slf4j
@RequiredArgsConstructor
public class JpaOmniSearch implements OmniSearch {

    private final EntityManager em;

    @Override
    public <E> List<E> search(Class<E> entityClass, OmniSearchOptions options) {
        var spec = JpaOmniSearchPredicateBuilder.buildSearchWhereSpec(em, entityClass, options);

        var cb = spec.criteriaBuilder();
        var query = spec.criteriaQuery();
        var root = spec.root();

        var sort = options.getSort();
        if (sort.isSorted()) {
            query.orderBy(sort.getOrders().stream()
                    .map(order -> {
                        var path = root.get(order.getProperty());
                        return order.isAscending() ? cb.asc(path) : cb.desc(path);
                    })
                    .toList()
            );
        }

        var pagination = options.getPagination();
        if (pagination.isUnpaginated()) {
            return em.createQuery(query).getResultList();
        }

        return em
                .createQuery(query)
                .setFirstResult(pagination.getOffset())
                .setMaxResults(pagination.getPageSize())
                .getResultList();
    }

    @Override
    public <E> long count(Class<E> entityClass, OmniSearchBaseOptions options) {
        var spec = JpaOmniSearchPredicateBuilder.buildSearchWhereSpec(em, Long.class, entityClass, options);

        var cb = spec.criteriaBuilder();
        var query = spec.criteriaQuery();
        var root = spec.root();

        query.select(cb.count(root));
        return em.createQuery(query).getSingleResult();
    }
}
