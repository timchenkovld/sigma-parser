package com.example.sigmaparser.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ParserUtils {

    public static <T> void execFor(Collection<T> collection, Consumer<T> processor) {
        for (T el : collection) {
            processor.accept(el);
        }
    }

    public static int getRandomTimeout() {
        return (int) (20 + Math.random() * 10);
    }

    public static <T> List<List<T>> divideList(List<T> list, int parts) {
        List<List<T>> dividedList = new ArrayList<>();
        int chunkSize = (int) Math.ceil((double) list.size() / parts);
        for (int i = 0; i < list.size(); i += chunkSize) {
            dividedList.add(new ArrayList<>(list.subList(i, Math.min(i + chunkSize, list.size()))));
        }
        return dividedList;
    }
}
