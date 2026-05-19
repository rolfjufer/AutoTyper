// @step 1
package ch.demo;

// @step 1
public class BankAccount {

// @step 2
    private String owner;
    private double balance;
    private String iban;

// @step 3
    public BankAccount(String owner, String iban) {
        this.owner = owner;
        this.iban = iban;
        this.balance = 0.0;
    }

// @step 3
    public BankAccount(String owner, String iban, double initialBalance) {
        this.owner = owner;
        this.iban = iban;
        this.balance = initialBalance;
    }

// @step 4
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive!");
        }
        balance += amount;
        System.out.println("Deposited " + amount + " → Balance: " + balance);
    }

// @step 5
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive!");
        }
        if (amount > balance) {
            throw new IllegalStateException("Insufficient funds!");
        }
        balance -= amount;
        System.out.println("Withdrawn " + amount + " → Balance: " + balance);
    }

// @step 6
    public void transferTo(BankAccount other, double amount) {
        this.withdraw(amount);
        other.deposit(amount);
        System.out.println("Transferred " + amount + " from " + this.owner + " to " + other.owner);
    }

// @step 7
    public String getOwner() {
        return owner;
    }

// @step 7
    public double getBalance() {
        return balance;
    }

// @step 7
    public String getIban() {
        return iban;
    }

// @step 8
    @Override
    public String toString() {
        return "BankAccount{owner='" + owner + "', iban='" + iban + "', balance=" + balance + "}";
    }

// @step 9
    public static void main(String[] args) {
        BankAccount alice = new BankAccount("Alice", "CH12 3456 7890", 1000.0);
        BankAccount bob = new BankAccount("Bob", "CH98 7654 3210");

        System.out.println(alice);
        System.out.println(bob);

        alice.deposit(500);
        alice.transferTo(bob, 300);

        System.out.println("--- Final ---");
        System.out.println(alice);
        System.out.println(bob);
    }

// @step 1
}
