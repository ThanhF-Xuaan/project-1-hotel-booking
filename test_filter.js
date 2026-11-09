const http = require('http');

async function main() {
  // 1. Get token
  const tokenReq = await fetch('http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: 'hotel-frontend',
      username: 'admin',
      password: 'password', // wait, password was 123456
    })
  });
  let tokenData = await tokenReq.json();
  if (!tokenData.access_token) {
    const tokenReq2 = await fetch('http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: 'hotel-frontend',
        username: 'admin',
        password: '123456',
      })
    });
    tokenData = await tokenReq2.json();
  }
  console.log('Got token:', tokenData.access_token ? 'YES' : tokenData);

  // 2. Call filter endpoint
  const res = await fetch('http://localhost:8080/api/v1/maintenance-blocks/filter', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${tokenData.access_token}`
    },
    body: JSON.stringify({
      hotelId: 1,
      page: 0,
      pageSize: 10
    })
  });

  const status = res.status;
  const data = await res.json();
  console.log('Filter API Status:', status);
  console.log('Filter API Response:', JSON.stringify(data, null, 2));
}

main().catch(console.error);

