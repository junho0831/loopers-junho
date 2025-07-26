# 시퀀스 다이어그램

## 1. 상품 좋아요 기능

```
sequenceDiagram
    participant U as User
    participant LC as LikeController
    participant PS as ProductService
    participant LS as LikeService
    participant LR as LikeRepository

    U->>LC: 좋아요/좋아요 취소 요청 (productId)
    LC->>LS: 좋아요/좋아요 취소 처리 (userId, productId)
    alt 사용자 인증 실패 (X-USER-ID 누락 또는 미존재)
        LS-->>LC: 401 Unauthorized / 404 Not Found
    else
        LS->>PS: 상품 존재 여부 확인 (productId)
        alt 상품 미존재
            PS-->>LS: 404 Not Found
            LS-->>LC: 404 Not Found
        else
            PS-->>LS: 상품 정보 반환
            LS->>LR: 좋아요 기록 조회 (userId, productId)
            alt 좋아요 기록 존재
                LR-->>LS: 좋아요 기록 반환
                LS->>LR: 좋아요 기록 삭제
                LR-->>LS: 삭제 성공
            else 좋아요 기록 없음
                LR-->>LS: 기록 없음
                LS->>LR: 새 좋아요 기록 저장
                LR-->>LS: 저장 성공
            end
            alt 좋아요 저장/삭제 실패
                LR-->>LS: 500 Internal Server Error
                LS-->>LC: 500 Internal Server Error
            else 좋아요 저장/삭제 성공
                LS-->>LC: 200 OK
            end
        end
    end
```

## 2. 장바구니 담기 기능

```
sequenceDiagram
    participant U as User
    participant CC as CartController
    participant US as UserService
    participant CS as CartService
    participant PS as ProductService
    participant CR as CartRepository

    U->>CC: 장바구니 담기 요청 (productId, quantity)
    CC->>US: 사용자 인증 확인 (X-USER-ID)
    alt 인증 실패 (사용자 미존재, 헤더 미존재)
        US-->>CC: 401 Unauthorized
    else 인증 성공
        US-->>CC: 사용자 정보 반환
        CC->>PS: 상품 상태 조회 (productId)
        alt 상품이 없음
            PS-->>CC: 404 Not Found
        else 판매중이 아님
            PS-->>CC: 409 Conflict
        else
            PS-->>CC: 상품 정보 반환
            CC->>CS: 장바구니 처리 요청 (userId, productId, quantity)
            CS->>CR: 장바구니 항목 조회 (userId, productId)
            alt 항목 존재
                CS->>CR: 수량 증가 후 저장
            else 항목 없음
                CS->>CR: 새 항목 추가 후 저장
            end
            alt 저장 실패 ( 사유 불문 )
                CR-->>CS: 500 Internal Server Error
            else 저장 성공
                CR-->>CS: 장바구니 반영 결과
            end
        end
    end
```

## 3. 주문 생성 기능

```
sequenceDiagram
    participant U as User
    participant OC as OrderController
    participant OS as OrderService
    participant PS as ProductService
    participant US as UserService
    participant OR as OrderRepository
    participant PR as ProductRepository
    participant UR as UserRepository
    participant EPS as ExternalPaymentSystem

    U->>OC: 주문 생성 요청 (orderItems, totalAmount)
    OC->>OS: 주문 처리 (userId, orderItems, totalAmount)
    alt 사용자 인증 실패 (X-USER-ID 누락 또는 미존재)
        OS-->>OC: 401 Unauthorized / 404 Not Found
    else
        OS->>US: 사용자 포인트 잔액 조회 (userId)
        US-->>OS: 사용자 포인트 잔액 반환
        alt 포인트 부족
            OS-->>OC: 400 Bad Request
        else
            OS->>PS: 각 상품 재고 및 판매 상태 확인 (orderItems)
            alt 재고 부족 또는 판매 중단 상품 포함
                PS-->>OS: 409 Conflict
                OS-->>OC: 409 Conflict
            else
                PS-->>OS: 모든 상품 유효
                OS->>UR: 사용자 포인트 차감 (userId, totalAmount)
                alt 포인트 차감 실패
                    UR-->>OS: 500 Internal Server Error
                    OS-->>OC: 500 Internal Server Error
                else
                    UR-->>OS: 포인트 차감 성공
                    OS->>PR: 각 상품 재고 차감 (orderItems)
                    alt 재고 차감 실패
                        PR-->>OS: 500 Internal Server Error
                        OS-->>OC: 500 Internal Server Error
                    else
                        PR-->>OS: 재고 차감 성공
                        OS->>OR: 주문 정보 저장 (orderData)
                        alt 주문 저장 실패
                            OR-->>OS: 500 Internal Server Error
                            OS-->>OC: 500 Internal Server Error
                        else
                            OR-->>OS: 주문 저장 성공
                            OS->>EPS: 외부 결제 시스템으로 주문 정보 전송 (orderData)
                            alt 외부 시스템 연동 실패 (비동기 처리 가능)
                                EPS-->>OS: (비동기) 연동 실패 알림
                                OS-->>OC: 201 Created (주문은 저장됨)
                            else 외부 시스템 연동 성공
                                EPS-->>OS: (비동기) 연동 성공 알림
                                OS-->>OC: 201 Created
                            end
                        end
                    end
                end
            end
        end
    end
```

## 4. 상품 목록 조회 기능

```
sequenceDiagram
    participant U as User
    participant PC as ProductController
    participant PS as ProductService
    participant PR as ProductRepository

    U->>PC: 상품 목록 조회 요청 (필터, 정렬, 페이지)
    PC->>PS: 상품 목록 조회 처리 (queryParameters)
    alt 유효하지 않은 파라미터
        PS-->>PC: 400 Bad Request
    else
        PS->>PR: 조건에 맞는 상품 목록 조회 (queryParameters)
        alt 조회 실패
            PR-->>PS: 500 Internal Server Error
            PS-->>PC: 500 Internal Server Error
        else 조회 성공
            PR-->>PS: 상품 목록 반환
            PS-->>PC: 200 OK (상품 목록 포함)
        end
    end
```