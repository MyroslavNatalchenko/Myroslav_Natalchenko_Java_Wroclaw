package org.example;

import org.json.JSONArray;
import org.example.data.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length!=2) {
            System.out.println("Usage: java -jar application.jar PATH/orders.json PATH/paymentmethods.json");
            return;
        }

        JSONArray orders_json = new JSONArray(Files.readString(Paths.get(args[0])));
        JSONArray paymentMethods_json = new JSONArray(Files.readString(Paths.get(args[1])));

        Map<String, Double> totalSpent = OrderAnalyzer.total_spent_per_method(orders_json, paymentMethods_json);

        for (var entry : totalSpent.entrySet()) {
            if (entry.getValue() > 0)
                System.out.printf("%s %.2f%n", entry.getKey(), entry.getValue());
        }
    }
}