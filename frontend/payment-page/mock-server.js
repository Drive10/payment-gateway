import express from 'express';
import cors from 'cors';

const app = express();
const PORT = 3001;

app.use(cors());
app.use(express.json());

const transactions = new Map();
const paymentLinks = new Map();

paymentLinks.set('TEST123', {
  id: 'link_123',
  referenceId: 'TEST123',
  amount: 100,
  currency: 'INR',
  merchantName: 'Demo Merchant',
  description: 'Test Payment',
  status: 'PENDING',
  createdAt: new Date().toISOString()
});

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

app.post('/api/v1/auth/login', async (req, res) => {
  await delay(300);
  const { email, password } = req.body;
  
  if (email && password && password.length >= 6) {
    return res.json({
      success: true,
      data: {
        accessToken: 'mock_jwt_token_' + Date.now(),
        refreshToken: 'mock_refresh_' + Date.now(),
        expiresIn: 3600,
        user: { id: '1', email: email, name: email.split('@')[0] }
      }
    });
  }
  
  res.status(401).json({ success: false, message: 'Invalid credentials' });
});

app.post('/api/v1/auth/register', async (req, res) => {
  await delay(500);
  const { email, password, firstName, lastName } = req.body;
  
  const existingUsers = ['user@test.com'];
  if (existingUsers.includes(email)) {
    return res.status(400).json({ success: false, message: 'User already exists' });
  }
  
  return res.json({
    success: true,
    data: {
      accessToken: 'mock_jwt_token_' + Date.now(),
      refreshToken: 'mock_refresh_' + Date.now(),
      expiresIn: 3600,
      user: { id: '2', email: email, name: `${firstName || 'Test'} ${lastName || 'User'}` }
    }
  });
});
  
app.post('/api/v1/auth/refresh', async (req, res) => {
  await delay(200);
  const { refreshToken } = req.body;
  
  if (refreshToken) {
    return res.json({
      success: true,
      data: {
        accessToken: 'mock_jwt_token_' + Date.now(),
        refreshToken: 'mock_refresh_' + Date.now(),
        expiresIn: 3600
      }
    });
  }
  
  res.status(401).json({ success: false, message: 'Invalid refresh token' });
});
  
app.get('/api/v1/payments/:transactionId', async (req, res) => {
  await delay(200);
  const { transactionId } = req.params;
  const transaction = transactions.get(transactionId);
  
  if (transaction) {
    return res.json({ success: true, data: transaction });
  }
  
  res.json({ success: false, message: 'Transaction not found' });
});

app.post('/api/v1/payments/create-order', async (req, res) => {
  await delay(500);
  
  const { orderId, amount, currency, paymentMethod, transactionMode } = req.body;
  const txId = 'TXN_' + Date.now().toString(36).toUpperCase();
  
  const transaction = {
    id: txId,
    orderId: orderId || 'ORD_' + Date.now().toString(36).toUpperCase(),
    amount: parseFloat(amount),
    currency: currency || 'INR',
    paymentMethod: paymentMethod || 'CARD',
    transactionMode: transactionMode || 'TEST',
    status: 'CREATED',
    createdAt: new Date().toISOString(),
    description: req.body.description || 'Payment for order'
  };
  
  transactions.set(txId, transaction);
  
  res.json({
    success: true,
    data: {
      paymentId: txId,
      transactionId: txId,
      orderId: transaction.orderId,
      amount: transaction.amount,
      currency: transaction.currency,
      status: 'CREATED',
      redirectUrl: transactionMode === 'PRODUCTION' 
        ? '/processing' 
        : `/processing?txn=${txId}`
    }
  });
});

app.post('/api/v1/payments/:transactionId/process', async (req, res) => {
  await delay(800);
  
  const { transactionId } = req.params;
  const { cardNumber, expiry, cvv, cardholder } = req.body;
  
  const card = cardNumber?.replace(/\s/g, '');
  
  let status = 'AUTHORIZATION_PENDING';
  let nextAction = { type: 'OTP', method: 'card' };
  
  if (card?.startsWith('4000')) {
    status = 'FAILED';
    nextAction = null;
  } else if (card?.startsWith('4002')) {
    nextAction = { type: '3DS', url: '/three-ds?txn=' + transactionId };
    status = 'CHALLENGE_REQUIRED';
  } else if (card?.startsWith('4003')) {
    nextAction = { type: 'OTP', method: 'card' };
    status = 'AUTHORIZATION_PENDING';
  }
  
  const transaction = transactions.get(transactionId);
  if (transaction) {
    transaction.status = status;
    transaction.nextAction = nextAction;
  }
  
  res.json({
    success: true,
    data: {
      transactionId,
      status,
      nextAction,
      message: status === 'SUCCESS' ? 'Payment successful' : status === 'FAILED' ? 'Payment failed' : 'Additional verification required'
    }
  });
});

app.post('/api/v1/payments/:transactionId/verify-otp', async (req, res) => {
  await delay(300);
  
  const { transactionId } = req.params;
  const { otp } = req.body;
  
  if (otp === '123456' || otp === '000000') {
    const transaction = transactions.get(transactionId);
    if (transaction) transaction.status = 'CAPTURED';
    
    return res.json({
      success: true,
      data: { status: 'CAPTURED', message: 'OTP verified successfully' }
    });
  }
  
res.json({ success: false, message: 'Invalid OTP' });
});

app.post('/api/v1/payments/:transactionId/verify-3ds', async (req, res) => {
  await delay(500);
  const { transactionId } = req.params;
  const { status } = req.body;
  
  const transaction = transactions.get(transactionId);
  if (transaction) {
    transaction.status = status === 'success' ? 'CAPTURED' : 'FAILED';
  }
  
  res.json({
    success: true,
    data: {
      transactionId,
      status: transaction?.status || 'CAPTURED',
      message: '3DS verification completed'
    }
  });
});
  
app.get('/api/v1/payments/link/:referenceId', async (req, res) => {
  await delay(200);
  
  const { referenceId } = req.params;
  const link = paymentLinks.get(referenceId);
  
  if (link) {
    return res.json({ success: true, data: link });
  }
  
  res.json({ success: false, message: 'Payment link not found' });
});

app.get('/api/v1/payments/:transactionId/status', async (req, res) => {
  await delay(200);
  
  const { transactionId } = req.params;
  const transaction = transactions.get(transactionId);
  
  if (transaction) {
    return res.json({ success: true, data: transaction });
  }
  
  res.json({ success: false, message: 'Transaction not found' });
});

app.post('/api/v1/payments/upiv2/create', async (req, res) => {
  await delay(300);
  
  const { amount, vpa, transactionMode } = req.body;
  const txId = 'UPI_' + Date.now().toString(36).toUpperCase();
  
  const transaction = {
    id: txId,
    amount: parseFloat(amount),
    paymentMethod: 'UPI',
    vpa: vpa,
    status: 'PENDING',
    qrCode: 'upi://pay?pa=merchant@upi&pn=Merchant&am=' + amount + '&cu=INR&tn=' + txId,
    createdAt: new Date().toISOString()
  };
  
  transactions.set(txId, transaction);
  
  res.json({
    success: true,
    data: {
      transactionId: txId,
      qrCode: transaction.qrCode,
      status: 'PENDING'
    }
  });
});

app.post('/api/v1/payments/upiv2/:transactionId/check-status', async (req, res) => {
  await delay(200);
  
  const { transactionId } = req.params;
  const { vpa } = req.body;
  const transaction = transactions.get(transactionId);
  
  if (transaction) {
    let newStatus = 'PENDING';
    const vpaLower = (vpa || transaction.vpa || '').toLowerCase();
    
    if (vpaLower.includes('success') || vpaLower.includes('pass')) {
      newStatus = 'SUCCESS';
    } else if (vpaLower.includes('fail') || vpaLower.includes('error')) {
      newStatus = 'FAILED';
    } else if (Math.random() > 0.3) {
      newStatus = 'SUCCESS';
    }
    
    transaction.status = newStatus;
    return res.json({ success: true, data: { status: newStatus } });
  }
  
  res.json({ success: false, message: 'Transaction not found' });
});

app.post('/api/v1/payments/netbanking/initiate', async (req, res) => {
  await delay(400);
  
  const { amount, bankCode, transactionMode } = req.body;
  const txId = 'NB_' + Date.now().toString(36).toUpperCase();
  
  const transaction = {
    id: txId,
    amount: parseFloat(amount),
    paymentMethod: 'NETBANKING',
    bankCode: bankCode,
    status: 'PENDING',
    redirectUrl: '/processing?txn=' + txId,
    createdAt: new Date().toISOString()
  };
  
  transactions.set(txId, transaction);
  
  res.json({
    success: true,
    data: {
      transactionId: txId,
      redirectUrl: transaction.redirectUrl,
      status: 'PENDING'
    }
  });
});

app.post('/api/v1/payments/wallet/initiate', async (req, res) => {
  await delay(400);
  
  const { amount, wallet, transactionMode } = req.body;
  const txId = 'WL_' + Date.now().toString(36).toUpperCase();
  
  const transaction = {
    id: txId,
    amount: parseFloat(amount),
    paymentMethod: 'WALLET',
    wallet: wallet,
    status: 'PENDING',
    redirectUrl: '/processing?txn=' + txId,
    createdAt: new Date().toISOString()
  };
  
  transactions.set(txId, transaction);
  
  res.json({
    success: true,
    data: {
      transactionId: txId,
      redirectUrl: transaction.redirectUrl,
      status: 'PENDING'
    }
  });
});

app.post('/api/v1/payments/:transactionId/capture', async (req, res) => {
  await delay(500);
  
  const { transactionId } = req.params;
  const transaction = transactions.get(transactionId);
  
  if (transaction) {
    transaction.status = 'CAPTURED';
    return res.json({ success: true, data: transaction });
  }
  
  res.json({ success: false, message: 'Transaction not found' });
});

app.get('/api/v1/banks', (req, res) => {
  res.json({
    success: true,
    data: [
      { code: 'SBIN', name: 'State Bank of India' },
      { code: 'HDFC', name: 'HDFC Bank' },
      { code: 'ICICI', name: 'ICICI Bank' },
      { code: 'AXIS', name: 'Axis Bank' },
      { code: 'KOTAK', name: 'Kotak Mahindra Bank' },
      { code: 'YESB', name: 'Yes Bank' },
      { code: 'PUNB', name: 'Punjab National Bank' },
      { code: 'CANB', name: 'Canara Bank' },
      { code: 'BOB', name: 'Bank of Baroda' },
      { code: 'IDIB', name: 'Indian Bank' }
    ]
  });
});

app.get('/api/v1/wallets', (req, res) => {
  res.json({
    success: true,
    data: [
      { id: 'paytm', name: 'Paytm', logo: 'P' },
      { id: 'amazonpay', name: 'Amazon Pay', logo: 'A' },
      { id: 'phonepe', name: 'PhonePe', logo: 'P' },
      { id: 'mobikwik', name: 'MobiKwik', logo: 'M' },
      { id: 'freecharge', name: 'FreeCharge', logo: 'F' }
    ]
  });
});

app.listen(PORT, () => {
  console.log(`\n🔧 Mock Server running at http://localhost:${PORT}`);
  console.log(`   API Base URL: http://localhost:${PORT}/api/v1\n`);
});

export default app;