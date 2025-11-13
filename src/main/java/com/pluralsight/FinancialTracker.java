package com.pluralsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class FinancialTracker {
    /* ------------------------------------------------------------------
       Constants and shared data
       ------------------------------------------------------------------ */

    /**
     * In-memory list that holds every Transaction object.
     */
    private static final ArrayList<Transaction> transactionList = new ArrayList<>();

    /**
     * Data file – created automatically if it does not exist.
     */
    private static final String FILE_NAME = "transactions.csv";

    /**
     * Date / time patterns used in prompts and parsing.
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";
    private static final String DATETIME_PATTERN = DATE_PATTERN + " " + TIME_PATTERN;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    /* ------------------------------------------------------------------
       Main menu loop
       ------------------------------------------------------------------ */
    public static void main(String[] args) {

        loadTransactions(FILE_NAME);                          // read existing data

        Scanner scanner = new Scanner(System.in);
        boolean keepRunning = true;

        while (keepRunning) {
            System.out.println();
            System.out.println("Welcome to TransactionApp");
            System.out.println("Choose an option:");
            System.out.println(" D) Add Deposit");
            System.out.println(" P) Make Payment (Debit)");
            System.out.println(" L) Ledger");
            System.out.println(" X) Exit");
            System.out.print("Your choice: ");

            String menuChoice = scanner.nextLine().trim().toUpperCase();

            switch (menuChoice) {
                case "D" -> addDeposit(scanner);
                case "P" -> addPayment(scanner);
                case "L" -> ledgerMenu(scanner);
                case "X" -> keepRunning = false;
                default -> System.out.println("Invalid option – please try again.");
            }
        }
        scanner.close();
    }

    /* ------------------------------------------------------------------
       Load data file (creates it if missing)
       ------------------------------------------------------------------ */

    /**
     * Reads the pipe-delimited data file and fills {@code transactionList}.
     * If the file is not present, an empty file is created so later writes succeed.
     */
    private static void loadTransactions(String fileName) {
        try {
            // Ensure the file exists – create a blank one if needed.
            File dataFile = new File(fileName);
            if (dataFile.createNewFile()) {
                System.out.println("Created new data file: " + fileName);
            }

            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|");             // 5 parts expected
                LocalDate date = LocalDate.parse(fields[0], DATE_FORMATTER);
                LocalTime time = LocalTime.parse(fields[1], TIME_FORMATTER);
                String description = fields[2];
                String vendor = fields[3];
                double amount = Double.parseDouble(fields[4]);

                Transaction transaction =
                        new Transaction(date, time, description, vendor, amount);
                transactionList.add(transaction);
            }
            reader.close();
        } catch (IOException ioException) {
            System.out.println("Error reading data file: " + ioException.getMessage());
        }
    }

    /* ------------------------------------------------------------------
       Add new transactions
       ------------------------------------------------------------------ */

    /**
     * Reads one date-time line (yyyy-MM-dd HH:mm:ss) then description, vendor,
     * and positive amount, validates the input, stores the deposit.
     */
    private static void addDeposit(Scanner scanner) {

        LocalDateTime dateTime = promptDateTime(scanner);
        if (dateTime == null) return;                  // user typed bad date

        System.out.print("Description: ");
        String description = scanner.nextLine();

        System.out.print("Vendor: ");
        String vendor = scanner.nextLine();

        double amount = promptPositiveAmount(scanner);
        if (Double.isNaN(amount)) return;              // invalid amount entered

        Transaction deposit = new Transaction(
                dateTime.toLocalDate(),
                dateTime.toLocalTime(),
                description,
                vendor,
                amount);                               // positive number
        saveTransaction(deposit);
        System.out.println("Deposit recorded.");
    }

    /**
     * Reads user input for a payment and stores it as a negative amount.
     */
    private static void addPayment(Scanner scanner) {

        LocalDateTime dateTime = promptDateTime(scanner);
        if (dateTime == null) return;

        System.out.print("Description: ");
        String description = scanner.nextLine();

        System.out.print("Vendor: ");
        String vendor = scanner.nextLine();

        double amount = promptPositiveAmount(scanner);
        if (Double.isNaN(amount)) return;

        Transaction payment = new Transaction(
                dateTime.toLocalDate(),
                dateTime.toLocalTime(),
                description,
                vendor,
                -amount);                              // convert to negative
        saveTransaction(payment);
        System.out.println("Payment recorded.");
    }

    /* ------------- small input helpers (used by both addDeposit and addPayment) ------------- */

    /**
     * Prompt for a single date-time line and return a LocalDateTime (or null on failure).
     */
    private static LocalDateTime promptDateTime(Scanner scanner) {
        System.out.print("Date & time (" + DATETIME_PATTERN + "): ");
        String userInput = scanner.nextLine().trim();
        try {
            return LocalDateTime.parse(userInput, DATETIME_FORMATTER);
        } catch (Exception parseException) {
            System.out.println("Invalid date/time.");
            return null;
        }
    }

    /**
     * Prompt for a positive number. Returns NaN if the user enters invalid data.
     */
    private static double promptPositiveAmount(Scanner scanner) {
        System.out.print("Amount (positive): ");
        String userInput = scanner.nextLine().trim();
        try {
            double amount = Double.parseDouble(userInput);
            if (amount <= 0) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException badNumber) {
            System.out.println("Amount must be a positive number.");
            return Double.NaN;
        }
    }

    /* ------------------------------------------------------------------
       Persist a new Transaction (to list + file)
       ------------------------------------------------------------------ */
    private static void saveTransaction(Transaction transaction) {

        // Add to in-memory list
        transactionList.add(transaction);

        // Append to file
        String recordLine = "%s|%s|%s|%s|%.2f".formatted(
                transaction.getDate().format(DATE_FORMATTER),
                transaction.getTime().format(TIME_FORMATTER),
                transaction.getDescription(),
                transaction.getVendor(),
                transaction.getAmount());

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(FILE_NAME, true))) {

            writer.write(recordLine);
            writer.newLine();

        } catch (IOException ioException) {
            System.out.println("Failed to write to file: " + ioException.getMessage());
        }
    }

    /* ------------------------------------------------------------------
       Ledger submenu (lists & reports)
       ------------------------------------------------------------------ */
    private static void ledgerMenu(Scanner scanner) {

        boolean inLedgerMenu = true;
        while (inLedgerMenu) {
            System.out.println();
            System.out.println("Ledger Menu");
            System.out.println(" A) All Transactions");
            System.out.println(" D) Deposits Only");
            System.out.println(" P) Payments Only");
            System.out.println(" R) Reports");
            System.out.println(" H) Home");
            System.out.print("Your choice: ");

            String ledgerChoice = scanner.nextLine().trim().toUpperCase();

            switch (ledgerChoice) {
                case "A" -> displayLedger();
                case "D" -> displayDeposits();
                case "P" -> displayPayments();
                case "R" -> reportsMenu(scanner);
                case "H" -> inLedgerMenu = false;
                default -> System.out.println("Invalid option – please try again.");
            }
        }
    }

    /* ---------- Pretty printing helpers ---------- */

    private static void printTableHeader() {
        System.out.printf("%-12s %-10s %-24s %-18s %10s%n",
                "Date", "Time", "Description", "Vendor", "Amount");
        System.out.println("--------------------------------------------------------------------------");
    }

    private static void printTransactionRow(Transaction transaction) {
        System.out.printf("%-12s %-10s %-24s %-18s %10.2f%n",
                transaction.getDate().format(DATE_FORMATTER),
                transaction.getTime().format(TIME_FORMATTER),
                transaction.getDescription(),
                transaction.getVendor(),
                transaction.getAmount());
    }

    /**
     * Helper Method – returns a copy of transactionList sorted by date/time
     */
    private static ArrayList<Transaction> getTransactionsSortedNewestFirst() {

        // Copy list so original order is untouched
        ArrayList<Transaction> sortedList = new ArrayList<>(transactionList);

        /* Comparator:
           • Later date should come first (newest)
           • If same date, later time comes first
         */
        Collections.sort(sortedList, (first, second) -> {
            int dateCompare = second.getDate().compareTo(first.getDate());   // reverse date
            if (dateCompare != 0) return dateCompare;
            return second.getTime().compareTo(first.getTime());              // reverse time
        });

        return sortedList;
    }

    private static void displayLedger() {
        printTableHeader();
        for (Transaction transaction : getTransactionsSortedNewestFirst()) {
            printTransactionRow(transaction);
        }
    }

    private static void displayDeposits() {
        printTableHeader();
        for (Transaction transaction : getTransactionsSortedNewestFirst()) {
            if (transaction.getAmount() > 0) {
                printTransactionRow(transaction);
            }
        }
    }

    private static void displayPayments() {
        printTableHeader();
        for (Transaction transaction : getTransactionsSortedNewestFirst()) {
            if (transaction.getAmount() < 0) {
                printTransactionRow(transaction);
            }
        }
    }

    /* ------------------------------------------------------------------
       Reports submenu
       ------------------------------------------------------------------ */
    private static void reportsMenu(Scanner scanner) {

        boolean inReportsMenu = true;
        while (inReportsMenu) {
            System.out.println();
            System.out.println("Reports Menu");
            System.out.println(" 1) Month To Date");
            System.out.println(" 2) Previous Month");
            System.out.println(" 3) Year To Date");
            System.out.println(" 4) Previous Year");
            System.out.println(" 5) Search by Vendor");
            System.out.println(" 6) Custom Search");
            System.out.println(" 0) Back");
            System.out.print("Your choice: ");

            String reportChoice = scanner.nextLine().trim();
            LocalDate today = LocalDate.now();

            switch (reportChoice) {
                case "1" -> filterByDate(today.withDayOfMonth(1), today);
                case "2" -> {
                    LocalDate lastMonth = today.minusMonths(1);
                    LocalDate startOfLastMonth = LocalDate.of(lastMonth.getYear(),
                            lastMonth.getMonth(), 1);
                    LocalDate endOfLastMonth = startOfLastMonth.plusMonths(1).minusDays(1);
                    filterByDate(startOfLastMonth, endOfLastMonth);
                }
                case "3" -> filterByDate(today.withDayOfYear(1), today);
                case "4" -> {
                    LocalDate lastYear = today.minusYears(1);
                    filterByDate(lastYear.withDayOfYear(1),
                            lastYear.withDayOfYear(365));
                }
                case "5" -> {
                    System.out.print("Vendor name: ");
                    String vendorName = scanner.nextLine().trim();
                    filterByVendor(vendorName);
                }
                case "6" -> customSearch(scanner);
                case "0" -> inReportsMenu = false;
                default -> System.out.println("Invalid option – please try again.");
            }
        }
    }

    /* -------------------------- Report helpers -------------------------- */

    /**
     * Prints every transaction whose date is within [startDate … endDate] inclusive.
     */
    private static void filterByDate(LocalDate startDate, LocalDate endDate) {
        printTableHeader();
        boolean anyFound = false;

        for (Transaction transaction : getTransactionsSortedNewestFirst()) {
            LocalDate transactionDate = transaction.getDate();
            if (!transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate)) {
                printTransactionRow(transaction);
                anyFound = true;
            }
        }
        if (!anyFound) System.out.println("No transactions found for the selected dates.");
    }

    /**
     * Prints every transaction for the given vendor (case-insensitive).
     */
    private static void filterByVendor(String vendorName) {
        printTableHeader();
        boolean anyFound = false;

        for (Transaction transaction : getTransactionsSortedNewestFirst()) {
            if (transaction.getVendor().equalsIgnoreCase(vendorName)) {
                printTransactionRow(transaction);
                anyFound = true;
            }
        }
        if (!anyFound) System.out.println("No transactions found for that vendor.");
    }

    /**
     * Prompts user for optional criteria (date range, description,
     * vendor, exact amount) and prints matching rows.
     */
    private static void customSearch(Scanner scanner) {

        System.out.print("Start date (" + DATE_PATTERN + ", blank = none): ");
        LocalDate startDate = parseDate(scanner.nextLine().trim());

        System.out.print("End date   (" + DATE_PATTERN + ", blank = none): ");
        LocalDate endDate = parseDate(scanner.nextLine().trim());

        System.out.print("Description (blank = any): ");
        String descriptionFilter = scanner.nextLine().trim();

        System.out.print("Vendor      (blank = any): ");
        String vendorFilter = scanner.nextLine().trim();

        System.out.print("Amount      (blank = any): ");
        Double amountFilter = parseDouble(scanner.nextLine().trim());

        printTableHeader();
        boolean anyFound = false;


        for (Transaction transaction : transactionList) {
            boolean matches = true;

            if (startDate != null && transaction.getDate().isBefore(startDate)) matches = false;
            if (endDate != null && transaction.getDate().isAfter(endDate)) matches = false;
            if (!descriptionFilter.isEmpty()
                    && !transaction.getDescription().equalsIgnoreCase(descriptionFilter)) matches = false;
            if (!vendorFilter.isEmpty()
                    && !transaction.getVendor().equalsIgnoreCase(vendorFilter)) matches = false;
            if (amountFilter != null && transaction.getAmount() != amountFilter) matches = false;

            if (matches) {
                printTransactionRow(transaction);
                anyFound = true;
            }
        }
        if (!anyFound) System.out.println("No transactions match the chosen criteria.");
    }

    /* -------------------------- Parsing helpers -------------------------- */

    /**
     * Parses a date or returns null if the string is blank or bad.
     */
    private static LocalDate parseDate(String dateString) {
        if (dateString.isEmpty()) return null;
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (Exception bad) {
            System.out.println("Invalid date: " + dateString);
            return null;
        }
    }

    /**
     * Parses a double or returns null if the string is blank or bad.
     */
    private static Double parseDouble(String numberString) {
        if (numberString.isEmpty()) return null;
        try {
            return Double.parseDouble(numberString);
        } catch (Exception bad) {
            System.out.println("Invalid number: " + numberString);
            return null;
        }
    }
}

