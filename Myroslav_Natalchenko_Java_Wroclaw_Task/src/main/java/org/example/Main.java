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

        JSONArray ordersJson = new JSONArray(Files.readString(Paths.get(args[0])));
        JSONArray methodsJson = new JSONArray(Files.readString(Paths.get(args[1])));

        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < ordersJson.length(); i++) {
            orders.add(new Order(ordersJson.getJSONObject(i)));
        }

        Map<String, PaymentMethod> paymentMethods = new LinkedHashMap<>();
        Map<String, Double> totalSpent = new LinkedHashMap<>();
        for (int i = 0; i < methodsJson.length(); i++) {
            PaymentMethod m = new PaymentMethod(methodsJson.getJSONObject(i));
            paymentMethods.put(m.id, m);
            totalSpent.put(m.id, 0.0);
        }

        PaymentMethod bonus_points_punkty = paymentMethods.get("PUNKTY");

        // Orders are sorted primarily by amount of PROMOTIONS and secondary with VALUE
        // This sort is made for better prioritizing ORDERS
        orders.sort(Comparator
                .comparingInt((Order o) -> -o.promotions.size())
                .thenComparing((Order o) -> -o.value));

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            double order_value = order.value;
            double final_value_after_discounts = Double.MAX_VALUE;
            String payment_method = null;
            double bonus_point_punkty_used = 0;
            double money_from_card_used = 0;
            boolean completed = false;

            // 1. Fully with PUNKTY
            if (bonus_points_punkty != null && bonus_points_punkty.limit >= order_value) {
                double discounted = order_value * (1 - bonus_points_punkty.discount / 100.0);
                final_value_after_discounts = discounted;
                payment_method = "PUNKTY";
                bonus_point_punkty_used = discounted;
                money_from_card_used = 0;
                completed = true;
            }

            // 2. Fully with eligible card
            if (!completed) {
                for (String promo : order.promotions) {
                    PaymentMethod m = paymentMethods.get(promo);
                    if (m == null) continue;
                    double discounted = order_value * (1 - m.discount / 100.0);
                    if (m.limit >= discounted) {
                        final_value_after_discounts = discounted;
                        payment_method = m.id;
                        bonus_point_punkty_used = 0;
                        money_from_card_used = discounted;
                        completed = true;
                        break;
                    }
                }
            }

            // 3. PARTIAL points (10%) + any card, 10% discount
            if (!completed && bonus_points_punkty != null && bonus_points_punkty.limit >= 0.1 * order_value) {
                double rawTotal = order_value;

                double pointsToUse = decidePointsToUse(bonus_points_punkty, rawTotal, orders.subList(i + 1, orders.size()), paymentMethods);
                double discountedTotal = order_value * 0.9;
              //  System.out.println("points to use for order " + (i + 1) + ": " + pointsToUse);
                double remaining = discountedTotal - pointsToUse;

                for (PaymentMethod m : paymentMethods.values()) {
                    if (m.id.equals("PUNKTY")) continue;
                    if (m.limit >= remaining) {
                        final_value_after_discounts = discountedTotal;
                        payment_method = m.id + " + PUNKTY";
                        bonus_point_punkty_used = pointsToUse;
                        money_from_card_used = remaining;
                        completed = true;
                        break;
                    }
                }
            }

            // 4. Brute-force (no discount): card + points
            if (!completed) {
                for (PaymentMethod m : paymentMethods.values()) {
                    if (m.id.equals("PUNKTY")) continue;
                    double payWithCard = Math.min(order_value, m.limit);
                    double remaining = order_value - payWithCard;
                    double payWithPoints = Math.min(remaining, bonus_points_punkty.limit);
                    if (payWithCard + payWithPoints >= order_value) {
                        final_value_after_discounts = payWithCard + payWithPoints;
                        payment_method = m.id + " + PUNKTY (no discount)";
                        money_from_card_used = payWithCard;
                        bonus_point_punkty_used = payWithPoints;
                        completed = true;
                        break;
                    }
                }
            }

            if (payment_method != null) {
               // System.out.printf("Order %s: %.2f paid using %s%n", order.id, final_value_after_discounts, payment_method);

                if (bonus_point_punkty_used > 0) {
                    bonus_points_punkty.limit -= bonus_point_punkty_used;
                    totalSpent.put("PUNKTY", totalSpent.get("PUNKTY") + bonus_point_punkty_used);
                }

                if (money_from_card_used > 0 && !payment_method.equals("PUNKTY")) {
                    String card = payment_method.split(" ")[0];
                    PaymentMethod method = paymentMethods.get(card);
                    method.limit -= money_from_card_used;
                    totalSpent.put(card, totalSpent.get(card) + money_from_card_used);
                }
            } else {
                System.out.printf("Order %s: could not be paid.%n", order.id);
            }
        }

        for (var entry : totalSpent.entrySet()) {
            if (entry.getValue() > 0)
                System.out.printf("%s %.2f%n", entry.getKey(), entry.getValue());
        }
    }

    public static double decidePointsToUse(PaymentMethod points, double orderAmount,
            List<Order> futureOrders, Map<String, PaymentMethod> paymentMethods) {
        double discountedTotal = orderAmount * 0.9;
        double tenPercent = orderAmount * 0.1;
        double restToPay = discountedTotal - tenPercent;

        Map<String, Double> simulatedFunds = new HashMap<>();
        for (Map.Entry<String, PaymentMethod> entry : paymentMethods.entrySet()) {
            simulatedFunds.put(entry.getKey(), entry.getValue().limit);
        }

        boolean canPayCurrent = false;
        for (PaymentMethod m : paymentMethods.values()) {
            if (m.id.equals("PUNKTY")) continue;
            if (simulatedFunds.get(m.id) >= restToPay && simulatedFunds.get("PUNKTY") >= tenPercent) {
                simulatedFunds.put(m.id, simulatedFunds.get(m.id) - restToPay);
                simulatedFunds.put("PUNKTY", simulatedFunds.get("PUNKTY") - tenPercent);
                canPayCurrent = true;
                break;
            }
        }
        if (!canPayCurrent) return Math.min(points.limit, discountedTotal);

        if (!futureOrders.isEmpty()) {
            Order future = futureOrders.get(0);
            double futureValue = future.value;
            double tenPercentFuture = futureValue * 0.1;
            double discountedFuture = futureValue * 0.9;
            double futureRest = discountedFuture - simulatedFunds.get("PUNKTY");

            if (simulatedFunds.get("PUNKTY") >= tenPercentFuture) {
                for (PaymentMethod m : paymentMethods.values()) {
                    if (m.id.equals("PUNKTY")) continue;
                    if (simulatedFunds.get(m.id) >= futureRest) {
                        return tenPercent;
                    }
                }
            }
        }
        return Math.min(points.limit, discountedTotal);
    }
}