import org.example.OrderAnalyzer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import java.util.*;

import org.example.data.*;

import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    @Test
    public void ClassicSituationTest() {
        String ordersJson = """
            [
              { "id": "ORDER1", "value": "100.00", "promotions": [ "mZysk" ] },
              { "id": "ORDER2", "value": "200.00", "promotions": [ "BosBankrut" ] },
              { "id": "ORDER3", "value": "150.00", "promotions": [ "mZysk", "BosBankrut" ] },
              { "id": "ORDER4", "value": "50.00" }
            ]
        """;

        String paymentMethodsJson = """
            [
              { "id": "PUNKTY", "discount": "15", "limit": "100.00" },
              { "id": "mZysk", "discount": "10", "limit": "180.00" },
              { "id": "BosBankrut", "discount": "5", "limit": "200.00" }
            ]
        """;

        JSONArray ordersArray = new JSONArray(ordersJson);
        JSONArray methodsArray = new JSONArray(paymentMethodsJson);

        Map<String, Double> result = OrderAnalyzer.total_spent_per_method(ordersArray, methodsArray);

        assertEquals(165.00, result.get("mZysk"));
        assertEquals(190.00, result.get("BosBankrut"));
        assertEquals(100.00, result.get("PUNKTY"));
        // ORDER3 - 135 with mZysk, ORDER2 - 180 with BosBankrut, ORDER1 - 85 with PUNKTY
        // ORDER4 - 15 from PUNKTY + 30 from mZysk
    }

    @Test
    public void SituationWithNextExistingOrder_5_Test() {
        String ordersJson = """
            [
              { "id": "ORDER1", "value": "100.00", "promotions": [ "mZysk" ] },
              { "id": "ORDER2", "value": "200.00", "promotions": [ "BosBankrut" ] },
              { "id": "ORDER3", "value": "150.00", "promotions": [ "mZysk", "BosBankrut" ] },
              { "id": "ORDER4", "value": "50.00" },
              { "id": "ORDER5", "value": "20.00" }
            ]
        """;

        String paymentMethodsJson = """
            [
              { "id": "PUNKTY", "discount": "15", "limit": "100.00" },
              { "id": "mZysk", "discount": "10", "limit": "180.00" },
              { "id": "BosBankrut", "discount": "5", "limit": "200.00" }
            ]
        """;

        JSONArray ordersArray = new JSONArray(ordersJson);
        JSONArray methodsArray = new JSONArray(paymentMethodsJson);

        Map<String, Double> result = OrderAnalyzer.total_spent_per_method(ordersArray, methodsArray);

        assertEquals(175.00, result.get("mZysk"));
        assertEquals(198.00, result.get("BosBankrut"));
        assertEquals(100.00, result.get("PUNKTY"));
        // ORDER3 - 135 with mZysk, ORDER2 - 180 with BosBankrut, ORDER1 - 85 with PUNKTY
        // ORDER4 - 5 from PUNKTY + 40 from mZysk, ORDER5 - 10 from PUNKTY + 8 from BosBankrut
    }

    @Test
    public void SituationWithSkippingOrder_5_Test() {
        String ordersJson = """
            [
              { "id": "ORDER1", "value": "100.00", "promotions": [ "mZysk" ] },
              { "id": "ORDER2", "value": "200.00", "promotions": [ "BosBankrut" ] },
              { "id": "ORDER3", "value": "150.00", "promotions": [ "mZysk", "BosBankrut" ] },
              { "id": "ORDER4", "value": "50.00" },
              { "id": "ORDER5", "value": "50.00" },
              { "id": "ORDER5", "value": "20.00" }
            ]
        """;

        String paymentMethodsJson = """
            [
              { "id": "PUNKTY", "discount": "15", "limit": "100.00" },
              { "id": "mZysk", "discount": "10", "limit": "180.00" },
              { "id": "BosBankrut", "discount": "5", "limit": "200.00" }
            ]
        """;

        JSONArray ordersArray = new JSONArray(ordersJson);
        JSONArray methodsArray = new JSONArray(paymentMethodsJson);

        Map<String, Double> result = OrderAnalyzer.total_spent_per_method(ordersArray, methodsArray);

        assertEquals(175.00, result.get("mZysk"));
        assertEquals(198.00, result.get("BosBankrut"));
        assertEquals(100.00, result.get("PUNKTY"));
        // ORDER3 - 135 with mZysk, ORDER2 - 180 with BosBankrut, ORDER1 - 85 with PUNKTY
        // ORDER4 - 5 from PUNKTY + 40 from mZysk, ORDER6 - 10 from PUNKTY + 8 from BosBankrut
        // ORDER5 - skipped
    }

    @Test
    public void NeedMoreThan10ProcentButLessThatAllPunkty_Test() {
        String ordersJson = """
            [
              { "id": "ORDER1", "value": "100.00", "promotions": [ "mZysk" ] },
              { "id": "ORDER2", "value": "200.00", "promotions": [ "BosBankrut" ] },
              { "id": "ORDER3", "value": "150.00", "promotions": [ "mZysk", "BosBankrut" ] },
              { "id": "ORDER4", "value": "50.00" },
              { "id": "ORDER5", "value": "15.00" }
            ]
        """;

        String paymentMethodsJson = """
            [
              { "id": "PUNKTY", "discount": "15", "limit": "100.00" },
              { "id": "mZysk", "discount": "10", "limit": "172.00" },
              { "id": "BosBankrut", "discount": "5", "limit": "200.00" }
            ]
        """;

        JSONArray ordersArray = new JSONArray(ordersJson);
        JSONArray methodsArray = new JSONArray(paymentMethodsJson);

        Map<String, Double> result = OrderAnalyzer.total_spent_per_method(ordersArray, methodsArray);

        assertEquals(172.00, result.get("mZysk"));
        assertEquals(196.50, result.get("BosBankrut"));
        assertEquals(100.00, result.get("PUNKTY"));
    }

    @Test
    public void testDecidePointsToUse_SavesPointsForFuture() {
        PaymentMethod points = new PaymentMethod(new JSONObject()
                .put("id", "PUNKTY")
                .put("discount", "15")
                .put("limit", "100.00")
        );

        PaymentMethod mZysk = new PaymentMethod(new JSONObject()
                .put("id", "mZysk")
                .put("discount", "10")
                .put("limit", "180.00")
        );

        Map<String, PaymentMethod> paymentMethods = new HashMap<>();
        paymentMethods.put("PUNKTY", points);
        paymentMethods.put("mZysk", mZysk);

        Order currentOrder = new Order(new JSONObject()
                .put("id", "ORDER1")
                .put("value", 100.00)
        );

        Order futureOrder = new Order(new JSONObject()
                .put("id", "ORDER2")
                .put("value", 200.00)
                .put("promotions", new ArrayList<>(List.of("mZysk")))
        );

        double pointsUsed = OrderAnalyzer.decidePointsToUse(points, currentOrder.value, List.of(futureOrder), paymentMethods);

        assertEquals(10, pointsUsed);
    }

    @Test
    public void testDecidePointsToUse_NoFutureOrders() {
        PaymentMethod points = new PaymentMethod(new JSONObject()
                .put("id", "PUNKTY")
                .put("discount", "15")
                .put("limit", "50.0")
        );

        PaymentMethod card = new PaymentMethod(new JSONObject()
                .put("id", "mZysk")
                .put("discount", "10")
                .put("limit", "200.0")
        );

        Map<String, PaymentMethod> methods = new HashMap<>();
        methods.put("PUNKTY", points);
        methods.put("mZysk", card);

        double used = OrderAnalyzer.decidePointsToUse(points, 100.0, new ArrayList<>(), methods);

        assertEquals(50.0, used);
    }
}
