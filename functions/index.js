require("dotenv").config();
const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const OpenAI = require("openai").default;

admin.initializeApp();

/* ======================================================
   0. 공통 설정
   ====================================================== */

const REGION = "asia-northeast3";   // 필요하면 "asia-northeast3"로 통일해서 변경

/* ======================================================
   1. Email Verification
   ====================================================== */

const gmailUser = process.env.GMAIL_USER;
const gmailPass = process.env.GMAIL_PASS;

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: gmailUser,
    pass: gmailPass,
  },
});

/** 1-1. 인증코드 발송 */
exports.sendVerificationCode = onCall({ region: REGION }, async (req) => {
  const email = (req.data?.email || "").trim().toLowerCase();
  if (!email) throw new Error("Missing email");

  const code = String(Math.floor(100000 + Math.random() * 900000));
  const expiresAt = Date.now() + 10 * 60 * 1000; // 10분

  await admin.firestore()
    .collection("email_verifications")
    .doc(email)
    .set({ code, expiresAt });

  await transporter.sendMail({
    from: `"LendMark" <${gmailUser}>`,
    to: email,
    subject: "[LendMark] Email Authentication Code",
    html: `
      <div style="font-family:sans-serif;">
        <h2>Welcome to LendMark!</h2>
        <p>Please enter the authentication code below in the app:</p>
        <h1 style="letter-spacing:4px;">${code}</h1>
        <p>Valid time: 10 minutes</p>
      </div>
    `,
  });

  return { ok: true };
});

/** 1-2. 인증코드 검증 */
exports.verifyEmailCode = onCall({ region: REGION }, async (req) => {
  const email = (req.data?.email || "").trim().toLowerCase();
  const code = (req.data?.code || "").toString().trim();

  const snap = await admin.firestore()
    .collection("email_verifications")
    .doc(email)
    .get();

  if (!snap.exists) return { ok: false, reason: "NOT_FOUND" };

  const { code: saved, expiresAt } = snap.data();

  if (Date.now() > expiresAt) return { ok: false, reason: "EXPIRED" };
  if (saved !== code) return { ok: false, reason: "INVALID" };

  await snap.ref.delete();
  return { ok: true };
});

/* ======================================================
   2. 예약 상태 관리 (Scheduler)
   ====================================================== */

/** 2-1. 매 30분마다 지난 예약 finished 처리 */
exports.finishPastReservations = onSchedule(
  { schedule: "every 30 minutes", region: REGION },
  async () => {
    const db = admin.firestore();
    const now = new Date();

    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, "0");
    const dd = String(now.getDate()).padStart(2, "0");
    const todayStr = `${yyyy}-${mm}-${dd}`;

    const snapshot = await db
      .collection("reservations")
      .where("status", "==", "approved")
      .get();

    if (snapshot.empty) {
      logger.info("finishPastReservations: no approved reservations.");
      return null;
    }

    const batch = db.batch();

    snapshot.forEach((doc) => {
      const r = doc.data();
      const dateStr = r.date; // "YYYY-MM-DD"
      const periodEnd = r.periodEnd; // 예: 0,1,2...

      const endHour = 8 + (periodEnd + 1); // 8시 + (끝교시+1)
      const nowHour = now.getHours();

      if (dateStr < todayStr) {
        batch.update(doc.ref, { status: "finished" });
      } else if (dateStr === todayStr && nowHour >= endHour) {
        batch.update(doc.ref, { status: "finished" });
      }
    });

    await batch.commit();
    logger.info("finishPastReservations: done");
    return null;
  }
);

/** 2-2. 매일 00시, finished 후 1주 지난 예약 expired 처리 */
exports.expireOldReservations = onSchedule(
  { schedule: "every day 00:00", region: REGION },
  async () => {
    const db = admin.firestore();
    const now = Date.now();
    const oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000;

    const snapshot = await db
      .collection("reservations")
      .where("status", "==", "finished")
      .where("timestamp", "<", oneWeekAgo)
      .get();

    if (snapshot.empty) {
      logger.info("expireOldReservations: nothing to expire.");
      return null;
    }

    const batch = db.batch();
    snapshot.forEach((doc) => batch.update(doc.ref, { status: "expired" }));
    await batch.commit();

    logger.info(
      `expireOldReservations: expired ${snapshot.size} old reservations.`
    );
    return null;
  }
);

/* ======================================================
   3. 예약 생성 (충돌 검사 포함)
   ====================================================== */

exports.createReservation = onCall({ region: REGION }, async (req) => {
  const db = admin.firestore();

  const {
    userId,
    userName,
    major,
    people,
    purpose,
    buildingId,
    roomId,
    day,
    date,
    periodStart,
    periodEnd,
  } = req.data;

  // 3-1. 시간 충돌 체크
  const snap = await db
    .collection("reservations")
    .where("buildingId", "==", buildingId)
    .where("roomId", "==", roomId)
    .where("date", "==", date)
    .where("status", "==", "approved")
    .get();

  for (const doc of snap.docs) {
    const r = doc.data();
    const s = r.periodStart;
    const e = r.periodEnd;

    const overlapped = !(periodEnd < s || periodStart > e);
    if (overlapped) {
      return { success: false, reason: "TIME_CONFLICT" };
    }
  }

  // 3-2. 예약 저장
  const newReservation = {
    userId,
    userName,
    major,
    people,
    purpose,
    buildingId,
    roomId,
    day,
    date,
    periodStart,
    periodEnd,
    timestamp: Date.now(),
    status: "approved",
  };

  await db.collection("reservations").add(newReservation);
  return { success: true };
});

/* ======================================================
   4. AI Assistant – 사용 가능한 강의실 조회
   ====================================================== */

exports.chatbotAvailableRoomsV2 = onCall({ region: REGION }, async (req) => {
  try {
    // OpenAI API 불러오기
    const openai = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY || process.env.openai_key || process.env.openai?.key,
    });

    const { buildingId, buildingName, date, hour } = req.data;

    if (!buildingId) throw new Error("buildingId missing");
    if (!buildingName) throw new Error("buildingName missing");
    if (!date) throw new Error("date missing (YYYY-MM-DD)");
    if (hour === undefined) throw new Error("hour missing (0~23)");

    /* ============================================================
       ⭐ 0) 예약 가능 시간 범위 체크
       ============================================================ */
    if (hour < 8 || hour >= 18) {
      return {
        ok: true,
        answer: `지금 선택하신 ${hour}시는 예약이 불가능한 시간입니다.
강의실 예약은 매일 오전 08시부터 오후 18시까지만 가능합니다.`,
        rooms: [],
      };
    }

    const db = admin.firestore();

    /* ============================================================
       1) hour → 교시(period) 변환
       ============================================================ */
    function hourToPeriod(h) {
      return h - 8; // 08:00 = 0, 09:00 = 1 …
    }
    const targetPeriod = hourToPeriod(hour);

    /* ============================================================
       2) date → 요일 변환 (Sun, Mon, Tue …)
       ============================================================ */
    function getDayCode(dateStr) {
      const d = new Date(dateStr);
      return ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d.getDay()];
    }
    const targetDay = getDayCode(date);

    /* ============================================================
       3) building timetable 불러오기
       ============================================================ */
    const buildingDoc = await db.collection("buildings").doc(buildingId).get();

    if (!buildingDoc.exists) throw new Error("Building not found");

    const timetable = buildingDoc.get("timetable") ?? {};

    /* ============================================================
       4) 예약 데이터(period 기반)
       ============================================================ */
    const reservationSnap = await db
      .collection("reservations")
      .where("buildingId", "==", buildingId)
      .where("date", "==", date)
      .where("status", "==", "approved")
      .get();

    const reservationEvents = reservationSnap.docs.map((doc) => {
      const r = doc.data();
      return {
        roomId: r.roomId,
        periodStart: r.periodStart,
        periodEnd: r.periodEnd,
      };
    });

    /* ============================================================
       5) 사용 가능 여부 체크
       ============================================================ */
    function isRoomAvailable(roomId, roomData) {
      // (A) 정규수업
      if (roomData?.schedule) {
        for (const ev of roomData.schedule) {
          if (ev.day !== targetDay) continue;

          const s = ev.periodStart;
          const e = ev.periodEnd;

          const overlapped = !(targetPeriod < s || targetPeriod > e);
          if (overlapped) return false;
        }
      }

      // (B) 학생 예약
      for (const r of reservationEvents) {
        if (r.roomId !== roomId) continue;

        const s = r.periodStart;
        const e = r.periodEnd;

        const overlapped = !(targetPeriod < s || targetPeriod > e);
        if (overlapped) return false;
      }

      return true;
    }

    /* ============================================================
       6) 사용 가능한 강의실 목록 추출
       ============================================================ */
    const availableRooms = Object.entries(timetable)
      .filter(([roomId, roomData]) => isRoomAvailable(roomId, roomData))
      .map(([roomId]) => roomId);

    logger.info("Available rooms:", availableRooms);

    /* ============================================================
       7) 강의실 목록 예쁘게 포맷팅
       ============================================================ */
    const prettyList =
      availableRooms.length > 0
        ? availableRooms.map((r) => `- ${r}`).join("\n")
        : "없음";

    /* ============================================================
       8) OpenAI 자연어 응답 생성
       ============================================================ */
    const prompt = `
너는 서울과학기술대학교 강의실 예약 도우미 AI야.

- 날짜: ${date}
- 시간: ${hour}시
- 건물: ${buildingName}

해당 시간에 사용 가능한 강의실 목록은 다음과 같아:

${prettyList}

학생이 보기 쉽도록:
• 줄바꿈 유지
• 불렛포인트 형식 유지
• 자연스럽고 친절한 한 단락 설명

이 기준을 지켜서 답변해줘.
`;

    const completion = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [{ role: "user", content: prompt }],
    });

    const answer = completion.choices[0].message.content;

    return { ok: true, answer, rooms: availableRooms };
  } catch (err) {
    logger.error("chatbotAvailableRoomsV2 error:", err);
    return { ok: false, error: err.message };
  }
});
