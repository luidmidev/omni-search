package io.github.luidmidev.omnisearch.core;

import java.util.List;
import java.util.function.Consumer;

public interface OmniSearch {

    <E> List<E> search(Class<E> entityClass, OmniSearchOptions options);

    default <E> List<E> search(Class<E> entityClass, Consumer<OmniSearchOptions> optionsConsumer) {
        var options = new OmniSearchOptions();
        optionsConsumer.accept(options);
        return search(entityClass, options);
    }

    <E> long count(Class<E> entityClass, OmniSearchBaseOptions options);

    default <E> long count(Class<E> entityClass, Consumer<OmniSearchBaseOptions> optionsConsumer) {
        var options = new OmniSearchBaseOptions();
        optionsConsumer.accept(options);
        return count(entityClass, options);
    }
}