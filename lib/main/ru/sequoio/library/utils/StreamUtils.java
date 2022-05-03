package ru.sequoio.library.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {

    public static <T> Stream<IndexedEntry<T>> indexate(Stream<T> s) {
        AtomicInteger integer = new AtomicInteger(0);
        return s.map(el -> new IndexedEntry<>(integer.getAndIncrement(), el))
                .collect(Collectors.toList()).stream();
    }

    public static class IndexedEntry<V> {

        private int index;
        private V value;

        public IndexedEntry(int index, V value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }
}
