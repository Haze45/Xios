
import { createClient } from "npm:@supabase/supabase-js@2";

const MODEL = "gemini-embedding-2";
const DIMENSIONS = 768;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type, x-embedding-admin-secret",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(
  body: Record<string, unknown>,
  status = 200
) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
    },
  });
}

type Song = {
  id: string;
  title: string;
  artist: string;
  album: string | null;
  genre: string | null;
};

async function generateEmbedding(text: string): Promise<number[]> {
  const apiKey = Deno.env.get("GEMINI_API_KEY");

  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured");
  }

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
          parts: [{ text }],
        },
        output_dimensionality: DIMENSIONS,
      }),
    }
  );

  const result = await response.json();

  if (!response.ok) {
    throw new Error(
      `Gemini API error ${response.status}: ${
        JSON.stringify(result)
      }`
    );
  }

  const values = result.embedding?.values;

  if (!Array.isArray(values) || values.length !== DIMENSIONS) {
    throw new Error(
      `Invalid Gemini embedding. Expected ${DIMENSIONS} dimensions.`
    );
  }

  return values;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  // This endpoint is for manual backend indexing only.
  // Never expose this secret in the Android application.
  const expectedSecret = Deno.env.get("EMBEDDING_ADMIN_SECRET");
  const suppliedSecret = req.headers.get("x-embedding-admin-secret");

  if (!expectedSecret || suppliedSecret !== expectedSecret) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !serviceRoleKey) {
    return jsonResponse({
      error: "Supabase environment variables are missing",
    }, 500);
  }

  const supabase = createClient(
    supabaseUrl,
    serviceRoleKey,
    {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    }
  );

  try {
    const body = await req.json();

    const batchSize = Math.min(
      Math.max(Number(body.batchSize ?? 5), 1),
      10
    );

    const afterId =
      typeof body.afterId === "string"
        ? body.afterId
        : null;

    const force = body.force === true;

    let songsQuery = supabase
      .from("songs")
      .select("id, title, artist, album, genre")
      .order("id", { ascending: true })
      .limit(batchSize);

    if (afterId) {
      songsQuery = songsQuery.gt("id", afterId);
    }

    const { data: songs, error: songsError } =
      await songsQuery;

    if (songsError) {
      throw songsError;
    }

    if (!songs || songs.length === 0) {
      return jsonResponse({
        success: true,
        message: "All songs have been processed.",
        processed: 0,
        skipped: 0,
        hasMore: false,
        nextAfterId: afterId,
      });
    }

    let processed = 0;
    let skipped = 0;
    let lastCompletedId = afterId;

    for (const song of songs as Song[]) {

      // Avoid regenerating existing embeddings unless force=true.
      if (!force) {
        const { data: existing, error: lookupError } =
          await supabase
            .from("song_embeddings")
            .select("song_id")
            .eq("song_id", song.id)
            .maybeSingle();

        if (lookupError) {
          throw lookupError;
        }

        if (existing) {
          skipped++;
          lastCompletedId = song.id;
          continue;
        }
      }

      // Gemini retrieval-document format.
      const embeddingText =
        `title: ${song.title} | text: ` +
        `Artist: ${song.artist}. ` +
        `Album: ${song.album || "Not specified"}. ` +
        `Genre: ${song.genre || "Not specified"}. ` +
        `Music track in a streaming music catalogue.`;

      try {
        const embedding = await generateEmbedding(
          embeddingText
        );

        const vectorString =
          `[${embedding.join(",")}]`;

        const { error: saveError } = await supabase
          .from("song_embeddings")
          .upsert({
            song_id: song.id,
            embedding_text: embeddingText,
            embedding: vectorString,
            model: MODEL,
            updated_at: new Date().toISOString(),
          });

        if (saveError) {
          throw saveError;
        }

        processed++;
        lastCompletedId = song.id;

      } catch (error) {
        return jsonResponse({
          success: false,
          error: error instanceof Error
            ? error.message
            : "Embedding generation failed",
          processed,
          skipped,
          failedSongId: song.id,
          nextAfterId: lastCompletedId,
          hasMore: true,
        }, 502);
      }
    }

    return jsonResponse({
      success: true,
      message: "Embedding batch completed.",
      processed,
      skipped,
      totalInBatch: songs.length,
      hasMore: songs.length === batchSize,
      nextAfterId: lastCompletedId,
    });

  } catch (error) {
    return jsonResponse({
      success: false,
      error: error instanceof Error
        ? error.message
        : "Unexpected server error",
    }, 500);
  }
});
