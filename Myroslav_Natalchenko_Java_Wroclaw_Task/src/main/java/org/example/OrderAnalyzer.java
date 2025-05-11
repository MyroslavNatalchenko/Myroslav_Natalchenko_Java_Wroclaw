package org.example;

import org.example.data.Order;
import org.example.data.PaymentMethod;
import org.json.JSONArray;

import java.util.*;

public class OrderAnalyzer {
    public static Map<String, Double> total_spent_per_method(
            JSONArray orders_json, JSONArray paymentMethods_json) {

        List<Order> orders = new ArrayList<>();
        for (int i=0; i<orders_json.length(); i++)
            orders.add(new Order(orders_json.getJSONObject(i)));

        Map<String, PaymentMethod> paymentMethods = new LinkedHashMap<>();
        Map<String, Double> totalSpent = new LinkedHashMap<>();
        for (int i=0; i<paymentMethods_json.length(); i++) {
            PaymentMethod m = new PaymentMethod(paymentMethods_json.getJSONObject(i));
            paymentMethods.put(m.id, m);
            totalSpent.put(m.id, 0.0);
        }

        PaymentMethod bonus_points_punkty = paymentMethods.get("PUNKTY");

        // Orders are sorted primarily by amount of PROMOTIONS and secondary with VALUE
        // This sort is made for better prioritizing ORDERS
        orders.sort(Comparator
                .comparingInt((Order o) -> -o.promotions.size())
                .thenComparing((Order o) -> -o.value));

        for (int i=0; i<orders.size(); i++) {
            Order order = orders.get(i);
            double order_value = order.value;
            String payment_method = null;
            double bonus_point_punkty_used = 0;
            double money_from_card_used = 0;
            boolean completed = false;

            // 1. Fully with PUNKTY
            if (bonus_points_punkty!=null && bonus_points_punkty.limit >= order_value) {
                double discounted = order_value * (1 - bonus_points_punkty.discount/100.0);
                payment_method = "PUNKTY";
                bonus_point_punkty_used = discounted;
                money_from_card_used = 0;
                completed = true;
            }

            // 2. Fully with eligible PROMOTION
            if (!completed) {
                for (String promo: order.promotions) {
                    PaymentMethod m = paymentMethods.get(promo);
                    if (m == null) continue;
                    double discounted = order_value * (1 - m.discount/100.0);
                    if (m.limit >= discounted) {
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
                double pointsToUse = decidePointsToUse(bonus_points_punkty, order_value, orders.subList(i+1, orders.size()), paymentMethods);
                double discountedTotal = order_value * 0.9;
                double remaining = discountedTotal - pointsToUse;

                for (PaymentMethod m: paymentMethods.values()) {
                    if (m.id.equals("PUNKTY")) continue;
                    if (m.limit >= remaining) {
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
                        payment_method = m.id + " + PUNKTY (no discount)";
                        money_from_card_used = payWithCard;
                        bonus_point_punkty_used = payWithPoints;
                        break;
                    }
                }
            }

            if (payment_method!=null) {
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
            }
        }

        String firstMethodId = paymentMethods.keySet().iterator().next();
        PaymentMethod firstMethod = paymentMethods.remove(firstMethodId);
        paymentMethods.put(firstMethodId, firstMethod);
        Double firstSpent = totalSpent.remove(firstMethodId);
        totalSpent.put(firstMethodId, firstSpent);
        return totalSpent;
    }

    public static double decidePointsToUse(PaymentMethod points, double orderAmount,
                                           List<Order> futureOrders, Map<String, PaymentMethod> paymentMethods) {
        double discountedTotal = orderAmount*0.9;
        double tenPercent = orderAmount*0.1;
        double restToPay = discountedTotal-tenPercent;

        Map<String, Double> simulatedFunds = new HashMap<>();
        for (Map.Entry<String, PaymentMethod> entry : paymentMethods.entrySet()) {
            simulatedFunds.put(entry.getKey(), entry.getValue().limit);
        }
        // simulation of our budget for understanding if we will have anough money to
        // pay for any next order

        boolean can_pay_current = false;
        for (PaymentMethod m: paymentMethods.values()) {
            if (m.id.equals("PUNKTY")) continue;
            if (simulatedFunds.get(m.id) >= restToPay && simulatedFunds.get("PUNKTY") >= tenPercent) {
                simulatedFunds.put(m.id, simulatedFunds.get(m.id) - restToPay);
                simulatedFunds.put("PUNKTY", simulatedFunds.get("PUNKTY") - tenPercent);
                can_pay_current = true;
                break;
            }
            else if (simulatedFunds.get(m.id) >= discountedTotal-simulatedFunds.get("PUNKTY") && simulatedFunds.get("PUNKTY") >= tenPercent) {
                simulatedFunds.put(m.id, discountedTotal - simulatedFunds.get(m.id));
                simulatedFunds.put("PUNKTY", simulatedFunds.get("PUNKTY") - simulatedFunds.get(m.id));
                break;
            }
        }

        boolean can_pay_future = false;
        if (!futureOrders.isEmpty()) {
            Order future = futureOrders.getLast();
            // Our list sorted in way that order with minimum value is last
            // so we need to check if there is at least one order [with minimum value]
            // that can use discount from paying 10% of value with PUNKTY
            double tenPercentFuture = future.value*0.1;
            double discountedFuture = future.value*0.9;

            if (simulatedFunds.get("PUNKTY") >= tenPercentFuture) {
                for (PaymentMethod m: paymentMethods.values()) {
                    if (m.id.equals("PUNKTY")) continue;
                    if (simulatedFunds.get(m.id) >= discountedFuture - simulatedFunds.get("PUNKTY")) {
                        can_pay_future = true;
                    }
                }
            }
        }

        if (can_pay_future && !can_pay_current) return points.limit - simulatedFunds.get("PUNKTY");
        if (can_pay_future) return tenPercent;
        return Math.min(points.limit, discountedTotal);
    }
}
