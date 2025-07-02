package com.conversationalai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConversationalEntity {
    String value() default ""; // Default synonym for the entity
    String description() default ""; // Description for LLM context
    String[] searchableFields() default {}; // Fields that can be used for fuzzy search
    String[] uniqueFields() default {}; // Unique fields (e.g., ID) for UPDATE/DELETE
}