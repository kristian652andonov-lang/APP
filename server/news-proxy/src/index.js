/**
 * Aura news proxy (Cloudflare Worker).
 *
 * Keeps the NewsData.io API key on the server so it never ships inside the Android app.
 * The app calls  GET {proxy}/api/1/latest?country=bg&category=top&size=10[&page=...]
 * and this worker forwards the request to NewsData.io with the secret key attached.
 *
 * Secrets:  npx wrangler secret put NEWSDATA_API_KEY
 */

const UPSTREAM = "https://newsdata.io/api/1/latest";
const ALLOWED_PARAMS = ["country", "category", "size", "page", "removeduplicate", "language"];
const CACHE_SECONDS = 600;

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method !== "GET") {
      return json({ status: "error", results: { message: "Method not allowed" } }, 405);
    }
    if (url.pathname !== "/api/1/latest") {
      return json({ status: "error", results: { message: "Not found" } }, 404);
    }
    if (!env.NEWSDATA_API_KEY) {
      return json({ status: "error", results: { message: "Proxy is not configured" } }, 500);
    }

    // Forward only known, validated parameters.
    const upstream = new URL(UPSTREAM);
    for (const key of ALLOWED_PARAMS) {
      const value = url.searchParams.get(key);
      if (value && /^[A-Za-z0-9_,-]{1,64}$/.test(value)) upstream.searchParams.set(key, value);
    }
    if (!upstream.searchParams.has("country")) {
      return json({ status: "error", results: { message: "country is required" } }, 400);
    }

    // Cache by the public request (without the key) to save API credits.
    const cacheKey = new Request(`${url.origin}${url.pathname}?${upstream.searchParams}`, { method: "GET" });
    const cache = caches.default;
    const cached = await cache.match(cacheKey);
    if (cached) return cached;

    upstream.searchParams.set("apikey", env.NEWSDATA_API_KEY);
    const upstreamResponse = await fetch(upstream.toString(), { headers: { Accept: "application/json" } });
    const response = new Response(upstreamResponse.body, {
      status: upstreamResponse.status,
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": `public, max-age=${CACHE_SECONDS}`,
      },
    });
    if (upstreamResponse.ok) ctx.waitUntil(cache.put(cacheKey, response.clone()));
    return response;
  },
};

function json(body, status) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}
