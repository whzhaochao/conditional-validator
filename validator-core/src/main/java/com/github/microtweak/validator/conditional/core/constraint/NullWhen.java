package com.github.microtweak.validator.conditional.core.constraint;

import com.github.microtweak.validator.conditional.core.ValidateAs;

import javax.validation.Payload;
import javax.validation.constraints.Null;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Conditional version of constraint {@link Null @Null}
 */
@ValidateAs(Null.class)
@Repeatable(NullWhen.List.class)
@Documented
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface NullWhen {

	String expression();

	String message() default "{javax.validation.constraints.Null.message}";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	/**
	 * Conditional version of constraint {@link Null.List @Null.List}
	 */
	@Documented
	@Target({ METHOD, FIELD })
	@Retention(RUNTIME)
	@interface List {
		NullWhen[] value();
	}
}
