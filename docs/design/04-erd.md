## ERD

```
erDiagram
    USER {
        bigint id PK
        varchar email
        varchar password
        varchar gender
        date birth_date
        bigint point_balance
        boolean is_active
    }

    PRODUCT {
        bigint id PK
        varchar name
        varchar description
        bigint price
        int stock
        varchar status
    }

    CART {
        bigint id PK
        bigint user_id FK
    }

    CART_ITEM {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
    }

    

    PRODUCT_LIKE {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
    }

    USER_POINT_HISTORY {
        bigint id PK
        bigint user_id FK
        bigint amount
        varchar type
        timestamp created_at
    }

    ORDERS {
        bigint id PK
        bigint user_id FK
        bigint total_amount
        varchar status
        timestamp created_at
    }

    ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        bigint price_at_order
    }

    
    USER ||--o{ PRODUCT_LIKE : likes
    PRODUCT ||--o{ PRODUCT_LIKE : liked_by
    USER ||--o{ USER_POINT_HISTORY : manages
    USER ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEM : contains
    PRODUCT ||--o{ ORDER_ITEM : references
```