// @step 1
public class HelloWorld {

// @step 2
    private String greeting;

// @step 3
    public HelloWorld(String greeting) {
        this.greeting = greeting;
    }

// @step 4
    public void sayHello() {
        System.out.println(greeting);
    }

// @step 5
    public void sayHelloMultiple(int times) {
        for (int i = 1; i <= times; i++) {
            System.out.println(i + ": " + greeting);
        }
    }

// @step 6
    public static void main(String[] args) {
        HelloWorld hello = new HelloWorld("Hello, World!");
        hello.sayHello();
        hello.sayHelloMultiple(3);
    }

// @step 1
}
