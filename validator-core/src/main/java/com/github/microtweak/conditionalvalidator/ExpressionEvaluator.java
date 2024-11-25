package com.github.microtweak.conditionalvalidator;

import com.github.microtweak.conditionalvalidator.exception.InvalidConditionalExpressionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;

@Slf4j
public class ExpressionEvaluator {
    private final ExpressionParser parser ;
    private final EvaluationContext elContext ;

    private boolean staticContextInitialized;

    public ExpressionEvaluator(Class<?>... contextClasses) {
        parser = new SpelExpressionParser();
        elContext = new StandardEvaluationContext();
        Stream.of(contextClasses).forEach(this::addEnumToContext);
    }

    private void fillStaticContextWithInnerClasses(Class<?> beanClass) {
        if (!staticContextInitialized) {
            Stream.of(beanClass.getClasses()).forEach(this::addEnumToContext);
            staticContextInitialized = true;
        }
    }

    private <E extends Enum> void addEnumToContext(Class<?> clazz) {
        if (!clazz.isEnum()) {
            return;
        }

        for (E constant : ((Class<E>) clazz).getEnumConstants()) {
            final String key = clazz.getSimpleName() + "." + constant.name();
            elContext.setVariable(key, constant);
        }
    }

    public boolean isTrueExpression(Object bean, String expression) {
        fillStaticContextWithInnerClasses(bean.getClass());
        fillContextWithBean(elContext, bean);

        Expression el = parser.parseExpression(expression);
        Object result = el.getValue(elContext);
        if (!Boolean.class.isInstance(result)) {
            throw new InvalidConditionalExpressionException("The expression \"" + expression + "\" should return boolean!");
        }
        return Boolean.class.cast(result);
    }

    private void fillContextWithBean(EvaluationContext ctx, Object bean) {
        for (Field field : getAllFieldsList(bean.getClass())) {
            if (!isVisibleByValidator(bean.getClass(), field)) {
                continue;
            }
            try {
                Object value = FieldUtils.readField(field, bean, true);
                ctx.setVariable(field.getName(), value);
            } catch (IllegalAccessException e) {
                throw new InvalidConditionalExpressionException("Unable to read/access field \"" + field.getName() + "\" used in expression");
            }
        }
    }

    private boolean isVisibleByValidator(Class<?> beanClass, Field field) {
        final int mod = field.getModifiers();

        if (isPublic(mod) || isProtected(mod)) {
            return true;
        }

        if (isPrivate(mod) && field.getDeclaringClass().equals(beanClass)) {
            return true;
        }

        return false;
    }

}
