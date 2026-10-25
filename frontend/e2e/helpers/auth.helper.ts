export const BACKEND_URL = process.env.VITE_API_URL || 'http://localhost:18080';
export const KEYCLOAK_URL = process.env.KEYCLOAK_SERVER_URL || 'http://localhost:18081';
export const REALM = 'hotel-acceptance';
export const CLIENT_ID = 'hotel-frontend';

export interface KeycloakTokenResponse {
  access_token: string;
  expires_in: number;
  refresh_token: string;
  token_type: string;
}

export async function getKeycloakToken(
  username: string,
  password = 'Password123!'
): Promise<string> {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
  const params = new URLSearchParams();
  params.append('grant_type', 'password');
  params.append('client_id', CLIENT_ID);
  params.append('username', username);
  params.append('password', password);

  const res = await fetch(tokenUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Failed to obtain Keycloak token for ${username}: ${res.status} ${text}`);
  }

  const data = (await res.json()) as KeycloakTokenResponse;
  return data.access_token;
}
