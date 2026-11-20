const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pairId(a, b) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

function sortedPair(a, b) {
  return a < b ? [a, b] : [b, a];
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("publicProfiles").doc(ALICE_UID).get());
  await assertFails(unauth.collection("sharedLocations").doc(ALICE_UID).get());
  await assertFails(unauth.collection("friendships").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      handle: "anon_user",
      statusMessage: "",
      avatarColorHex: "#22D3EE",
      friendCode: "ABC123",
      sharingEnabled: false,
      batterySharingEnabled: false,
      historyEnabled: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile & lookup: owner create/update works; cross-user PII & list scraping blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      handle: "alice_w",
      statusMessage: "Exploring Seattle",
      avatarColorHex: "#22D3EE",
      friendCode: "WLD881",
      sharingEnabled: false,
      batterySharingEnabled: false,
      historyEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      sharingEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field / immutable field tampering fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot read Alice's private user doc (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Public profile & exact handle lookup allow get, block list scraping
  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice_w",
      statusMessage: "Exploring Seattle",
      avatarColorHex: "#22D3EE",
      friendCode: "WLD881",
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("handleLookup").doc("alice_w").set({
      userId: ALICE_UID,
      handle: "alice_w",
      displayName: "Alice",
      avatarColorHex: "#22D3EE",
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("publicProfiles").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("handleLookup").doc("alice_w").get());
  await assertFails(bobDb.collection("publicProfiles").get());
  await assertFails(bobDb.collection("handleLookup").get());
});

test("Friendships & SharedLocations: only accepted unpaused friends can read active locations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Alice writes her active shared location
  await assertSucceeds(
    aliceDb.collection("sharedLocations").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "alice_w",
      avatarColorHex: "#22D3EE",
      sharingActive: true,
      latitude: 47.6062,
      longitude: -122.3321,
      accuracyMeters: 12.0,
      batteryLevel: 84,
      charging: false,
      placeLabel: "Downtown",
      updatedAt: now,
    })
  );

  // Non-friend Bob cannot read Alice's location
  await assertFails(bobDb.collection("sharedLocations").doc(ALICE_UID).get());

  // List query on sharedLocations is strictly forbidden
  await assertFails(bobDb.collection("sharedLocations").get());

  // Alice sends friend request to Bob
  const fId = pairId(ALICE_UID, BOB_UID);
  const participants = sortedPair(ALICE_UID, BOB_UID);
  await assertSucceeds(
    aliceDb.collection("friendships").doc(fId).set({
      participantUids: participants,
      requesterUid: ALICE_UID,
      recipientUid: BOB_UID,
      requesterName: "Alice",
      requesterHandle: "alice_w",
      requesterColor: "#22D3EE",
      recipientName: "Bob",
      recipientHandle: "bob_r",
      recipientColor: "#10B981",
      status: "pending",
      pausedByUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Pending request does NOT grant location access
  await assertFails(bobDb.collection("sharedLocations").doc(ALICE_UID).get());

  // Alice cannot self-accept her own outgoing request
  await assertFails(
    aliceDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob accepts the request
  await assertSucceeds(
    bobDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Accepted friend Bob CAN read Alice's active shared location
  await assertSucceeds(bobDb.collection("sharedLocations").doc(ALICE_UID).get());

  // Stranger Charlie still CANNOT read Alice's location
  await assertFails(charlieDb.collection("sharedLocations").doc(ALICE_UID).get());

  // Alice pauses sharing with Bob -> Bob immediately loses access
  await assertSucceeds(
    aliceDb.collection("friendships").doc(fId).update({
      pausedByUids: [ALICE_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("sharedLocations").doc(ALICE_UID).get());

  // Alice unpauses Bob, then disables global sharingActive -> Bob loses access
  await assertSucceeds(
    aliceDb.collection("friendships").doc(fId).update({
      pausedByUids: [],
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    aliceDb.collection("sharedLocations").doc(ALICE_UID).update({
      sharingActive: false,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(bobDb.collection("sharedLocations").doc(ALICE_UID).get());

  // Filtered friendships query succeeds; unfiltered list fails
  await assertSucceeds(
    aliceDb.collection("friendships").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("friendships").get());
});

test("Private messaging: only accepted friends can chat and send messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const cId = pairId(ALICE_UID, BOB_UID);
  const participants = sortedPair(ALICE_UID, BOB_UID);

  // Cannot chat before being accepted friends
  await assertFails(
    aliceDb.collection("chats").doc(cId).set({
      participantUids: participants,
      lastMessageText: "Hey!",
      lastSenderUid: ALICE_UID,
      updatedAt: now,
    })
  );

  // Seed accepted friendship
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("friendships").doc(cId).set({
      participantUids: participants,
      requesterUid: ALICE_UID,
      recipientUid: BOB_UID,
      requesterName: "Alice",
      requesterHandle: "alice_w",
      requesterColor: "#22D3EE",
      recipientName: "Bob",
      recipientHandle: "bob_r",
      recipientColor: "#10B981",
      status: "accepted",
      pausedByUids: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  // Accepted friends can create chat & send messages
  await assertSucceeds(
    aliceDb.collection("chats").doc(cId).set({
      participantUids: participants,
      lastMessageText: "Meet at Pier 66?",
      lastSenderUid: ALICE_UID,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(cId).collection("messages").doc("msg_1").set({
      participantUids: participants,
      senderUid: ALICE_UID,
      text: "Meet at Pier 66?",
      sharedLat: 47.6114,
      sharedLng: -122.3489,
      sharedPlaceName: "Pier 66",
      createdAt: now,
    })
  );

  // Bob can query messages
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc(cId)
      .collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );

  // Charlie cannot read or write in Alice-Bob chat
  await assertFails(charlieDb.collection("chats").doc(cId).get());
  await assertFails(
    charlieDb.collection("chats").doc(cId).collection("messages").doc("msg_1").get()
  );
});

test("Location history & favorite places: strictly owner-only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("locationHistory").doc("h1").set({
      latitude: 47.6062,
      longitude: -122.3321,
      accuracyMeters: 10.5,
      recordedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favoritePlaces").doc("p1").set({
      name: "Home Base",
      category: "home",
      latitude: 47.6205,
      longitude: -122.3493,
      addressHint: "Queen Anne, Seattle",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner queries succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("locationHistory").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favoritePlaces").get()
  );

  // Bob cannot read or write Alice's history or favorite places
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("locationHistory").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favoritePlaces").get()
  );
});
