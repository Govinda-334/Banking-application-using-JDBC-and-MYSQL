package project;
import java.sql.*;
import java.util.Scanner;

public class BankingApp {

    // DB Connection
    private static Connection getConnection() throws Exception {
        String url = "jdbc:mysql://localhost:3306/banking_app"; // database name
        String user = "root";  
        String pass = "root"; 
        return DriverManager.getConnection(url, user, pass);
    }

    // Create account
    private static void createAccount(Connection conn, Scanner sc) throws Exception {
        System.out.print("Enter Name: ");
        sc.nextLine();
        String name = sc.nextLine();
        System.out.print("Enter PIN: ");
        String pin = sc.nextLine();

        String sql = "INSERT INTO accounts(name, pin, balance) VALUES(?, ?, 0.0)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.setString(2, pin);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            System.out.println("✅ Account created! Your Account No: " + rs.getInt(1));
        }
    }

    // Login
    private static int login(Connection conn, Scanner sc) throws Exception {
        System.out.print("Enter Account No: ");
        int accNo = sc.nextInt();
        System.out.print("Enter PIN: ");
        String pin = sc.next();

        String sql = "SELECT * FROM accounts WHERE account_no=? AND pin=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, accNo);
        ps.setString(2, pin);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("✅ Welcome " + rs.getString("name"));
            return accNo;
        } else {
            System.out.println("❌ Invalid credentials!");
            return -1;
        }
    }

    // Deposit
    private static void deposit(Connection conn, Scanner sc, int accNo) throws Exception {
        System.out.print("Enter amount to deposit: ");
        double amount = sc.nextDouble();

        conn.setAutoCommit(false);
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance=balance+? WHERE account_no=?");
            ps.setDouble(1, amount);
            ps.setInt(2, accNo);
            ps.executeUpdate();

            PreparedStatement psTxn = conn.prepareStatement("INSERT INTO transactions(account_no, type, amount) VALUES(?, 'deposit', ?)");
            psTxn.setInt(1, accNo);
            psTxn.setDouble(2, amount);
            psTxn.executeUpdate();

            conn.commit();
            System.out.println("✅ Deposit successful!");
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    // Withdraw
    private static void withdraw(Connection conn, Scanner sc, int accNo) throws Exception {
        System.out.print("Enter amount to withdraw: ");
        double amount = sc.nextDouble();

        String checkSql = "SELECT balance FROM accounts WHERE account_no=?";
        PreparedStatement psCheck = conn.prepareStatement(checkSql);
        psCheck.setInt(1, accNo);
        ResultSet rs = psCheck.executeQuery();

        if (rs.next() && rs.getDouble("balance") >= amount) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance=balance-? WHERE account_no=?");
                ps.setDouble(1, amount);
                ps.setInt(2, accNo);
                ps.executeUpdate();

                PreparedStatement psTxn = conn.prepareStatement("INSERT INTO transactions(account_no, type, amount) VALUES(?, 'withdraw', ?)");
                psTxn.setInt(1, accNo);
                psTxn.setDouble(2, amount);
                psTxn.executeUpdate();

                conn.commit();
                System.out.println("✅ Withdrawal successful!");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } else {
            System.out.println("❌ Insufficient balance!");
        }
    }

    // Check Balance
    private static void checkBalance(Connection conn, int accNo) throws Exception {
        String sql = "SELECT balance FROM accounts WHERE account_no=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println("💰 Current Balance: " + rs.getDouble("balance"));
        }
    }

    // Transaction History
    private static void viewTransactions(Connection conn, int accNo) throws Exception {
        String sql = "SELECT * FROM transactions WHERE account_no=? ORDER BY txn_date DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, accNo);
        ResultSet rs = ps.executeQuery();
        System.out.println("\n--- Transaction History ---");
        while (rs.next()) {
            System.out.println(rs.getInt("txn_id") + " | " +
                               rs.getString("type") + " | " +
                               rs.getDouble("amount") + " | " +
                               rs.getTimestamp("txn_date"));
        }
    }

    // Main Program
    public static void main(String[] args) {
        try (Connection conn = getConnection(); Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n==== Banking Application ====");
                System.out.println("1. Create Account");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose option: ");
                int choice = sc.nextInt();

                if (choice == 1) {
                    createAccount(conn, sc);
                } else if (choice == 2) {
                    int accNo = login(conn, sc);
                    if (accNo != -1) {
                        boolean loggedIn = true;
                        while (loggedIn) {
                            System.out.println("\n--- Menu ---");
                            System.out.println("1. Deposit");
                            System.out.println("2. Withdraw");
                            System.out.println("3. Check Balance");
                            System.out.println("4. View Transactions");
                            System.out.println("5. Logout");
                            System.out.print("Enter choice: ");
                            int ch = sc.nextInt();

                            switch (ch) {
                                case 1 -> deposit(conn, sc, accNo);
                                case 2 -> withdraw(conn, sc, accNo);
                                case 3 -> checkBalance(conn, accNo);
                                case 4 -> viewTransactions(conn, accNo);
                                case 5 -> loggedIn = false;
                                default -> System.out.println("❌ Invalid choice!");
                            }
                        }
                    }
                } else {
                    System.out.println("👋 Goodbye!");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
