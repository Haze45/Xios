
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { createRemoteJWKSet, jwtVerify } from "https://esm.sh/jose@5";

const firebaseProjectId = Deno.env.get("FIREBASE_PROJECT_ID")!;
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const firebaseKeys = createRemoteJWKSet(
  new URL(
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
  )
);

const supabase = createClient(supabaseUrl, serviceRoleKey, {
  auth: {
    persistSession: false,
    autoRefreshToken: false,
  },
});

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  try {
    const authorization = req.headers.get("Authorization");
    const token = authorization?.replace(/^Bearer\s+/i, "");

    if (!token || token === authorization) {
      return new Response("Missing Firebase token", { status: 401 });
    }

    const { payload } = await jwtVerify(token, firebaseKeys, {
      algorithms: ["RS256"],
      issuer: `https://securetoken.google.com/${firebaseProjectId}`,
      audience: firebaseProjectId,
    });

    if (!payload.sub) {
      return new Response("Invalid Firebase user", { status: 401 });
    }

    const userId = payload.sub;
    const body = await req.json();
    const events = body.events;

    if (!Array.isArray(events) || events.length === 0 || events.length > 100) {
      return new Response("Invalid events batch", { status: 400 });
    }

    const rows = events.map((event: Record<string, unknown>) => {
      if (
        typeof event.session_id !== "string" ||
        typeof event.song_id !== "string" ||
        typeof event.title !== "string" ||
        typeof event.artist !== "string" ||
        typeof event.played_at_ms !== "number" ||
        typeof event.duration_ms !== "number" ||
        typeof event.listened_ms !== "number" ||
        typeof event.last_position_ms !== "number" ||
        typeof event.event_type !== "string"
      ) {
        throw new Error("Invalid listening history record");
      }

      if (
        !["PLAYING", "COMPLETED", "SKIPPED", "INTERRUPTED"].includes(
          event.event_type
        )
      ) {
        throw new Error("Invalid event type");
      }

      return {
        session_id: event.session_id,
        user_id: userId,
        song_id: event.song_id,
        title: event.title,
        artist: event.artist,
        album_art_url:
          typeof event.album_art_url === "string"
            ? event.album_art_url
            : "",
        played_at_ms: event.played_at_ms,
        duration_ms: Math.max(0, event.duration_ms),
        listened_ms: Math.max(0, event.listened_ms),
        last_position_ms: Math.max(0, event.last_position_ms),
        event_type: event.event_type,
      };
    });

    const { error } = await supabase
      .from("listening_history")
      .upsert(rows, { onConflict: "session_id,user_id" });

    if (error) {
      console.error("Listening history upsert failed:", error);
      return new Response("Could not save listening history", {
        status: 500,
      });
    }

    return Response.json({
      success: true,
      syncedCount: rows.length,
    });
  } catch (error) {
    console.error("Listening history sync error:", error);

    return new Response("Unauthorized or invalid request", {
      status: 401,
    });
  }
});
