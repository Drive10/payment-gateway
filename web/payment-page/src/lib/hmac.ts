const SECRET_KEY = import.meta.env.VITE_HMAC_SECRET_KEY || 'default-dev-secret-key';

export async function generateHMACSignature(payload: string): Promise<string> {
  const encoder = new TextEncoder();
  const keyData = encoder.encode(SECRET_KEY);
  
  const key = await crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  
  const signature = await crypto.subtle.sign(
    'HMAC',
    key,
    encoder.encode(payload)
  );
  
  return Array.from(new Uint8Array(signature))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function signPayload(payload: object): Promise<string> {
  const payloadString = JSON.stringify(payload);
  return generateHMACSignature(payloadString);
}
