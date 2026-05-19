// @step 1
package ch.demo;

// @step 1
public class Calculator {

// @step 2
    private double result;

// @step 3
    public Calculator() {
        this.result = 0;
    }

// @step 4
    public double add(double a, double b) {
        result = a + b;
        return result;
    }

// @step 4
    public double subtract(double a, double b) {
        result = a - b;
        return result;
    }

// @step 5
    public double multiply(double a, double b) {
        result = a * b;
        return result;
    }

// @step 5
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero!");
        }
        result = a / b;
        return result;
    }

// @step 6
    public double getLastResult() {
        return result;
    }

// @step 7
    public static void main(String[] args) {
        Calculator calc = new Calculator();

        System.out.println("5 + 3 = " + calc.add(5, 3));
        System.out.println("10 - 4 = " + calc.subtract(10, 4));
        System.out.println("6 * 7 = " + calc.multiply(6, 7));
        System.out.println("20 / 4 = " + calc.divide(20, 4));

        System.out.println("Last result: " + calc.getLastResult());
    }

// @step 1
}
