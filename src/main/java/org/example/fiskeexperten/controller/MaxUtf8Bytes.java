package org.example.fiskeexperten.controller;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Texten får högst vara {@link #value()} byte lång i UTF-8.
 * <p>
 * Behövs för lösenord eftersom BCrypt bara klarar 72 <em>byte</em>, inte 72 tecken:
 * å, ä och ö tar 2 byte var, så {@code @Size(max = 72)} släpper igenom lösenord som är för långa.
 * {@code null} godkänns här (kombinera med {@code @NotBlank} för att kräva värde).
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "Texten är för lång";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {

        private int max;

        @Override
        public void initialize(MaxUtf8Bytes constraint) {
            this.max = constraint.value();
        }

        @Override
        public boolean isValid(String text, ConstraintValidatorContext context) {
            return text == null || text.getBytes(StandardCharsets.UTF_8).length <= max;
        }
    }
}
