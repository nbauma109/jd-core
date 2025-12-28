package org.jd.core.v1;

public record RecordWithAnnotations(@Sensitive("C1") String a, @Sensitive("C0") double d) {
}
