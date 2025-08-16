import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const productListRequests = new Counter('product_list_requests');
const productListErrorRate = new Rate('product_list_errors');
const productListDuration = new Trend('product_list_duration');

// 성능 테스트 옵션
export const options = {
  scenarios: {
    // 시나리오 1: 브랜드 필터 + 좋아요 정렬 (UC1)
    brand_likes_sort: {
      executor: 'constant-vus',
      vus: 50,
      duration: '2m',
      tags: { scenario: 'UC1_brand_likes' },
    },
    // 시나리오 2: 전체 상품 좋아요순 정렬 (UC2) 
    global_likes_sort: {
      executor: 'constant-vus', 
      vus: 30,
      duration: '2m',
      tags: { scenario: 'UC2_global_likes' },
    },
    // 시나리오 3: 브랜드 필터 + 가격순 정렬 (UC3)
    brand_price_sort: {
      executor: 'constant-vus',
      vus: 40,
      duration: '2m', 
      tags: { scenario: 'UC3_brand_price' },
    },
    // 시나리오 4: 가격 범위 필터링 (UC4)
    price_range_filter: {
      executor: 'constant-vus',
      vus: 35,
      duration: '2m',
      tags: { scenario: 'UC4_price_range' },
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% 요청이 500ms 이내
    http_req_failed: ['rate<0.05'],   // 에러율 5% 미만
    'product_list_duration': ['p(90)<200'], // 90% 요청이 200ms 이내
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// 브랜드 ID 풀 (1-50)
const BRAND_IDS = Array.from({length: 50}, (_, i) => i + 1);

// 가격 범위 (다양한 범위로 테스트)
const PRICE_RANGES = [
  { min: 50000, max: 200000 },
  { min: 100000, max: 300000 },
  { min: 200000, max: 500000 },
  { min: 300000, max: 800000 }
];

export default function () {
  const scenario = __ENV.K6_SCENARIO || 'UC1_brand_likes';
  
  switch(scenario) {
    case 'UC1_brand_likes':
      testBrandWithLikesSort();
      break;
    case 'UC2_global_likes': 
      testGlobalLikesSort();
      break;
    case 'UC3_brand_price':
      testBrandWithPriceSort();
      break;
    case 'UC4_price_range':
      testPriceRangeFilter();
      break;
    default:
      // 랜덤하게 모든 유즈케이스 테스트
      const testCase = Math.floor(Math.random() * 4);
      switch(testCase) {
        case 0: testBrandWithLikesSort(); break;
        case 1: testGlobalLikesSort(); break;
        case 2: testBrandWithPriceSort(); break;
        case 3: testPriceRangeFilter(); break;
      }
  }
}

// UC1: 브랜드 필터 + 좋아요 정렬 테스트
function testBrandWithLikesSort() {
  const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
  const page = Math.floor(Math.random() * 5); // 0-4 페이지
  
  const startTime = Date.now();
  
  const response = http.get(
    `${BASE_URL}/products?brandId=${brandId}&sortType=LIKES_DESC&page=${page}&size=20`,
    {
      tags: { 
        name: 'UC1_brand_likes_sort',
        brand_id: brandId.toString(),
      },
    }
  );
  
  const duration = Date.now() - startTime;
  productListDuration.add(duration);
  productListRequests.add(1);
  
  const success = check(response, {
    'UC1 status is 200': (r) => r.status === 200,
    'UC1 response time < 100ms': (r) => r.timings.duration < 100,
    'UC1 has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.content && body.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!success) {
    productListErrorRate.add(1);
  }
  
  sleep(Math.random() * 0.5 + 0.1); // 0.1-0.6초 대기
}

// UC2: 전체 좋아요순 정렬 테스트  
function testGlobalLikesSort() {
  const page = Math.floor(Math.random() * 10); // 0-9 페이지
  
  const startTime = Date.now();
  
  const response = http.get(
    `${BASE_URL}/products?sortType=LIKES_DESC&page=${page}&size=20`,
    {
      tags: { name: 'UC2_global_likes_sort' },
    }
  );
  
  const duration = Date.now() - startTime;
  productListDuration.add(duration);
  productListRequests.add(1);
  
  const success = check(response, {
    'UC2 status is 200': (r) => r.status === 200,
    'UC2 response time < 200ms': (r) => r.timings.duration < 200,
    'UC2 has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.content && body.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!success) {
    productListErrorRate.add(1);
  }
  
  sleep(Math.random() * 0.3 + 0.1); // 0.1-0.4초 대기
}

// UC3: 브랜드 필터 + 가격순 정렬 테스트
function testBrandWithPriceSort() {
  const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
  const sortType = Math.random() > 0.5 ? 'PRICE_ASC' : 'PRICE_DESC';
  const page = Math.floor(Math.random() * 3); // 0-2 페이지
  
  const startTime = Date.now();
  
  const response = http.get(
    `${BASE_URL}/products?brandId=${brandId}&sortType=${sortType}&page=${page}&size=20`,
    {
      tags: { 
        name: 'UC3_brand_price_sort',
        brand_id: brandId.toString(),
        sort_type: sortType,
      },
    }
  );
  
  const duration = Date.now() - startTime;
  productListDuration.add(duration);
  productListRequests.add(1);
  
  const success = check(response, {
    'UC3 status is 200': (r) => r.status === 200,
    'UC3 response time < 150ms': (r) => r.timings.duration < 150,
    'UC3 has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.content && body.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!success) {
    productListErrorRate.add(1);
  }
  
  sleep(Math.random() * 0.4 + 0.1); // 0.1-0.5초 대기
}

// UC4: 가격 범위 필터링 테스트 (실제 구현되면 활성화)
function testPriceRangeFilter() {
  // 현재는 브랜드 필터로 대체 (가격 범위 API가 없는 경우)
  const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
  const page = Math.floor(Math.random() * 3);
  
  const startTime = Date.now();
  
  // 가격 범위 API가 구현되면 아래와 같이 변경
  // const priceRange = PRICE_RANGES[Math.floor(Math.random() * PRICE_RANGES.length)];
  // const url = `${BASE_URL}/products?minPrice=${priceRange.min}&maxPrice=${priceRange.max}&sortType=PRICE_ASC&page=${page}&size=20`;
  
  const response = http.get(
    `${BASE_URL}/products?brandId=${brandId}&sortType=PRICE_ASC&page=${page}&size=20`,
    {
      tags: { 
        name: 'UC4_price_range_filter',
        brand_id: brandId.toString(),
      },
    }
  );
  
  const duration = Date.now() - startTime;
  productListDuration.add(duration);
  productListRequests.add(1);
  
  const success = check(response, {
    'UC4 status is 200': (r) => r.status === 200,
    'UC4 response time < 120ms': (r) => r.timings.duration < 120,
    'UC4 has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.content && body.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!success) {
    productListErrorRate.add(1);
  }
  
  sleep(Math.random() * 0.3 + 0.1); // 0.1-0.4초 대기
}

// 테스트 완료 시 요약 정보 출력
export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'performance-summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data, options = {}) {
  const { indent = '', enableColors = false } = options;
  
  let summary = `
${indent}=====================================
${indent}  상품 조회 성능 테스트 결과 요약
${indent}=====================================

${indent}📊 총 요청 수: ${data.metrics.http_reqs ? data.metrics.http_reqs.count : 'N/A'}
${indent}❌ 실패 요청: ${data.metrics.http_req_failed ? Math.round(data.metrics.http_req_failed.rate * 100) : 'N/A'}%
${indent}⏱️  평균 응답시간: ${data.metrics.http_req_duration ? Math.round(data.metrics.http_req_duration.avg) : 'N/A'}ms
${indent}🎯 95% 응답시간: ${data.metrics.http_req_duration ? Math.round(data.metrics.http_req_duration['p(95)']) : 'N/A'}ms
${indent}🚀 처리량 (RPS): ${data.metrics.http_reqs ? Math.round(data.metrics.http_reqs.rate) : 'N/A'} req/s

${indent}유즈케이스별 상세 분석:
${indent}- UC1 (브랜드+좋아요): 목표 < 100ms
${indent}- UC2 (전체 좋아요순): 목표 < 200ms  
${indent}- UC3 (브랜드+가격): 목표 < 150ms
${indent}- UC4 (가격 범위): 목표 < 120ms
${indent}=====================================
`;

  return summary;
}