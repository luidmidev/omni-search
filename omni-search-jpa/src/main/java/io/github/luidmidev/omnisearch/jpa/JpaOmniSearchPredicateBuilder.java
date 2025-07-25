package io.github.luidmidev.omnisearch.jpa;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import com.github.tennaito.rsql.misc.EntityManagerAdapter;
import io.github.luidmidev.omnisearch.core.OmniSearchBaseOptions;
import io.github.luidmidev.omnisearch.core.OmniSearchOptions;
import io.github.luidmidev.omnisearch.core.SearchIgnore;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class JpaOmniSearchPredicateBuilder {

    /**
     * Pattern used to match UUID strings.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static List<Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, ReflectionUtils::getAllFields);
    }

    private static <E> Predicate searchInAllColumns(@NotNull String search, Root<E> root, CriteriaBuilder cb, Set<String> joinColumns) {

        var predicates = new ArrayList<>(getSearchPredicates(search, root, cb));

        for (var joinColumn : joinColumns) {
            var join = root.join(joinColumn, JoinType.LEFT);
            predicates.addAll(getSearchPredicates(search, join, cb));
        }

        return cb.or(predicates.toArray(Predicate[]::new));
    }

    @SuppressWarnings({"java:S135", "java:S3776"})
    private static Collection<Predicate> getSearchPredicates(String search, Path<?> path, CriteriaBuilder cb) {

        var javaType = path.getJavaType();
        var predicates = new ArrayList<Predicate>();

        for (var field : getCachedFields(javaType)) {

            if (field.isAnnotationPresent(SearchIgnore.class) || field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            var propertyName = field.getName();

            if (field.isAnnotationPresent(Embedded.class)) {
                var nextPath = path.get(propertyName);
                predicates.addAll(getSearchPredicates(search, nextPath, cb));
                continue;
            }

            if (field.isAnnotationPresent(ElementCollection.class) && path instanceof From<?, ?> from) {
                var nextPath = from.join(propertyName, JoinType.LEFT);
                predicates.addAll(getElementCollectionPredicates(search, nextPath, cb));
                continue;
            }

            try {
                var nextPath = path.get(propertyName);
                predicates.addAll(getBasicPredicates(search, nextPath, cb));
            } catch (IllegalArgumentException e) {
                log.debug("Field {} not found in {}", field.getName(), javaType.getName(), e);
            }

        }
        return predicates;
    }

    private static Collection<Predicate> getElementCollectionPredicates(String search, From<?, ?> from, CriteriaBuilder cb) {
        var javaType = from.getJavaType();

        if (javaType.isAnnotationPresent(Embeddable.class)) {
            return getSearchPredicates(search, from, cb);
        }

        return getBasicPredicates(search, from, cb);
    }

    private static Collection<Enum<?>> searchEnumCandidates(Class<? extends Enum<?>> enumType, String value) {
        var set = new HashSet<Enum<?>>();
        var constants = enumType.getEnumConstants();
        for (var constant : constants) {
            if (constant.name().toLowerCase().contains(value.toLowerCase())) {
                set.add(constant);
                continue;
            }
            if (constant instanceof JpaEnumSearchCandidate enumCandidate && enumCandidate.isCandidate(value)) {
                set.add(constant);
            }
        }
        return set;
    }

    @SuppressWarnings("java:S3776")
    private static Collection<Predicate> getBasicPredicates(String search, Path<?> path, CriteriaBuilder cb) {
        var javaType = path.getJavaType();

        if (String.class.isAssignableFrom(javaType)) {
            return Collections.singleton(cb.like(cb.lower(path.as(String.class)), "%" + search.toLowerCase() + "%"));
        }

        if (UUID.class.isAssignableFrom(javaType) && UUID_PATTERN.matcher(search).matches()) {
            return Collections.singleton(cb.equal(path, UUID.fromString(search)));
        }

        var isNumber = Number.class.isAssignableFrom(javaType.isPrimitive() ? ReflectionUtils.getWrapperType(javaType) : javaType);
        if (isNumber) {
            var numberPredicates = new ArrayList<Predicate>();
            for (var parser : ParseNumber.PARSERS) {
                var numberType = parser.type();
                if (javaType.isAssignableFrom(numberType) && search.matches("\\d+")) {
                    var converted = parser.parse(search);
                    numberPredicates.add(cb.equal(path, converted));
                }
            }
            return numberPredicates;
        }

        var isBoolean = Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType);
        if (isBoolean && search.matches("true|false")) {
            return Collections.singleton(cb.equal(path, Boolean.parseBoolean(search)));
        }

        if (Year.class.isAssignableFrom(javaType) && search.matches("\\d{4}")) {
            return Collections.singleton((cb.equal(path, Year.parse(search))));
        }

        if (javaType.isEnum()) {
            @SuppressWarnings("unchecked")
            var candidates = searchEnumCandidates((Class<? extends Enum<?>>) javaType, search);
            if (candidates.isEmpty()) return List.of();
            return Collections.singleton(path.in(candidates));
        }

        return Collections.emptyList();
    }

    <E> SearchQuery<E, E> buildSearchWhereSpec(
            EntityManager em,
            Class<E> entityClass,
            OmniSearchOptions options
    ) {
        return buildSearchWhereSpec(em, entityClass, entityClass, options);
    }

    <Q, E> SearchQuery<Q, E> buildSearchWhereSpec(
            EntityManager em,
            Class<Q> queryClass,
            Class<E> entityClass,
            OmniSearchBaseOptions options
    ) {

        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(queryClass);
        var root = query.from(entityClass);

        var predicate = buildPredicate(em, cb, root, options);

        query.where(predicate);
        return new SearchQuery<>(cb, query, root);
    }


    public static <M> Predicate buildPredicate(
            EntityManager em,
            CriteriaBuilder cb,
            Root<M> root,
            OmniSearchBaseOptions options
    ) {

        var predicate = cb.conjunction();

        var search = options.getSearch();
        var isNullOrEmpty = search == null || search.isBlank();
        if (!isNullOrEmpty) {
            predicate = searchInAllColumns(search, root, cb, options.getJoins());
        }

        var conditions = options.getConditions();
        if (conditions != null) {
            @SuppressWarnings("unchecked")
            var visitor = new JpaPredicateVisitor<M>().defineRoot(root);
            var filtersPredicate = conditions.accept(visitor, new EntityManagerAdapter(em));
            predicate = cb.and(predicate, filtersPredicate);
        }

        return predicate;
    }


    record SearchQuery<Q, M>(CriteriaBuilder criteriaBuilder, CriteriaQuery<Q> criteriaQuery, Root<M> root) {
    }

}
