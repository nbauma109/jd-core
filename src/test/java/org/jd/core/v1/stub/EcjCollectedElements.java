/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 'ModuleBinding.combineWithPackagesFromOtherRelevantModules' of the Eclipse compiler: the elements which are collected are of a subtype of the ones of the list
 */
public class EcjCollectedElements {
    static class Name {
    }

    static class PlainName extends Name {
    }

    PlainName plain(String text) {
        return new PlainName();
    }

    List<Name> names(List<String> texts) {
        List<Name> names = texts.stream()
                .map(text -> plain(text))
                .collect(Collectors.toList());

        names.add(new Name());
        return names;
    }

    int distinct(List<String> texts) {
        Set<Name> names = texts.stream()
                .map(text -> plain(text))
                .collect(Collectors.toSet());

        names.add(new Name());
        return names.size();
    }

    List<PlainName> plains(List<String> texts) {
        return texts.stream()
                .map(text -> plain(text))
                .collect(Collectors.toList());
    }

    /** the casts of the other calls stay */
    interface Sink {
        <T> T collect(Collector<PlainName, ?, T> collector);
    }

    List<PlainName> fromASink(Sink sink) {
        return sink.collect(Collectors.toList());
    }

    List<PlainName> withAGivenCollector(Stream<PlainName> stream, Collector<PlainName, ?, List<PlainName>> collector) {
        return stream.collect(collector);
    }

    List<PlainName> withACollectorOfTheClass(Stream<PlainName> stream) {
        return stream.collect(collector());
    }

    ArrayList<PlainName> toACollection(Stream<PlainName> stream) {
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }

    Collector<PlainName, ?, List<PlainName>> collector() {
        return Collectors.toList();
    }
}
