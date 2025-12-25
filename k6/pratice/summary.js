import http from 'k6/http'
import { check, sleep } from 'k6'
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

export const options = {
  vus: 1,
  iterations: 5	
};

export default (data) => {
  const url = 'http://localhost:8080/api/v1/products'
  const res = http.get(url);
  
  check(res, {
    'response 200' : (r) => r.status === 200,
  });

  sleep(1);
};

export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data),
  };
}
