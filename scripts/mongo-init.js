// ===========================================
// PayFlow MongoDB Initialization
// Creates audit database with sample data
// ===========================================

db = db.getSiblingDB('audit_db');

// Create audit events collection
db.createCollection('audit_events');

// Create indexes
db.audit_events.createIndex({ "timestamp": -1 });
db.audit_events.createIndex({ "eventType": 1 });
db.audit_events.createIndex({ "entityType": 1 });
db.audit_events.createIndex({ "userId": 1 });

// Insert sample audit events
db.audit_events.insertMany([
  {
    eventType: "USER_LOGIN",
    entityType: "User",
    entityId: "00000000-0000-0000-0000-000000000001",
    userId: "00000000-0000-0000-0000-000000000001",
    timestamp: new Date(),
    changes: {
      action: "LOGIN",
      ipAddress: "127.0.0.1",
      userAgent: "Mozilla/5.0"
    }
  },
  {
    eventType: "PAYMENT_CREATED",
    entityType: "Payment",
    entityId: "11111111-1111-1111-1111-111111111111",
    userId: "00000000-0000-0000-0000-000000000003",
    timestamp: new Date(Date.now() - 3600000),
    changes: {
      amount: 5000,
      currency: "INR",
      status: "CREATED"
    }
  },
  {
    eventType: "ORDER_CREATED",
    entityType: "Order",
    entityId: "22222222-2222-2222-2222-222222222222",
    userId: "00000000-0000-0000-0000-000000000003",
    timestamp: new Date(Date.now() - 7200000),
    changes: {
      orderReference: "ORD-SAMPLE001",
      amount: 5000,
      currency: "INR"
    }
  },
  {
    eventType: "MERCHANT_CREATED",
    entityType: "Merchant",
    entityId: "00000000-0000-0000-0000-000000000001",
    userId: "00000000-0000-0000-0000-000000000002",
    timestamp: new Date(Date.now() - 86400000),
    changes: {
      name: "PayFlow Demo Store",
      businessType: "ECOMMERCE",
      kycStatus: "VERIFIED"
    }
  }
]);

print("MongoDB initialization complete!");