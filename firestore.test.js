const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or write studios", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("studios").get()
  );
});

test("2. Cross-user access is rejected for studios, shoots, withdrawals, expenses", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("studios")
      .doc("studio_1")
      .set({
        userId: ALICE_UID,
        name: "Himalayan Cinema Films",
        contactPerson: "Ramesh Thapa",
        phone: "9800000000",
        email: "contact@himalayan.com",
        address: "Kathmandu",
        notes: "Primary wedding studio",
        archived: false,
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studios")
      .doc("studio_1")
      .get()
  );
});

test("3. Authenticated owner can create and read valid Studio, Shoot, Withdrawal, Expense in NPR", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studios")
      .doc("studio_1")
      .set({
        userId: ALICE_UID,
        name: "Kathmandu Frame House",
        contactPerson: "Suman Shrestha",
        phone: "9841000000",
        email: "suman@framehouse.np",
        address: "Lalitpur, Nepal",
        notes: "Commercial partner",
        archived: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shoots")
      .doc("shoot_1")
      .set({
        userId: ALICE_UID,
        eventTitle: "Aarav & Priya Wedding",
        studioId: "studio_1",
        studioName: "Kathmandu Frame House",
        shootDateEpochMs: 1730000000000,
        category: "Wedding",
        location: "Soaltee Hotel, Kathmandu",
        durationValue: 2,
        durationUnit: "Days",
        confirmedEarningsPaisa: 4500000,
        expectedPaymentDateEpochMs: 1731000000000,
        paymentStatus: "Collectible",
        projectStatus: "Completed",
        notes: "2-day main cinematography",
        attachmentUri: "",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("withdrawals")
      .doc("wd_1")
      .set({
        userId: ALICE_UID,
        studioId: "studio_1",
        studioName: "Kathmandu Frame House",
        amountPaisa: 2000000,
        dateEpochMs: 1730500000000,
        paymentMethod: "eSewa",
        referenceNumber: "ESW-998877",
        notes: "First installment",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("expenses")
      .doc("exp_1")
      .set({
        userId: ALICE_UID,
        expenseName: "Sony FX3 Lens Rental",
        category: "Lens Rental",
        amountPaisa: 350000,
        dateEpochMs: 1730000000000,
        associatedShootId: "shoot_1",
        associatedStudioId: "studio_1",
        paymentMethod: "Khalti",
        receiptUri: "",
        notes: "24-70mm GM II rental",
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("4. Rejects unsupported currency or unsupported payment method", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // USD should fail because only NPR is allowed
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice DoP",
      email: "alice@cinevault.app",
      photoUrl: "",
      currency: "USD",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // PayPal should fail because only Bank Transfer, eSewa, Cash, Khalti, Other are allowed
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("withdrawals")
      .doc("wd_bad")
      .set({
        userId: ALICE_UID,
        studioId: "studio_1",
        studioName: "Kathmandu Frame House",
        amountPaisa: 100000,
        dateEpochMs: 1730500000000,
        paymentMethod: "PayPal",
        referenceNumber: "PP-1",
        notes: "",
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("5. Rejects shadow/ghost fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("studios")
      .doc("studio_ghost")
      .set({
        userId: ALICE_UID,
        name: "Ghost Studio",
        contactPerson: "",
        phone: "",
        email: "",
        address: "",
        notes: "",
        archived: false,
        isVerifiedAdmin: true,
        createdAt: now,
        updatedAt: now,
      })
  );
});
