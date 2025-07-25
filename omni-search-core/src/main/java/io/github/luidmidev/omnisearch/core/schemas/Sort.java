package io.github.luidmidev.omnisearch.core.schemas;


import lombok.Data;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Data
public class Sort {

    private static final Sort UNSORTED = new Sort(List.of());

    private final List<Order> orders;

    public static Sort unsorted() {
        return UNSORTED;
    }

    public boolean isSorted() {
        return !orders.isEmpty();
    }

    public boolean isUnsorted() {
        return !isSorted();
    }

    public Sort(Order... orders) {
        this.orders = Arrays.asList(orders);
    }

    public Sort(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    @Getter
    @Data
    public static class Order {
        private String property;
        private boolean ascending;

        public Order(String property, boolean ascending) {
            this.property = property;
            this.ascending = ascending;
        }
    }
}
