export const BASE_URL = __ENV.BASE_URL || 'https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io';

export const AUTH_USER = {
  email: __ENV.TEST_EMAIL || 'thousandtws@gmail.com',
  password: __ENV.TEST_PASSWORD || '65301527',
};

export const THRESHOLDS = {
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  http_req_failed: ['rate<0.05'],
};
