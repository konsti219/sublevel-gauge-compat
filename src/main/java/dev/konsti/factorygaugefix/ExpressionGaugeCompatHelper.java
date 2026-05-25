package dev.konsti.factorygaugefix;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class ExpressionGaugeCompatHelper {
    public static final int MAX_EXPRESSION_LENGTH = 256;

    private static final double BOOLEAN_TRUE = 1;
    private static final double BOOLEAN_FALSE = 0;
    private static final double EPSILON = 1e-9;
    private static final int PRECEDENCE_COMPARISON = Operator.PRECEDENCE_ADDITION - 1;

    public static final List<Function> FUNCTIONS = List.of(
        new Function("and", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] & (int) args[1];
            }
        },
        new Function("or", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] | (int) args[1];
            }
        },
        new Function("xor", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] ^ (int) args[1];
            }
        },
        new Function("not", 1) {
            @Override
            public double apply(double... args) {
                return ~(int) args[0];
            }
        },
        new Function("nor", 2) {
            @Override
            public double apply(double... args) {
                return ~((int) args[0] | (int) args[1]);
            }
        },
        new Function("nand", 2) {
            @Override
            public double apply(double... args) {
                return ~((int) args[0] & (int) args[1]);
            }
        },
        new Function("xnor", 2) {
            @Override
            public double apply(double... args) {
                return ~((int) args[0] ^ (int) args[1]);
            }
        },
        new Function("shl", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] << (int) args[1];
            }
        },
        new Function("shr", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] >> (int) args[1];
            }
        },
        new Function("ushr", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] >>> (int) args[1];
            }
        },
        new Function("imply", 2) {
            @Override
            public double apply(double... args) {
                return ~(int) args[0] | (int) args[1];
            }
        },
        new Function("nimply", 2) {
            @Override
            public double apply(double... args) {
                return (int) args[0] & ~(int) args[1];
            }
        },
        new Function("if", 3) {
            @Override
            public double apply(double... args) {
                return truthy(args[0]) ? args[1] : args[2];
            }
        }
    );

    public static final List<Operator> OPERATORS = List.of(
        comparison("<", (left, right) -> left < right),
        comparison("<=", (left, right) -> left <= right),
        comparison(">", (left, right) -> left > right),
        comparison(">=", (left, right) -> left >= right),
        comparison("==", (left, right) -> Math.abs(left - right) < EPSILON),
        comparison("!=", (left, right) -> Math.abs(left - right) >= EPSILON)
    );

    private ExpressionGaugeCompatHelper() {
    }

    public static void validate(String expressionText, Map<String, Double> variables) {
        Expression expression = buildExpression(expressionText, variables.keySet());
        variables.forEach(expression::setVariable);

        ValidationResult validation = expression.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException(joinErrors(validation));
        }

        expression.evaluate();
    }

    public static double evaluate(String expressionText, Map<String, Double> variables) {
        Expression expression = buildExpression(expressionText, variables.keySet());
        variables.forEach(expression::setVariable);

        ValidationResult validation = expression.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException(joinErrors(validation));
        }

        return expression.evaluate();
    }

    public static String clampExpression(String expressionText) {
        if (expressionText.length() <= MAX_EXPRESSION_LENGTH) {
            return expressionText;
        }
        return expressionText.substring(0, MAX_EXPRESSION_LENGTH);
    }

    private static Expression buildExpression(String expressionText, Set<String> variables) {
        return new ExpressionBuilder(expressionText)
            .variables(variables)
            .functions(FUNCTIONS)
            .operator(OPERATORS.toArray(Operator[]::new))
            .build();
    }

    private static Operator comparison(String symbol, DoubleComparator comparator) {
        return new Operator(symbol, 2, true, PRECEDENCE_COMPARISON) {
            @Override
            public double apply(double... args) {
                return comparator.test(args[0], args[1]) ? BOOLEAN_TRUE : BOOLEAN_FALSE;
            }
        };
    }

    private static String joinErrors(ValidationResult validation) {
        StringJoiner joiner = new StringJoiner(", ");
        validation.getErrors().forEach(joiner::add);
        return joiner.toString();
    }

    private static boolean truthy(double value) {
        return Math.abs(value) >= EPSILON;
    }

    @FunctionalInterface
    private interface DoubleComparator {
        boolean test(double left, double right);
    }
}
