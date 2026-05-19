package ch.demo;

import java.util.Scanner;

/**
 * A simple calculator demonstrating OOP principles.
 */
public class Calculator {

    private double result;

    public Calculator() {
        this.result = 0;
    }

    public double add(double a, double b) {
        result = a + b;
        return result;
    }

    public double subtract(double a, double b) {
        result = a - b;
        return result;
    }

    public double multiply(double a, double b) {
        result = a * b;
        return result;
    }

    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero!");
        }
        result = a / b;
        return result;
    }

    public double getLastResult() {
        return result;
    }

    public static void main(String[] args) {
        Calculator calc = new Calculator();

        System.out.println("5 + 3 = " + calc.add(5, 3));
        System.out.println("10 - 4 = " + calc.subtract(10, 4));
        System.out.println("6 * 7 = " + calc.multiply(6, 7));
        System.out.println("20 / 4 = " + calc.divide(20, 4));

        System.out.println("Last result: " + calc.getLastResult());
    }
}
