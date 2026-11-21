
import { createClient } from "npm:@supabase/supabase-js@2";
import {
  createRemoteJWKSet,
  jwtVerify,
} from "npm:jose@5.9.6";

const MODEL = "gemini-embedding-2";
const DIMENSIONS = 768;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const jsonResponse = (
  body: Record<string, unknown>,
  status = 200,
) => {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
};

function getFirebaseProjectId(): string {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID");

  if (!projectId) {
    throw new Error("FIREBASE_PROJECT_ID is not configured");
  }

  return projectId;
}

async function verifyFirebaseToken(
  authorization: string | null,
): Promise<string> {
  if (!authorization?.startsWith("Bearer ")) {
    throw new Error("Missing Firebase authentication token");
  }

  const token = authorization.substring(7).trim();

  if (!token) {
    throw new Error("Empty authentication token");
  }

  const projectId = getFirebaseProjectId();

  const firebaseJwks = createRemoteJWKSet(
    new URL(
      "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
    ),
  );

  const { payload } = await jwtVerify(token, firebaseJwks, {
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId,
  });

  if (!payload.sub) {
    throw new Error("Invalid Firebase user identity");
  }

  return payload.sub;
}

async function generateQueryEmbedding(
  query: string,
): Promise<number[]> {
  const apiKey = Deno.env.get("GEMINI_API_KEY");

  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured");
  }

  // Gemini Embedding 2 retrieval-query format.
  const embeddingText =
    `task: search result | query: ${query}`;

  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:embedContent`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify({
        content: {
          parts: [{ text: embeddingText }],
        },
        output_dimensionality: DIMENSIONS,
      }),
    },
  );

  const responseText = await response.text();

  let result: Record<string, any>;

  try {
    result = JSON.parse(responseText);
  } catch {
    throw new Error("Gemini returned an invalid response");
  }

  if (!response.ok) {
    throw new Error(
      `Gemini API error ${response.status}: ${
        JSON.stringify(result)
      }`,
    );
  }

  const values = result.embedding?.values;

  if (
    !Array.isArray(values) ||
    values.length !== DIMENSIONS ||
    !values.every(
      (value: unknown) =>
        typeof value === "number" && Number.isFinite(value),
    )
  ) {
    throw new Error("Invalid Gemini embedding dimensions or values");
  }

  return values;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse(
      { success: false, error: "Method not allowed" },
      405,
    );
  }

  try {
    // Verify the Firebase user before processing the search.
    const userId = await verifyFirebaseToken(
      req.headers.get("Authorization"),
    );

    let body: Record<string, unknown>;

    try {
      body = await req.json();
    } catch {
      return jsonResponse(
        { success: false, error: "Invalid JSON request body" },
        400,
      );
    }

    const query =
      typeof body.query === "string" ? body.query.trim() : "";

    if (!query) {
      return jsonResponse(
        { success: false, error: "Search query is required" },
        400,
      );
    }

    if (query.length > 300) {
      return jsonResponse(
        {
          success: false,
          error: "Search query must be 300 characters or fewer",
        },
        400,
      );
    }

    const requestedCount = Number(body.matchCount ?? 10);
    const matchCount = Number.isFinite(requestedCount)
      ? Math.min(Math.max(Math.floor(requestedCount), 1), 30)
      : 10;

    // This is an initial threshold. We will tune it using real results.
    const requestedThreshold = Number(body.matchThreshold ?? 0.35);
    const matchThreshold = Number.isFinite(requestedThreshold)
      ? Math.min(Math.max(requestedThreshold, 0), 1)
      : 0.35;

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get(
      "SUPABASE_SERVICE_ROLE_KEY",
    );

    if (!supabaseUrl || !serviceRoleKey) {
      throw new Error("Supabase server configuration is missing");
    }

    // Service-role access stays inside this Edge Function.
    const supabase = createClient(
      supabaseUrl,
      serviceRoleKey,
      {
        auth: {
          persistSession: false,
          autoRefreshToken: false,
        },
      },
    );

    // Convert the user's natural-language query into a vector.
    const embedding = await generateQueryEmbedding(query);
    const vectorString = `[${embedding.join(",")}]`;

    // Search the existing song_embeddings table using pgvector RPC.
    const { data, error } = await supabase.rpc(
      "match_songs_by_embedding",
      {
        query_embedding: vectorString,
        match_threshold: matchThreshold,
        match_count: matchCount,
      },
    );

    if (error) {
      console.error("Semantic search RPC error:", error);

      return jsonResponse(
        {
          success: false,
          error: "Unable to search songs",
        },
        500,
      );
    }

    const results = data ?? [];

    return jsonResponse({
      success: true,
      query,
      userId,
      count: results.length,
      results,
    });
  } catch (error) {
    console.error("Semantic search error:", error);

    const message = error instanceof Error
      ? error.message
      : "Unexpected server error";

    const isAuthenticationError =
      message.includes("Firebase") ||
      message.includes("authentication token") ||
      message.includes("Invalid Compact JWS") ||
      message.includes("signature") ||
      message.includes("JWT");

    return jsonResponse(
      {
        success: false,
        error: isAuthenticationError
          ? "Authentication failed. Please sign in again."
          : "Semantic search failed. Please try again.",
      },
      isAuthenticationError ? 401 : 500,
    );
  }
});
