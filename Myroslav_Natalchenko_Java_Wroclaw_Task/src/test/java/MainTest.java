import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import java.util.*;

import org.example.data.*;

import static org.example.Main.decidePointsToUse;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

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

        double pointsUsed = decidePointsToUse(points, currentOrder.value, List.of(futureOrder), paymentMethods);

        assertEquals(10.0, pointsUsed, 0.01, "Should only use 10% points to preserve for future use");
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

        double used = decidePointsToUse(points, 100.0, new ArrayList<>(), methods);

        assertEquals(50.0, used, 0.01);
    }
}
