#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
let admin;

const PROJECT_ROOT = path.resolve(__dirname, "..");
const GOOGLE_SERVICES_PATH = path.join(PROJECT_ROOT, "app", "google-services.json");
const SEED_TAG = "unimove-demo-seed-v2";
const DEFAULT_USER_COUNT = 30;
const DEFAULT_PASSWORD = "Demo123!";
const DEMO_EMAIL_DOMAIN = "demo.unimove.app";
const MALAYSIA_TZ = "Asia/Kuala_Lumpur";

const FIRST_NAMES = [
  "Aina", "Daniel", "Sarah", "Faris", "Nur", "Jason", "Mei", "Hakim", "Alya", "Irfan",
  "Sofia", "Adam", "Hannah", "Rayyan", "Zara", "Amir", "Clara", "Nadia", "Ethan", "Yusuf",
  "Leah", "Haziq", "Mira", "Arjun", "Alicia", "Harith", "Ivy", "Rizqi", "Aisyah", "Marcus",
  "Anis", "Ben", "Celine", "Danish", "Elina", "Faiz", "Grace", "Haikal", "Ines", "Jun",
  "Khai", "Lina", "Mikhail", "Nurin", "Owen", "Putri", "Qis", "Rina", "Syahmi", "Tara"
];

const LAST_NAMES = [
  "Rahman", "Wong", "Lee", "Adam", "Ismail", "Tan", "Ng", "Salleh", "Lim", "Hassan",
  "Othman", "Aziz", "Yap", "Ibrahim", "Nair", "Teo", "Khalid", "Chan", "Bakar", "Hamid",
  "Yusof", "Low", "Shah", "Raman", "Fong", "Liew", "Ariffin", "Chong", "Rashid", "Ong",
  "Karim", "Pillai", "Ho", "Ali", "Mahmud", "Foo", "Zain", "Cheah", "Mustafa", "Goh",
  "Noor", "Subra", "Koh", "Latif", "Chin", "Roslan", "Ting", "Kaur", "Hashim", "Singh"
];

const COLLEGES = [
  "Kolej Burhanuddin Helmi", "Kolej Ibu Zain", "Kolej Pendeta Za'ba", "Kolej Rahim Kajai",
  "Kolej Tun Dr Ismail", "Kolej Keris Mas", "Kolej Tun Hussein Onn", "Kolej Aminuddin Baki",
  "Kolej Ungku Omar", "Kolej 13", "Kolej Dato' Onn", "Kolej Ibu Yusof", "Kolej Tun Syed Nasir"
];

const CHALLENGES = [
  {
    id: "demo-challenge-sunrise-5k",
    title: "Sunrise 5K Sprint",
    description: "A clean 5 km campus sprint. Fast legs win this board.",
    targetDistanceKm: 5,
    rewardLabel: "Top 3 runners earn the Sunrise badge.",
    badgeText: "Race Mode",
    startOffsetDays: -3,
    endOffsetDays: 10
  },
  {
    id: "demo-challenge-lake-8k",
    title: "Lakeside 8K Push",
    description: "An 8 km effort run for steady pacing and strong finishes.",
    targetDistanceKm: 8,
    rewardLabel: "Finishers unlock the Lakeside title card.",
    badgeText: "Endurance",
    startOffsetDays: -2,
    endOffsetDays: 12
  },
  {
    id: "demo-challenge-night-3k",
    title: "Night Dash 3K",
    description: "Short distance, sharp pace, leaderboard decided by time.",
    targetDistanceKm: 3,
    rewardLabel: "Best time takes the Night Dash crown.",
    badgeText: "Quick Race",
    startOffsetDays: -1,
    endOffsetDays: 7
  }
];

const MEAL_LIBRARY = [
  { foodName: "Oatmeal Bowl", calories: 310, quantity: 1, servingSize: "1 bowl", mealType: "BREAKFAST" },
  { foodName: "Banana Protein Shake", calories: 240, quantity: 1, servingSize: "1 bottle", mealType: "BREAKFAST" },
  { foodName: "Chicken Rice", calories: 640, quantity: 1, servingSize: "1 plate", mealType: "LUNCH" },
  { foodName: "Grilled Salmon Salad", calories: 430, quantity: 1, servingSize: "1 plate", mealType: "LUNCH" },
  { foodName: "Greek Yogurt Cup", calories: 180, quantity: 1, servingSize: "1 cup", mealType: "SNACK" },
  { foodName: "Peanut Butter Toast", calories: 220, quantity: 2, servingSize: "2 slices", mealType: "SNACK" },
  { foodName: "Nasi Goreng", calories: 720, quantity: 1, servingSize: "1 plate", mealType: "DINNER" },
  { foodName: "Tofu Veggie Bowl", calories: 410, quantity: 1, servingSize: "1 bowl", mealType: "DINNER" }
];

const MONSTER_LIBRARY = [
  { id: "ukm-pendeta-zaaba-bee", name: "Bee Monster", modelAsset: "models/animal-bee.glb", lat: 2.919770, lng: 101.773583 },
  { id: "ukm-sel-fuel-bunny", name: "Bunny Monster", modelAsset: "models/animal-bunny.glb", lat: 2.922100, lng: 101.773704 },
  { id: "ukm-kbh-cat", name: "Cat Monster", modelAsset: "models/animal-cat.glb", lat: 2.927198, lng: 101.777917 },
  { id: "ukm-canselori-caterpillar", name: "Caterpillar Monster", modelAsset: "models/animal-caterpillar.glb", lat: 2.928833, lng: 101.779097 },
  { id: "ukm-dectar-chick", name: "Chick Monster", modelAsset: "models/animal-chick.glb", lat: 2.930138, lng: 101.778277 },
  { id: "ukm-kkm-cow", name: "Cow Monster", modelAsset: "models/animal-cow.glb", lat: 2.927137, lng: 101.788936 },
  { id: "ukm-arena-crab", name: "Crab Monster", modelAsset: "models/animal-crab.glb", lat: 2.936300, lng: 101.783794 },
  { id: "ukm-pusanika-deer", name: "Deer Monster", modelAsset: "models/animal-deer.glb", lat: 2.925991, lng: 101.780755 }
];

const SEED_SECTIONS = ["users", "sessions", "meals", "challenges", "leaderboard", "community", "fitbot", "monsters"];

function parseArgs(argv) {
  const args = {
    cleanup: false,
    users: DEFAULT_USER_COUNT,
    password: DEFAULT_PASSWORD,
    projectId: process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || "",
    serviceAccount: process.env.FIREBASE_SERVICE_ACCOUNT || "",
    dryRun: false,
    skipCleanup: false,
    only: new Set(SEED_SECTIONS)
  };

  for (let i = 0; i < argv.length; i += 1) {
    const current = argv[i];
    const next = argv[i + 1];

    switch (current) {
      case "--cleanup":
      case "cleanup":
        args.cleanup = true;
        break;
      case "--dry-run":
        args.dryRun = true;
        break;
      case "--skip-cleanup":
        args.skipCleanup = true;
        break;
      case "--only":
        args.only = parseOnlySections(next);
        i += 1;
        break;
      case "--users":
        args.users = toPositiveInt(next, "--users");
        i += 1;
        break;
      case "--password":
        args.password = next || DEFAULT_PASSWORD;
        i += 1;
        break;
      case "--project-id":
        args.projectId = next || "";
        i += 1;
        break;
      case "--service-account":
        args.serviceAccount = next || "";
        i += 1;
        break;
      case "--help":
      case "-h":
        printHelp();
        process.exit(0);
        break;
      default:
        if (current.startsWith("--")) {
          throw new Error(`Unknown argument: ${current}`);
        }
        break;
    }
  }

  return args;
}

function toPositiveInt(value, flagName) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`${flagName} must be a positive integer.`);
  }
  return parsed;
}

function parseOnlySections(value) {
  if (!value || !value.trim()) {
    throw new Error("--only must include a comma-separated list such as monsters,fitbot.");
  }

  const sections = value
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);

  if (sections.includes("all")) {
    return new Set(SEED_SECTIONS);
  }

  const normalized = new Set();
  for (const section of sections) {
    if (section === "monster" || section === "monsters") {
      normalized.add("monsters");
      continue;
    }
    if (!SEED_SECTIONS.includes(section)) {
      throw new Error(`Unknown --only section: ${section}. Valid values: all, ${SEED_SECTIONS.join(", ")}`);
    }
    normalized.add(section);
  }

  return normalized;
}

function printHelp() {
  console.log(`
UniMove demo seeder

Usage:
  npm run seed:demo
  npm run seed:demo -- --users 30
  npm run seed:demo -- --only monsters --skip-cleanup
  npm run seed:demo -- --only fitbot,monsters --skip-cleanup
  npm run seed:demo -- --project-id unimove-32e01 --service-account C:\\path\\service-account.json
  npm run seed:demo:cleanup

Options:
  --users <n>             Number of demo users to seed. Default: ${DEFAULT_USER_COUNT}
  --password <value>      Password used for all demo email/password accounts. Default: ${DEFAULT_PASSWORD}
  --project-id <id>       Firebase project id. Falls back to env or app/google-services.json.
  --service-account <p>   Path to a Firebase service account JSON.
  --only <list>           Seed only selected sections: all, ${SEED_SECTIONS.join(", ")}
  --skip-cleanup          Upsert selected data without deleting seeded demo data first.
  --cleanup               Remove only demo data created by this script.
  --dry-run               Print what would happen without writing data.
`);
}

function readProjectIdFromGoogleServices() {
  if (!fs.existsSync(GOOGLE_SERVICES_PATH)) return "";
  const json = JSON.parse(fs.readFileSync(GOOGLE_SERVICES_PATH, "utf8"));
  return json?.project_info?.project_id || "";
}

function resolveProjectId(cliProjectId) {
  return cliProjectId || readProjectIdFromGoogleServices();
}

function initializeFirebase({ projectId, serviceAccount }) {
  if (!admin) {
    admin = require("firebase-admin");
  }

  if (admin.apps.length > 0) {
    return admin.app();
  }

  const options = {};

  if (serviceAccount) {
    const resolvedPath = path.resolve(serviceAccount);
    const serviceAccountJson = JSON.parse(fs.readFileSync(resolvedPath, "utf8"));
    options.credential = admin.credential.cert(serviceAccountJson);
    options.projectId = projectId || serviceAccountJson.project_id;
  } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    options.credential = admin.credential.applicationDefault();
    if (projectId) {
      options.projectId = projectId;
    }
  } else if (process.env.FIRESTORE_EMULATOR_HOST) {
    if (!projectId) {
      throw new Error("A project id is required when using the Firestore emulator.");
    }
    options.projectId = projectId;
  } else {
    throw new Error(
      "No Firebase admin credentials found. Set GOOGLE_APPLICATION_CREDENTIALS or pass --service-account."
    );
  }

  return admin.initializeApp(options);
}

function currentWeekKey(baseDate = new Date()) {
  const local = new Date(baseDate);
  const day = local.getDay() || 7;
  const thursday = new Date(local);
  thursday.setDate(local.getDate() + 4 - day);
  const yearStart = new Date(thursday.getFullYear(), 0, 1);
  const week = Math.ceil((((thursday - yearStart) / 86400000) + 1) / 7);
  return `${thursday.getFullYear()}-W${String(week).padStart(2, "0")}`;
}

function malaysiaDateKey(baseDate = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: MALAYSIA_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).format(baseDate);
}

function addDays(baseDate, days) {
  const next = new Date(baseDate);
  next.setDate(next.getDate() + days);
  return next;
}

function round(value, places = 2) {
  return Number.parseFloat(value.toFixed(places));
}

function pickName(index) {
  const first = FIRST_NAMES[index % FIRST_NAMES.length];
  const last = LAST_NAMES[index % LAST_NAMES.length];
  return `${first} ${last}`;
}

function demoUser(index) {
  const number = String(index + 1).padStart(3, "0");
  return {
    uid: `demo-user-${number}`,
    email: `demo.user${number}@${DEMO_EMAIL_DOMAIN}`,
    name: pickName(index),
    avatarKey: "default",
    authProvider: "password",
    gender: index % 3 === 0 ? "Female" : index % 3 === 1 ? "Male" : "Non-binary",
    age: 19 + (index % 7),
    height: 156 + (index % 16),
    weight: 49 + (index % 18) * 1.8,
    dailyCalorieTarget: 1800 + (index % 6) * 150,
    yearOfStudy: 1 + (index % 4),
    college: COLLEGES[index % COLLEGES.length]
  };
}

function performanceRankIndex(index) {
  if (index === 0) return 4;
  if (index < 5) return index - 1;
  return index;
}

function createSessionsForUser(user, index, baseDate) {
  if (index === 0) {
    return createFeaturedSessionsForPresenter(user, baseDate);
  }

  const rankIndex = performanceRankIndex(index);
  const strength = 1 - rankIndex / 60;
  const sessionCount = 4 + (index % 4);
  const dayOffsets = [0, -1, -2, -4, -8, -13, -21];
  const challenge = index < 30 ? CHALLENGES[index % CHALLENGES.length] : null;
  const sessions = [];

  for (let sessionIndex = 0; sessionIndex < sessionCount; sessionIndex += 1) {
    const date = addDays(baseDate, dayOffsets[sessionIndex]);
    const challengeAssigned = sessionIndex === 0 && challenge;
    const distanceKm = challengeAssigned
      ? challenge.targetDistanceKm
      : round(2.6 + strength * 4.6 + (sessionIndex % 3) * 0.55 + ((index + sessionIndex) % 5) * 0.12);
    const paceSecPerKm = Math.max(
      285,
      Math.round(440 - strength * 95 - sessionIndex * 8 + ((index + sessionIndex) % 4) * 7)
    );
    const durationSeconds = Math.round(distanceKm * paceSecPerKm);
    const isShared = challengeAssigned ? false : sessionIndex < 2 || ((index + sessionIndex) % 4 === 0);
    const routePoints = buildRoute(user.uid, sessionIndex, distanceKm);

    sessions.push({
      id: `demo-session-${String(sessionIndex + 1).padStart(2, "0")}`,
      userId: user.uid,
      runName: challengeAssigned
        ? `${challenge.title} Attempt`
        : sessionIndex % 2 === 0
          ? "Campus Morning Run"
          : "Evening Recovery Loop",
      isShared,
      distanceKm,
      durationSeconds,
      avgPace: formatPace(durationSeconds, distanceKm),
      timestamp: date,
      routePoints,
      challengeId: challengeAssigned ? challenge.id : null,
      sessionMode: challengeAssigned ? "challenge_completed" : "standard"
    });
  }

  return sessions;
}

function createFeaturedSessionsForPresenter(user, baseDate) {
  const featuredRuns = [
    { dayOffset: 0, distanceKm: 6.4, paceSecPerKm: 339, runName: "Campus Tempo Run", isShared: true },
    { dayOffset: -1, distanceKm: 5.0, paceSecPerKm: 334, runName: "Sunrise 5K Sprint Attempt", challengeId: CHALLENGES[0].id },
    { dayOffset: -2, distanceKm: 4.8, paceSecPerKm: 347, runName: "Recovery Loop", isShared: true },
    { dayOffset: -3, distanceKm: 7.2, paceSecPerKm: 352, runName: "Lake Route Builder", isShared: false },
    { dayOffset: -4, distanceKm: 3.2, paceSecPerKm: 362, runName: "Night Dash 3K Attempt", challengeId: CHALLENGES[2].id },
    { dayOffset: -5, distanceKm: 5.6, paceSecPerKm: 358, runName: "Library Hill Repeats", isShared: true },
    { dayOffset: -6, distanceKm: 4.1, paceSecPerKm: 371, runName: "Easy Campus Run", isShared: false },
    { dayOffset: -8, distanceKm: 8.0, paceSecPerKm: 365, runName: "Lakeside 8K Push Attempt", challengeId: CHALLENGES[1].id },
    { dayOffset: -10, distanceKm: 6.1, paceSecPerKm: 377, runName: "Evening Endurance", isShared: true },
    { dayOffset: -12, distanceKm: 4.5, paceSecPerKm: 386, runName: "Back Gate Jog", isShared: false },
    { dayOffset: -15, distanceKm: 5.3, paceSecPerKm: 392, runName: "Campus Morning Run", isShared: true },
    { dayOffset: -18, distanceKm: 3.9, paceSecPerKm: 401, runName: "Starter Session", isShared: false }
  ];

  return featuredRuns.map((run, sessionIndex) => {
    const timestamp = addDays(baseDate, run.dayOffset);
    const durationSeconds = Math.round(run.distanceKm * run.paceSecPerKm);
    const challengeId = run.challengeId || null;

    return {
      id: `demo-session-${String(sessionIndex + 1).padStart(2, "0")}`,
      userId: user.uid,
      runName: run.runName,
      isShared: challengeId ? false : Boolean(run.isShared),
      distanceKm: run.distanceKm,
      durationSeconds,
      avgPace: formatPace(durationSeconds, run.distanceKm),
      timestamp,
      routePoints: buildRoute(user.uid, sessionIndex, run.distanceKm),
      challengeId,
      sessionMode: challengeId ? "challenge_completed" : "standard"
    };
  });
}

function buildRoute(uid, sessionIndex, distanceKm) {
  const baseLat = 3.139 + (uid.charCodeAt(uid.length - 1) % 10) * 0.0015;
  const baseLng = 101.686 + (sessionIndex % 4) * 0.0018;
  const points = [];
  const pointCount = 8 + Math.max(0, Math.round(distanceKm));

  for (let i = 0; i < pointCount; i += 1) {
    const lat = round(baseLat + i * 0.00035 + Math.sin((sessionIndex + 1) * (i + 1)) * 0.00022, 6);
    const lng = round(baseLng + Math.cos((sessionIndex + 2) * (i + 1)) * 0.00028 + i * 0.00018, 6);
    points.push({ lat, lng });
  }

  return [{ points }];
}

function formatPace(durationSeconds, distanceKm) {
  const pace = distanceKm > 0 ? durationSeconds / distanceKm : 0;
  const minutes = Math.floor(pace / 60);
  const seconds = Math.round(pace % 60);
  return `${minutes}:${String(seconds).padStart(2, "0")} /km`;
}

function paceSeconds(durationSeconds, distanceKm) {
  if (!distanceKm || distanceKm <= 0) return 0;
  return Math.round(durationSeconds / distanceKm);
}

function durationLabel(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return hours > 0
    ? `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
    : `${minutes}:${String(seconds).padStart(2, "0")}`;
}

function buildMealDays(index, baseDate) {
  const today = malaysiaDateKey(baseDate);
  const yesterday = malaysiaDateKey(addDays(baseDate, -1));
  const dayBefore = malaysiaDateKey(addDays(baseDate, -2));
  return [
    buildMealDay(today, index, 0),
    buildMealDay(yesterday, index, 2),
    buildMealDay(dayBefore, index, 4)
  ];
}

function buildMealDay(dateKey, userIndex, offset) {
  const entries = [
    MEAL_LIBRARY[(userIndex + offset) % MEAL_LIBRARY.length],
    MEAL_LIBRARY[(userIndex + offset + 1) % MEAL_LIBRARY.length],
    MEAL_LIBRARY[(userIndex + offset + 3) % MEAL_LIBRARY.length]
  ].map((meal, index) => ({
    id: `entry-${String(index + 1).padStart(2, "0")}`,
    ...meal
  }));

  return {
    dateKey,
    entries,
    totalCalories: entries.reduce((sum, entry) => sum + entry.calories, 0),
    totalItems: entries.length
  };
}

function buildFitBotConversations(users, baseDate) {
  const conversationsByUser = new Map();

  users.forEach((user, index) => {
    const conversationCount = index < 6 ? 2 : index < 16 ? 1 : 0;
    if (conversationCount === 0) {
      return;
    }

    const conversations = [];

    for (let conversationIndex = 0; conversationIndex < conversationCount; conversationIndex += 1) {
      const createdAtDate = addDays(baseDate, -(index + conversationIndex + 1));
      const updatedAtDate = new Date(createdAtDate.getTime() + (conversationIndex + 1) * 45 * 60 * 1000);
      const goalType = conversationIndex % 2 === 0 ? "tempo plan" : "meal plan";
      const firstPrompt = conversationIndex % 2 === 0
        ? `Can you make me a ${user.yearOfStudy}-week ${goalType} for a ${round(userSessionsDistanceHint(index), 1)} km target?`
        : `Suggest a ${goalType} around ${user.dailyCalorieTarget} calories for training days.`;

      conversations.push({
        id: `fitbot-demo-${String(conversationIndex + 1).padStart(2, "0")}`,
        title: conversationIndex % 2 === 0 ? "Tempo plan for next week" : "Training day meal plan",
        createdAt: createdAtDate.getTime(),
        updatedAt: updatedAtDate.getTime(),
        messages: [
          { text: `Hey ${user.name.split(" ")[0]}! I can help with running plans, nutrition, and pacing.`, isUser: false },
          { text: firstPrompt, isUser: true },
          {
            text: conversationIndex % 2 === 0
              ? `Yes. Keep two quality sessions, one long run, and two recovery days. Start around ${round(3.6 + (index % 4) * 0.6, 1)} km for intervals and progress gradually.`
              : `Yes. Anchor breakfast with protein, keep lunch balanced, and use dinner for recovery carbs. Aim for ${user.dailyCalorieTarget} kcal with hydration before the evening run.`,
            isUser: false
          }
        ]
      });
    }

    conversationsByUser.set(user.uid, conversations);
  });

  return conversationsByUser;
}

function userSessionsDistanceHint(index) {
  return 5 + (index % 5) * 1.5;
}

function buildMonsterActivity(users, baseDate) {
  const catchesByUser = new Map();
  const eventSessions = [];

  users.forEach((user, index) => {
    if (index >= 12) {
      return;
    }

    const catchCount = index < 4 ? 2 : 1;
    const catches = [];

    for (let catchIndex = 0; catchIndex < catchCount; catchIndex += 1) {
      const monster = MONSTER_LIBRARY[(index + catchIndex) % MONSTER_LIBRARY.length];
      const caughtAt = addDays(baseDate, -(index + catchIndex));
      const caughtLocation = {
        lat: round(monster.lat, 6),
        lng: round(monster.lng, 6)
      };

      const catchEntry = {
        id: `${user.uid}_${monster.id}`,
        creatureId: monster.id,
        creatureName: monster.name,
        modelAsset: monster.modelAsset,
        userId: user.uid,
        userDisplayName: user.name,
        caughtAt,
        isPublic: true,
        source: "monster_event",
        caughtLocation,
        distanceAtCatchMeters: 24 + (index % 5) * 8 + catchIndex * 4
      };

      catches.push(catchEntry);

      if (catchIndex === 0) {
        const distanceKm = round(2.4 + (index % 4) * 0.65, 2);
        const durationSeconds = Math.round(distanceKm * (360 + (index % 5) * 12));
        eventSessions.push({
          id: `monster-session-${String(index + 1).padStart(2, "0")}`,
          userId: user.uid,
          userDisplayName: user.name,
          runName: "Monster Event Run",
          isShared: false,
          distanceKm,
          durationSeconds,
          avgPace: formatPace(durationSeconds, distanceKm),
          timestamp: caughtAt,
          routePoints: buildRoute(user.uid, 50 + index, distanceKm),
          sessionMode: "monster_event",
          eventType: "monster",
          monsterEncountered: true,
          monsterCaught: true,
          monsterModelAsset: monster.modelAsset,
          monsterSpawn: {
            id: monster.id,
            name: monster.name,
            lat: caughtLocation.lat,
            lng: caughtLocation.lng,
            visibleRadiusMeters: 90,
            encounterRadiusMeters: 40
          }
        });
      }
    }

    catchesByUser.set(user.uid, catches);
  });

  return { catchesByUser, eventSessions };
}

function buildChallengeState(users, userSessions) {
  const progressByUser = new Map();
  const participantsByChallenge = new Map(CHALLENGES.map((challenge) => [challenge.id, []]));

  users.forEach((user, index) => {
    const sessions = userSessions.get(user.uid) || [];
    const challengeSession = sessions.find((session) => session.challengeId);
    const userEntries = [];

    if (challengeSession) {
      const challenge = CHALLENGES.find((item) => item.id === challengeSession.challengeId);
      const attempts = 1 + (index % 3);
      const bestCompletionSeconds = challengeSession.durationSeconds - (index % 2) * 18;
      const completedAt = challengeSession.timestamp;

      userEntries.push({
        challengeId: challenge.id,
        accumulatedDistanceKm: challenge.targetDistanceKm,
        bestCompletionSeconds,
        latestCompletionSeconds: challengeSession.durationSeconds,
        totalAttempts: attempts,
        status: "COMPLETED",
        joinedAt: addDays(challengeSession.timestamp, -2),
        completedAt,
        lastUpdated: completedAt
      });

      participantsByChallenge.get(challenge.id).push({
        userId: user.uid,
        name: user.name,
        bestCompletionSeconds,
        latestCompletionSeconds: challengeSession.durationSeconds,
        attemptCount: attempts,
        completedAt,
        updatedAt: completedAt
      });
    }

    if (index >= 30 && index < 36) {
      const challenge = CHALLENGES[index % CHALLENGES.length];
      const lastUpdated = addDays(new Date(), -(index % 3));
      userEntries.push({
        challengeId: challenge.id,
        accumulatedDistanceKm: round(challenge.targetDistanceKm * 0.45, 2),
        bestCompletionSeconds: null,
        latestCompletionSeconds: null,
        totalAttempts: 0,
        status: "ACTIVE",
        joinedAt: addDays(lastUpdated, -1),
        completedAt: null,
        lastUpdated
      });
    }

    if (userEntries.length > 0) {
      progressByUser.set(user.uid, userEntries);
    }
  });

  return { progressByUser, participantsByChallenge };
}

function buildUserDocuments(users, userSessions, weeklyLeaderboard, mealDaysByUser) {
  const userDocs = new Map();

  users.forEach((user, index) => {
    const sessions = userSessions.get(user.uid) || [];
    const totalDistance = round(sessions.reduce((sum, session) => sum + session.distanceKm, 0));
    const totalTime = sessions.reduce((sum, session) => sum + session.durationSeconds, 0);
    const totalSessions = sessions.length;
    const latestMealDay = mealDaysByUser.get(user.uid)?.[0];
    const createdAt = addDays(new Date(), index === 0 ? -90 : -35);

    userDocs.set(user.uid, {
      name: user.name,
      email: user.email,
      avatarKey: user.avatarKey,
      photoUrl: "",
      providerPhotoUrl: "",
      authProvider: user.authProvider,
      profileCompleted: true,
      onboardingCompleted: true,
      totalDistance,
      totalTime,
      totalSessions,
      dailyCalorieTarget: user.dailyCalorieTarget,
      gender: user.gender,
      age: user.age,
      weight: round(user.weight, 1),
      height: round(user.height, 1),
      yearOfStudy: user.yearOfStudy,
      college: user.college,
      lastLogin: new Date(),
      createdAt,
      updatedAt: new Date(),
      seedTag: SEED_TAG,
      demoProfile: true,
      weeklyDistance: weeklyLeaderboard.get(user.uid)?.weeklyDistance || 0,
      todayCalories: latestMealDay?.totalCalories || 0
    });
  });

  return userDocs;
}

function buildWeeklyLeaderboard(users, userSessions, weekKey) {
  const leaderboard = new Map();

  users.forEach((user) => {
    const sessions = (userSessions.get(user.uid) || []).filter((session) => currentWeekKey(session.timestamp) === weekKey);
    const weeklyDistance = round(sessions.reduce((sum, session) => sum + session.distanceKm, 0));
    const weeklySessions = sessions.length;

    leaderboard.set(user.uid, {
      id: `${user.uid}_${weekKey}`,
      userId: user.uid,
      name: user.name,
      weeklyDistance,
      weeklySessions,
      weekKey,
      updatedAt: new Date(),
      seedTag: SEED_TAG
    });
  });

  return leaderboard;
}

function buildCommunityPosts(users, userSessions) {
  const posts = [];

  users.forEach((user) => {
    const sessions = userSessions.get(user.uid) || [];
    sessions
      .filter((session) => session.isShared)
      .slice(0, 2)
      .forEach((session, index) => {
        const avgPaceSecPerKm = paceSeconds(session.durationSeconds, session.distanceKm);
        posts.push({
          id: `demo-feed-${user.uid}-${index + 1}`,
          userId: user.uid,
          username: user.name,
          runName: session.runName,
          distanceKm: session.distanceKm,
          distanceMeters: Math.round(session.distanceKm * 1000),
          durationSeconds: session.durationSeconds,
          durationMinutes: round(session.durationSeconds / 60, 2),
          durationLabel: durationLabel(session.durationSeconds),
          avgPace: session.avgPace,
          avgPaceSecPerKm,
          avgPaceMinPerKm: round(avgPaceSecPerKm / 60, 2),
          timestamp: session.timestamp,
          dateKey: malaysiaDateKey(session.timestamp),
          weekKey: currentWeekKey(session.timestamp),
          likes: (user.uid.charCodeAt(user.uid.length - 1) + index * 3) % 18,
          likedBy: [],
          comments: (index + user.uid.length) % 6,
          routePoints: session.routePoints,
          seedTag: SEED_TAG
        });
      });
  });

  return posts
    .sort((left, right) => right.timestamp.getTime() - left.timestamp.getTime())
    .slice(0, 20);
}

async function ensureAuthUser(auth, user, password) {
  try {
    await auth.getUser(user.uid);
    await auth.updateUser(user.uid, {
      email: user.email,
      password,
      displayName: user.name,
      emailVerified: true,
      disabled: false
    });
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      throw error;
    }

    await auth.createUser({
      uid: user.uid,
      email: user.email,
      password,
      displayName: user.name,
      emailVerified: true
    });
  }
}

async function seedUserData(db, user, userDoc, sessions, mealDays, challengeProgress, fitbotConversations, monsterCatches, options) {
  const userRef = db.collection("users").doc(user.uid);
  const batch = db.batch();
  const include = options.include;

  if (include("users")) {
    batch.set(userRef, userDoc, { merge: true });
  }

  if (include("sessions")) {
    sessions.forEach((session) => {
      const avgPaceSecPerKm = paceSeconds(session.durationSeconds, session.distanceKm);
      batch.set(userRef.collection("sessions").doc(session.id), {
        runName: session.runName,
        isShared: session.isShared,
        distanceKm: session.distanceKm,
        distanceMeters: Math.round(session.distanceKm * 1000),
        durationSeconds: session.durationSeconds,
        durationMinutes: round(session.durationSeconds / 60, 2),
        durationHours: round(session.durationSeconds / 3600, 4),
        durationLabel: durationLabel(session.durationSeconds),
        avgPace: session.avgPace,
        avgPaceSecPerKm,
        avgPaceMinPerKm: round(avgPaceSecPerKm / 60, 2),
        timestamp: session.timestamp,
        dateKey: malaysiaDateKey(session.timestamp),
        weekKey: currentWeekKey(session.timestamp),
        routePoints: session.routePoints,
        sessionMode: session.sessionMode,
        seedTag: SEED_TAG,
        ...(session.challengeId ? { challengeId: session.challengeId } : {})
      }, { merge: true });
    });
  }

  if (include("meals")) {
    mealDays.forEach((day) => {
      const mealDayRef = userRef.collection("meal_logs").doc(day.dateKey);
      batch.set(mealDayRef, {
        dateKey: day.dateKey,
        totalCalories: day.totalCalories,
        totalItems: day.totalItems,
        updatedAt: new Date(),
        seedTag: SEED_TAG
      }, { merge: true });

      day.entries.forEach((entry, index) => {
        batch.set(mealDayRef.collection("entries").doc(entry.id), {
          foodName: entry.foodName,
          calories: entry.calories,
          quantity: entry.quantity,
          servingSize: entry.servingSize,
          mealType: entry.mealType,
          dateKey: day.dateKey,
          createdAt: addDays(new Date(), -(index + 1)),
          seedTag: SEED_TAG
        }, { merge: true });
      });
    });
  }

  if (include("challenges")) {
    challengeProgress.forEach((progress) => {
      batch.set(userRef.collection("challenge_progress").doc(progress.challengeId), {
        challengeId: progress.challengeId,
        accumulatedDistanceKm: progress.accumulatedDistanceKm,
        bestCompletionSeconds: progress.bestCompletionSeconds,
        latestCompletionSeconds: progress.latestCompletionSeconds,
        totalAttempts: progress.totalAttempts,
        status: progress.status,
        joinedAt: progress.joinedAt,
        completedAt: progress.completedAt,
        lastUpdated: progress.lastUpdated,
        seedTag: SEED_TAG
      }, { merge: true });
    });
  }

  if (include("fitbot")) {
    fitbotConversations.forEach((conversation) => {
      batch.set(userRef.collection("fitbot_conversations").doc(conversation.id), {
        title: conversation.title,
        createdAt: conversation.createdAt,
        updatedAt: conversation.updatedAt,
        messages: conversation.messages,
        seedTag: SEED_TAG
      }, { merge: true });
    });
  }

  if (include("monsters")) {
    monsterCatches.forEach((entry) => {
      batch.set(userRef.collection("monster_collection").doc(entry.creatureId), {
        userId: entry.userId,
        userDisplayName: entry.userDisplayName,
        creatureId: entry.creatureId,
        creatureName: entry.creatureName,
        modelAsset: entry.modelAsset,
        caughtAt: entry.caughtAt,
        isPublic: entry.isPublic,
        source: entry.source,
        caughtLocation: entry.caughtLocation,
        distanceAtCatchMeters: entry.distanceAtCatchMeters,
        seedTag: SEED_TAG
      }, { merge: true });
    });
  }

  await batch.commit();
}

async function seedTopLevelCollections(db, challengeState, leaderboard, posts, monsterActivity, baseDate, options) {
  const include = options.include;

  if (include("challenges")) {
    for (const challenge of CHALLENGES) {
      const challengeRef = db.collection("challenges").doc(challenge.id);
      await challengeRef.set({
        title: challenge.title,
        description: challenge.description,
        targetDistanceKm: challenge.targetDistanceKm,
        rewardLabel: challenge.rewardLabel,
        badgeText: challenge.badgeText,
        startDate: addDays(baseDate, challenge.startOffsetDays),
        endDate: addDays(baseDate, challenge.endOffsetDays),
        isActive: true,
        seedTag: SEED_TAG
      }, { merge: true });

      const participants = challengeState.participantsByChallenge.get(challenge.id) || [];
      for (const participant of participants) {
        await challengeRef.collection("participants").doc(participant.userId).set({
          ...participant,
          seedTag: SEED_TAG
        }, { merge: true });
      }
    }
  }

  if (include("leaderboard")) {
    for (const entry of leaderboard.values()) {
      await db.collection("weekly_leaderboard").doc(entry.id).set(entry, { merge: true });
    }
  }

  if (include("community")) {
    for (const post of posts) {
      await db.collection("community_feed").doc(post.id).set(post, { merge: true });
    }
  }

  if (include("monsters")) {
    for (const session of monsterActivity.eventSessions) {
      await db.collection("monster_event_sessions").doc(session.id).set({
        userId: session.userId,
        userDisplayName: session.userDisplayName,
        runName: session.runName,
        isShared: session.isShared,
        distanceKm: session.distanceKm,
        durationSeconds: session.durationSeconds,
        avgPace: session.avgPace,
        timestamp: session.timestamp,
        routePoints: session.routePoints,
        sessionMode: session.sessionMode,
        eventType: session.eventType,
        monsterEncountered: session.monsterEncountered,
        monsterCaught: session.monsterCaught,
        monsterModelAsset: session.monsterModelAsset,
        monsterSpawn: session.monsterSpawn,
        seedTag: SEED_TAG
      }, { merge: true });
    }

    for (const catches of monsterActivity.catchesByUser.values()) {
      for (const entry of catches) {
        await db.collection("monster_event_catches").doc(entry.id).set({
          userId: entry.userId,
          userDisplayName: entry.userDisplayName,
          creatureId: entry.creatureId,
          creatureName: entry.creatureName,
          modelAsset: entry.modelAsset,
          caughtAt: entry.caughtAt,
          isPublic: entry.isPublic,
          source: entry.source,
          caughtLocation: entry.caughtLocation,
          distanceAtCatchMeters: entry.distanceAtCatchMeters,
          seedTag: SEED_TAG
        }, { merge: true });
      }
    }
  }
}

async function cleanupDemoData(db, auth) {
  const userIds = [];
  let nextPageToken;

  do {
    const page = await auth.listUsers(1000, nextPageToken);
    page.users.forEach((userRecord) => {
      if (userRecord.uid.startsWith("demo-user-")) {
        userIds.push(userRecord.uid);
      }
    });
    nextPageToken = page.pageToken;
  } while (nextPageToken);

  for (const uid of userIds) {
    const userRef = db.collection("users").doc(uid);
    await db.recursiveDelete(userRef);
    await auth.deleteUser(uid);
  }

  for (const challenge of CHALLENGES) {
    await db.recursiveDelete(db.collection("challenges").doc(challenge.id));
  }

  await deleteBySeedTag(db.collection("community_feed"), "seedTag", SEED_TAG);
  await deleteBySeedTag(db.collection("weekly_leaderboard"), "seedTag", SEED_TAG);
  await deleteBySeedTag(db.collection("monster_event_sessions"), "seedTag", SEED_TAG);
  await deleteBySeedTag(db.collection("monster_event_catches"), "seedTag", SEED_TAG);
}

async function deleteBySeedTag(collectionRef, field, value) {
  const snapshot = await collectionRef.where(field, "==", value).get();
  for (const doc of snapshot.docs) {
    await doc.ref.delete();
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const projectId = resolveProjectId(args.projectId);

  if (!projectId) {
    throw new Error("Could not resolve a Firebase project id. Pass --project-id or provide app/google-services.json.");
  }

  if (args.password.length < 6) {
    throw new Error("The demo password must be at least 6 characters.");
  }

  if (args.dryRun) {
    console.log(JSON.stringify({
      action: args.cleanup ? "cleanup" : "seed",
      users: args.users,
      projectId,
      weekKey: currentWeekKey(),
      malaysiaToday: malaysiaDateKey(),
      seedTag: SEED_TAG,
      skipCleanup: args.skipCleanup,
      only: Array.from(args.only),
      featureCoverage: [
        "users",
        "users/{uid}/sessions",
        "users/{uid}/meal_logs/{dateKey}/entries",
        "users/{uid}/challenge_progress",
        "users/{uid}/fitbot_conversations",
        "users/{uid}/monster_collection",
        "challenges/{challengeId}/participants",
        "weekly_leaderboard",
        "community_feed",
        "monster_event_sessions",
        "monster_event_catches"
      ]
    }, null, 2));
    return;
  }

  initializeFirebase({ projectId, serviceAccount: args.serviceAccount });
  const db = admin.firestore();
  const auth = admin.auth();
  const include = (section) => args.only.has(section);

  if (args.cleanup) {
    await cleanupDemoData(db, auth);
    console.log(`Removed demo data tagged ${SEED_TAG} from project ${projectId}.`);
    return;
  }

  if (!args.skipCleanup && args.only.size === SEED_SECTIONS.length) {
    await cleanupDemoData(db, auth);
  }

  const users = Array.from({ length: args.users }, (_, index) => demoUser(index));
  const baseDate = new Date();
  const weekKey = currentWeekKey(baseDate);
  const userSessions = new Map(users.map((user, index) => [user.uid, createSessionsForUser(user, index, baseDate)]));
  const mealDaysByUser = new Map(users.map((user, index) => [user.uid, buildMealDays(index, baseDate)]));
  const weeklyLeaderboard = buildWeeklyLeaderboard(users, userSessions, weekKey);
  const challengeState = buildChallengeState(users, userSessions);
  const userDocs = buildUserDocuments(users, userSessions, weeklyLeaderboard, mealDaysByUser);
  const posts = buildCommunityPosts(users, userSessions);
  const fitbotConversationsByUser = buildFitBotConversations(users, baseDate);
  const monsterActivity = buildMonsterActivity(users, baseDate);
  const seedOptions = { include };

  for (const user of users) {
    if (include("users")) {
      await ensureAuthUser(auth, user, args.password);
    }
    await seedUserData(
      db,
      user,
      userDocs.get(user.uid),
      userSessions.get(user.uid) || [],
      mealDaysByUser.get(user.uid) || [],
      challengeState.progressByUser.get(user.uid) || [],
      fitbotConversationsByUser.get(user.uid) || [],
      monsterActivity.catchesByUser.get(user.uid) || [],
      seedOptions
    );
  }

  await seedTopLevelCollections(db, challengeState, weeklyLeaderboard, posts, monsterActivity, baseDate, seedOptions);

  const primaryUser = users[0];
  console.log(`Seeded ${users.length} demo users into project ${projectId}.`);
  console.log(`Seeded sections: ${Array.from(args.only).join(", ")}`);
  console.log(`Current week key: ${weekKey}`);
  console.log(`Malaysia meal date key: ${malaysiaDateKey(baseDate)}`);
  console.log(`Login email pattern: demo.user001@${DEMO_EMAIL_DOMAIN} ... demo.user${String(users.length).padStart(3, "0")}@${DEMO_EMAIL_DOMAIN}`);
  console.log(`Shared password: ${args.password}`);
  console.log(`Recommended demo login: ${primaryUser.email}`);
  console.log(`Seed coverage: sessions, meals, challenges, leaderboard, community feed, FitBot history, monster catches, and monster event sessions.`);
}

main().catch((error) => {
  console.error(error.message || error);
  process.exitCode = 1;
});
