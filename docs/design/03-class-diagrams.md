## 클래스 다이어그램 ( 도메인 중심 설계 )

```
%% + : public
%% - : private
%% # : protected
classDiagram
    class User {
        +String id
        +String email
        +String password
        +Gender gender
        +LocalDate birthDate
        +long pointBalance
        +boolean isActive
    }

    class Product {
        +long id
        +String name
        +String description
        +long price
        +int stock
        +ProductStatus status
    }

    class Cart {
        -List~CartItem~ items
        +addItem(Product, quantity)
    }

    class CartItem {
        -Product product
        -int quantity
        +increaseQuantity()
    }

    class Like {
        -String userId
        -long productId
    }

    class Order {
        -User user
        -List~OrderItem~ items
        -long totalAmount
        -OrderStatus status
        +createOrder(User, List~OrderItem~)
    }

    class OrderItem {
        -Product product
        -int quantity
        -long priceAtOrder
    }

    class Gender {
        <<enumeration>>
        MALE
        FEMALE
        OTHER
    }

    class ProductStatus {
        <<enumeration>>
        AVAILABLE
        SOLD_OUT
        DISCONTINUED
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        COMPLETED
        CANCELLED
    }

    User "1" -- "1" Cart : has
    Cart "1" -- "*" CartItem : contains
    CartItem --> Product : references
    
    User "1" -- "*" Like : likes
    Product "1" -- "*" Like : liked by
    User "1" -- "*" Order : places
    Order "1" -- "*" OrderItem : contains
    OrderItem --> Product : references
```