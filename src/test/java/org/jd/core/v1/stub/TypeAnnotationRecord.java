package org.jd.core.v1.stub;

/** A record component whose type has an annotation which is only for types: the class file has it on the component, nowhere else */
public record TypeAnnotationRecord(@Maybe Object value, int count) {
}
