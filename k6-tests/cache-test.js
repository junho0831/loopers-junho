import http from 'k6/http';
import { check, sleep } from 'k6';

// 캐시 성능 테스트
export const options = {
  vus: 20, // 20명 동시 사용자
  duration: '1m', // 1분간
};

const BASE_URL = 'http://localhost:8080/api/v1';

export default function () {
  // 같은 상품을 반복 조회하여 캐시 효과 측정
  let productId = 1; // 고정된 상품 ID로 캐시 테스트
  
  let response = http.get(`${BASE_URL}/products/${productId}`, {
    tags: { name: 'cache_test' },
  });
  
  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 100ms (cache hit expected)': (r) => r.timings.duration < 100,
  });
  
  // 캐시 히트율을 높이기 위해 같은 요청을 여러 번
  for (let i = 0; i < 3; i++) {
    http.get(`${BASE_URL}/products/${productId}`);
    sleep(0.1);
  }
}