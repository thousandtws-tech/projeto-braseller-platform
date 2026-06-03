export const environment = {
  production: false,
  apiUrl: 'https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io/api',
  auth: {
    clientId: 'auth-service',
    realm: 'braseller',
    authority: 'https://gateway-api.salmonrock-4d3f2812.brazilsouth.azurecontainerapps.io/api/auth'
  },
  tenantHeaderKey: 'X-Tenant-Id'
};
