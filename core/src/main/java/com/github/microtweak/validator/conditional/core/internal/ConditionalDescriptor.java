package com.github.microtweak.validator.conditional.core.internal;

import com.github.microtweak.validator.conditional.core.ConditionalConstraint;
import com.github.microtweak.validator.conditional.core.exception.InvalidConditionalExpressionException;
import com.github.microtweak.validator.conditional.core.spi.ConstraintDescriptorFactory;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import javax.el.ELProcessor;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import static com.github.microtweak.validator.conditional.core.internal.AnnotationHelper.readAttribute;

@ToString(of = { "name", "expression", "actualConstraint" })
public class ConditionalDescriptor {

    private Field field;
    private String expression;

    @Getter
    private Annotation actualConstraint;

    @Getter
    private String constraintMessage;

    private ConstraintValidator validator;

    public ConditionalDescriptor(Field field, Annotation conditional) {
        this.field = field;
        field.setAccessible(true);

        try {
            expression = readAttribute(conditional, "expression", String.class);
            Validate.notEmpty(expression);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The conditional constraint " + conditional.annotationType() + " does not have the attribute \"expression\"!");
        }

        Class<? extends Annotation> actualConstraintType = conditional.annotationType().getAnnotation(ConditionalConstraint.class).value();

        if (actualConstraintType == null) {
            throw new IllegalArgumentException("Conditional constraint is not annotated with " + ConditionalConstraint.class + "!");
        }

        actualConstraint = createConstraintBy(conditional, actualConstraintType);

        try {
            constraintMessage = readAttribute(actualConstraint, "message", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("The Bean Validation constraint " + actualConstraint.annotationType() + " does not have the attribute \"message\"!");
        }
    }

    public String getName() {
        return field.getName();
    }

    private Object extractValueFromMember(Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    private Annotation createConstraintBy(Annotation conditionalConstraint, Class<? extends Annotation> targetConstraint) {
        Map<String, Object> attributes = AnnotationHelper.readAllAtributeExcept(conditionalConstraint, "expression");
        return AnnotationHelper.createAnnotation(targetConstraint, attributes);
    }

    public void initialize(ConstraintValidatorFactory factory) {
        Class<? extends ConstraintValidator> validatorClass = ConstraintDescriptorFactory.getInstance().of(actualConstraint)
                .getConstraintValidatorClasses().stream()
                .filter(this::isCapableValidator)
                .findFirst()
                .orElse( null );

        validator = factory.getInstance( validatorClass );
        validator.initialize( actualConstraint );
    }

    private boolean isCapableValidator(Class<? extends ConstraintValidator<?, ?>> clazz) {
        TypeVariable<?> typeVar = ConstraintValidator.class.getTypeParameters()[1];
        return TypeUtils.getRawType(typeVar, clazz).isAssignableFrom( field.getType() );
    }

    public boolean isConstraintEnabled(ELProcessor processor) {
        Object result = processor.eval(expression);

        if (!Boolean.class.isInstance(result)) {
            throw new InvalidConditionalExpressionException("The expression \"" + expression + "\" should return boolean!");
        }

        return Boolean.class.cast( result );
    }

    public boolean isValid(Object parent, ConstraintValidatorContext context) {
        Object v = extractValueFromMember(parent);
        return validator.isValid(v, context);
    }

}
