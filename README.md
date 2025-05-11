## Project Structure

```
/org.example
│
├─ Main.java               # Main
├─ OrderAnalyzer.java      # Logic for selecting optimal payment methods
│
├─ data/
│   ├─ Order.java          # Order Class
│   └─ PaymentMethod.java  # Payment Method Class
```

# Payment Strategy

1. **If a product can be fully paid using bonus points (PUNKTY),** then this transaction is executed — even if the product has PROMOTIONS and one of the promotional payment methods can also fully cover the cost — because the reward for full payment with PUNKTY is higher.

2. **If we have enough PUNKTY to cover 10% of the product's value,** we first check whether the product has any PROMOTIONS. If the best available promotion provides a discount equal to or greater than 10%, we do not use PUNKTY, and instead use the bonus offered for full payment using one of the listed PROMOTION methods.

3. **If the condition in point 2 is not met,** we check the remaining ORDERS in the list to see if any of them could also benefit from a 10% PUNKTY payment.
    - If no such ORDER exists, we use the **maximum possible amount of PUNKTY** on the current order.
    - If there are such orders, we spend only **10% in PUNKTY** on the current order (or more, if none of the available payment methods can fully cover the rest of the amount, but adding more PUNKTY would make it possible). The remainder is paid using a method that has sufficient funds.
    - We also check whether, **after paying for the current order, there will be enough PUNKTY left** to cover at least 10% of the next order. If not, we use **all available PUNKTY** on the current order.

4. **If none of the above conditions are met,** we try to pay for the order using the remaining PUNKTY plus one of the available payment methods.
